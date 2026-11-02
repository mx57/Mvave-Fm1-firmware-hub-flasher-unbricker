package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FirmwareCategory(val displayName: String, val badgeCode: String) {
    ALL("Все прошивки", "ALL"),
    FM_SYNTH("FM Синтез", "FM-OP"),
    GROOVEBOX("Грувбоксы & Секвенсоры", "SEQ"),
    CHIPTUNE("Chiptune / 8-Bit / SID", "CHIP"),
    VIRTUAL_ANALOG("Виртуальный Аналог", "VA"),
    AMBIENT("Генеративный Эмбиент", "AMB"),
    OFFICIAL_STOCK("Стоковые & Unbrick Recovery", "STOCK")
}

@Entity(tableName = "firmwares")
data class FirmwareEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val author: String,
    val category: String,
    val summary: String,
    val changelog: String,
    val targetDevice: String = "M-VAVE FM-1",
    val chipArchitecture: String = "JieLi AC791N / WL82",
    val flashOffsetHex: String = "0x00004000",
    val sizeBytes: Int,
    val sha256: String,
    val crc32: String,
    val md5: String,
    val downloadUrl: String,
    val tagsCsv: String,
    val isOfficialStock: Boolean = false,
    val isVerified: Boolean = true,
    val isOfflineBundled: Boolean = true,
    val releaseDate: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "backup_dumps")
data class BackupDumpEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val deviceName: String,
    val deviceMode: String,
    val createdAt: Long = System.currentTimeMillis(),
    val regionHex: String,
    val sizeBytes: Int,
    val presetCount: Int,
    val sha256: String,
    val crc32: String,
    val md5: String,
    val rawHexPreview: String,
    val presetsJson: String,
    val notes: String
)

@Entity(tableName = "flash_logs")
data class FlashLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String, // INFO, USB_IO, CHECKSUM, WARN, ERROR, SUCCESS
    val tag: String,
    val message: String,
    val hexPayload: String = ""
)
