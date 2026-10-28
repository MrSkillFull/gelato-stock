package com.flstudios.a2lopes.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flstudios.a2lopes.presentation.flavors.*
import com.flstudios.a2lopes.presentation.production.*
import com.flstudios.a2lopes.presentation.stock.*
import com.flstudios.a2lopes.presentation.sales.*
import com.flstudios.a2lopes.presentation.history.*
import com.flstudios.a2lopes.presentation.corrections.*
import com.flstudios.a2lopes.domain.model.CorrectionType

@Composable
fun TwoLopesApp(flavors: FlavorsViewModel, production: ProductionViewModel, stock: StockViewModel, sales: SalesViewModel, history: HistoryViewModel, corrections: CorrectionViewModel,
    backup: com.flstudios.a2lopes.presentation.backup.BackupViewModel? = null,
    settings: com.flstudios.a2lopes.presentation.settings.SettingsViewModel? = null, onRestored: () -> Unit = {}) {
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
    BackHandler(enabled = selectedTab != 3) { if (!busy) selectedTab = 3 }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        correctionState.message?.let { message ->
            TextButton(onClick = corrections::clearMessage) { Text(message) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                when (selectedTab) {
                    0 -> FlavorsScreen(
                        state = flavorsState,
                        onNew = { flavors.openForm() }, onEdit = flavors::openForm,
                        onSetActive = flavors::setActive, onNameChange = flavors::changeName,
                        onPriceChange = flavors::changePrice, onSave = flavors::save,
                        onDismiss = flavors::closeForm, onRetry = flavors::reload,
                        onDelete = flavors::requestDeletion, onConfirmDelete = flavors::confirmDeletion,
                        onCancelDelete = flavors::cancelDeletion, onMessageShown = flavors::clearMessage,
                    )
                    1 -> ProductionScreen(
                        state = productionState, onQuantityChange = production::changeQuantity,
                        onNotesChange = production::changeNotes, onSelectDay = production::selectDay,
                        onReview = production::review, onBackToEdit = production::backToEdit,
                        onConfirm = production::confirm, onRetry = production::reload,
                    )
                    2 -> StockScreen(state = stockState, onRetry = stock::reload,
                        onLoss = { corrections.openStock(CorrectionType.LOSS, it) },
                        onCount = { corrections.openStock(CorrectionType.ADJUSTMENT, it) })
                    3 -> SalesScreen(state = salesState, onIncrement = sales::increment, onDecrement = sales::decrement,
                        onRegister = sales::register, onRetry = sales::reload)
                    4 -> HistoryScreen(state = historyState, onFilter = history::selectFilter, onRetry = history::reload,
                        onNext = history::nextPage, onPrevious = history::previousPage, onOpen = history::openDetail,
                        onClose = history::closeDetail, onRetryDetail = history::retryDetail, onReverse = corrections::openReversal)
                    5 -> backup?.let { com.flstudios.a2lopes.presentation.backup.BackupScreen(it, onRestored) }
                    6 -> settings?.let { vm -> settingsState?.let { state ->
                        com.flstudios.a2lopes.presentation.settings.SettingsScreen(state, vm::change, vm::save, vm::reload)
                    }
                    }
                }
            }
        }
        AppNavigation(selectedTab, !busy, backup != null, settings != null) { selectedTab = it }
    }
    CorrectionForm(correctionState, onQuantity = corrections::changeQuantity, onReason = corrections::changeReason,
        onConfirm = corrections::confirm, onDismiss = corrections::dismiss)
}
