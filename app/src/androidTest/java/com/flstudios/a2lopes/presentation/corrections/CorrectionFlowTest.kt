package com.flstudios.a2lopes.presentation.corrections

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.data.repository.*
import com.flstudios.a2lopes.domain.model.*
import com.flstudios.a2lopes.presentation.TwoLopesApp
import com.flstudios.a2lopes.presentation.flavors.FlavorsViewModel
import com.flstudios.a2lopes.presentation.history.HistoryViewModel
import com.flstudios.a2lopes.presentation.production.ProductionViewModel
import com.flstudios.a2lopes.presentation.sales.SalesViewModel
import com.flstudios.a2lopes.presentation.stock.StockViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CorrectionFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var corrections: CorrectionViewModel
    private lateinit var history: HistoryViewModel
    private lateinit var stock: StockViewModel
    private var coco = 0L
    private var morango = 0L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        runBlocking {
            coco = db.flavorDao().insert(FlavorEntity(name = "Coco", defaultPriceCents = 550))
            morango = db.flavorDao().insert(FlavorEntity(name = "Morango"))
            RoomProductionRepository(db).record(ProductionRequest("production", 100, listOf(ProductionQuantity(coco, 10), ProductionQuantity(morango, 5))))
            RoomSaleRepository(db).record(SaleRequest("sale", 200, listOf(SaleQuantity(coco, 2, 550), SaleQuantity(morango, 1, null))))
        }
        compose.activityRule.scenario.onActivity { activity ->
            val flavorsRepository = RoomFlavorRepository(db)
            val flavors = ViewModelProvider(activity, FlavorsViewModel.Factory(flavorsRepository))[FlavorsViewModel::class.java]
            val production = ViewModelProvider(activity, ProductionViewModel.Factory(flavorsRepository, RoomProductionRepository(db)))[ProductionViewModel::class.java]
            stock = ViewModelProvider(activity, StockViewModel.Factory(RoomStockRepository(db)))[StockViewModel::class.java]
            val sales = ViewModelProvider(activity, SalesViewModel.Factory(flavorsRepository, RoomStockRepository(db), RoomSaleRepository(db)))[SalesViewModel::class.java]
            history = ViewModelProvider(activity, HistoryViewModel.Factory(RoomHistoryRepository(db)))[HistoryViewModel::class.java]
            corrections = ViewModelProvider(activity, CorrectionViewModel.Factory(RoomCorrectionRepository(db)))[CorrectionViewModel::class.java]
            activity.setContent { MaterialTheme { TwoLopesApp(flavors, production, stock, sales, history, corrections) } }
        }
        compose.waitUntil(5000) { !stock.state.value.loading && !history.state.value.loading }
    }
    @After fun tearDown() {
        compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        db.close()
    }
    private fun tab(title: String) {
        if (title in listOf("Sabores", "Histórico")) compose.onNodeWithTag("nav-Mais").performClick()
        compose.onNodeWithTag("nav-${title}").performClick()
    }
    private fun quantity(value: String) { compose.onNodeWithText("Quantidade contada").performTextInput(value) }
    private fun reason(value: String) { compose.onNodeWithText("Motivo obrigatório").performTextInput(value) }
    private fun detailAction(text: String) {
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).performClick()
    }
    private fun waitDetail() { compose.waitUntil(5000) { history.state.value.detail != null && !history.state.value.detailLoading } }

    @Test fun lossCountAndConfirmedSaleReversalUpdateStockAndHistory() {
        tab("Estoque")
        compose.onNodeWithTag("stock-loss-$coco").performClick()
        compose.onNodeWithText("Quantidade perdida").performTextInput("1")
        compose.onNodeWithText("Registrar").performClick()
        compose.onNodeWithText("Informe o motivo.").assertIsDisplayed()
        reason("Derreteu")
        compose.onNodeWithText("Registrar").performClick()
        compose.waitUntil(5000) { corrections.state.value.type == null && stock.state.value.flavors.first { it.flavorId == coco }.quantity == 7L }
        compose.onNodeWithTag("stock-count-$coco").performClick()
        quantity("0")
        reason("Conferência física")
        compose.onNodeWithText("Registrar").performClick()
        compose.waitUntil(5000) { corrections.state.value.type == null && stock.state.value.flavors.first { it.flavorId == coco }.quantity == 0L }
        tab("Histórico")
        compose.onNodeWithTag("history-filter-SALES").performClick()
        compose.waitUntil(5000) { !history.state.value.loading && history.state.value.filter == HistoryFilter.SALES }
        compose.onNodeWithTag("history-SALE:sale").performClick()
        waitDetail()
        detailAction("Estornar venda")
        assertEquals(0L, runBlocking { db.stockDao().quantity(coco) }) // Opening confirmation never writes.
        reason("Venda registrada por engano")
        compose.onNodeWithText("Confirmar estorno").performClick()
        compose.waitUntil(5000) { corrections.state.value.type == null && history.state.value.detail?.reversalId != null }
        assertEquals(2L, runBlocking { db.stockDao().quantity(coco) })
        assertEquals(5L, runBlocking { db.stockDao().quantity(morango) })
        compose.onNodeWithTag("history-detail").performScrollToNode(hasText("Venda estornada"))
        compose.onNodeWithText("Venda estornada").assertIsDisplayed()
        compose.onNodeWithText("Estornar venda").assertDoesNotExist()
        val reversalId = history.state.value.detail!!.reversalId!!
        detailAction("Consultar estorno #$reversalId")
        waitDetail()
        compose.onNodeWithText("Motivo: Venda registrada por engano").assertIsDisplayed()
        compose.onNodeWithText("Voltar ao histórico").performClick()
        compose.onNodeWithTag("history-filter-CORRECTIONS").performClick()
        compose.waitUntil(5000) { !history.state.value.loading && history.state.value.entries.size == 3 }
        assertEquals(setOf(CorrectionType.LOSS, CorrectionType.ADJUSTMENT, CorrectionType.SALE_REVERSAL), history.state.value.entries.map { it.correctionType }.toSet())
    }

    @Test fun productionWithoutEnoughStockReportsErrorAndLeavesEveryFlavorUntouched() {
        tab("Histórico")
        compose.onNodeWithTag("history-filter-PRODUCTIONS").performClick()
        compose.waitUntil(5000) { !history.state.value.loading && history.state.value.filter == HistoryFilter.PRODUCTIONS }
        compose.onNodeWithTag("history-PRODUCTION:production").performClick()
        waitDetail()
        detailAction("Estornar produção")
        reason("Registro duplicado")
        compose.onNodeWithText("Confirmar estorno").performClick()
        compose.waitUntil(5000) { !corrections.state.value.busy && corrections.state.value.error != null }
        compose.onNodeWithText("Estoque insuficiente para estornar toda a produção. Nenhum sabor foi alterado.").assertIsDisplayed()
        assertEquals(8L, runBlocking { db.stockDao().quantity(coco) })
        assertEquals(4L, runBlocking { db.stockDao().quantity(morango) })
        assertNull(runBlocking { db.correctionDao().reversal(null, "production") })
        compose.onNodeWithText("Cancelar").performClick()
    }
}
