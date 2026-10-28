package com.flstudios.a2lopes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS production_sessions (id TEXT NOT NULL PRIMARY KEY, produced_at INTEGER NOT NULL, created_at INTEGER NOT NULL, notes TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS production_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, session_id TEXT NOT NULL, flavor_id INTEGER NOT NULL, quantity INTEGER NOT NULL, FOREIGN KEY(session_id) REFERENCES production_sessions(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(flavor_id) REFERENCES flavors(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE UNIQUE INDEX index_production_items_session_id_flavor_id ON production_items(session_id, flavor_id)")
        db.execSQL("CREATE INDEX index_production_items_flavor_id ON production_items(flavor_id)")
        db.execSQL("CREATE UNIQUE INDEX index_production_items_id_flavor_id ON production_items(id, flavor_id)")
        db.execSQL("CREATE TABLE IF NOT EXISTS stock_movements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, flavor_id INTEGER NOT NULL, type TEXT NOT NULL, quantity_delta INTEGER NOT NULL, reference_type TEXT NOT NULL, reference_id INTEGER NOT NULL, created_at INTEGER NOT NULL, notes TEXT, production_item_id INTEGER, FOREIGN KEY(flavor_id) REFERENCES flavors(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(production_item_id, flavor_id) REFERENCES production_items(id, flavor_id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX index_stock_movements_flavor_id ON stock_movements(flavor_id)")
        db.execSQL("CREATE INDEX index_stock_movements_production_item_id_flavor_id ON stock_movements(production_item_id, flavor_id)")
        db.execSQL("CREATE UNIQUE INDEX index_stock_movements_production_item_id ON stock_movements(production_item_id)")
    }
}
