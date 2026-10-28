package com.flstudios.a2lopes.presentation

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
import com.flstudios.a2lopes.presentation.flavors.FlavorsViewModel
import com.flstudios.a2lopes.presentation.production.*
import com.flstudios.a2lopes.presentation.stock.StockViewModel
import com.flstudios.a2lopes.presentation.sales.SalesViewModel
import com.flstudios.a2lopes.presentation.history.HistoryViewModel
import com.flstudios.a2lopes.presentation.corrections.CorrectionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionStockScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var production: ProductionViewModel
    private lateinit var stock: StockViewModel
    private var coco = 0L
    private var morango = 0L
    private var empty = 0L
    private var inactive = 0L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        runBlocking {
            empty = db.flavorDao().insert(FlavorEntity(name = "Baunilha"))
            coco = db.flavorDao().insert(FlavorEntity(name = "Coco"))
            morango = db.flavorDao().insert(FlavorEntity(name = "Morango"))
            inactive = db.flavorDao().insert(FlavorEntity(name = "Uva", active = false))
        }
        compose.activityRule.scenario.onActivity(::showApp)
        compose.waitUntil(timeoutMillis = 5_000) { !production.state.value.loading && !stock.state.value.loading }
    }

    private fun showApp(activity: ComponentActivity) {
        val flavorsRepository = RoomFlavorRepository(db)
        val flavors = ViewModelProvider(activity, FlavorsViewModel.Factory(flavorsRepository))[FlavorsViewModel::class.java]
        production = ViewModelProvider(activity, ProductionViewModel.Factory(flavorsRepository, RoomProductionRepository(db)))[ProductionViewModel::class.java]
        stock = ViewModelProvider(activity, StockViewModel.Factory(RoomStockRepository(db)))[StockViewModel::class.java]
        val sales = ViewModelProvider(activity, SalesViewModel.Factory(flavorsRepository, RoomStockRepository(db), RoomSaleRepository(db)))[SalesViewModel::class.java]
        val history = ViewModelProvider(activity, HistoryViewModel.Factory(RoomHistoryRepository(db)))[HistoryViewModel::class.java]
        val corrections = ViewModelProvider(activity, CorrectionViewModel.Factory(RoomCorrectionRepository(db)))[CorrectionViewModel::class.java]
        activity.setContent { MaterialTheme { TwoLopesApp(flavors, production, stock, sales, history, corrections) } }
    }

    @After fun tearDown() {
        compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }
        db.close()
    }

    private fun tab(text: String) {
        if (text in listOf("Sabores", "Histórico")) compose.onNodeWithTag("nav-Mais").performClick()
        compose.onNodeWithTag("nav-${text}").performClick()
    }

    @Test fun historyTabOpensProductionAndKeepsStockUnchanged() {
        runBlocking {
            RoomProductionRepository(db) { 2000 }.record(com.flstudios.a2lopes.domain.model.ProductionRequest(
                "history-production", 1000, listOf(com.flstudios.a2lopes.domain.model.ProductionQuantity(coco, 12)), "Lote do histórico"))
        }
        tab("Histórico")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("history-PRODUCTION:history-production").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("history-PRODUCTION:history-production").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Coco: 12 unidades").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Coco: 12 unidades").assertIsDisplayed()
        compose.onNodeWithText("Data do registro:", substring = true).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::showApp)
        compose.onNodeWithText("Coco: 12 unidades").assertIsDisplayed()
        compose.onNodeWithText("Voltar ao histórico").performClick()
        compose.onNodeWithTag("history-filter-MOVEMENTS").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Coco: +12 unidades").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Origem: Produção #history-production").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Coco: 12 unidades").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Coco: 12 unidades").assertIsDisplayed()
        assertEquals(12L, runBlocking { db.stockDao().quantity(coco) })
    }

    private fun fill(label: String, value: String) {
        compose.onNodeWithTag("production-form").performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performTextInput(value)
    }

    @Test fun reviewsMultipleItemsWithoutWritingThenConfirmsAndUpdatesStock() {
        tab("Produção")
        compose.onNodeWithText("Quantidade de Uva").assertDoesNotExist()
        fill("Quantidade de Coco", "12")
        fill("Quantidade de Morango", "3")
        fill("Observações (opcional)", "Lote pronto")
        compose.onNodeWithText("Revisar produção").performClick()
        compose.onNodeWithText("Coco: 12 unidades").assertIsDisplayed()
        compose.onNodeWithText("Morango: 3 unidades").assertIsDisplayed()
        compose.onNodeWithText("Total: 15 unidades").assertIsDisplayed()
        compose.onNodeWithText("Observações: Lote pronto").assertIsDisplayed()
        compose.onNodeWithText("Data da produção:", substring = true).assertIsDisplayed()
        val request = production.state.value.review!!.request
        assertEquals(2, request.items.size)
        assertNull(runBlocking { db.productionDao().findSession(request.operationId) })
        compose.onNodeWithText("Voltar e editar").performClick()
        assertEquals("12", production.state.value.quantities[coco])
        compose.onNodeWithText("Revisar produção").performClick()
        compose.onNodeWithText("Confirmar produção").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { production.state.value.message != null && !production.state.value.busy }
        compose.onNodeWithText("Produção registrada. Estoque atualizado.").assertIsDisplayed()
        assertTrue(production.state.value.quantities.isEmpty())
        assertNotNull(runBlocking { db.productionDao().findSession(request.operationId) })
        tab("Estoque")
        compose.waitUntil(timeoutMillis = 5_000) { stock.state.value.total == "15" }
        compose.onNodeWithText("Total disponível: 15 unidades").assertIsDisplayed()
        compose.onNodeWithTag("stock-list").performScrollToNode(hasTestTag("stock-flavor-$coco"))
        compose.onNodeWithTag("stock-flavor-$coco").assert(hasAnyDescendant(hasText("12 unidades")))
        compose.onNodeWithTag("stock-flavor-$coco").assert(hasAnyDescendant(hasText("Estoque baixo")).not())
        compose.onNodeWithTag("stock-list").performScrollToNode(hasTestTag("stock-flavor-$morango"))
        compose.onNodeWithTag("stock-flavor-$morango").assert(hasAnyDescendant(hasText("Estoque baixo")))
        compose.onNodeWithTag("stock-list").performScrollToNode(hasTestTag("stock-flavor-$inactive"))
        compose.onNodeWithTag("stock-flavor-$inactive").assert(hasAnyDescendant(hasText("Inativo")))
        compose.onNodeWithTag("stock-flavor-$inactive").assert(hasAnyDescendant(hasText("0 unidades")))
        compose.onNodeWithTag("stock-list").performScrollToNode(hasTestTag("stock-flavor-$empty"))
        compose.onNodeWithTag("stock-flavor-$empty").assert(hasAnyDescendant(hasText("0 unidades")))
        tab("Sabores")
        compose.onNodeWithText("Novo sabor").assertIsDisplayed()
    }

    @Test fun rejectsEmptyAndInvalidQuantitiesBeforeReview() {
        tab("Produção")
        compose.onNodeWithText("Revisar produção").performClick()
        compose.onNodeWithText("Informe a quantidade de pelo menos um sabor.").assertIsDisplayed()
        fill("Quantidade de Coco", "-2")
        compose.onNodeWithText("Revisar produção").performClick()
        compose.onNodeWithText("Confira as quantidades informadas.").assertIsDisplayed()
        assertNull(production.state.value.review)
        assertEquals(0L, runBlocking { db.stockDao().quantity(coco) })
    }

    @Test fun activityRecreationPreservesDraftReviewAndDoesNotSubmit() {
        tab("Produção")
        fill("Quantidade de Coco", "6")
        compose.onNodeWithText("Revisar produção").performClick()
        val previous = production
        val request = production.state.value.review!!.request
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::showApp)
        compose.waitForIdle()
        assertSame(previous, production)
        assertEquals(request, production.state.value.review!!.request)
        compose.onNodeWithText("Total: 6 unidades").assertIsDisplayed()
        assertNull(runBlocking { db.productionDao().findSession(request.operationId) })
        compose.onNodeWithText("Confirmar produção").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !production.state.value.busy && production.state.value.review == null }
        assertEquals(6L, runBlocking { db.stockDao().quantity(coco) })
    }
}
