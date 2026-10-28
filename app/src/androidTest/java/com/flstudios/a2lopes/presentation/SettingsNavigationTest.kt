package com.flstudios.a2lopes.presentation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flstudios.a2lopes.data.backup.RoomBackupRepository
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.data.repository.*
import com.flstudios.a2lopes.presentation.backup.BackupViewModel
import com.flstudios.a2lopes.presentation.corrections.CorrectionViewModel
import com.flstudios.a2lopes.presentation.flavors.FlavorsViewModel
import com.flstudios.a2lopes.presentation.history.HistoryViewModel
import com.flstudios.a2lopes.presentation.production.ProductionViewModel
import com.flstudios.a2lopes.presentation.sales.SalesViewModel
import com.flstudios.a2lopes.presentation.settings.SettingsViewModel
import com.flstudios.a2lopes.presentation.stock.StockViewModel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: AppDatabase
    private lateinit var settings: SettingsViewModel
    private lateinit var stock: StockViewModel
    private var largeFont by mutableStateOf(false)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        runBlocking { db.flavorDao().insert(FlavorEntity(1, "Coco")) }
        compose.activityRule.scenario.onActivity { activity ->
            val flavorsRepo = RoomFlavorRepository(db)
            val settingsRepo = RoomSettingsRepository(db)
            val flavors = ViewModelProvider(activity, FlavorsViewModel.Factory(flavorsRepo))[FlavorsViewModel::class.java]
            val production = ViewModelProvider(activity, ProductionViewModel.Factory(flavorsRepo, RoomProductionRepository(db)))[ProductionViewModel::class.java]
            val sales = ViewModelProvider(activity, SalesViewModel.Factory(flavorsRepo, RoomStockRepository(db), RoomSaleRepository(db)))[SalesViewModel::class.java]
            stock = ViewModelProvider(activity, StockViewModel.Factory(RoomStockRepository(db), settingsRepo))[StockViewModel::class.java]
            val history = ViewModelProvider(activity, HistoryViewModel.Factory(RoomHistoryRepository(db)))[HistoryViewModel::class.java]
            val corrections = ViewModelProvider(activity, CorrectionViewModel.Factory(RoomCorrectionRepository(db)))[CorrectionViewModel::class.java]
            val backup = ViewModelProvider(activity, BackupViewModel.Factory(RoomBackupRepository(db)))[BackupViewModel::class.java]
            settings = ViewModelProvider(activity, SettingsViewModel.Factory(settingsRepo))[SettingsViewModel::class.java]
            activity.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (largeFont) 2f else 1f)) {
                    MaterialTheme { TwoLopesApp(flavors, production, stock, sales, history, corrections, backup, settings) }
                }
            }
        }
        compose.waitUntil(5000) { !settings.state.value.loading && !stock.state.value.loading }
    }
    @After fun close() { compose.activityRule.scenario.onActivity { it.viewModelStore.clear() }; db.close() }
    private fun navigate(title: String) {
        if (title in listOf("Sabores", "Histórico", "Backup", "Configurações")) {
            compose.onNodeWithTag("nav-Mais").performClick()
            compose.onNodeWithTag("nav-$title").performScrollTo().performClick()
        } else compose.onNodeWithTag("nav-$title").performClick()
    }
    @Test fun everyScreenIsReachableWithoutHorizontalScrollingAndDraftSurvivesNavigation() {
        compose.onNodeWithTag("nav-Vendas").assertIsSelected()
        navigate("Produção")
        compose.onNodeWithText("Quantidade de Coco").performTextInput("7")
        navigate("Estoque")
        compose.onNodeWithText("Total disponível: 0 unidades").assertIsDisplayed()
        navigate("Sabores")
        compose.onNodeWithText("Novo sabor").assertIsDisplayed()
        navigate("Histórico")
        compose.onNodeWithText("Nenhum registro neste filtro.").assertIsDisplayed()
        navigate("Backup")
        compose.onNodeWithText("Exportar backup").assertIsDisplayed()
        navigate("Configurações")
        compose.onNodeWithText("Limite salvo: 10 unidades").assertIsDisplayed()
        navigate("Produção")
        compose.onNodeWithText("Quantidade de Coco").assertTextContains("7")
        assertEquals(0L, runBlocking { db.stockDao().quantity(1) })
    }
    @Test fun changingThresholdUpdatesStockAlertsAndZeroDisablesThem() {
        navigate("Estoque")
        compose.onNodeWithText("Estoque baixo", substring = false).assertIsDisplayed()
        navigate("Configurações")
        compose.onNodeWithText("Limite de estoque baixo").performTextReplacement("-1")
        compose.onNodeWithText("Salvar limite").performClick()
        compose.onNodeWithText("Informe um número inteiro igual ou maior que zero.").assertIsDisplayed()
        assertEquals(10L, settings.state.value.saved)
        compose.onNodeWithText("Limite de estoque baixo").performTextReplacement("0")
        compose.onNodeWithText("Salvar limite").performClick()
        compose.waitUntil(5000) { !settings.state.value.busy && stock.state.value.lowStockThreshold == 0L }
        navigate("Estoque")
        compose.onNodeWithText("Alertas de estoque baixo desativados (limite zero).").assertIsDisplayed()
        compose.onNodeWithText("Estoque baixo", substring = false).assertDoesNotExist()
        runBlocking { RoomSettingsRepository(db).setLowStockThreshold(1) }
        compose.waitUntil(5000) { stock.state.value.lowStockThreshold == 1L }
        compose.onNodeWithText("Estoque baixo", substring = false).assertIsDisplayed()
    }
    @Test fun enlargedFontKeepsNavigationSettingsAndSaleActionReachable() {
        compose.runOnIdle { largeFont = true }
        for (title in listOf("Vendas", "Produção", "Estoque", "Mais")) compose.onNodeWithTag("nav-$title").assertIsDisplayed()
        compose.onNodeWithText("Registrar venda").assertIsDisplayed()
        navigate("Configurações")
        compose.onNodeWithText("Salvar limite").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Limite de estoque baixo").performScrollTo().performTextReplacement("25")
        compose.onNodeWithText("Salvar limite").performScrollTo().performClick()
        compose.waitUntil(5000) { settings.state.value.saved == 25L && !settings.state.value.busy }
        assertEquals(25L, runBlocking { db.settingsDao().read() })
    }
}
