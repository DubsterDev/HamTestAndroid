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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE question_info_new (id TEXT NOT NULL, pool TEXT NOT NULL, score INTEGER NOT NULL, firstTime INTEGER NOT NULL, PRIMARY KEY(id, pool))")
        db.execSQL("INSERT INTO question_info_new (id, pool, score, firstTime) SELECT id, pool, score, firstTime FROM question_info")
        db.execSQL("DROP TABLE question_info")
        db.execSQL("ALTER TABLE question_info_new RENAME TO question_info")
    }
}

@Database(
    entities = [
        UserQuestionInfo::class,
        SettingsItem::class
    ],
    version = 4
)
abstract class HamTestDatabase : RoomDatabase() {
    abstract fun userQuestionDao(): UserQuestionDao
    abstract fun settingsDao(): SettingsDao
}