package com.flstudios.a2lopes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sales (id TEXT NOT NULL PRIMARY KEY, sold_at INTEGER NOT NULL, created_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sale_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sale_id TEXT NOT NULL, flavor_id INTEGER NOT NULL, quantity INTEGER NOT NULL, unit_price_cents INTEGER, FOREIGN KEY(sale_id) REFERENCES sales(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(flavor_id) REFERENCES flavors(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE UNIQUE INDEX index_sale_items_sale_id_flavor_id ON sale_items(sale_id, flavor_id)")
        db.execSQL("CREATE INDEX index_sale_items_flavor_id ON sale_items(flavor_id)")
        db.execSQL("CREATE UNIQUE INDEX index_sale_items_id_flavor_id ON sale_items(id, flavor_id)")
        // SQLite não acrescenta FK composta por ALTER COLUMN: recriar preservando todos os dados.
        db.execSQL("CREATE TABLE stock_movements_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, flavor_id INTEGER NOT NULL, type TEXT NOT NULL, quantity_delta INTEGER NOT NULL, reference_type TEXT NOT NULL, reference_id INTEGER NOT NULL, created_at INTEGER NOT NULL, notes TEXT, production_item_id INTEGER, sale_item_id INTEGER, FOREIGN KEY(flavor_id) REFERENCES flavors(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(production_item_id, flavor_id) REFERENCES production_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(sale_item_id, flavor_id) REFERENCES sale_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("INSERT INTO stock_movements_new (id, flavor_id, type, quantity_delta, reference_type, reference_id, created_at, notes, production_item_id) SELECT id, flavor_id, type, quantity_delta, reference_type, reference_id, created_at, notes, production_item_id FROM stock_movements")
        // Room habilita FKs ao abrir o banco; validar a cópia ainda na transação de migração.
        db.query("PRAGMA foreign_key_check(stock_movements_new)").use { cursor ->
            check(!cursor.moveToFirst()) { "Há vínculos de estoque inválidos. A atualização foi interrompida e os dados foram preservados." }
        }
        db.execSQL("DROP TABLE stock_movements")
        db.execSQL("ALTER TABLE stock_movements_new RENAME TO stock_movements")
        db.execSQL("CREATE INDEX index_stock_movements_flavor_id ON stock_movements(flavor_id)")
        db.execSQL("CREATE INDEX index_stock_movements_production_item_id_flavor_id ON stock_movements(production_item_id, flavor_id)")
        db.execSQL("CREATE UNIQUE INDEX index_stock_movements_production_item_id ON stock_movements(production_item_id)")
        db.execSQL("CREATE INDEX index_stock_movements_sale_item_id_flavor_id ON stock_movements(sale_item_id, flavor_id)")
        db.execSQL("CREATE UNIQUE INDEX index_stock_movements_sale_item_id ON stock_movements(sale_item_id)")
    }
}
