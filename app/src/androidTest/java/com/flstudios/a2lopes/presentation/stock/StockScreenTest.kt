package com.flstudios.a2lopes.presentation.stock

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.flstudios.a2lopes.domain.model.FlavorStock
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StockScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lowStockIsStrictlyBelowThresholdAndInactiveZeroIsVisible() {
        compose.setContent {
            MaterialTheme {
                StockScreen(StockUiState(loading = false, flavors = listOf(
                    FlavorStock(1, "Coco", true, 9), FlavorStock(2, "Morango", true, 10), FlavorStock(3, "Uva", false, 0),
                )), onRetry = {})
            }
        }
        compose.onNodeWithText("Total disponível: 19 unidades").assertIsDisplayed()
        compose.onNodeWithTag("stock-flavor-1").assert(hasAnyDescendant(hasText("Estoque baixo")))
        compose.onNodeWithTag("stock-flavor-2").assert(hasAnyDescendant(hasText("Estoque baixo")).not())
        compose.onNodeWithTag("stock-flavor-3").assert(hasAnyDescendant(hasText("Inativo")))
        compose.onNodeWithTag("stock-flavor-3").assert(hasAnyDescendant(hasText("0 unidades")))
        compose.onNodeWithTag("stock-flavor-3").assert(hasAnyDescendant(hasText("Estoque baixo")))
    }
}
