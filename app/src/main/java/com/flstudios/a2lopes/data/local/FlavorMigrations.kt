package com.flstudios.a2lopes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flstudios.a2lopes.domain.model.FlavorDataException
import com.flstudios.a2lopes.domain.model.FlavorNames

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Valide antes de alterar a tabela. Room executa a migração em transação.
        val names = mutableSetOf<String>()
        db.query("SELECT name FROM flavors").use { cursor ->
            while (cursor.moveToNext()) {
                val key = FlavorNames.key(cursor.getString(0))
                if (key.isEmpty() || !names.add(key)) {
                    throw FlavorDataException(
                        "O banco antigo contém nomes de sabores vazios ou repetidos. " +
                            "A atualização foi interrompida e os dados foram preservados. " +
                            "É necessário corrigir esses registros antes de atualizar.",
                    )
                }
            }
        }
        db.execSQL(
            "CREATE TABLE flavors_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "name TEXT NOT NULL, default_price_cents INTEGER, active INTEGER NOT NULL, " +
                "normalized_name TEXT NOT NULL)",
        )
        db.query("SELECT id, name, default_price_cents, active FROM flavors").use { cursor ->
            while (cursor.moveToNext()) {
                val name = FlavorNames.display(cursor.getString(1))
                db.execSQL(
                    "INSERT INTO flavors_new (id, name, default_price_cents, active, normalized_name) " +
                        "VALUES (?, ?, ?, ?, ?)",
                    arrayOf<Any?>(
                        cursor.getLong(0), name,
                        if (cursor.isNull(2)) null else cursor.getLong(2),
                        cursor.getInt(3), FlavorNames.key(name),
                    ),
                )
            }
        }
        db.execSQL("DROP TABLE flavors")
        db.execSQL("ALTER TABLE flavors_new RENAME TO flavors")
        db.execSQL("CREATE UNIQUE INDEX index_flavors_normalized_name ON flavors (normalized_name)")
    }
}
