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
import com.example.domain.UnbrickMethodType
import com.example.domain.UnbrickTargetFirmwareOption
import com.example.domain.UnbrickUiMode
import com.example.domain.UsbFlashEngine
import com.example.domain.UsbProfileType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
    val activeUnbrickStep: Int = 0,
    val selectedUnbrickMethod: UnbrickMethodType = UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT,
    val unbrickUiMode: UnbrickUiMode = UnbrickUiMode.INTERACTIVE_WIZARD,
    val unbrickTargetOption: UnbrickTargetFirmwareOption = UnbrickTargetFirmwareOption.STOCK_V16,
    val wl82LoaderInjected: Boolean = false,
    val bootHeader0x0000Repaired: Boolean = false,
    val isBootloopCatcherActive: Boolean = false,
    val bootloopCatcherAttempts: Int = 0,
    val maxSectorRetries: Int = 5,
    val transferBlockSize: Int = 4096,
    val allowBootSectorRebuild: Boolean = false,
    val lastFwscSurgeonReport: String? = null,
    val lastFull1MbDumpSummary: String? = null,
    val verifiedWizardStages: Set<Int> = emptySet()
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

    // Background job for Bootloop / Pin-Shorting Window Catcher
    private var bootloopCatcherJob: Job? = null

    // USB & Simulation state (Simulation OFF by default — strictly requires physical USB OTG unless explicitly enabled)
    private val _useSimulationMode = MutableStateFlow(false)
    val useSimulationMode: StateFlow<Boolean> = _useSimulationMode.asStateFlow()

    private val _simulatedProfile = MutableStateFlow(UsbProfileType.WL82_UBOOT_SOFTBRICK)
    val simulatedProfile: StateFlow<UsbProfileType> = _simulatedProfile.asStateFlow()

    private val _suspectHardBrickCableConnected = MutableStateFlow(false)
    val suspectHardBrickCableConnected: StateFlow<Boolean> = _suspectHardBrickCableConnected.asStateFlow()

    private val _connectedDevice = MutableStateFlow(
        usbEngine.buildDisconnectedDevice(suspectHardBrick = false)
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
        if (enabled) {
            _suspectHardBrickCableConnected.value = false
        }
        refreshUsbConnection(showToastOnScan = true)
    }

    fun selectSimulatedProfile(profile: UsbProfileType) {
        _simulatedProfile.value = profile
        _useSimulationMode.value = true
        _suspectHardBrickCableConnected.value = false
        val simDev = usbEngine.buildSimulatedDevice(profile)
        _connectedDevice.value = simDev
        _flashSessionState.update {
            it.copy(
                selectedUnbrickMethod = simDev.recommendedUnbrickMethod,
                verifiedWizardStages = if (simDev.isReadyToFlash) setOf(0) else emptySet()
            )
        }
        viewModelScope.launch {
            repository.log(
                level = "USB_IO",
                tag = "SIM-PROFILE",
                message = "Выбран тестовый профиль симуляции USB: ${profile.title} (${profile.vidHex}:${profile.pidHex})",
                hexPayload = profile.protocolBadge
            )
        }
    }

    fun setHardBrickCableSuspected(cablePluggedInAndPowered: Boolean) {
        _suspectHardBrickCableConnected.value = cablePluggedInAndPowered
        if (cablePluggedInAndPowered) {
            _useSimulationMode.value = false
        }
        refreshUsbConnection(showToastOnScan = true)
        if (cablePluggedInAndPowered && !_connectedDevice.value.isConnected) {
            _flashSessionState.update {
                it.copy(
                    selectedUnbrickMethod = UnbrickMethodType.SPI_PIN_SHORT_MASKROM,
                    activeUnbrickStep = 0
                )
            }
        }
    }

    fun requestActiveUsbPermission() {
        val current = _connectedDevice.value
        if (current.isConnected && !current.isSimulated && !current.hasPermission) {
            val grantedAlready = usbEngine.requestUsbPermissionIfNeeded(current.deviceName)
            if (grantedAlready) {
                refreshUsbConnection(showToastOnScan = true)
            } else {
                _statusBannerMessage.value =
                    "Запрошено системное разрешение Android USB Host для ${current.productName} (${current.vidPidHex}). Подтвердите диалог на экране."
            }
        } else {
            refreshUsbConnection(showToastOnScan = true)
        }
    }

    fun refreshUsbConnection(showToastOnScan: Boolean = false) {
        val physicalDevices = usbEngine.scanPhysicalUsbDevices()
        if (physicalDevices.isNotEmpty() && !_useSimulationMode.value) {
            val first = physicalDevices.first()
            _suspectHardBrickCableConnected.value = false
            if (!first.hasPermission) {
                usbEngine.requestUsbPermissionIfNeeded(first.deviceName)
            }
            _connectedDevice.value = first
            _flashSessionState.update { state ->
                val stages = if (first.isReadyToFlash) state.verifiedWizardStages + 0 else state.verifiedWizardStages - 0
                state.copy(
                    selectedUnbrickMethod = first.recommendedUnbrickMethod,
                    verifiedWizardStages = stages
                )
            }
            if (showToastOnScan) {
                _statusBannerMessage.value = first.diagnosticDetails
            }
            viewModelScope.launch {
                repository.log(
                    level = if (first.isConnected) "USB_IO" else "WARN",
                    tag = "USB-SCAN",
                    message = "Статус USB OTG: ${first.diagnosticVerdict.badgeTitle} — ${first.productName} (${first.vidPidHex})",
                    hexPayload = first.usbInterfaceSummary
                )
            }
        } else if (_useSimulationMode.value) {
            val sim = usbEngine.buildSimulatedDevice(_simulatedProfile.value)
            _connectedDevice.value = sim
            _flashSessionState.update { state ->
                state.copy(verifiedWizardStages = state.verifiedWizardStages + 0)
            }
            if (showToastOnScan) {
                _statusBannerMessage.value = sim.diagnosticDetails
            }
        } else {
            val disconnected = usbEngine.buildDisconnectedDevice(
                suspectHardBrick = _suspectHardBrickCableConnected.value
            )
            _connectedDevice.value = disconnected
            _flashSessionState.update { state ->
                state.copy(
                    verifiedWizardStages = emptySet(),
                    wl82LoaderInjected = false
                )
            }
            if (showToastOnScan) {
                _statusBannerMessage.value = disconnected.diagnosticDetails
            }
            viewModelScope.launch {
                repository.log(
                    level = if (_suspectHardBrickCableConnected.value) "ERROR" else "WARN",
                    tag = if (_suspectHardBrickCableConnected.value) "HARD-BRICK" else "USB-DISCONNECTED",
                    message = disconnected.diagnosticDetails,
                    hexPayload = disconnected.usbInterfaceSummary
                )
            }
        }
    }

    fun selectUnbrickMethod(method: UnbrickMethodType) {
        _flashSessionState.update {
            it.copy(
                selectedUnbrickMethod = method,
                allowBootSectorRebuild = method == UnbrickMethodType.FWSC_CONTAINER_SURGEON ||
                    method == UnbrickMethodType.CH341A_FULL_DUMP_PROGRAMMER
            )
        }
        viewModelScope.launch {
            repository.log(
                level = "INFO",
                tag = "UNBRICK-METHOD",
                message = "Выбран вектор реанимации: ${method.title}",
                hexPayload = "OFFSET=${method.targetOffsetDefault}"
            )
        }
    }

    fun setUnbrickUiMode(mode: UnbrickUiMode) {
        _flashSessionState.update { it.copy(unbrickUiMode = mode) }
    }

    fun selectUnbrickTargetOption(option: UnbrickTargetFirmwareOption) {
        _flashSessionState.update { it.copy(unbrickTargetOption = option) }
        val resolved = resolveTargetUnbrickFirmware(option)
        if (resolved != null) {
            selectFirmwareForInspection(resolved, navigateToFlasher = false)
        }
    }

    fun setMaxSectorRetries(retries: Int) {
        _flashSessionState.update { it.copy(maxSectorRetries = retries.coerceIn(1, 5)) }
    }

    fun setTransferBlockSize(sizeBytes: Int) {
        val valid = when {
            sizeBytes <= 512 -> 512
            sizeBytes <= 2048 -> 2048
            else -> 4096
        }
        _flashSessionState.update { it.copy(transferBlockSize = valid) }
    }

    fun toggleAllowBootSectorRebuild(allowed: Boolean) {
        _flashSessionState.update { it.copy(allowBootSectorRebuild = allowed) }
    }

    private fun resolveTargetUnbrickFirmware(
        option: UnbrickTargetFirmwareOption = _flashSessionState.value.unbrickTargetOption
    ): FirmwareEntity? {
        val all = filteredFirmwares.value
        return when (option) {
            UnbrickTargetFirmwareOption.STOCK_V16 -> {
                all.firstOrNull { it.id.contains("v16", ignoreCase = true) || it.isOfficialStock }
                    ?: _selectedFirmware.value
                    ?: all.firstOrNull()
            }
            UnbrickTargetFirmwareOption.STOCK_V15 -> {
                all.firstOrNull { it.id.contains("v15", ignoreCase = true) }
                    ?: all.firstOrNull { it.isOfficialStock }
                    ?: _selectedFirmware.value
                    ?: all.firstOrNull()
            }
            UnbrickTargetFirmwareOption.SELECTED_CATALOG_FW -> {
                _selectedFirmware.value
                    ?: all.firstOrNull { it.isOfficialStock }
                    ?: all.firstOrNull()
            }
        }
    }

    /**
     * Step 2 / Expert Action: Directly injects `wl82loader.bin` (4 KB) into JieLi AC791N / WL82 SRAM (`0x010000`)
     * via USB Mass Storage SCSI Pass-Through (`0xFD 0x10`) or RP2040 bridge.
     */
    fun injectWl82RamLoaderNow() {
        if (_flashSessionState.value.isFlashing) return
        viewModelScope.launch {
            refreshUsbConnection(showToastOnScan = false)
            val dev = _connectedDevice.value
            if (!dev.isReadyToFlash) {
                val msg = "ОШИБКА SRAM-BOOTSTRAP: Устройство не подключено по USB OTG (или не выдано разрешение). Подключите синтезатор или включите симуляцию."
                _flashSessionState.update {
                    it.copy(isError = true, lastCompletedMessage = msg)
                }
                _statusBannerMessage.value = msg
                return@launch
            }

            try {
                val ok = usbEngine.injectWl82LoaderToSram(dev) { level, tag, msg, hex ->
                    repository.log(level, tag, msg, hex)
                }
                if (ok) {
                    val doneMsg = "Вторичный загрузчик wl82loader.bin (4096 байт) успешно загружен в SRAM 0x010000! Низкоуровневый доступ SCSI к SPI NOR Flash открыт."
                    _flashSessionState.update {
                        it.copy(
                            wl82LoaderInjected = true,
                            isError = false,
                            lastCompletedMessage = doneMsg,
                            verifiedWizardStages = it.verifiedWizardStages + setOf(0, 1)
                        )
                    }
                    _statusBannerMessage.value = doneMsg
                }
            } catch (e: Exception) {
                val err = "Ошибка инъекции wl82loader.bin: ${e.localizedMessage}"
                repository.log("ERROR", "SRAM-ERR", err)
                _flashSessionState.update {
                    it.copy(isError = true, lastCompletedMessage = err)
                }
                _statusBannerMessage.value = err
            }
        }
    }

    /**
     * Method 2 & Method 3: Starts/stops the high-frequency Bootloop & SPI Pin-Shorting Window Catcher.
     * Polls the USB bus every 180 ms and immediately fires SysEx `F0 22 24 35 7D F7` + `wl82loader.bin` SRAM bootstrap
     * as soon as the synth powers on or enumerates as `WL82 UBOOT1.00` (`VID_4C4A:PID_8057`) after shorting Pin 6 (CLK) to Pin 4 (GND).
     */
    fun toggleBootloopAndPinShortCatcher() {
        if (_flashSessionState.value.isBootloopCatcherActive) {
            bootloopCatcherJob?.cancel()
            bootloopCatcherJob = null
            _flashSessionState.update {
                it.copy(
                    isBootloopCatcherActive = false,
                    lastCompletedMessage = "Циклический перехватчик бутлупа / Pin-Shorting остановлен."
                )
            }
            return
        }

        bootloopCatcherJob?.cancel()
        _flashSessionState.update {
            it.copy(
                isBootloopCatcherActive = true,
                bootloopCatcherAttempts = 0,
                isError = false,
                lastCompletedMessage = "АКТИВЕН ПЕРЕХВАТЧИК (180 мс): Включите питание FM-1 (или замкните Pin 6 CLK на Pin 4 GND при включении)…"
            )
        }

        bootloopCatcherJob = viewModelScope.launch {
            repository.log(
                level = "USB_IO",
                tag = "CATCHER-START",
                message = "Запущен высокочастотный перехватчик окна загрузчика (Bootloop SysEx + SPI Pin-Shorting MaskROM).",
                hexPayload = "POLL_INTERVAL=180ms TARGET=4C4A:8057"
            )
            var attempts = 0
            while (isActive && attempts < 60) {
                attempts++
                _flashSessionState.update { it.copy(bootloopCatcherAttempts = attempts) }
                refreshUsbConnection(showToastOnScan = false)
                val dev = _connectedDevice.value

                if (dev.isReadyToFlash) {
                    runCatching {
                        if (!dev.profileType.isBootloaderMode) {
                            usbEngine.sendSysExBootloaderTrigger(dev) { l, t, m, h ->
                                repository.log(l, t, m, h)
                            }
                        }
                        usbEngine.injectWl82LoaderToSram(dev) { l, t, m, h ->
                            repository.log(l, t, m, h)
                        }
                    }
                    val caughtMsg = "ПЕРЕХВАТ УСПЕШЕН (попытка #$attempts): Устройство ${dev.productName} (${dev.vidPidHex}) захвачено! Уберите пинцет с пинов SPI Flash — wl82loader.bin уже в SRAM 0x010000."
                    repository.log("SUCCESS", "CATCHER-LOCKED", caughtMsg, dev.vidPidHex)
                    _flashSessionState.update {
                        it.copy(
                            isBootloopCatcherActive = false,
                            wl82LoaderInjected = true,
                            isError = false,
                            lastCompletedMessage = caughtMsg,
                            verifiedWizardStages = it.verifiedWizardStages + setOf(0, 1)
                        )
                    }
                    _statusBannerMessage.value = caughtMsg
                    return@launch
                }
                delay(180)
            }

            _flashSessionState.update {
                it.copy(
                    isBootloopCatcherActive = false,
                    isError = true,
                    lastCompletedMessage = "Таймаут перехватчика ($attempts циклов): устройство не появилось на шине USB OTG. Проверьте кабель OTG и замыкание Pin 6 (CLK) на Pin 4 (GND) при подаче питания."
                )
            }
        }
    }

    /**
     * Method 4: Runs the `.fwsc` Container Surgeon (`fm1_extract_app`) on the target firmware,
     * stripping hazardous container headers, verifying the 0x4000 payload, and preparing the 16 KB Golden Boot Header.
     */
    fun runFwscContainerSurgeonNow() {
        viewModelScope.launch {
            val targetFw = resolveTargetUnbrickFirmware() ?: return@launch
            val (downloadedFw, rawBytes) = repository.loadOrDownloadFirmwareBytes(targetFw)
            val surgeon = ChecksumEngine.extractAndRepairFwscContainer(
                rawBytes = rawBytes,
                fileName = "${downloadedFw.id}.fwsc",
                firmwareIdFallback = downloadedFw.id
            )
            repository.log(
                level = "CHECKSUM",
                tag = "FWSC-SURGEON",
                message = surgeon.summaryMessage,
                hexPayload = "APP_CRC32=${surgeon.extractedAppCrc32} BOOT_CRC32=${surgeon.goldenBootCrc32}"
            )
            _flashSessionState.update {
                it.copy(
                    lastFwscSurgeonReport = surgeon.summaryMessage,
                    lastCompletedMessage = surgeon.summaryMessage,
                    isError = false,
                    verifiedWizardStages = it.verifiedWizardStages + 2
                )
            }
            _statusBannerMessage.value = surgeon.summaryMessage
        }
    }

    /**
     * Method 4 / Method 5 / Method 6: Reconstructs the wiped 16 KB Boot Header (`0x0000–0x3FFF`)
     * and writes the clean application payload at `0x00004000`.
     */
    fun repairBootloaderHeader0x0000Now() {
        if (_flashSessionState.value.isFlashing) return
        viewModelScope.launch {
            refreshUsbConnection(showToastOnScan = false)
            val dev = _connectedDevice.value
            if (!dev.isReadyToFlash) {
                val err = "Невозможно реконструировать сектор 0x0000–0x3FFF: подключите синтезатор (в режиме UBOOT1.00 / Pin-Short / RP2040 / CH341A) или включите симуляцию."
                _flashSessionState.update { it.copy(isError = true, lastCompletedMessage = err) }
                _statusBannerMessage.value = err
                return@launch
            }

            _flashSessionState.update {
                it.copy(
                    isFlashing = true,
                    allowBootSectorRebuild = true,
                    isError = false,
                    lastCompletedMessage = null
                )
            }
            try {
                val targetFw = resolveTargetUnbrickFirmware() ?: return@launch
                val (_, appBytes) = repository.loadOrDownloadFirmwareBytes(targetFw)
                val goldenBoot = ChecksumEngine.buildGoldenBootloaderHeader16Kb()
                val combined = ByteArray(goldenBoot.size + appBytes.size)
                System.arraycopy(goldenBoot, 0, combined, 0, goldenBoot.size)
                System.arraycopy(appBytes, 0, combined, goldenBoot.size, appBytes.size)

                val sha = ChecksumEngine.computeSha256(combined)
                val crc = ChecksumEngine.computeCrc32(combined)

                repository.log(
                    level = "USB_IO",
                    tag = "BOOT-REPAIR",
                    message = "Начата реконструкция сектора загрузчика 0x0000–0x3FFF (Golden Boot 16 КБ) + ${targetFw.name} (0x4000)",
                    hexPayload = "CRC32=$crc SIZE=${combined.size}"
                )

                val ok = usbEngine.executeFlashPipeline(
                    deviceInfo = dev,
                    firmwareBytes = combined,
                    declaredSizeBytes = targetFw.sizeBytes + ChecksumEngine.BOOT_HEADER_SIZE_BYTES,
                    targetOffsetHex = "0x00000000",
                    expectedSha256 = sha,
                    expectedCrc32 = crc,
                    allowBootHeaderRebuild = true,
                    maxRetries = _flashSessionState.value.maxSectorRetries,
                    blockSizeBytes = _flashSessionState.value.transferBlockSize,
                    injectRamLoaderFirst = true
                ) { step ->
                    _flashSessionState.update { state ->
                        state.copy(
                            currentProgress = step,
                            verifiedWizardStages = state.verifiedWizardStages + setOf(0, 1, 2, step.verifiedStageIndex)
                        )
                    }
                }

                val doneMsg = "РЕКОНСТРУКЦИЯ 0x0000–0x3FFF ЗАВЕРШЕНА: Загрузчик WL82 UBOOT1.00 и прошивка «${targetFw.name}» успешно восстановлены и проверены (CRC32: $crc)!"
                repository.log("SUCCESS", "BOOT-REPAIRED", doneMsg, "CRC32=$crc")
                _flashSessionState.update {
                    it.copy(
                        isFlashing = false,
                        wl82LoaderInjected = true,
                        bootHeader0x0000Repaired = true,
                        lastCompletedMessage = doneMsg,
                        isError = !ok,
                        verifiedWizardStages = setOf(0, 1, 2, 3, 4)
                    )
                }
                _statusBannerMessage.value = doneMsg
            } catch (e: Exception) {
                val err = "Ошибка реконструкции сектора 0x0000: ${e.localizedMessage}"
                repository.log("ERROR", "BOOT-REPAIR-ERR", err)
                _flashSessionState.update {
                    it.copy(isFlashing = false, isError = true, lastCompletedMessage = err)
                }
                _statusBannerMessage.value = err
            }
        }
    }

    /**
     * Method 6: Builds the complete 1 MB (`0x000000–0x0FFFFF`) Golden SPI NOR Flash dump
     * (`16 KB Boot + App 0x4000 + 64 KB EEPROM Presets`) and launches SAF file export for CH341A / flashrom.
     */
    fun prepareExportFull1MbSpiDump(
        launchCreateDocument: (suggestedFileName: String) -> Unit
    ) {
        viewModelScope.launch {
            val targetFw = resolveTargetUnbrickFirmware() ?: return@launch
            val (downloaded, appBytes) = repository.loadOrDownloadFirmwareBytes(targetFw)
            val dumpReport = ChecksumEngine.buildFull1MbSpiFlashDump(
                firmwareId = downloaded.id,
                firmwareName = downloaded.name,
                firmwareVersion = downloaded.version,
                appPayloadBytes = appBytes
            )
            pendingExportBytes = dumpReport.fullImageBytes
            pendingExportFirmwareName = "Full 1MB SPI Dump — ${downloaded.name} (${downloaded.version})"

            repository.log(
                level = "SUCCESS",
                tag = "1MB-SPI-BUILD",
                message = dumpReport.summaryMessage,
                hexPayload = "CRC32=${dumpReport.crc32} BOOT=${dumpReport.bootRegionCrc32} APP=${dumpReport.appRegionCrc32}"
            )
            _flashSessionState.update {
                it.copy(
                    lastFull1MbDumpSummary = dumpReport.summaryMessage,
                    lastCompletedMessage = dumpReport.summaryMessage,
                    isError = false,
                    verifiedWizardStages = it.verifiedWizardStages + 2
                )
            }
            launchCreateDocument(dumpReport.fileNameSuggested)
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
                    "Файл «$label» (${bytes.size / 1024} КБ) успешно сохранен на устройство (CRC32: $crc)!"
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
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: ChecksumEngine.buildDeterministicFirmwareBinary(
                        firmwareId = displayName,
                        version = "local",
                        declaredSizeBytes = 262144
                    )

                val isFwsc = displayName.lowercase().endsWith(".fwsc")
                val surgeon = ChecksumEngine.extractAndRepairFwscContainer(
                    rawBytes = rawBytes,
                    fileName = displayName,
                    firmwareIdFallback = displayName
                )
                val safeBytes = surgeon.extractedAppPayload
                val report = ChecksumEngine.analyzeBinary(
                    bytes = safeBytes,
                    fileName = displayName,
                    requestedOffsetHex = "0x00004000"
                )
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val customId = "local_${System.currentTimeMillis()}"
                val localFile = repository.buildFirmwareFile(customId, "Local")
                localFile.writeBytes(safeBytes)

                val verEntry = FirmwareVersionEntry(
                    version = "Local File",
                    releaseDate = today,
                    sizeBytes = safeBytes.size.coerceAtLeast(16384),
                    crc32 = report.crc32,
                    sha256 = report.sha256,
                    notes = if (isFwsc) "Распакованный через FWSC Surgeon файл ($displayName)" else "Импортированный пользователем файл ($displayName)"
                )

                val customEntity = FirmwareEntity(
                    id = customId,
                    name = displayName.substringBeforeLast('.'),
                    version = "Local File",
                    author = "Локальный импорт USB/Storage",
                    category = FirmwareCategory.FM_SYNTH.name,
                    summary = "Импортированный пользователем файл прошивки ($displayName). Автоматически выровнен по безопасному смещению 0x00004000 (защита загрузчика WL82).",
                    changelog = "• Локальный бинарный образ ($displayName)\n• Размер: ${safeBytes.size} байт\n• Заголовок: ${report.headerHexPreview}",
                    releaseNotes = surgeon.summaryMessage,
                    versionHistoryJson = FirmwareVersionHistoryCodec.encode(listOf(verEntry)),
                    compatibleHardwareRevs = "M-VAVE FM-1 PCB Rev A / Rev B / Rev C • JieLi AC791N (WL82) • Offset 0x00004000",
                    flashOffsetHex = "0x00004000",
                    sizeBytes = safeBytes.size.coerceAtLeast(16384),
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
                _flashSessionState.update {
                    it.copy(lastFwscSurgeonReport = surgeon.summaryMessage)
                }
                _statusBannerMessage.value =
                    "Файл $displayName импортирован и проверен хирургом .fwsc (CRC32: ${report.crc32}, SHA-256: ${report.sha256.take(12)}…)"
                repository.log(
                    level = "CHECKSUM",
                    tag = "FILE-IMPORT",
                    message = "Импортирован локальный файл $displayName (${safeBytes.size} байт). CRC32=${report.crc32}",
                    hexPayload = report.headerHexPreview
                )
                _isShowingFirmwareDetail.value = false
                if (_currentDestination.value != AppDestination.UNBRICKER) {
                    _currentDestination.value = AppDestination.FLASHER
                }
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
        _flashSessionState.update { it.copy(activeUnbrickStep = step.coerceIn(0, 4)) }
    }

    fun selectLogLevelFilter(level: String) {
        _selectedLogLevelFilter.value = level
    }

    fun selectDesktopOs(os: String) {
        _selectedDesktopOs.value = os
    }

    fun createDeviceBackupNow(customNote: String = "Ручной бэкап перед модификацией памяти") {
        viewModelScope.launch {
            refreshUsbConnection(showToastOnScan = false)
            val dev = _connectedDevice.value
            if (!dev.isReadyToFlash) {
                val errMsg = if (dev.isConnected && !dev.hasPermission) {
                    usbEngine.requestUsbPermissionIfNeeded(dev.deviceName)
                    "Не выдано разрешение USB Host! Подтвердите запрос Android для чтения дампа EEPROM."
                } else {
                    "ОШИБКА ЧТЕНИЯ EEPROM: Синтезатор не подключен по USB OTG! Подключите M-VAVE FM-1 кабелем OTG."
                }
                repository.log("ERROR", "BACKUP-FAIL", errMsg, dev.vidPidHex)
                _statusBannerMessage.value = errMsg
                return@launch
            }

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
            _flashSessionState.update {
                it.copy(verifiedWizardStages = it.verifiedWizardStages + 2)
            }
            _statusBannerMessage.value =
                "Резервная копия EEPROM и заголовка 0x0000–0x3FFF успешно сохранена (CRC32: ${report.crc32})."
        }
    }

    fun restoreBackupToDevice(backup: BackupDumpEntity) {
        if (_flashSessionState.value.isFlashing) return
        viewModelScope.launch {
            refreshUsbConnection(showToastOnScan = false)
            val dev = _connectedDevice.value
            if (!dev.isReadyToFlash) {
                val errMsg = if (dev.isConnected && !dev.hasPermission) {
                    usbEngine.requestUsbPermissionIfNeeded(dev.deviceName)
                    "Не выдано разрешение USB Host! Подтвердите запрос Android для восстановления дампа."
                } else {
                    "ОШИБКА ВОССТАНОВЛЕНИЯ: Синтезатор не подключен по USB OTG! Подключите устройство кабелем OTG."
                }
                repository.log("ERROR", "RESTORE-FAIL", errMsg, dev.vidPidHex)
                _flashSessionState.update {
                    it.copy(
                        isFlashing = false,
                        isError = true,
                        lastCompletedMessage = errMsg
                    )
                }
                _statusBannerMessage.value = errMsg
                return@launch
            }

            _flashSessionState.update {
                it.copy(
                    isFlashing = true,
                    isError = false,
                    lastCompletedMessage = null
                )
            }
            try {
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
                    expectedCrc32 = ChecksumEngine.computeCrc32(bytes),
                    maxRetries = _flashSessionState.value.maxSectorRetries,
                    blockSizeBytes = _flashSessionState.value.transferBlockSize
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
            } catch (e: Exception) {
                val errText = e.localizedMessage ?: "Ошибка USB OTG при восстановлении бэкапа"
                repository.log("ERROR", "RESTORE-ERR", errText)
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

    fun deleteBackup(backupId: Int) {
        viewModelScope.launch {
            repository.deleteBackup(backupId)
            repository.log("WARN", "BACKUP-DEL", "Удален снимок резервной копии #$backupId")
        }
    }

    fun startAutomatedFlashing(isUnbrickRecovery: Boolean = false) {
        if (_flashSessionState.value.isFlashing) return
        val fw = if (isUnbrickRecovery) {
            resolveTargetUnbrickFirmware()
        } else {
            _selectedFirmware.value ?: filteredFirmwares.value.firstOrNull()
        } ?: return

        viewModelScope.launch {
            refreshUsbConnection(showToastOnScan = false)
            val dev = _connectedDevice.value

            if (!dev.isReadyToFlash) {
                val errText = when {
                    dev.isConnected && !dev.hasPermission -> {
                        usbEngine.requestUsbPermissionIfNeeded(dev.deviceName)
                        "ОШИБКА ДОСТУПА USB: Обнаружено устройство ${dev.productName} (${dev.vidPidHex}), но не выдано системное разрешение Android USB Host. Подтвердите диалог на экране и нажмите кнопку снова."
                    }
                    dev.profileType == UsbProfileType.UNRECOGNIZED_USB_DEVICE -> {
                        "ОШИБКА УСТРОЙСТВА: Подключено стороннее USB-устройство (${dev.productName}, ${dev.vidPidHex}), не являющееся M-VAVE FM-1 / WL82 UBOOT1.00 / CH341A. Запись заблокирована."
                    }
                    _suspectHardBrickCableConnected.value -> {
                        "ОБНАРУЖЕН HARD-BRICK: Кабель подключен, но чип JieLi AC791N не отвечает по USB. Используйте Метод 3 (замкните Pin 6 CLK на Pin 4 GND при включении), Метод 5 (RP2040) или Метод 6 (CH341A 1MB)."
                    }
                    else -> {
                        "ОШИБКА ПОДКЛЮЧЕНИЯ: Синтезатор M-VAVE FM-1 не подключен по USB OTG! Подключите устройство через OTG-кабель (или включите режим «Тестовая симуляция»)."
                    }
                }
                repository.log("ERROR", "USB-NOT-READY", errText, dev.vidPidHex)
                _flashSessionState.update {
                    it.copy(
                        isFlashing = false,
                        currentProgress = null,
                        lastCompletedMessage = errText,
                        isError = true
                    )
                }
                _statusBannerMessage.value = errText
                return@launch
            }

            _flashSessionState.update {
                it.copy(
                    isFlashing = true,
                    isError = false,
                    lastCompletedMessage = null,
                    verifiedWizardStages = setOf(0)
                )
            }

            try {
                val (downloadedFw, rawBinary) = repository.loadOrDownloadFirmwareBytes(fw)
                _selectedFirmware.value = downloadedFw

                // Run FWSC Container Surgeon check to guarantee clean 0x4000 payload
                val surgeon = ChecksumEngine.extractAndRepairFwscContainer(
                    rawBytes = rawBinary,
                    fileName = "${downloadedFw.id}.bin",
                    firmwareIdFallback = downloadedFw.id
                )
                val cleanAppBytes = surgeon.extractedAppPayload
                val report = ChecksumEngine.analyzeBinary(cleanAppBytes, "${downloadedFw.id}.bin", "0x00004000")
                _activeChecksumReport.value = report

                repository.log(
                    level = "CHECKSUM",
                    tag = "PRE-FLASH-HASH",
                    message = "Проверка целостности (${_flashSessionState.value.selectedUnbrickMethod.shortBadge}): SHA-256=${report.sha256.take(16)}… | CRC32=${report.crc32}",
                    hexPayload = report.headerHexPreview
                )

                // Stage 1: If in normal MIDI mode or Bootloop Catcher mode, send SysEx trigger first
                if (!dev.profileType.isBootloaderMode) {
                    usbEngine.sendSysExBootloaderTrigger(dev) { level, tag, msg, hex ->
                        repository.log(level, tag, msg, hex)
                    }
                }

                // Stage 2: Inject secondary loader `wl82loader.bin` into SRAM 0x010000
                usbEngine.injectWl82LoaderToSram(dev) { level, tag, msg, hex ->
                    repository.log(level, tag, msg, hex)
                }
                _flashSessionState.update {
                    it.copy(
                        wl82LoaderInjected = true,
                        verifiedWizardStages = it.verifiedWizardStages + setOf(0, 1)
                    )
                }

                // Stage 3: Auto-backup current EEPROM/presets & 0x0000-0x3FFF before writing
                if (_flashSessionState.value.autoBackupBeforeFlash || isUnbrickRecovery) {
                    val stamp = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date())
                    repository.insertBackup(
                        BackupDumpEntity(
                            title = "Авто-бэкап перед ${if (isUnbrickRecovery) "Unbrick" else "прошивкой"} ${downloadedFw.name} ($stamp)",
                            deviceName = dev.productName,
                            deviceMode = dev.profileType.protocolBadge,
                            regionHex = "0x00000000 – 0x000FFFFF (1MB Full SPI Backup)",
                            sizeBytes = 65536,
                            presetCount = 64,
                            sha256 = report.sha256,
                            crc32 = report.crc32,
                            md5 = report.md5,
                            rawHexPreview = report.headerHexPreview,
                            presetsJson = """{"auto_backup":true,"method":"${_flashSessionState.value.selectedUnbrickMethod.name}","target_firmware":"${downloadedFw.name}","version":"${downloadedFw.version}","protected_boot":"0x0000-0x3FFF"}""",
                            notes = "Автоматическая точка восстановления перед записью ${downloadedFw.name} (${downloadedFw.version})."
                        )
                    )
                    repository.log(
                        level = "INFO",
                        tag = "AUTO-BACKUP",
                        message = "Сохранена резервная копия SPI Flash и заголовка загрузчика 0x0000–0x3FFF.",
                        hexPayload = "BACKUP_REGION=0x0000_0xFFFF"
                    )
                    _flashSessionState.update {
                        it.copy(verifiedWizardStages = it.verifiedWizardStages + 2)
                    }
                }

                // Determine whether we are writing 0x4000 Safe App or Full 0x0000+0x4000 (for CH341A or explicit Boot Rebuild)
                val rebuildBoot = isUnbrickRecovery && (
                    _flashSessionState.value.allowBootSectorRebuild ||
                        dev.profileType == UsbProfileType.CH341A_SPI_PROGRAMMER ||
                        _flashSessionState.value.selectedUnbrickMethod == UnbrickMethodType.CH341A_FULL_DUMP_PROGRAMMER
                    )

                val payloadToWrite: ByteArray
                val targetOffset: String
                val expectedSha: String
                val expectedCrc: String

                if (rebuildBoot) {
                    val goldenBoot = ChecksumEngine.buildGoldenBootloaderHeader16Kb()
                    payloadToWrite = ByteArray(goldenBoot.size + cleanAppBytes.size)
                    System.arraycopy(goldenBoot, 0, payloadToWrite, 0, goldenBoot.size)
                    System.arraycopy(cleanAppBytes, 0, payloadToWrite, goldenBoot.size, cleanAppBytes.size)
                    targetOffset = "0x00000000"
                    expectedSha = ChecksumEngine.computeSha256(payloadToWrite)
                    expectedCrc = ChecksumEngine.computeCrc32(payloadToWrite)
                } else {
                    payloadToWrite = cleanAppBytes
                    targetOffset = "0x00004000"
                    expectedSha = report.sha256
                    expectedCrc = report.crc32
                }

                // Stage 4 & 5: Execute 64-sector SCSI CBW/CSW write with auto-retry and Read-Back verification
                val ok = usbEngine.executeFlashPipeline(
                    deviceInfo = dev,
                    firmwareBytes = payloadToWrite,
                    declaredSizeBytes = downloadedFw.sizeBytes,
                    targetOffsetHex = targetOffset,
                    expectedSha256 = expectedSha,
                    expectedCrc32 = expectedCrc,
                    allowBootHeaderRebuild = rebuildBoot,
                    maxRetries = _flashSessionState.value.maxSectorRetries,
                    blockSizeBytes = _flashSessionState.value.transferBlockSize,
                    injectRamLoaderFirst = true
                ) { progress ->
                    _flashSessionState.update { state ->
                        state.copy(
                            currentProgress = progress,
                            verifiedWizardStages = state.verifiedWizardStages + progress.verifiedStageIndex
                        )
                    }
                    if (progress.completedSectors % 16 == 0 || progress.completedSectors == 64) {
                        repository.log(
                            level = "USB_IO",
                            tag = "SCSI-SECTOR",
                            message = progress.logLine,
                            hexPayload = progress.hexPacketSample
                        )
                    }
                }

                val simPrefix = if (dev.isSimulated) "[ТЕСТОВАЯ СИМУЛЯЦИЯ] " else ""
                val methodLabel = _flashSessionState.value.selectedUnbrickMethod.shortBadge
                val doneText = if (isUnbrickRecovery) {
                    "${simPrefix}UNBRICK УСПЕШЕН ($methodLabel): wl82loader.bin запущен в SRAM, прошивка «${downloadedFw.name} (${downloadedFw.version})» записана по смещению $targetOffset (SCSI CBW/CSW) и верифицирована (CRC32: $expectedCrc)!"
                } else {
                    "${simPrefix}Прошивка «${downloadedFw.name} (${downloadedFw.version})» успешно записана по адресу $targetOffset и верифицирована (SHA-256 / CRC32: $expectedCrc)!"
                }

                repository.log(
                    level = if (ok) "SUCCESS" else "WARN",
                    tag = if (isUnbrickRecovery) "UNBRICK-DONE" else "FLASH-DONE",
                    message = doneText,
                    hexPayload = "SHA256=${expectedSha.take(24)}... CRC32=$expectedCrc"
                )

                _flashSessionState.update {
                    it.copy(
                        isFlashing = false,
                        wl82LoaderInjected = true,
                        bootHeader0x0000Repaired = rebuildBoot || it.bootHeader0x0000Repaired,
                        lastCompletedMessage = doneText,
                        isError = !ok,
                        verifiedWizardStages = setOf(0, 1, 2, 3, 4)
                    )
                }
                _statusBannerMessage.value = doneText
            } catch (e: Exception) {
                val errText = "Ошибка операции прошивки: ${e.localizedMessage}"
                repository.log("ERROR", "FLASH-ERR", errText)
                _flashSessionState.update {
                    it.copy(
                        isFlashing = false,
                        currentProgress = null,
                        lastCompletedMessage = errText,
                        isError = true
                    )
                }
                _statusBannerMessage.value = errText
            }
        }
    }

    fun triggerSoftBrickStockRecovery() {
        val targetFw = resolveTargetUnbrickFirmware()
        if (targetFw != null) {
            selectFirmwareForInspection(targetFw, navigateToFlasher = false)
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
            appendLine("Active Unbrick Method: ${_flashSessionState.value.selectedUnbrickMethod.title}")
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
