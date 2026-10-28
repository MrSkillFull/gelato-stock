package com.flstudios.a2lopes.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupScreen(viewModel: BackupViewModel, onRestored: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.applicationContext.contentResolver
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) viewModel.cancelSelection() else viewModel.export { resolver.openOutputStream(uri, "wt") }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) viewModel.cancelSelection() else viewModel.inspect { resolver.openInputStream(uri) }
    }
    LaunchedEffect(state.restored, state.busy) { if (state.restored && !state.busy) onRestored() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Backup e restauração", style = MaterialTheme.typography.headlineMedium)
        Text("Guarde uma cópia fora deste aparelho. Desinstalar o aplicativo ou perder o dispositivo pode apagar os dados locais.")
        Button(enabled = !state.busy && !state.selecting && state.pending == null, onClick = {
            if (viewModel.beginSelection()) try {
                create.launch("2lopes-backup-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(Date())}.json")
            } catch (_: Exception) { viewModel.selectionFailed() }
        }, modifier = Modifier.fillMaxWidth()) { Text("Exportar backup") }
        OutlinedButton(enabled = !state.busy && !state.selecting && state.pending == null, onClick = {
            if (viewModel.beginSelection()) try { open.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
            catch (_: Exception) { viewModel.selectionFailed() }
        }, modifier = Modifier.fillMaxWidth()) { Text("Restaurar backup") }
        Text("Na troca de aparelho, pare os lançamentos no antigo, exporte e transfira o arquivo. Confira os dados restaurados antes de usar o reserva como principal.")
        if (state.busy) { CircularProgressIndicator(); Text("Processando. Aguarde…") }
        state.message?.let { Text(it) }
        state.pending?.summary?.let { data ->
            Text("Backup de ${SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale.forLanguageTag("pt-BR")).format(Date(data.createdAt))}")
            Text("Limite de estoque baixo: ${data.lowStockThreshold} unidades")
            Text("${data.flavors} sabores · ${data.productions} produções · ${data.sales} vendas · ${data.corrections} perdas, ajustes e estornos · ${data.movements} movimentos · ${data.records} registros no total")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("Todos os dados atuais serão substituídos pelos dados deste arquivo. Exporte uma cópia atual antes de continuar.", Modifier.padding(16.dp))
            }
            Button(onClick = viewModel::confirmRestore, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Confirmar substituição completa") }
            TextButton(onClick = viewModel::cancelRestore, enabled = !state.busy) { Text("Cancelar restauração") }
        }
    }
}
