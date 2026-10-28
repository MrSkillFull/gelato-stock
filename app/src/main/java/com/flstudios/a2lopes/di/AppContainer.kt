package com.flstudios.a2lopes.di

import android.content.Context
import androidx.room.Room
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.MIGRATION_1_2
import com.flstudios.a2lopes.data.local.MIGRATION_2_3
import com.flstudios.a2lopes.data.local.MIGRATION_3_4
import com.flstudios.a2lopes.data.local.MIGRATION_4_5
import com.flstudios.a2lopes.domain.repository.CorrectionRepository
import com.flstudios.a2lopes.data.repository.RoomCorrectionRepository
import com.flstudios.a2lopes.data.repository.RoomSaleRepository
import com.flstudios.a2lopes.domain.repository.SaleRepository
import com.flstudios.a2lopes.data.repository.RoomProductionRepository
import com.flstudios.a2lopes.data.repository.RoomStockRepository
import com.flstudios.a2lopes.domain.repository.ProductionRepository
import com.flstudios.a2lopes.domain.repository.StockRepository
import com.flstudios.a2lopes.data.local.dao.FlavorDao
import com.flstudios.a2lopes.data.repository.RoomFlavorRepository
import com.flstudios.a2lopes.domain.repository.FlavorRepository
import com.flstudios.a2lopes.domain.repository.HistoryRepository
import com.flstudios.a2lopes.data.repository.RoomHistoryRepository

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "2lopes.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, com.flstudios.a2lopes.data.local.MIGRATION_5_6)
            .build()
    }

    val flavorDao: FlavorDao by lazy { database.flavorDao() }
    val settingsRepository: com.flstudios.a2lopes.domain.repository.SettingsRepository by lazy { com.flstudios.a2lopes.data.repository.RoomSettingsRepository(database) }
    val flavorRepository: FlavorRepository by lazy { RoomFlavorRepository(database) }
    val productionRepository: ProductionRepository by lazy { RoomProductionRepository(database) }
    val stockRepository: StockRepository by lazy { RoomStockRepository(database) }
    val saleRepository: SaleRepository by lazy { RoomSaleRepository(database) }
    val historyRepository: HistoryRepository by lazy { RoomHistoryRepository(database) }
    val correctionRepository: CorrectionRepository by lazy { RoomCorrectionRepository(database) }
    val backupRepository: com.flstudios.a2lopes.domain.repository.BackupRepository by lazy { com.flstudios.a2lopes.data.backup.RoomBackupRepository(database) }
}
