package com.flstudios.a2lopes.presentation.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Guarde uma cópia fora deste aparelho. Desinstalar o aplicativo ou perder o dispositivo pode apagar os dados locais.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Button(
                enabled = !state.busy && !state.selecting && state.pending == null,
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    if (viewModel.beginSelection()) try {
                        create.launch("2lopes-backup-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(Date())}.json")
                    } catch (_: Exception) { viewModel.selectionFailed() }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
            ) {
                Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportar backup", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                enabled = !state.busy && !state.selecting && state.pending == null,
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    if (viewModel.beginSelection()) try {
                        open.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                    } catch (_: Exception) { viewModel.selectionFailed() }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
            ) {
                Icon(imageVector = Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restaurar backup", style = MaterialTheme.typography.titleMedium)
            }

            Text(
                text = "Na troca de aparelho, pare os lançamentos no antigo, exporte e transfira o arquivo. Confira os dados restaurados antes de usar o reserva como principal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Processando. Aguarde…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

            state.pending?.summary?.let { data ->
                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Resumo do Arquivo Selecionado",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Backup de ${SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss", Locale.forLanguageTag("pt-BR")).format(Date(data.createdAt))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Limite de estoque baixo: ${data.lowStockThreshold} unidades",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${data.flavors} sabores · ${data.productions} produções · ${data.sales} vendas · ${data.corrections} perdas, ajustes e estornos · ${data.movements} movimentos · ${data.records} registros no total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    text = "Todos os dados atuais serão substituídos pelos dados deste arquivo. Exporte uma cópia atual antes de continuar.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Button(
                            onClick = viewModel::confirmRestore,
                            enabled = !state.busy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Text("Confirmar substituição completa")
                        }

                        TextButton(
                            onClick = viewModel::cancelRestore,
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar restauração")
                        }
                    }
                }
            }
        }
    }
}
