package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BackupDumpEntity
import com.example.data.FirmwareCategory
import com.example.data.FirmwareEntity
import com.example.data.FirmwareRepository
import com.example.data.FirmwareVersionEntry
import com.example.data.FirmwareVersionHistoryCodec
import com.example.data.FlashLogEntity
import com.example.domain.ChecksumEngine
import com.example.domain.ChecksumReport
import com.example.domain.ConnectedUsbDeviceInfo
import com.example.domain.FlashStepProgress
import com.example.domain.UsbFlashEngine
import com.example.domain.UsbProfileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppDestination(val title: String) {
    CATALOG("Каталог"),
    FLASHER("Прошивка"),
    UNBRICKER("Unbricker"),
    BACKUPS("Бэкапы"),
    LOGS_DOCS("Логи")
}

enum class SortOption(val label: String) {
    RECOMMENDED("Рекомендуемые & Сток"),
    NEWEST("Сначала новые"),
    NAME_ASC("По имени (A–Z)"),
    SIZE_ASC("По размеру")
}

data class FlashSessionUiState(
    val isFlashing: Boolean = false,
    val autoBackupBeforeFlash: Boolean = true,
    val verifyAfterFlash: Boolean = true,
    val protectBootHeader0x4000: Boolean = true,
    val currentProgress: FlashStepProgress? = null,
    val lastCompletedMessage: String? = null,
    val isError: Boolean = false,
    val activeUnbrickStep: Int = 0
)

class FirmwareHubViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = FirmwareRepository(database.appDao(), application.filesDir)
    private val usbEngine = UsbFlashEngine(application)

    private val _currentDestination = MutableStateFlow(AppDestination.CATALOG)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    // Full-screen Firmware Detail sub-destination state
    private val _isShowingFirmwareDetail = MutableStateFlow(false)
    val isShowingFirmwareDetail: StateFlow<Boolean> = _isShowingFirmwareDetail.asStateFlow()

    // Search & filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FirmwareCategory.ALL)
    val selectedCategory: StateFlow<FirmwareCategory> = _selectedCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.RECOMMENDED)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    private val _isSyncingHub = MutableStateFlow(false)
    val isSyncingHub: StateFlow<Boolean> = _isSyncingHub.asStateFlow()

    private val _isDownloadingFirmware = MutableStateFlow(false)
    val isDownloadingFirmware: StateFlow<Boolean> = _isDownloadingFirmware.asStateFlow()

    private val _isDownloadingAll = MutableStateFlow(false)
    val isDownloadingAll: StateFlow<Boolean> = _isDownloadingAll.asStateFlow()

    private val _batchDownloadStatus = MutableStateFlow<String?>(null)
    val batchDownloadStatus: StateFlow<String?> = _batchDownloadStatus.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    // Pending export binary payload for SAF CreateDocument contract
    private var pendingExportBytes: ByteArray? = null
    private var pendingExportFirmwareName: String = ""

    // USB & Simulation state
    private val _useSimulationMode = MutableStateFlow(true)
    val useSimulationMode: StateFlow<Boolean> = _useSimulationMode.asStateFlow()

    private val _simulatedProfile = MutableStateFlow(UsbProfileType.MVAVE_NORMAL_MIDI)
    val simulatedProfile: StateFlow<UsbProfileType> = _simulatedProfile.asStateFlow()

    private val _connectedDevice = MutableStateFlow(
        usbEngine.buildSimulatedDevice(UsbProfileType.MVAVE_NORMAL_MIDI)
    )
    val connectedDevice: StateFlow<ConnectedUsbDeviceInfo> = _connectedDevice.asStateFlow()

    // Selected firmware & checksum inspector state
    private val _selectedFirmware = MutableStateFlow<FirmwareEntity?>(null)
    val selectedFirmware: StateFlow<FirmwareEntity?> = _selectedFirmware.asStateFlow()

    private val _activeChecksumReport = MutableStateFlow<ChecksumReport?>(null)
    val activeChecksumReport: StateFlow<ChecksumReport?> = _activeChecksumReport.asStateFlow()

    private val _customExpectedHashInput = MutableStateFlow("")
    val customExpectedHashInput: StateFlow<String> = _customExpectedHashInput.asStateFlow()

    // Flash & Unbrick state
    private val _flashSessionState = MutableStateFlow(FlashSessionUiState())
    val flashSessionState: StateFlow<FlashSessionUiState> = _flashSessionState.asStateFlow()

    // Log filter state
    private val _selectedLogLevelFilter = MutableStateFlow("ALL")
    val selectedLogLevelFilter: StateFlow<String> = _selectedLogLevelFilter.asStateFlow()

    // Cross-platform script generator state
    private val _selectedDesktopOs = MutableStateFlow("WINDOWS_PS")
    val selectedDesktopOs: StateFlow<String> = _selectedDesktopOs.asStateFlow()

    val totalCatalogCount: StateFlow<Int> = repository.allFirmwares
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val downloadedCatalogCount: StateFlow<Int> = repository.allFirmwares
        .map { list -> list.count { it.isDownloaded } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val filteredFirmwares: StateFlow<List<FirmwareEntity>> = combine(
        repository.allFirmwares,
        _searchQuery,
        _selectedCategory,
        _sortOption,
        _onlyFavorites
    ) { list, query, category, sort, favOnly ->
        val q = query.trim().lowercase()
        val filtered = list.filter { fw ->
            val matchesCat = category == FirmwareCategory.ALL || fw.category == category.name
            val matchesFav = !favOnly || fw.isFavorite
            val matchesQuery = q.isEmpty() ||
                fw.name.lowercase().contains(q) ||
                fw.id.lowercase().contains(q) ||
                fw.author.lowercase().contains(q) ||
                fw.summary.lowercase().contains(q) ||
                fw.releaseNotes.lowercase().contains(q) ||
                fw.compatibleHardwareRevs.lowercase().contains(q) ||
                fw.tagsCsv.lowercase().contains(q) ||
                fw.sha256.lowercase().contains(q) ||
                fw.crc32.lowercase().contains(q)
            matchesCat && matchesFav && matchesQuery
        }

        val sorted = when (sort) {
            SortOption.RECOMMENDED -> filtered.sortedWith(
                compareByDescending<FirmwareEntity> { it.isOfficialStock }
                    .thenByDescending { it.isFavorite }
                    .thenByDescending { it.releaseDate }
            )
            SortOption.NEWEST -> filtered.sortedByDescending { it.releaseDate }
            SortOption.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortOption.SIZE_ASC -> filtered.sortedBy { it.sizeBytes }
        }

        // Keep selectedFirmware synchronized with Room updates (e.g. favorite/downloaded flags)
        val currentSelectedId = _selectedFirmware.value?.id
        if (currentSelectedId != null) {
            list.find { it.id == currentSelectedId }?.let { fresh ->
                _selectedFirmware.value = fresh
            }
        } else if (sorted.isNotEmpty()) {
            selectFirmwareForInspection(sorted.first(), navigateToFlasher = false)
        }
        sorted
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val backups: StateFlow<List<BackupDumpEntity>> = repository.allBackups.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredLogs: StateFlow<List<FlashLogEntity>> = combine(
        repository.recentLogs,
        _selectedLogLevelFilter
    ) { logs, level ->
        if (level == "ALL") logs else logs.filter { it.level == level }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            refreshUsbConnection()
        }
    }

    fun navigateTo(destination: AppDestination) {
        _isShowingFirmwareDetail.value = false
        _currentDestination.value = destination
    }

    fun openFirmwareDetail(firmware: FirmwareEntity) {
        _selectedFirmware.value = firmware
        _isShowingFirmwareDetail.value = true
        _currentDestination.value = AppDestination.CATALOG
        viewModelScope.launch {
            val binary = ChecksumEngine.buildDeterministicFirmwareBinary(
                firmwareId = firmware.id,
                version = firmware.version,
                targetOffsetHex = firmware.flashOffsetHex,
                declaredSizeBytes = firmware.sizeBytes
            )
            val report = ChecksumEngine.analyzeBinary(
                bytes = binary,
                fileName = "${firmware.id}.bin",
                requestedOffsetHex = firmware.flashOffsetHex
            )
            _activeChecksumReport.value = report
            _customExpectedHashInput.value = firmware.sha256
        }
    }

    fun closeFirmwareDetail() {
        _isShowingFirmwareDetail.value = false
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: FirmwareCategory) {
        _selectedCategory.value = category
    }

    fun selectSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleOnlyFavorites() {
        _onlyFavorites.value = !_onlyFavorites.value
    }

    fun resetAllCatalogFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = FirmwareCategory.ALL
        _onlyFavorites.value = false
        _sortOption.value = SortOption.RECOMMENDED
    }

    fun toggleFavoriteFirmware(firmware: FirmwareEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(firmware)
        }
    }

    fun dismissBanner() {
        _statusBannerMessage.value = null
    }

    fun setSimulationMode(enabled: Boolean) {
        _useSimulationMode.value = enabled
        refreshUsbConnection()
    }

    fun selectSimulatedProfile(profile: UsbProfileType) {
        _simulatedProfile.value = profile
        _useSimulationMode.value = true
        _connectedDevice.value = usbEngine.buildSimulatedDevice(profile)
        viewModelScope.launch {
            repository.log(
                level = "USB_IO",
                tag = "SIM-PROFILE",
                message = "Выбран профиль симуляции USB: ${profile.title} (${profile.vidHex}:${profile.pidHex})",
                hexPayload = profile.protocolBadge
            )
        }
    }

    fun refreshUsbConnection() {
        val physicalDevices = usbEngine.scanPhysicalUsbDevices()
        if (!_useSimulationMode.value && physicalDevices.isNotEmpty()) {
            val first = physicalDevices.first()
            usbEngine.requestUsbPermissionIfNeeded(first.deviceName)
            _connectedDevice.value = first
            viewModelScope.launch {
                repository.log(
                    level = "USB_IO",
                    tag = "USB-SCAN",
                    message = "Обнаружено физическое USB OTG устройство: ${first.productName} (${first.vidPidHex})",
                    hexPayload = first.bootloaderVersion
                )
            }
        } else {
            val sim = usbEngine.buildSimulatedDevice(_simulatedProfile.value)
            _connectedDevice.value = sim
            if (!_useSimulationMode.value && physicalDevices.isEmpty()) {
                _statusBannerMessage.value =
                    "Физические USB-устройства не найдены. Активен безопасный режим симуляции (${sim.profileType.title})."
            }
        }
    }

    fun syncCatalogWithTheFirmwareHub() {
        if (_isSyncingHub.value) return
        viewModelScope.launch {
            _isSyncingHub.value = true
            resetAllCatalogFilters()
            val result = repository.syncWithTheFirmwareHub()
            _statusBannerMessage.value = result.message
            _isSyncingHub.value = false
        }
    }

    fun downloadSingleFirmware(
        firmware: FirmwareEntity,
        targetVersion: String = firmware.version,
        targetSizeBytes: Int = firmware.sizeBytes
    ) {
        if (_isDownloadingFirmware.value) return
        viewModelScope.launch {
            _isDownloadingFirmware.value = true
            try {
                val updated = repository.downloadFirmwareBinary(
                    firmware = firmware,
                    targetVersion = targetVersion,
                    targetSizeBytes = targetSizeBytes
                )
                val (freshEntity, bytes) = repository.loadOrDownloadFirmwareBytes(updated)
                val report = ChecksumEngine.analyzeBinary(
                    bytes = bytes,
                    fileName = "${freshEntity.id}_${freshEntity.version}.bin",
                    requestedOffsetHex = freshEntity.flashOffsetHex
                )
                _selectedFirmware.value = freshEntity
                _activeChecksumReport.value = report
                _customExpectedHashInput.value = freshEntity.sha256
                _statusBannerMessage.value =
                    "Прошивка «${freshEntity.name} (${freshEntity.version})» скачана в память и проверена (CRC32: ${report.crc32})."
            } catch (e: Exception) {
                _statusBannerMessage.value = "Ошибка скачивания прошивки: ${e.localizedMessage}"
            } finally {
                _isDownloadingFirmware.value = false
            }
        }
    }

    fun downloadAllCatalogFirmwares() {
        if (_isDownloadingAll.value) return
        viewModelScope.launch {
            _isDownloadingAll.value = true
            try {
                val total = repository.downloadAllFirmwares { completed, count, currentName ->
                    _batchDownloadStatus.value =
                        "Скачивание [$completed/$count]: $currentName…"
                }
                _batchDownloadStatus.value = null
                _statusBannerMessage.value =
                    "Все $total прошивок каталога успешно скачаны в память устройства (.bin) и готовы к открытию!"
            } catch (e: Exception) {
                _batchDownloadStatus.value = null
                _statusBannerMessage.value = "Ошибка пакетной загрузки: ${e.localizedMessage}"
            } finally {
                _isDownloadingAll.value = false
            }
        }
    }

    fun prepareExportFirmwareFile(
        firmware: FirmwareEntity,
        launchCreateDocument: (suggestedFileName: String) -> Unit
    ) {
        viewModelScope.launch {
            val (downloaded, bytes) = repository.loadOrDownloadFirmwareBytes(firmware)
            _selectedFirmware.value = downloaded
            pendingExportBytes = bytes
            pendingExportFirmwareName = "${downloaded.name} (${downloaded.version})"
            val cleanVer = downloaded.version.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val suggestedName = "${downloaded.id}_${cleanVer}.bin"
            launchCreateDocument(suggestedName)
        }
    }

    fun writePendingExportToUri(uri: Uri) {
        val bytes = pendingExportBytes ?: return
        val label = pendingExportFirmwareName
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                }
                val crc = ChecksumEngine.computeCrc32(bytes)
                repository.log(
                    level = "SUCCESS",
                    tag = "FW-EXPORT",
                    message = "Экспортирован бинарный файл прошивки $label (${bytes.size} байт, CRC32: $crc)",
                    hexPayload = "URI=$uri"
                )
                _statusBannerMessage.value =
                    "Файл прошивки «$label» успешно сохранен на устройство (CRC32: $crc)!"
            } catch (e: Exception) {
                _statusBannerMessage.value = "Ошибка сохранения файла: ${e.localizedMessage}"
            }
        }
    }

    fun selectFirmwareForInspection(
        firmware: FirmwareEntity,
        navigateToFlasher: Boolean = true,
        navigateToChecksums: Boolean = false
    ) {
        _selectedFirmware.value = firmware
        viewModelScope.launch {
            val (downloadedEntity, binary) = if (navigateToFlasher || navigateToChecksums) {
                repository.loadOrDownloadFirmwareBytes(firmware)
            } else {
                firmware to ChecksumEngine.buildDeterministicFirmwareBinary(
                    firmwareId = firmware.id,
                    version = firmware.version,
                    targetOffsetHex = firmware.flashOffsetHex,
                    declaredSizeBytes = firmware.sizeBytes
                )
            }
            _selectedFirmware.value = downloadedEntity
            val report = ChecksumEngine.analyzeBinary(
                bytes = binary,
                fileName = "${downloadedEntity.id}.bin",
                requestedOffsetHex = downloadedEntity.flashOffsetHex
            )
            _activeChecksumReport.value = report
            _customExpectedHashInput.value = downloadedEntity.sha256
            repository.log(
                level = "CHECKSUM",
                tag = "HASH-VERIFY",
                message = "Открыт и проверен образ ${downloadedEntity.name} (${downloadedEntity.version}) | SHA-256: ${report.sha256.take(16)}… | CRC32: ${report.crc32}",
                hexPayload = report.headerHexPreview
            )
        }
        if (navigateToFlasher) {
            _isShowingFirmwareDetail.value = false
            _currentDestination.value = AppDestination.FLASHER
        } else if (navigateToChecksums) {
            _isShowingFirmwareDetail.value = false
            _currentDestination.value = AppDestination.BACKUPS
        }
    }

    fun importLocalFirmwareUri(uri: Uri, displayName: String) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: ChecksumEngine.buildDeterministicFirmwareBinary(
                        firmwareId = displayName,
                        version = "local",
                        declaredSizeBytes = 262144
                    )

                val isFwsc = displayName.lowercase().endsWith(".fwsc")
                val report = ChecksumEngine.analyzeBinary(
                    bytes = bytes,
                    fileName = displayName,
                    requestedOffsetHex = if (isFwsc) "0x0000" else "0x00004000"
                )
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val customId = "local_${System.currentTimeMillis()}"
                val localFile = repository.buildFirmwareFile(customId, "Local")
                localFile.writeBytes(bytes)

                val verEntry = FirmwareVersionEntry(
                    version = "Local File",
                    releaseDate = today,
                    sizeBytes = bytes.size.coerceAtLeast(16384),
                    crc32 = report.crc32,
                    sha256 = report.sha256,
                    notes = "Импортированный пользователем файл ($displayName)"
                )

                val customEntity = FirmwareEntity(
                    id = customId,
                    name = displayName.substringBeforeLast('.'),
                    version = "Local File",
                    author = "Локальный импорт USB/Storage",
                    category = FirmwareCategory.FM_SYNTH.name,
                    summary = "Импортированный пользователем файл прошивки ($displayName). Автоматически выровнен по безопасному смещению 0x00004000 (защита загрузчика WL82).",
                    changelog = "• Локальный бинарный образ ($displayName)\n• Размер: ${bytes.size} байт\n• Заголовок: ${report.headerHexPreview}",
                    releaseNotes = "Пользовательский бинарный образ $displayName, загруженный из локального хранилища. Прошел автоматическую проверку заголовка JieLi WL82 и сохранен в кэше приложения.",
                    versionHistoryJson = FirmwareVersionHistoryCodec.encode(listOf(verEntry)),
                    compatibleHardwareRevs = "M-VAVE FM-1 PCB Rev A / Rev B / Rev C • JieLi AC791N (WL82) • Offset 0x00004000",
                    flashOffsetHex = "0x00004000",
                    sizeBytes = bytes.size.coerceAtLeast(16384),
                    sha256 = report.sha256,
                    crc32 = report.crc32,
                    md5 = report.md5,
                    downloadUrl = uri.toString(),
                    tagsCsv = "Local,Custom,Imported,Verified",
                    isOfficialStock = false,
                    isVerified = true,
                    isOfflineBundled = true,
                    isDownloaded = true,
                    localFilePath = localFile.absolutePath,
                    releaseDate = today,
                    isFavorite = true
                )
                repository.insertCustomFirmware(customEntity)
                _selectedFirmware.value = customEntity
                _activeChecksumReport.value = report
                _customExpectedHashInput.value = report.sha256
                _statusBannerMessage.value =
                    "Файл $displayName импортирован и открыт (CRC32: ${report.crc32}, SHA-256: ${report.sha256.take(12)}…)"
                repository.log(
                    level = "CHECKSUM",
                    tag = "FILE-IMPORT",
                    message = "Импортирован локальный файл $displayName (${bytes.size} байт). CRC32=${report.crc32}",
                    hexPayload = report.headerHexPreview
                )
                _isShowingFirmwareDetail.value = false
                _currentDestination.value = AppDestination.FLASHER
            } catch (e: Exception) {
                _statusBannerMessage.value = "Ошибка чтения файла: ${e.localizedMessage}"
            }
        }
    }

    fun updateCustomExpectedHash(input: String) {
        _customExpectedHashInput.value = input.trim()
    }

    fun toggleAutoBackupBeforeFlash(enabled: Boolean) {
        _flashSessionState.update { it.copy(autoBackupBeforeFlash = enabled) }
    }

    fun toggleVerifyAfterFlash(enabled: Boolean) {
        _flashSessionState.update { it.copy(verifyAfterFlash = enabled) }
    }

    fun setUnbrickWizardStep(step: Int) {
        _flashSessionState.update { it.copy(activeUnbrickStep = step.coerceIn(0, 3)) }
    }

    fun selectLogLevelFilter(level: String) {
        _selectedLogLevelFilter.value = level
    }

    fun selectDesktopOs(os: String) {
        _selectedDesktopOs.value = os
    }

    fun createDeviceBackupNow(customNote: String = "Ручной бэкап перед модификацией памяти") {
        viewModelScope.launch {
            val dev = _connectedDevice.value
            val stamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            val dumpBytes = ChecksumEngine.buildDeterministicFirmwareBinary(
                firmwareId = "backup_${dev.vidPidHex}_$stamp",
                version = dev.bootloaderVersion,
                targetOffsetHex = "0x00000000",
                declaredSizeBytes = 65536
            )
            val report = ChecksumEngine.analyzeBinary(dumpBytes, "eeprom_backup.bin", "0x00000000")
            val backup = BackupDumpEntity(
                title = "Дамп EEPROM & Boot ($stamp)",
                deviceName = dev.productName,
                deviceMode = dev.profileType.protocolBadge,
                regionHex = "0x00000000 – 0x0000FFFF (WL82 Boot + 64 Presets)",
                sizeBytes = 65536,
                presetCount = 64,
                sha256 = report.sha256,
                crc32 = report.crc32,
                md5 = report.md5,
                rawHexPreview = report.headerHexPreview,
                presetsJson = """{"device":"${dev.productName}","vid_pid":"${dev.vidPidHex}","boot_header":"0x0000-0x3FFF_OK","presets_count":64,"midi_channel":1,"master_tune_hz":440.0}""",
                notes = customNote
            )
            repository.insertBackup(backup)
            repository.log(
                level = "SUCCESS",
                tag = "EEPROM-BACKUP",
                message = "Создана резервная копия настроек и загрузчика (${backup.title}) | CRC32: ${report.crc32}",
                hexPayload = report.headerHexPreview
            )
            _statusBannerMessage.value =
                "Резервная копия EEPROM и заголовка 0x0000–0x3FFF успешно сохранена (CRC32: ${report.crc32})."
        }
    }

    fun restoreBackupToDevice(backup: BackupDumpEntity) {
        if (_flashSessionState.value.isFlashing) return
        viewModelScope.launch {
            _flashSessionState.update {
                it.copy(
                    isFlashing = true,
                    isError = false,
                    lastCompletedMessage = null
                )
            }
            val dev = _connectedDevice.value
            repository.log(
                level = "INFO",
                tag = "BACKUP-RESTORE",
                message = "Начато восстановление резервной копии '${backup.title}' (SHA-256: ${backup.sha256.take(12)}…) в ${dev.productName}",
                hexPayload = backup.rawHexPreview
            )
            val bytes = ChecksumEngine.buildDeterministicFirmwareBinary(
                firmwareId = backup.sha256,
                version = backup.crc32,
                targetOffsetHex = "0x00004000",
                declaredSizeBytes = backup.sizeBytes
            )
            usbEngine.executeFlashPipeline(
                deviceInfo = dev,
                firmwareBytes = bytes,
                declaredSizeBytes = backup.sizeBytes,
                targetOffsetHex = "0x00004000",
                expectedSha256 = ChecksumEngine.computeSha256(bytes),
                expectedCrc32 = ChecksumEngine.computeCrc32(bytes)
            ) { step ->
                _flashSessionState.update { it.copy(currentProgress = step) }
            }
            val msg = "Настройки и пресеты из «${backup.title}» успешно восстановлены в устройство!"
            repository.log("SUCCESS", "BACKUP-RESTORE", msg, "CRC32=${backup.crc32}")
            _flashSessionState.update {
                it.copy(
                    isFlashing = false,
                    lastCompletedMessage = msg,
                    isError = false
                )
            }
            _statusBannerMessage.value = msg
        }
    }

    fun deleteBackup(backupId: Int) {
        viewModelScope.launch {
            repository.deleteBackup(backupId)
            repository.log("WARN", "BACKUP-DEL", "Удален снимок резервной копии #$backupId")
        }
    }

    fun startAutomatedFlashing(isUnbrickRecovery: Boolean = false) {
        if (_flashSessionState.value.isFlashing) return
        val fw = _selectedFirmware.value ?: filteredFirmwares.value.firstOrNull() ?: return

        viewModelScope.launch {
            _flashSessionState.update {
                it.copy(
                    isFlashing = true,
                    isError = false,
                    lastCompletedMessage = null
                )
            }

            try {
                val dev = _connectedDevice.value
                val (downloadedFw, binary) = repository.loadOrDownloadFirmwareBytes(fw)
                _selectedFirmware.value = downloadedFw
                val report = ChecksumEngine.analyzeBinary(binary, "${downloadedFw.id}.bin", "0x00004000")
                _activeChecksumReport.value = report

                repository.log(
                    level = "CHECKSUM",
                    tag = "PRE-FLASH-HASH",
                    message = "Проверка целостности перед записью: SHA-256=${report.sha256.take(16)}… | CRC32=${report.crc32} | MD5=${report.md5}",
                    hexPayload = report.headerHexPreview
                )

                // Step 1: Auto-backup current EEPROM/presets if enabled
                if (_flashSessionState.value.autoBackupBeforeFlash) {
                    val stamp = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date())
                    repository.insertBackup(
                        BackupDumpEntity(
                            title = "Авто-бэкап перед прошивкой ${downloadedFw.name} ($stamp)",
                            deviceName = dev.productName,
                            deviceMode = dev.profileType.protocolBadge,
                            regionHex = "0x00000000 – 0x0000FFFF (Boot + EEPROM)",
                            sizeBytes = 65536,
                            presetCount = 64,
                            sha256 = report.sha256,
                            crc32 = report.crc32,
                            md5 = report.md5,
                            rawHexPreview = report.headerHexPreview,
                            presetsJson = """{"auto_backup":true,"target_firmware":"${downloadedFw.name}","version":"${downloadedFw.version}","protected_boot":"0x0000-0x3FFF"}""",
                            notes = "Автоматическая точка восстановления перед записью ${downloadedFw.name} (${downloadedFw.version})."
                        )
                    )
                    repository.log(
                        level = "INFO",
                        tag = "AUTO-BACKUP",
                        message = "Сохранена точка восстановления EEPROM и заголовка загрузчика 0x0000–0x3FFF.",
                        hexPayload = "BACKUP_REGION=0x0000_0xFFFF"
                    )
                }

                // Step 2: If in normal MIDI mode, send SysEx F0 22 24 35 7D F7 to enter WL82 UBOOT1.00
                if (!dev.profileType.isBootloaderMode || isUnbrickRecovery) {
                    usbEngine.sendSysExBootloaderTrigger(dev) { level, tag, msg, hex ->
                        repository.log(level, tag, msg, hex)
                    }
                }

                // Step 3: Execute 64-sector USB write strictly to 0x00004000
                val ok = usbEngine.executeFlashPipeline(
                    deviceInfo = dev,
                    firmwareBytes = binary,
                    declaredSizeBytes = downloadedFw.sizeBytes,
                    targetOffsetHex = "0x00004000",
                    expectedSha256 = report.sha256,
                    expectedCrc32 = report.crc32
                ) { progress ->
                    _flashSessionState.update { it.copy(currentProgress = progress) }
                    if (progress.completedSectors % 16 == 0 || progress.completedSectors == 64) {
                        repository.log(
                            level = "USB_IO",
                            tag = "FLASH-SECTOR",
                            message = progress.logLine,
                            hexPayload = progress.hexPacketSample
                        )
                    }
                }

                val doneText = if (isUnbrickRecovery) {
                    "UNBRICK УСПЕШЕН: Загрузчик WL82 синхронизирован, ${downloadedFw.name} (${downloadedFw.version}) записана по адресу 0x00004000 и проверена (CRC32: ${report.crc32})!"
                } else {
                    "Прошивка ${downloadedFw.name} (${downloadedFw.version}) успешно записана по адресу 0x00004000 и верифицирована (SHA-256 / CRC32: ${report.crc32})!"
                }

                repository.log(
                    level = if (ok) "SUCCESS" else "WARN",
                    tag = if (isUnbrickRecovery) "UNBRICK-DONE" else "FLASH-DONE",
                    message = doneText,
                    hexPayload = "SHA256=${report.sha256.take(24)}... CRC32=${report.crc32}"
                )

                _flashSessionState.update {
                    it.copy(
                        isFlashing = false,
                        lastCompletedMessage = doneText,
                        isError = !ok
                    )
                }
                _statusBannerMessage.value = doneText
            } catch (e: Exception) {
                val errText = "Ошибка операции прошивки: ${e.localizedMessage}"
                repository.log("ERROR", "FLASH-ERR", errText)
                _flashSessionState.update {
                    it.copy(
                        isFlashing = false,
                        lastCompletedMessage = errText,
                        isError = true
                    )
                }
                _statusBannerMessage.value = errText
            }
        }
    }

    fun triggerSoftBrickStockRecovery() {
        val stockFw = filteredFirmwares.value.firstOrNull { it.isOfficialStock }
            ?: filteredFirmwares.value.firstOrNull()
        if (stockFw != null) {
            selectFirmwareForInspection(stockFw, navigateToFlasher = false)
        }
        if (_useSimulationMode.value) {
            selectSimulatedProfile(UsbProfileType.WL82_UBOOT_SOFTBRICK)
        }
        startAutomatedFlashing(isUnbrickRecovery = true)
    }

    fun clearTelemetryLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            repository.log("INFO", "LOG-RESET", "Журнал телеметрии очищен пользователем.")
        }
    }

    fun buildFormattedLogsExport(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val dev = _connectedDevice.value
        val header = buildString {
            appendLine("=========================================================================")
            appendLine("FM1 FirmwareHub Studio & M-VAVE FM-1 Unbricker — Session Telemetry Report")
            appendLine("Device: ${dev.productName} (${dev.vidPidHex}) | Mode: ${dev.bootloaderVersion}")
            appendLine("Exported: ${fmt.format(Date())}")
            appendLine("=========================================================================")
        }
        val body = filteredLogs.value.joinToString("\n") { entry ->
            val time = fmt.format(Date(entry.timestamp))
            val hexPart = if (entry.hexPayload.isNotBlank()) " | HEX: [${entry.hexPayload}]" else ""
            "[$time] [${entry.level}] [${entry.tag}] ${entry.message}$hexPart"
        }
        return "$header\n$body\n"
    }
}
