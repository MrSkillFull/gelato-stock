package com.flstudios.a2lopes.presentation

import android.content.Context
import android.content.Intent
import android.util.Log
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private data class AppRelease(val version: String, val notes: String, val apkUrl: String)

@Composable
fun UpdateChecker(context: Context, snackbarHostState: SnackbarHostState) {
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var downloading by remember { mutableStateOf(false) }
    val downloadScope = androidx.compose.runtime.rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        if (!context.applicationContext.let { app ->
                (app as com.flstudios.a2lopes.TwoLopesApplication).tryStartUpdateCheck()
            }) return@LaunchedEffect
        try {
            val latest = withContext(Dispatchers.IO) { fetchLatestRelease() }
            val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
            if (compareVersions(latest.version, installed) > 0) release = latest
            else snackbarHostState.showSnackbar(
                message = "Você já está usando a versão mais recente.",
                duration = SnackbarDuration.Short
            )
        } catch (exception: Exception) {
            Log.e("GelatoStockUpdate", "Falha ao consultar release do GitHub", exception)
            snackbarHostState.showSnackbar(
                message = "Não foi possível verificar atualizações agora.",
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
                val targetVersion = prefs.getString("awaiting_install_permission", null)
                if (targetVersion != null) {
                    prefs.edit().remove("awaiting_install_permission").apply()
                    if (Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()) {
                        installApk(context, apkFile(context, targetVersion))
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        try {
            kotlinx.coroutines.awaitCancellation()
        } finally {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    release?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!downloading) release = null },
            title = { Text("Atualização disponível: ${target.version}") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(target.notes.ifBlank { "Uma nova versão está disponível." }, Modifier.padding(bottom = 8.dp))
                    if (downloading) Text("Baixando atualização…")
                }
            },
            confirmButton = {
                Button(enabled = !downloading, onClick = {
                    val file = apkFile(context, target.version)
                    if (file.isFile && file.length() > 0L) {
                        beginInstall(context, file)
                    } else {
                        downloading = true
                        downloadScope.launch {
                            val success = withContext(Dispatchers.IO) { downloadApk(target.apkUrl, file) }
                            downloading = false
                            if (success) beginInstall(context, file)
                            if (!success) snackbarHostState.showSnackbar(
                                message = "Não foi possível baixar a atualização. Tente novamente.",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }) { Text(if (downloading) "Baixando…" else "Instalar agora") }
            },
            dismissButton = {
                TextButton(enabled = !downloading, onClick = { release = null }) { Text("Deixar para depois") }
            }
        )
    }
}

private fun fetchLatestRelease(): AppRelease {
    val connection = URL("https://api.github.com/repos/MrSkillFull/gelato-stock/releases/latest")
        .openConnection() as HttpURLConnection
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    connection.setRequestProperty("Accept", "application/vnd.github+json")
    connection.setRequestProperty("User-Agent", "GelatoStock-Android")
    try {
        check(connection.responseCode == HttpURLConnection.HTTP_OK)
        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val version = json.getString("tag_name").removePrefix("v").removePrefix("V")
        val assets = json.getJSONArray("assets")
        var downloadUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            if (asset.getString("name").matches(Regex("(?i)gelato-?stock-v\\d+\\.\\d+\\.\\d+\\.apk"))) {
                downloadUrl = asset.getString("browser_download_url")
                break
            }
        }
        return AppRelease(version, json.optString("body"), checkNotNull(downloadUrl) {
            "APK não encontrado nos assets da release."
        })
    } finally {
        connection.disconnect()
    }
}

private fun compareVersions(first: String, second: String): Int {
    val left = first.split('.').map { it.toIntOrNull() ?: 0 }
    val right = second.removePrefix("v").removePrefix("V").split('.').map { it.toIntOrNull() ?: 0 }
    for (index in 0 until maxOf(left.size, right.size)) {
        val difference = left.getOrElse(index) { 0 }.compareTo(right.getOrElse(index) { 0 })
        if (difference != 0) return difference
    }
    return 0
}

private fun apkFile(context: Context, version: String) =
    File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "gelatostock-v$version.apk")

private fun downloadApk(url: String, destination: File): Boolean {
    return try {
        destination.parentFile?.mkdirs()
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.setRequestProperty("User-Agent", "GelatoStock-Android")
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK)
            val temporary = File(destination.parentFile, destination.name + ".part")
            connection.inputStream.use { input -> temporary.outputStream().use(input::copyTo) }
            check(temporary.length() > 0L)
            if (destination.exists()) destination.delete()
            check(temporary.renameTo(destination))
            true
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        false
    }
}

private fun beginInstall(context: Context, file: File) {
    if (!file.isFile || file.length() == 0L) return
    if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
        context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit()
            .putString("awaiting_install_permission", file.nameWithoutExtension.removePrefix("gelatostock-v"))
            .apply()
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    }
    installApk(context, file)
}

private fun installApk(context: Context, file: File) {
    if (!file.isFile || file.length() == 0L) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    context.startActivity(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
