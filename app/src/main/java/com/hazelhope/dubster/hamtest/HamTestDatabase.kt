package com.hazelhope.dubster.hamtest

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserQuestionInfo::class], version = 1)
abstract class HamTestDatabase : RoomDatabase() {
    abstract fun userQuestionDao(): UserQuestionDao
}