package com.flstudios.a2lopes.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.flstudios.a2lopes.data.local.AppDatabase
import com.flstudios.a2lopes.data.local.entity.FlavorEntity
import com.flstudios.a2lopes.domain.model.Flavor
import com.flstudios.a2lopes.domain.model.FlavorHistoryException
import com.flstudios.a2lopes.domain.model.FlavorField
import com.flstudios.a2lopes.domain.model.FlavorRules
import com.flstudios.a2lopes.domain.model.FlavorValidationException
import com.flstudios.a2lopes.domain.repository.FlavorRepository
import kotlinx.coroutines.flow.map

class RoomFlavorRepository(private val database: AppDatabase) : FlavorRepository {
    private val dao = database.flavorDao()

    override fun observeAll() = dao.observeAll().map { rows ->
        rows.map { Flavor(it.id, it.name, it.defaultPriceCents, it.active) }
    }

    override suspend fun save(id: Long?, name: String, price: String) {
        val input = FlavorRules.validate(name, price)
        try {
            database.withTransaction {
                val existing = dao.findByNormalizedName(input.normalizedName)
                if (existing != null && existing.id != id) duplicateName()
                if (id == null) {
                    dao.insert(FlavorEntity(name = input.name, defaultPriceCents = input.priceCents))
                } else {
                    check(dao.updateDetails(id, input.name, input.normalizedName, input.priceCents) == 1) {
                        "O sabor não foi encontrado."
                    }
                }
            }
        } catch (_: SQLiteConstraintException) {
            duplicateName()
        }
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        database.withTransaction {
            check(active || database.stockDao().quantity(id) == 0L) { "O sabor só pode ser desativado com estoque zerado." }
            check(dao.setActive(id, active) == 1) { "O sabor não foi encontrado." }
        }
    }

    override suspend fun delete(id: Long) {
        database.withTransaction {
            if (database.stockDao().hasHistory(id)) throw FlavorHistoryException()
            check(dao.deleteById(id) == 1) { "O sabor não foi encontrado." }
        }
    }

    private fun duplicateName(): Nothing = throw FlavorValidationException(
        FlavorField.NAME, "Já existe um sabor com esse nome, ativo ou inativo.",
    )
}
