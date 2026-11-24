package com.flstudios.a2lopes.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flstudios.a2lopes.domain.model.CorrectionType
import com.flstudios.a2lopes.presentation.backup.BackupScreen
import com.flstudios.a2lopes.presentation.backup.BackupViewModel
import com.flstudios.a2lopes.presentation.corrections.CorrectionForm
import com.flstudios.a2lopes.presentation.corrections.CorrectionViewModel
import com.flstudios.a2lopes.presentation.flavors.FlavorsScreen
import com.flstudios.a2lopes.presentation.flavors.FlavorsViewModel
import com.flstudios.a2lopes.presentation.history.HistoryScreen
import com.flstudios.a2lopes.presentation.history.HistoryViewModel
import com.flstudios.a2lopes.presentation.production.ProductionScreen
import com.flstudios.a2lopes.presentation.production.ProductionViewModel
import com.flstudios.a2lopes.presentation.sales.SalesScreen
import com.flstudios.a2lopes.presentation.sales.SalesViewModel
import com.flstudios.a2lopes.presentation.settings.SettingsScreen
import com.flstudios.a2lopes.presentation.settings.SettingsViewModel
import com.flstudios.a2lopes.presentation.stock.StockScreen
import com.flstudios.a2lopes.presentation.stock.StockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoLopesApp(
    flavors: FlavorsViewModel,
    production: ProductionViewModel,
    stock: StockViewModel,
    sales: SalesViewModel,
    history: HistoryViewModel,
    corrections: CorrectionViewModel,
    backup: BackupViewModel? = null,
    settings: SettingsViewModel? = null,
    snackbarHostState: SnackbarHostState,
    onRestored: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(3) }
    val productionState by production.state.collectAsStateWithLifecycle()
    val stockState by stock.state.collectAsStateWithLifecycle()
    val flavorsState by flavors.state.collectAsStateWithLifecycle()
    val salesState by sales.state.collectAsStateWithLifecycle()
    val historyState by history.state.collectAsStateWithLifecycle()
    val correctionState by corrections.state.collectAsStateWithLifecycle()
    val backupState = backup?.state?.collectAsStateWithLifecycle()?.value
    val settingsState = settings?.state?.collectAsStateWithLifecycle()?.value

    val busy = flavorsState.busy || productionState.busy || salesState.busy || correctionState.busy ||
        backupState?.let { it.busy || it.selecting || it.pending != null } == true || settingsState?.busy == true

    BackHandler(enabled = selectedTab != 3) {
        if (!busy) selectedTab = 3
    }

    val tabTitle = when (selectedTab) {
        0 -> "Catálogo de Sabores"
        1 -> "Registro de Produção"
        2 -> "Controle de Estoque"
        3 -> "Registro de Vendas"
        4 -> "Histórico e Movimentações"
        5 -> "Segurança de Dados & Backup"
        6 -> "Configurações Gerais"
        else -> "GelatoStock"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "GelatoStock",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = tabTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
        },
        bottomBar = {
            AppNavigation(
                selected = selectedTab,
                enabled = !busy,
                hasBackup = backup != null,
                hasSettings = settings != null,
                onSelect = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = correctionState.message != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                correctionState.message?.let { message ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            IconButton(
                                onClick = corrections::clearMessage,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Fechar mensagem",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxSize()
                ) {
                    when (selectedTab) {
                        0 -> FlavorsScreen(
                            state = flavorsState,
                            onNew = { flavors.openForm() },
                            onEdit = flavors::openForm,
                            onSetActive = flavors::setActive,
                            onNameChange = flavors::changeName,
                            onPriceChange = flavors::changePrice,
                            onSave = flavors::save,
                            onDismiss = flavors::closeForm,
                            onRetry = flavors::reload,
                            onDelete = flavors::requestDeletion,
                            onConfirmDelete = flavors::confirmDeletion,
                            onCancelDelete = flavors::cancelDeletion,
                            onMessageShown = flavors::clearMessage,
                        )
                        1 -> ProductionScreen(
                            state = productionState,
                            onQuantityChange = production::changeQuantity,
                            onNotesChange = production::changeNotes,
                            onSelectDay = production::selectDay,
                            onReview = production::review,
                            onBackToEdit = production::backToEdit,
                            onConfirm = production::confirm,
                            onRetry = production::reload,
                        )
                        2 -> StockScreen(
                            state = stockState,
                            onRetry = stock::reload,
                            onLoss = { corrections.openStock(CorrectionType.LOSS, it) },
                            onCount = { corrections.openStock(CorrectionType.ADJUSTMENT, it) }
                        )
                        3 -> SalesScreen(
                            state = salesState,
                            onIncrement = sales::increment,
                            onDecrement = sales::decrement,
                            onRegister = sales::register,
                            onRetry = sales::reload
                        )
                        4 -> HistoryScreen(
                            state = historyState,
                            onFilter = history::selectFilter,
                            onRetry = history::reload,
                            onNext = history::nextPage,
                            onPrevious = history::previousPage,
                            onOpen = history::openDetail,
                            onClose = history::closeDetail,
                            onRetryDetail = history::retryDetail,
                            onReverse = corrections::openReversal
                        )
                        5 -> backup?.let { BackupScreen(it, onRestored) }
                        6 -> settings?.let { vm ->
                            settingsState?.let { state ->
                                SettingsScreen(state, vm::change, vm::save, vm::reload)
                            }
                        }
                    }
                }
            }
        }
    }

    CorrectionForm(
        correctionState,
        onQuantity = corrections::changeQuantity,
        onReason = corrections::changeReason,
        onConfirm = corrections::confirm,
        onDismiss = corrections::dismiss
    )
}
