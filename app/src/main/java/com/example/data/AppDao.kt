package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Firmwares Catalog ---
    @Query("SELECT * FROM firmwares ORDER BY isOfficialStock DESC, isFavorite DESC, releaseDate DESC")
    fun getAllFirmwares(): Flow<List<FirmwareEntity>>

    @Query("SELECT * FROM firmwares")
    suspend fun getAllFirmwaresSnapshot(): List<FirmwareEntity>

    @Query("SELECT * FROM firmwares WHERE id = :id LIMIT 1")
    suspend fun getFirmwareById(id: String): FirmwareEntity?

    @Query("SELECT COUNT(*) FROM firmwares")
    suspend fun getFirmwareCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirmwares(items: List<FirmwareEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFirmware(item: FirmwareEntity)

    @Update
    suspend fun updateFirmware(item: FirmwareEntity)

    // --- Device EEPROM / Flash Backups ---
    @Query("SELECT * FROM backup_dumps ORDER BY createdAt DESC")
    fun getAllBackups(): Flow<List<BackupDumpEntity>>

    @Query("SELECT COUNT(*) FROM backup_dumps")
    suspend fun getBackupCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupDumpEntity): Long

    @Query("DELETE FROM backup_dumps WHERE id = :id")
    suspend fun deleteBackupById(id: Int)

    // --- Flash & USB Telemetry Logs ---
    @Query("SELECT * FROM flash_logs ORDER BY timestamp DESC LIMIT 400")
    fun getRecentLogs(): Flow<List<FlashLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FlashLogEntity)

    @Query("DELETE FROM flash_logs")
    suspend fun clearAllLogs()
}
