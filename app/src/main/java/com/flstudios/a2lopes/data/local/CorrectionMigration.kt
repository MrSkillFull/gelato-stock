package com.flstudios.a2lopes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE corrections (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, reason TEXT NOT NULL,
            created_at INTEGER NOT NULL, flavor_id INTEGER, input_quantity INTEGER, sale_id TEXT, production_id TEXT,
            FOREIGN KEY(flavor_id) REFERENCES flavors(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(sale_id) REFERENCES sales(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(production_id) REFERENCES production_sessions(id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE INDEX index_corrections_flavor_id ON corrections(flavor_id)")
        db.execSQL("CREATE UNIQUE INDEX index_corrections_sale_id ON corrections(sale_id)")
        db.execSQL("CREATE UNIQUE INDEX index_corrections_production_id ON corrections(production_id)")
        db.execSQL("""CREATE TABLE correction_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            correction_id TEXT NOT NULL, flavor_id INTEGER NOT NULL, previous_quantity INTEGER NOT NULL,
            resulting_quantity INTEGER NOT NULL, quantity_delta INTEGER NOT NULL,
            source_production_item_id INTEGER, source_sale_item_id INTEGER,
            FOREIGN KEY(correction_id) REFERENCES corrections(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(flavor_id) REFERENCES flavors(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(source_production_item_id, flavor_id) REFERENCES production_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(source_sale_item_id, flavor_id) REFERENCES sale_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("CREATE UNIQUE INDEX index_correction_items_correction_id_flavor_id ON correction_items(correction_id, flavor_id)")
        db.execSQL("CREATE INDEX index_correction_items_flavor_id ON correction_items(flavor_id)")
        db.execSQL("CREATE UNIQUE INDEX index_correction_items_id_flavor_id ON correction_items(id, flavor_id)")
        db.execSQL("CREATE INDEX index_correction_items_source_production_item_id_flavor_id ON correction_items(source_production_item_id, flavor_id)")
        db.execSQL("CREATE INDEX index_correction_items_source_sale_item_id_flavor_id ON correction_items(source_sale_item_id, flavor_id)")
        // Rebuild only movements to add the concrete composite FK; all legacy columns/IDs are copied.
        db.execSQL("""CREATE TABLE stock_movements_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            flavor_id INTEGER NOT NULL, type TEXT NOT NULL, quantity_delta INTEGER NOT NULL, reference_type TEXT NOT NULL,
            reference_id INTEGER NOT NULL, created_at INTEGER NOT NULL, notes TEXT, production_item_id INTEGER,
            sale_item_id INTEGER, correction_item_id INTEGER,
            FOREIGN KEY(flavor_id) REFERENCES flavors(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(production_item_id, flavor_id) REFERENCES production_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(sale_item_id, flavor_id) REFERENCES sale_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(correction_item_id, flavor_id) REFERENCES correction_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT)""")
        db.execSQL("""INSERT INTO stock_movements_new (id, flavor_id, type, quantity_delta, reference_type, reference_id, created_at, notes, production_item_id, sale_item_id)
            SELECT id, flavor_id, type, quantity_delta, reference_type, reference_id, created_at, notes, production_item_id, sale_item_id FROM stock_movements""")
        db.query("PRAGMA foreign_key_check(stock_movements_new)").use {
            check(!it.moveToFirst()) { "Vínculos de estoque inválidos. A atualização foi interrompida sem substituir os dados." }
        }
        db.execSQL("DROP TABLE stock_movements")
        db.execSQL("ALTER TABLE stock_movements_new RENAME TO stock_movements")
        db.execSQL("CREATE INDEX index_stock_movements_flavor_id ON stock_movements(flavor_id)")
        for (column in listOf("production_item_id", "sale_item_id", "correction_item_id")) {
            db.execSQL("CREATE INDEX index_stock_movements_${column}_flavor_id ON stock_movements($column, flavor_id)")
            db.execSQL("CREATE UNIQUE INDEX index_stock_movements_$column ON stock_movements($column)")
        }
    }
}
