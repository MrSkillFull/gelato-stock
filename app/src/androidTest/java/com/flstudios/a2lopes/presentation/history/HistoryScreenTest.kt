package com.flstudios.a2lopes.presentation.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flstudios.a2lopes.domain.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyFiltersAndPageControlsArePresentedWithoutWriteActions() {
        var filter: HistoryFilter? = null
        compose.setContent { MaterialTheme {
            HistoryScreen(HistoryUiState(loading = false), onFilter = { filter = it }, onRetry = {},
                onNext = {}, onPrevious = {}, onOpen = { _, _ -> }, onClose = {}, onRetryDetail = {})
        } }
        compose.onNodeWithText("Nenhum registro neste filtro.").assertIsDisplayed()
        compose.onNodeWithText("Anteriores").assertIsNotEnabled()
        compose.onNodeWithText("Próximos").assertIsNotEnabled()
        HistoryFilter.entries.forEach { expected ->
            compose.onNodeWithTag("history-filter-${expected.name}").performClick()
            compose.runOnIdle { assertEquals(expected, filter) }
        }
        compose.onNodeWithText("Editar").assertDoesNotExist()
        compose.onNodeWithText("Excluir").assertDoesNotExist()
    }

    @Test fun productionCanBeOpenedAndDatesAreClearlyDistinguished() {
        var state by mutableStateOf(HistoryUiState(loading = false, entries = listOf(
            HistoryEntry("p", HistoryKind.PRODUCTION, 0, "15", 2))))
        compose.setContent { MaterialTheme {
            HistoryScreen(state, onFilter = {}, onRetry = {}, onNext = {}, onPrevious = {},
                onOpen = { kind, id -> state = state.copy(selectedId = id, selectedKind = kind,
                    detail = HistoryDetail(id, kind, 0, 86_400_000, "Lote pronto", listOf(
                        HistoryItem(1, "Coco", 10, null), HistoryItem(2, "Morango", 5, null)))) },
                onClose = { state = state.copy(selectedId = null, detail = null) }, onRetryDetail = {})
        } }
        compose.onNodeWithTag("history-PRODUCTION:p").performClick()
        compose.onNodeWithText("Data da produção:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Data do registro:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Coco: 10 unidades").assertIsDisplayed()
        compose.onNodeWithText("Morango: 5 unidades").assertIsDisplayed()
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText("Observações: Lote pronto"))
        compose.onNodeWithText("Observações: Lote pronto").assertIsDisplayed()
        compose.onNodeWithText("Voltar ao histórico").performClick()
        compose.onNodeWithTag("history-PRODUCTION:p").assertIsDisplayed()
    }

    @Test fun saleShowsHistoricalPricesAndUnavailableFinancialTotal() {
        compose.setContent { MaterialTheme {
            HistoryScreen(HistoryUiState(loading = false, selectedId = "s", selectedKind = HistoryKind.SALE,
                detail = HistoryDetail("s", HistoryKind.SALE, 0, 1, null,
                    listOf(HistoryItem(1, "Coco", 2, 550), HistoryItem(2, "Morango", 1, null)))),
                onFilter = {}, onRetry = {}, onNext = {}, onPrevious = {}, onOpen = { _, _ -> }, onClose = {}, onRetryDetail = {})
        } }
        compose.onNodeWithText("Preço unitário histórico: R$ 5,50").assertIsDisplayed()
        compose.onNodeWithText("Preço histórico não informado").assertIsDisplayed()
        val message = "Total financeiro não disponível: há item sem preço histórico."
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText(message))
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Total: 3 unidades").assertIsDisplayed()
    }

    @Test fun movementShowsSignedQuantityAndOpensItsOrigin() {
        var opened: Pair<HistoryKind, String>? = null
        compose.setContent { MaterialTheme {
            HistoryScreen(HistoryUiState(loading = false, filter = HistoryFilter.MOVEMENTS, entries = listOf(
                HistoryEntry("7", HistoryKind.MOVEMENT, 0, "-2", flavorName = "Coco", movementType = "SALE",
                    originId = "s1", originKind = HistoryKind.SALE))), onFilter = {}, onRetry = {},
                onNext = {}, onPrevious = {}, onOpen = { kind, id -> opened = kind to id }, onClose = {}, onRetryDetail = {})
        } }
        compose.onNodeWithText("Saída por venda").assertIsDisplayed()
        compose.onNodeWithText("Coco: -2 unidades").assertIsDisplayed()
        compose.onNodeWithText("Origem: Venda #s1").performClick()
        compose.runOnIdle { assertEquals(HistoryKind.SALE to "s1", opened) }
    }

    @Test fun readErrorsProvideRetry() {
        var retries = 0
        compose.setContent { MaterialTheme {
            HistoryScreen(HistoryUiState(loading = false, error = "Falha de leitura"), onFilter = {}, onRetry = { retries++ },
                onNext = {}, onPrevious = {}, onOpen = { _, _ -> }, onClose = {}, onRetryDetail = {})
        } }
        compose.onNodeWithText("Falha de leitura").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
