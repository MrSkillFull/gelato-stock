package com.flstudios.a2lopes.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    selected: Int,
    enabled: Boolean,
    hasBackup: Boolean,
    hasSettings: Boolean,
    onSelect: (Int) -> Unit
) {
    var more by rememberSaveable { mutableStateOf(false) }
    val extraHeight = 32 * (LocalDensity.current.fontScale - 1f).coerceAtLeast(0f)

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = (80 + extraHeight).dp)
            .testTag("app-navigation"),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        val navItems = listOf(
            NavigationDestination(3, "Vendas", Icons.Outlined.PointOfSale),
            NavigationDestination(1, "Produção", Icons.Outlined.PrecisionManufacturing),
            NavigationDestination(2, "Estoque", Icons.Outlined.Inventory2),
            NavigationDestination(-1, "Mais", Icons.Outlined.MoreHoriz)
        )

        navItems.forEach { (index, title, icon) ->
            val isSelected = if (index == -1) selected !in listOf(1, 2, 3) else selected == index
            NavigationBarItem(
                selected = isSelected,
                enabled = enabled,
                onClick = {
                    if (index == -1) more = true else onSelect(index)
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav-$title")
            )
        }
    }

    if (more) {
        ModalBottomSheet(
            onDismissRequest = { more = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Mais opções",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Catálogo, registros e configurações do sistema",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                val entries = listOf(
                    MenuEntry(0, "Sabores", Icons.Outlined.Icecream),
                    MenuEntry(4, "Histórico", Icons.Outlined.History)
                ) + (if (hasBackup) listOf(MenuEntry(5, "Backup", Icons.Outlined.Backup)) else emptyList()) +
                    (if (hasSettings) listOf(MenuEntry(6, "Configurações", Icons.Outlined.Settings)) else emptyList())

                entries.forEach { (index, title, icon) ->
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        selected = selected == index,
                        onClick = {
                            if (enabled) {
                                onSelect(index)
                                more = false
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .testTag("nav-$title")
                            .heightIn(min = 52.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private data class NavigationDestination(
    val index: Int,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class MenuEntry(
    val index: Int,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
