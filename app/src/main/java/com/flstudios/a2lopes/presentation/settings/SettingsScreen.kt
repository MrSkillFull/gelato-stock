package com.flstudios.a2lopes.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(state: SettingsUiState, onChange: (String) -> Unit, onSave: () -> Unit, onRetry: () -> Unit) {
    val focus = LocalFocusManager.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Chocolate & Cream Hero Header Banner for Settings Screen
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
                            "Configurações do App ⚙️🍦",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFE0B2) // Creme Suave
                        )
                        Text(
                            "Preferências do aplicativo",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFFFFF8E1) // Creme Alvo
                        )
                        Text(
                            "Personalize o aplicativo de acordo com suas preferências",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD180)
                        )
                    }
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Alerta de estoque baixo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("O alerta aparece quando o saldo é menor que o limite. Com limite zero, nenhum saldo é marcado como estoque baixo.", style = MaterialTheme.typography.bodyMedium)

                    when {
                        state.loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        state.loadError != null -> Column {
                            Text(state.loadError, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Tentar novamente") }
                        }
                        else -> {
                            Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("Limite atual salvo: ${state.saved} unidades", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            OutlinedTextField(
                                value = state.input,
                                onValueChange = onChange,
                                enabled = !state.busy,
                                label = { Text("Limite de estoque baixo") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = state.error != null,
                                supportingText = { Text(state.error ?: "Informe um número inteiro igual ou maior que zero.") },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { focus.clearFocus(); onSave() },
                                enabled = !state.busy,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) { Text(if (state.busy) "Salvando…" else "Salvar limite", style = MaterialTheme.typography.titleMedium) }

                            state.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.primary) }
                            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}
