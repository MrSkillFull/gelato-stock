package com.flstudios.a2lopes.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(selected: Int, enabled: Boolean, hasBackup: Boolean, hasSettings: Boolean, onSelect: (Int) -> Unit) {
    var more by rememberSaveable { mutableStateOf(false) }
    val extraHeight = 32 * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)
    NavigationBar(Modifier.fillMaxWidth().heightIn(min = (80 + extraHeight).dp).testTag("app-navigation"), windowInsets = WindowInsets(0, 0, 0, 0)) {
        listOf(Triple(3, "Vendas", android.R.drawable.ic_menu_send),
            Triple(1, "Produção", android.R.drawable.ic_menu_add),
            Triple(2, "Estoque", android.R.drawable.ic_menu_agenda),
            Triple(-1, "Mais", android.R.drawable.ic_menu_more)).forEach { (index, title, icon) ->
            NavigationBarItem(selected = if (index == -1) selected !in listOf(1, 2, 3) else selected == index,
                enabled = enabled, onClick = { if (index == -1) more = true else onSelect(index) },
                icon = { Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                label = { Text(title) }, modifier = Modifier.testTag("nav-$title"))
        }
    }
    if (more) ModalBottomSheet(onDismissRequest = { more = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mais opções", style = MaterialTheme.typography.headlineSmall)
            val entries = listOf(0 to "Sabores", 4 to "Histórico") +
                (if (hasBackup) listOf(5 to "Backup") else emptyList()) +
                (if (hasSettings) listOf(6 to "Configurações") else emptyList())
            entries.forEach { (index, title) ->
                    NavigationDrawerItem(label = { Text(title) }, selected = selected == index,
                        onClick = { if (enabled) { onSelect(index); more = false } },
                        modifier = Modifier.testTag("nav-$title").heightIn(min = 56.dp))
            }
        }
    }
}
