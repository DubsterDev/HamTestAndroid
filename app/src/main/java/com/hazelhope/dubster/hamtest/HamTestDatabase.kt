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

@Database(entities = [UserQuestionInfo::class], version = 2)
abstract class HamTestDatabase : RoomDatabase() {
    abstract fun userQuestionDao(): UserQuestionDao
}