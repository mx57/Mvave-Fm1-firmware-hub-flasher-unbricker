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

data class FirmwareVersionEntry(
    val version: String,
    val releaseDate: String,
    val sizeBytes: Int,
    val crc32: String,
    val sha256: String,
    val notes: String
)

object FirmwareVersionHistoryCodec {
    private const val ENTRY_SEP = "\n===VER===\n"
    private const val FIELD_SEP = "|||"

    fun encode(entries: List<FirmwareVersionEntry>): String {
        return entries.joinToString(ENTRY_SEP) { entry ->
            listOf(
                entry.version,
                entry.releaseDate,
                entry.sizeBytes.toString(),
                entry.crc32,
                entry.sha256,
                entry.notes
            ).joinToString(FIELD_SEP)
        }
    }

    fun decode(serialized: String, fallbackFirmware: FirmwareEntity): List<FirmwareVersionEntry> {
        if (serialized.isBlank()) {
            return listOf(
                FirmwareVersionEntry(
                    version = fallbackFirmware.version,
                    releaseDate = fallbackFirmware.releaseDate,
                    sizeBytes = fallbackFirmware.sizeBytes,
                    crc32 = fallbackFirmware.crc32,
                    sha256 = fallbackFirmware.sha256,
                    notes = fallbackFirmware.changelog
                )
            )
        }
        val parsed = serialized.split(ENTRY_SEP).mapNotNull { block ->
            val parts = block.split(FIELD_SEP)
            if (parts.size >= 6) {
                FirmwareVersionEntry(
                    version = parts[0],
                    releaseDate = parts[1],
                    sizeBytes = parts[2].toIntOrNull() ?: fallbackFirmware.sizeBytes,
                    crc32 = parts[3],
                    sha256 = parts[4],
                    notes = parts[5]
                )
            } else {
                null
            }
        }
        return parsed.ifEmpty {
            listOf(
                FirmwareVersionEntry(
                    version = fallbackFirmware.version,
                    releaseDate = fallbackFirmware.releaseDate,
                    sizeBytes = fallbackFirmware.sizeBytes,
                    crc32 = fallbackFirmware.crc32,
                    sha256 = fallbackFirmware.sha256,
                    notes = fallbackFirmware.changelog
                )
            )
        }
    }
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
    val releaseNotes: String = "",
    val versionHistoryJson: String = "",
    val compatibleHardwareRevs: String = "M-VAVE FM-1 PCB Rev A / Rev B / Rev C • JieLi AC791N (WL82) • Bootloader UBOOT1.00 / V14 / V15 / V16",
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
    val isDownloaded: Boolean = false,
    val localFilePath: String = "",
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
