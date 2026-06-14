package com.hazelhope.dubster.hamtest

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE question_info ADD COLUMN firstTime INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE settings (id TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
    }
}

@Database(entities = [UserQuestionInfo::class, SettingsItem::class], version = 3)
abstract class HamTestDatabase : RoomDatabase() {
    abstract fun userQuestionDao(): UserQuestionDao
    abstract fun settingsDao(): SettingsDao
}