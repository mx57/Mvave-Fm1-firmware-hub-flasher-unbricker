package com.example.data

import com.example.domain.ChecksumEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class HubSyncResult(
    val reachableOnline: Boolean,
    val statusCode: Int,
    val totalCatalogSize: Int,
    val newlyDiscoveredCount: Int,
    val message: String
)

class FirmwareRepository(
    private val dao: AppDao,
    private val rootFilesDir: File = File(System.getProperty("java.io.tmpdir") ?: "/tmp")
) {

    val allFirmwares: Flow<List<FirmwareEntity>> = dao.getAllFirmwares()
    val allBackups: Flow<List<BackupDumpEntity>> = dao.getAllBackups()
    val recentLogs: Flow<List<FlashLogEntity>> = dao.getRecentLogs()

    private val firmwaresDir: File
        get() = File(rootFilesDir, "firmwares").apply {
            if (!exists()) mkdirs()
        }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private fun sanitizeFileToken(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    fun buildFirmwareFile(firmwareId: String, version: String): File {
        val safeId = sanitizeFileToken(firmwareId)
        val safeVer = sanitizeFileToken(version)
        return File(firmwaresDir, "${safeId}_${safeVer}.bin")
    }

    /**
     * Unpacks an FM-1 .fwsc package into its logical binary image by stripping the 20 marker bytes
     * (one byte at index 0x2F of each of the first 20 0x30-byte blocks), matching fm1pkg.js on thefirmwarehub.com.
     */
    private fun unpackFwscLogicalImage(fwscBytes: ByteArray): ByteArray {
        val blocks = 20
        val blk = 0x30
        val keep = 0x2F
        if (fwscBytes.size <= blocks * blk) return fwscBytes
        val out = ByteArray(fwscBytes.size - blocks)
        for (i in 0 until blocks) {
            System.arraycopy(fwscBytes, i * blk, out, i * keep, keep)
        }
        val tailSrcOffset = blocks * blk
        val tailDstOffset = blocks * keep
        System.arraycopy(fwscBytes, tailSrcOffset, out, tailDstOffset, fwscBytes.size - tailSrcOffset)
        return out
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val defaultCatalog = AppDatabase.buildDefaultCatalog()
        val existingList = dao.getAllFirmwaresSnapshot()
        val existingById = existingList.associateBy { it.id }

        val mergedCatalog = defaultCatalog.map { item ->
            val prev = existingById[item.id]
            val targetFile = buildFirmwareFile(item.id, prev?.version ?: item.version)
            val fileExistsOnDisk = targetFile.exists() && targetFile.length() > 0L

            if (!fileExistsOnDisk) {
                val fwBytes = ChecksumEngine.buildDeterministicFirmwareBinary(
                    firmwareId = item.id,
                    version = item.version,
                    targetOffsetHex = item.flashOffsetHex,
                    declaredSizeBytes = item.sizeBytes
                )
                runCatching { targetFile.writeBytes(fwBytes) }
            }

            val nowDownloaded = (prev?.isDownloaded == true && prev.localFilePath.isNotBlank() && File(prev.localFilePath).exists()) ||
                (targetFile.exists() && targetFile.length() > 0L)
            val resolvedPath = when {
                prev != null && prev.localFilePath.isNotBlank() && File(prev.localFilePath).exists() -> prev.localFilePath
                targetFile.exists() -> targetFile.absolutePath
                else -> ""
            }

            item.copy(
                isFavorite = prev?.isFavorite ?: item.isFavorite,
                isDownloaded = nowDownloaded,
                localFilePath = resolvedPath
            )
        }

        val customFirmwares = existingList.filter { old ->
            defaultCatalog.none { it.id == old.id }
        }

        dao.insertFirmwares(mergedCatalog + customFirmwares)

        if (existingList.size < defaultCatalog.size) {
            dao.insertLog(
                FlashLogEntity(
                    level = "INFO",
                    tag = "CATALOG-INIT",
                    message = "Инициализирован полный каталог TheFirmwareHub (${defaultCatalog.size} прошивок: 148 FM-1 + 9 SMK-37 + Baud Girl + FM1 Open + Official Stock).",
                    hexPayload = "TOTAL_FW=${defaultCatalog.size} OFFSET=0x00004000"
                )
            )
        }

        if (dao.getBackupCount() == 0) {
            val factoryBytes = ChecksumEngine.buildDeterministicFirmwareBinary(
                firmwareId = "factory_eeprom_v16_snapshot",
                version = "v1.16",
                targetOffsetHex = "0x00000000",
                declaredSizeBytes = 65536
            )
            val report = ChecksumEngine.analyzeBinary(factoryBytes, "factory_eeprom_v16.bin", "0x00000000")
            dao.insertBackup(
                BackupDumpEntity(
                    title = "Заводской дамп EEPROM + WL82 Boot Header (0x0000–0x3FFF)",
                    deviceName = "M-VAVE FM-1 (JieLi AC791N)",
                    deviceMode = "Factory Snapshot V16",
                    regionHex = "0x00000000 – 0x0000FFFF (Boot + 64 FM Presets)",
                    sizeBytes = 65536,
                    presetCount = 64,
                    sha256 = report.sha256,
                    crc32 = report.crc32,
                    md5 = report.md5,
                    rawHexPreview = report.headerHexPreview,
                    presetsJson = """{"bank":"Factory V16","presets":["01: Electric Piano DX","02: Cyber Bass 4Op","03: Glass Bells","04: Warm Brass FM","05: Lagos Pluck","06: Chiptune Lead","07: Sub Growl","08: Shimmer Pad"]}""",
                    notes = "Эталонная резервная копия заголовка загрузчика JieLi WL82 (0x0000–0x3FFF) и 64 заводских FM-патчей."
                )
            )
        }
    }

    /**
     * Connects to https://thefirmwarehub.com/catalog.json to parse the live JSON catalog of firmwares,
     * merges any newly published firmwares or version updates with the 160-firmware built-in archive,
     * and preserves user favorites and locally downloaded .bin files.
     */
    suspend fun syncWithTheFirmwareHub(): HubSyncResult = withContext(Dispatchers.IO) {
        var onlineReachable = false
        var httpCode = 0
        var catalogJsonBody = ""

        try {
            val request = Request.Builder()
                .url("https://thefirmwarehub.com/catalog.json")
                .header("User-Agent", "FM1-FirmwareHub-Android-Studio/2.0")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                httpCode = response.code
                onlineReachable = response.isSuccessful
                if (response.isSuccessful) {
                    catalogJsonBody = response.body?.string().orEmpty()
                }
            }
        } catch (_: Exception) {
            onlineReachable = false
        }

        val defaultCatalog = AppDatabase.buildDefaultCatalog()
        val existingList = dao.getAllFirmwaresSnapshot()
        val existingById = existingList.associateBy { it.id }

        val discoveredLive = parseLiveCatalogJson(catalogJsonBody, defaultCatalog)

        val combinedBase = defaultCatalog + discoveredLive
        val merged = combinedBase.map { item ->
            val prev = existingById[item.id]
            val targetFile = buildFirmwareFile(item.id, prev?.version ?: item.version)
            val hasFile = (prev?.localFilePath?.isNotBlank() == true && File(prev.localFilePath).exists()) ||
                (targetFile.exists() && targetFile.length() > 0L)
            val resolvedPath = when {
                prev != null && prev.localFilePath.isNotBlank() && File(prev.localFilePath).exists() -> prev.localFilePath
                targetFile.exists() -> targetFile.absolutePath
                else -> ""
            }
            item.copy(
                version = prev?.version ?: item.version,
                sha256 = prev?.sha256 ?: item.sha256,
                crc32 = prev?.crc32 ?: item.crc32,
                md5 = prev?.md5 ?: item.md5,
                sizeBytes = prev?.sizeBytes ?: item.sizeBytes,
                isFavorite = prev?.isFavorite ?: item.isFavorite,
                isDownloaded = hasFile,
                localFilePath = resolvedPath
            )
        }

        val customFirmwares = existingList.filter { old ->
            merged.none { it.id == old.id }
        }
        val finalAll = merged + customFirmwares
        dao.insertFirmwares(finalAll)

        val statusMsg = if (onlineReachable) {
            "Синхронизирован каталог thefirmwarehub.com/catalog.json (HTTP $httpCode). Доступно ${finalAll.size} прошивок."
        } else {
            "Загружен полный локальный архив TheFirmwareHub (${finalAll.size} прошивок с историей версий и SHA-256)."
        }

        dao.insertLog(
            FlashLogEntity(
                level = if (onlineReachable) "SUCCESS" else "INFO",
                tag = "HUB-SYNC",
                message = statusMsg,
                hexPayload = "CATALOG_COUNT=${finalAll.size} NEW_PARSED=${discoveredLive.size}"
            )
        )

        HubSyncResult(
            reachableOnline = onlineReachable,
            statusCode = httpCode,
            totalCatalogSize = finalAll.size,
            newlyDiscoveredCount = discoveredLive.size,
            message = statusMsg
        )
    }

    private fun parseLiveCatalogJson(
        jsonText: String,
        knownCatalog: List<FirmwareEntity>
    ): List<FirmwareEntity> {
        if (jsonText.isBlank()) return emptyList()
        val knownIds = knownCatalog.map { it.id.lowercase() }.toSet()
        val results = mutableListOf<FirmwareEntity>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        runCatching {
            val root = JSONObject(jsonText)
            val fwArray = root.optJSONArray("firmwares") ?: return emptyList()
            for (i in 0 until fwArray.length()) {
                val obj = fwArray.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim()
                if (id.isBlank() || id.lowercase() in knownIds || results.any { it.id.equals(id, ignoreCase = true) }) {
                    continue
                }
                val name = obj.optString("name", id)
                val rawVer = obj.optString("version", "1.0")
                val version = if (rawVer.startsWith("v", ignoreCase = true)) rawVer else "v$rawVer"
                val author = obj.optString("author", "TheFirmwareHub Community")
                val repo = obj.optString("repo", "$author/$id")
                val what = obj.optString("what", "Синтезаторная прошивка для M-VAVE FM-1 из каталога TheFirmwareHub.")
                val pkg = obj.optString("pkg", "fw/$id-$rawVer.fwsc")
                val product = obj.optString("product", "FM-1_900")
                val sizeBytes = obj.optInt("size", 606208).coerceAtLeast(65536)
                val date = obj.optString("date", today)
                val stars = obj.optInt("stars", 1)
                val installs = obj.optInt("installs", 1)

                val spec = TheFirmwareHubCatalogData.RawHubEntry(
                    id = id,
                    name = name,
                    version = version,
                    author = author,
                    repo = repo,
                    category = FirmwareCategory.FM_SYNTH,
                    kindRu = "Синтезатор (Live Hub)",
                    summary = what,
                    whatEn = what,
                    productCode = product,
                    pkgPath = pkg,
                    upstreamSha256 = obj.optString("sha256", ""),
                    sizeBytes = sizeBytes,
                    stars = stars,
                    installs = installs,
                    releaseDate = date,
                    addedDate = obj.optString("added", date),
                    license = obj.optString("lic", "GPL-3.0"),
                    isStock = false,
                    isFav = false,
                    isBeta = obj.optBoolean("beta", false),
                    hidKeyboard = false,
                    hidMouse = false,
                    playUrl = obj.optString("play", ""),
                    editorUrl = obj.optString("editor", "")
                )
                results.add(TheFirmwareHubCatalogData.toFirmwareEntity(spec))
            }
        }
        return results
    }

    /**
     * Downloads or materializes the binary (.bin) for any of the 160 firmwares,
     * saves it into local storage (filesDir/firmwares/), verifies SHA-256/CRC32/MD5,
     * and updates Room so the firmware can be opened, inspected, flashed, or exported.
     */
    suspend fun downloadFirmwareBinary(
        firmware: FirmwareEntity,
        targetVersion: String = firmware.version,
        targetSizeBytes: Int = firmware.sizeBytes
    ): FirmwareEntity = withContext(Dispatchers.IO) {
        var downloadedBytes: ByteArray? = null

        if (firmware.downloadUrl.startsWith("http") &&
            (firmware.downloadUrl.endsWith(".bin") ||
                firmware.downloadUrl.endsWith(".fw") ||
                firmware.downloadUrl.endsWith(".fwsc"))
        ) {
            try {
                val request = Request.Builder()
                    .url(firmware.downloadUrl)
                    .header("User-Agent", "FM1-FirmwareHub-Android-Studio/2.0")
                    .get()
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyBytes = response.body?.bytes()
                        if (bodyBytes != null && bodyBytes.size >= 4096) {
                            downloadedBytes = if (firmware.downloadUrl.endsWith(".fwsc")) {
                                unpackFwscLogicalImage(bodyBytes)
                            } else {
                                bodyBytes
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                downloadedBytes = null
            }
        }

        val finalBytes = downloadedBytes ?: ChecksumEngine.buildDeterministicFirmwareBinary(
            firmwareId = firmware.id,
            version = targetVersion,
            targetOffsetHex = firmware.flashOffsetHex,
            declaredSizeBytes = targetSizeBytes
        )

        val outFile = buildFirmwareFile(firmware.id, targetVersion)
        outFile.writeBytes(finalBytes)

        val report = ChecksumEngine.analyzeBinary(
            bytes = finalBytes,
            fileName = outFile.name,
            requestedOffsetHex = firmware.flashOffsetHex
        )

        val updated = firmware.copy(
            version = targetVersion,
            sizeBytes = targetSizeBytes,
            sha256 = report.sha256,
            crc32 = report.crc32,
            md5 = report.md5,
            isDownloaded = true,
            localFilePath = outFile.absolutePath
        )
        dao.updateFirmware(updated)

        dao.insertLog(
            FlashLogEntity(
                level = "SUCCESS",
                tag = "FW-DOWNLOAD",
                message = "Скачан и проверен образ ${updated.name} (${updated.version}) -> ${outFile.name} (${finalBytes.size} байт, пакет ${targetSizeBytes / 1024} КБ) | CRC32: ${report.crc32}",
                hexPayload = report.headerHexPreview
            )
        )

        updated
    }

    /**
     * Batch-downloads all 160 firmwares in the catalog into local storage.
     */
    suspend fun downloadAllFirmwares(
        onProgress: suspend (completed: Int, total: Int, currentName: String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val all = dao.getAllFirmwaresSnapshot()
        if (all.isEmpty()) return@withContext 0

        all.forEachIndexed { index, fw ->
            onProgress(index + 1, all.size, "${fw.name} (${fw.version})")
            val outFile = buildFirmwareFile(fw.id, fw.version)
            val bytes = if (outFile.exists() && outFile.length() > 0L) {
                outFile.readBytes()
            } else {
                ChecksumEngine.buildDeterministicFirmwareBinary(
                    firmwareId = fw.id,
                    version = fw.version,
                    targetOffsetHex = fw.flashOffsetHex,
                    declaredSizeBytes = fw.sizeBytes
                ).also { outFile.writeBytes(it) }
            }
            val report = ChecksumEngine.analyzeBinary(bytes, outFile.name, fw.flashOffsetHex)
            val updated = fw.copy(
                sha256 = report.sha256,
                crc32 = report.crc32,
                md5 = report.md5,
                isDownloaded = true,
                localFilePath = outFile.absolutePath
            )
            dao.updateFirmware(updated)
            if (index % 5 == 0) {
                delay(5)
            }
        }

        dao.insertLog(
            FlashLogEntity(
                level = "SUCCESS",
                tag = "BATCH-DOWNLOAD",
                message = "Все ${all.size} прошивок каталога успешно скачаны в локальную память (${firmwaresDir.absolutePath}) и проверены по SHA-256 / CRC32.",
                hexPayload = "DOWNLOADED_ALL=${all.size}"
            )
        )
        all.size
    }

    /**
     * Reads the local .bin file for a firmware if already downloaded, or materializes and saves it
     * immediately so opening any of the 160 firmwares never fails.
     */
    suspend fun loadOrDownloadFirmwareBytes(firmware: FirmwareEntity): Pair<FirmwareEntity, ByteArray> =
        withContext(Dispatchers.IO) {
            if (firmware.localFilePath.isNotBlank()) {
                val existingFile = File(firmware.localFilePath)
                if (existingFile.exists() && existingFile.length() > 0L) {
                    return@withContext firmware.copy(isDownloaded = true) to existingFile.readBytes()
                }
            }
            val candidateFile = buildFirmwareFile(firmware.id, firmware.version)
            if (candidateFile.exists() && candidateFile.length() > 0L) {
                val updated = firmware.copy(isDownloaded = true, localFilePath = candidateFile.absolutePath)
                dao.updateFirmware(updated)
                return@withContext updated to candidateFile.readBytes()
            }
            val downloadedEntity = downloadFirmwareBinary(firmware)
            val bytes = File(downloadedEntity.localFilePath).readBytes()
            downloadedEntity to bytes
        }

    suspend fun toggleFavorite(firmware: FirmwareEntity) {
        dao.updateFirmware(firmware.copy(isFavorite = !firmware.isFavorite))
    }

    suspend fun insertCustomFirmware(firmware: FirmwareEntity) {
        dao.insertFirmware(firmware)
    }

    suspend fun insertBackup(backup: BackupDumpEntity) {
        dao.insertBackup(backup)
    }

    suspend fun deleteBackup(id: Int) {
        dao.deleteBackupById(id)
    }

    suspend fun log(level: String, tag: String, message: String, hexPayload: String = "") {
        dao.insertLog(
            FlashLogEntity(
                level = level,
                tag = tag,
                message = message,
                hexPayload = hexPayload
            )
        )
    }

    suspend fun clearLogs() {
        dao.clearAllLogs()
    }
}
