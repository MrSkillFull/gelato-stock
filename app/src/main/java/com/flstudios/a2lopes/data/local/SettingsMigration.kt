package com.flstudios.a2lopes.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (id INTEGER NOT NULL, low_stock_threshold INTEGER NOT NULL, PRIMARY KEY(id))")
        db.execSQL("INSERT INTO app_settings (id, low_stock_threshold) VALUES (1, 10)")
    }
}
