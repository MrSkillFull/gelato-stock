package com.flstudios.a2lopes.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Chocolate & Cream Hero Header Banner for Backup Screen
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3E2723), // Cobertura de Chocolate Amargo
                                    Color(0xFF5D4037), // Chocolate ao Leite
                                    Color(0xFF8D6E63), // Creme Aveludado
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Segurança de Dados 💾🍦",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFE0B2) // Creme Suave
                        )
                        Text(
                            "Proteção dos seus dados",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFFF8E1) // Creme Alvo
                        )
                        Text(
                            "Gerencie cópias de segurança e restaure os dados do aplicativo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD180)
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Guarde uma cópia fora deste aparelho. Desinstalar o aplicativo ou perder o dispositivo pode apagar os dados locais.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(enabled = !state.busy && !state.selecting && state.pending == null, onClick = {
                if (viewModel.beginSelection()) try {
                    create.launch("2lopes-backup-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(Date())}.json")
                } catch (_: Exception) { viewModel.selectionFailed() }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("💾 Exportar backup", style = MaterialTheme.typography.titleMedium) }

            OutlinedButton(enabled = !state.busy && !state.selecting && state.pending == null, onClick = {
                if (viewModel.beginSelection()) try { open.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
                catch (_: Exception) { viewModel.selectionFailed() }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("📂 Restaurar backup", style = MaterialTheme.typography.titleMedium) }

            Text(
                "Na troca de aparelho, pare os lançamentos no antigo, exporte e transfira o arquivo. Confira os dados restaurados antes de usar o reserva como principal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.busy) { CircularProgressIndicator(); Text("Processando. Aguarde…") }
            state.message?.let { Text(it) }
            state.pending?.summary?.let { data ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Resumo do Arquivo Selecionado", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Backup de ${SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale.forLanguageTag("pt-BR")).format(Date(data.createdAt))}", style = MaterialTheme.typography.bodyMedium)
                        Text("Limite de estoque baixo: ${data.lowStockThreshold} unidades", style = MaterialTheme.typography.bodyMedium)
                        Text("${data.flavors} sabores · ${data.productions} produções · ${data.sales} vendas · ${data.corrections} perdas, ajustes e estornos · ${data.movements} movimentos · ${data.records} registros no total", style = MaterialTheme.typography.bodySmall)

                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text("⚠️ Todos os dados atuais serão substituídos pelos dados deste arquivo. Exporte uma cópia atual antes de continuar.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        Button(onClick = viewModel::confirmRestore, enabled = !state.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Confirmar substituição completa") }
                        TextButton(onClick = viewModel::cancelRestore, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Cancelar restauração") }
                    }
                }
            }
        }
    }
}
