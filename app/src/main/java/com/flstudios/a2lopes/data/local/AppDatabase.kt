package com.flstudios.a2lopes.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flstudios.a2lopes.data.local.dao.FlavorDao
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.data.local.entity.ProductionSessionEntity
import com.flstudios.a2lopes.data.local.entity.ProductionItemEntity
import com.flstudios.a2lopes.data.local.entity.StockMovementEntity
import com.flstudios.a2lopes.data.local.dao.ProductionDao
import com.flstudios.a2lopes.data.local.dao.StockDao
import com.flstudios.a2lopes.data.local.dao.SaleDao
import com.flstudios.a2lopes.data.local.dao.HistoryDao
import com.flstudios.a2lopes.data.local.entity.SaleEntity
import com.flstudios.a2lopes.data.local.entity.SaleItemEntity
import com.flstudios.a2lopes.data.local.entity.CorrectionEntity
import com.flstudios.a2lopes.data.local.entity.CorrectionItemEntity
import com.flstudios.a2lopes.data.local.dao.CorrectionDao

@Database(entities = [com.flstudios.a2lopes.data.local.entity.AppSettingsEntity::class, FlavorEntity::class, ProductionSessionEntity::class, ProductionItemEntity::class, StockMovementEntity::class, SaleEntity::class, SaleItemEntity::class, CorrectionEntity::class, CorrectionItemEntity::class], version = 6, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): com.flstudios.a2lopes.data.local.dao.SettingsDao
    abstract fun backupDao(): com.flstudios.a2lopes.data.local.dao.BackupDao
    abstract fun correctionDao(): CorrectionDao
    abstract fun historyDao(): HistoryDao
    abstract fun flavorDao(): FlavorDao
    abstract fun productionDao(): ProductionDao
    abstract fun stockDao(): StockDao
    abstract fun saleDao(): SaleDao
}
