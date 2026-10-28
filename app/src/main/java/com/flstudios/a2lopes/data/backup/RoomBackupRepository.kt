package com.flstudios.a2lopes.data.backup

import androidx.room.withTransaction
import com.flstudios.a2lopes.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import com.flstudios.a2lopes.domain.repository.BackupDocument
import com.flstudios.a2lopes.domain.repository.BackupRepository
import com.flstudios.a2lopes.domain.model.BackupInvalid

class RoomBackupRepository(private val database: AppDatabase) : BackupRepository {
    suspend fun snapshot(): BackupSnapshot = database.withTransaction {
        val dao = database.backupDao()
        if (dao.recordCount() > BackupJson.MAX_RECORDS) throw BackupInvalid("O banco excede 100.000 registros por backup.")
        BackupSnapshot(System.currentTimeMillis(), UUID.randomUUID().toString(),
            flavors = dao.readFlavor(),
            production_sessions = dao.readProductionSession(),
            production_items = dao.readProductionItem(),
            sales = dao.readSale(),
            sale_items = dao.readSaleItem(),
            corrections = dao.readCorrection(),
            correction_items = dao.readCorrectionItem(),
            stock_movements = dao.readStockMovement(),
            lowStockThreshold = database.settingsDao().read() ?: com.flstudios.a2lopes.domain.model.DEFAULT_LOW_STOCK_THRESHOLD)
    }
    override suspend fun export(open: () -> OutputStream?) = withContext(Dispatchers.IO) {
        val data = snapshot()
        val bytes = object : java.io.ByteArrayOutputStream() {
            private fun checkSize(amount: Int) {
                if (count.toLong() + amount > BackupJson.MAX_BYTES) throw BackupInvalid("O backup excede o limite de 32 MiB.")
            }
            override fun write(b: Int) { checkSize(1); super.write(b) }
            override fun write(b: ByteArray, off: Int, len: Int) { checkSize(len); super.write(b, off, len) }
        }
        BackupJson.write(data, bytes)
        if (bytes.size() > BackupJson.MAX_BYTES) throw BackupInvalid("O backup excede o limite de 32 MiB.")
        (open() ?: throw java.io.IOException("Não foi possível abrir o destino.")).use { it.write(bytes.toByteArray()); it.flush() }
    }
    override suspend fun inspect(open: () -> InputStream?): BackupSnapshot = withContext(Dispatchers.IO) {
        (open() ?: throw java.io.IOException("Não foi possível abrir o arquivo.")).use(BackupJson::read)
    }
    override suspend fun restore(data: BackupDocument) = withContext(Dispatchers.IO) {
        if (data !is BackupSnapshot) throw BackupInvalid("Documento de backup inválido.")
        BackupValidator.validate(data)
        database.withTransaction {
            val dao = database.backupDao()
            dao.deleteStockMovement()
            dao.deleteCorrectionItem()
            dao.deleteCorrection()
            dao.deleteSaleItem()
            dao.deleteSale()
            dao.deleteProductionItem()
            dao.deleteProductionSession()
            dao.deleteFlavor()
            dao.insertFlavor(data.flavors)
            dao.insertProductionSession(data.production_sessions)
            dao.insertProductionItem(data.production_items)
            dao.insertSale(data.sales)
            dao.insertSaleItem(data.sale_items)
            dao.insertCorrection(data.corrections)
            dao.insertCorrectionItem(data.correction_items)
            dao.insertStockMovement(data.stock_movements)
            database.settingsDao().write(com.flstudios.a2lopes.data.local.entity.AppSettingsEntity(lowStockThreshold = data.lowStockThreshold))
        }
    }
}
