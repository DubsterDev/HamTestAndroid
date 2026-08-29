package com.hazelhope.dubster.hamtest

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    fun getAll(): List<SettingsItem>

    @Query("SELECT * FROM settings WHERE id = :key")
    fun getValue(key: String): List<SettingsItem>

    @Query("SELECT * FROM settings WHERE id = :key")
    fun getValueAsFlow(key: String): Flow<List<SettingsItem>>

    @Query("UPDATE settings SET value = :value WHERE id = :key")
    fun updateSetting(key: String, value: String)

    @Upsert
    fun upsertSetting(setting: SettingsItem)

    @Insert
    fun insertAll(vararg settings: SettingsItem)

    @Delete
    fun delete(setting: SettingsItem)
}