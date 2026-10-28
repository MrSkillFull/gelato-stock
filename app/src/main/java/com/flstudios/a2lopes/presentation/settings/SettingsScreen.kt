package com.flstudios.a2lopes.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(state: SettingsUiState, onChange: (String) -> Unit, onSave: () -> Unit, onRetry: () -> Unit) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Configurações", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text("Alerta de estoque baixo", style = MaterialTheme.typography.titleLarge)
        Text("O alerta aparece quando o saldo é menor que o limite. Com limite zero, nenhum saldo é marcado como estoque baixo.")
        when {
            state.loading -> CircularProgressIndicator()
            state.loadError != null -> {
                Text(state.loadError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) { Text("Tentar novamente") }
            }
            else -> {
                Text("Limite salvo: ${state.saved} unidades")
                OutlinedTextField(value = state.input, onValueChange = onChange, enabled = !state.busy,
                    label = { Text("Limite de estoque baixo") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = state.error != null,
                    supportingText = { Text(state.error ?: "Informe um número inteiro igual ou maior que zero.") },
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = { focus.clearFocus(); onSave() }, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (state.busy) "Salvando…" else "Salvar limite") }
                state.message?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
