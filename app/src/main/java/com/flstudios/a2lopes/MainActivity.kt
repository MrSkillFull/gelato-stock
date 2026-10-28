package com.flstudios.a2lopes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.flstudios.a2lopes.presentation.TwoLopesApp
import com.flstudios.a2lopes.presentation.history.HistoryViewModel
import com.flstudios.a2lopes.presentation.corrections.CorrectionViewModel
import com.flstudios.a2lopes.presentation.production.ProductionViewModel
import com.flstudios.a2lopes.presentation.stock.StockViewModel
import com.flstudios.a2lopes.presentation.sales.SalesViewModel
import com.flstudios.a2lopes.presentation.flavors.FlavorsViewModel
import com.flstudios.a2lopes.ui.theme._2LopesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TwoLopesApplication).container
        val viewModel = ViewModelProvider(this, FlavorsViewModel.Factory(container.flavorRepository))[
            FlavorsViewModel::class.java,
        ]
        val production = ViewModelProvider(this, ProductionViewModel.Factory(container.flavorRepository, container.productionRepository))[
            ProductionViewModel::class.java,
        ]
        val stock = ViewModelProvider(this, StockViewModel.Factory(container.stockRepository, container.settingsRepository))[StockViewModel::class.java]
        val settings = ViewModelProvider(this, com.flstudios.a2lopes.presentation.settings.SettingsViewModel.Factory(container.settingsRepository))[com.flstudios.a2lopes.presentation.settings.SettingsViewModel::class.java]
        val sales = ViewModelProvider(this, SalesViewModel.Factory(container.flavorRepository, container.stockRepository, container.saleRepository))[
            SalesViewModel::class.java,
        ]
        val history = ViewModelProvider(this, HistoryViewModel.Factory(container.historyRepository))[HistoryViewModel::class.java]
        val corrections = ViewModelProvider(this, CorrectionViewModel.Factory(container.correctionRepository))[CorrectionViewModel::class.java]
        val backup = ViewModelProvider(this, com.flstudios.a2lopes.presentation.backup.BackupViewModel.Factory(container.backupRepository))[com.flstudios.a2lopes.presentation.backup.BackupViewModel::class.java]
        setContent {
            _2LopesTheme {
                TwoLopesApp(viewModel, production, stock, sales, history, corrections, backup, settings) {
                    // Discard drafts, detail caches and uncertain operation keys from the replaced dataset.
                    viewModelStore.clear()
                    recreate()
                    android.widget.Toast.makeText(this, "Backup restaurado com sucesso.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
