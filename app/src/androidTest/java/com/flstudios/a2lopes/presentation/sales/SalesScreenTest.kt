package com.flstudios.a2lopes.presentation.sales

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
import com.flstudios.a2lopes.presentation.production.ProductionViewModel
import com.flstudios.a2lopes.presentation.stock.StockViewModel
import com.flstudios.a2lopes.presentation.history.HistoryViewModel
import com.flstudios.a2lopes.presentation.corrections.CorrectionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SalesScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var sales: SalesViewModel
    private lateinit var stock: StockViewModel
    private var coco = 0L
    private var morango = 0L
    private var empty = 0L
    private var inactive = 0L

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        runBlocking {
            coco = db.flavorDao().insert(FlavorEntity(name = "Coco", defaultPriceCents = 550))
            morango = db.flavorDao().insert(FlavorEntity(name = "Morango"))
            empty = db.flavorDao().insert(FlavorEntity(name = "Baunilha"))
            inactive = db.flavorDao().insert(FlavorEntity(name = "Uva", active = false))
            RoomProductionRepository(db).record(ProductionRequest("seed", 100, listOf(ProductionQuantity(coco, 2), ProductionQuantity(morango, 3))))
        }
        compose.activityRule.scenario.onActivity(::showApp)
        compose.waitUntil(timeoutMillis = 5_000) { !sales.state.value.loading && !stock.state.value.loading }
    }

    private fun showApp(activity: ComponentActivity) {
        val flavorsRepository = RoomFlavorRepository(db)
        val flavors = ViewModelProvider(activity, FlavorsViewModel.Factory(flavorsRepository))[FlavorsViewModel::class.java]
        val production = ViewModelProvider(activity, ProductionViewModel.Factory(flavorsRepository, RoomProductionRepository(db)))[ProductionViewModel::class.java]
        stock = ViewModelProvider(activity, StockViewModel.Factory(RoomStockRepository(db)))[StockViewModel::class.java]
        sales = ViewModelProvider(activity, SalesViewModel.Factory(flavorsRepository, RoomStockRepository(db), RoomSaleRepository(db)))[SalesViewModel::class.java]
        val history = ViewModelProvider(activity, HistoryViewModel.Factory(RoomHistoryRepository(db)))[HistoryViewModel::class.java]
        val corrections = ViewModelProvider(activity, CorrectionViewModel.Factory(RoomCorrectionRepository(db)))[CorrectionViewModel::class.java]
        activity.setContent { MaterialTheme { TwoLopesApp(flavors, production, stock, sales, history, corrections) } }
    }
    @After fun tearDown() { compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }; db.close() }
    private fun tab(text: String) {
        if (text in listOf("Sabores", "Histórico")) compose.onNodeWithTag("nav-Mais").performClick()
        compose.onNodeWithTag("nav-${text}").performClick()
    }
    private fun saleCount() = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sales").use { it.moveToFirst(); it.getLong(0) }

    @Test fun gridQuantityButtonsAndMixedPricesRegisterImmediatelyAndRefreshStock() {
        tab("Vendas")
        compose.onNodeWithText("Registrar venda").assertIsNotEnabled()
        compose.onNodeWithTag("sale-product-$empty").assertDoesNotExist()
        compose.onNodeWithTag("sale-product-$inactive").assertDoesNotExist()
        compose.onNodeWithText("Preço não informado").assertIsDisplayed()
        compose.onNodeWithTag("sale-product-$coco").performClick()
        compose.onNodeWithContentDescription("Adicionar Coco").performClick()
        compose.onNodeWithContentDescription("Adicionar Coco").assertIsNotEnabled()
        compose.onNodeWithText("Valor total: R$ 11,00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Diminuir Coco").performClick()
        compose.onNodeWithContentDescription("Adicionar Morango").performClick()
        compose.onNodeWithText("Total: 2 unidades").assertIsDisplayed()
        compose.onNodeWithText("Valor total indisponível: há itens sem preço cadastrado.").assertIsDisplayed()
        compose.onNodeWithText("Valor total: R$ 5,50").assertDoesNotExist()
        assertEquals(0L, saleCount())
        compose.onNodeWithText("Registrar venda").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { sales.state.value.message != null && !sales.state.value.busy }
        compose.onNodeWithText("Venda registrada. Estoque atualizado.").assertIsDisplayed()
        compose.onNodeWithText("Confirmar venda").assertDoesNotExist()
        compose.onNodeWithText("Registrar venda").assertIsNotEnabled()
        assertTrue(sales.state.value.cart.isEmpty())
        assertEquals(1L, saleCount())
        assertEquals(1L, runBlocking { db.stockDao().quantity(coco) })
        assertEquals(2L, runBlocking { db.stockDao().quantity(morango) })
        tab("Estoque")
        compose.waitUntil(timeoutMillis = 5_000) { stock.state.value.total == "3" }
        compose.onNodeWithText("Total disponível: 3 unidades").assertIsDisplayed()
    }

    @Test fun activityRecreationKeepsCartAndDoesNotAutomaticallyRegister() {
        tab("Vendas")
        compose.onNodeWithContentDescription("Adicionar Coco").performClick()
        val previous = sales
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity(::showApp)
        compose.waitForIdle()
        assertSame(previous, sales)
        compose.onNodeWithText("Total: 1 unidades").assertIsDisplayed()
        assertEquals(0L, saleCount())
        compose.onNodeWithText("Registrar venda").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { sales.state.value.message != null && !sales.state.value.busy }
        assertEquals(1L, saleCount())
        assertEquals(1L, runBlocking { db.stockDao().quantity(coco) })
    }
}
