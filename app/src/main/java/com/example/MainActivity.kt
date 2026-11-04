package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.UsbFlashEngine
import com.example.ui.AppDestination
import com.example.ui.FirmwareHubViewModel
import com.example.ui.screens.BackupsAndChecksumScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.EmulatorScreen
import com.example.ui.screens.FirmwareDetailScreen
import com.example.ui.screens.FlasherScreen
import com.example.ui.screens.LogsAndDocsScreen
import com.example.ui.screens.UnbrickerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FirmwareHubApp()
            }
        }
    }
}

private fun destinationIcon(destination: AppDestination): ImageVector = when (destination) {
    AppDestination.CATALOG -> Icons.Default.LibraryMusic
    AppDestination.EMULATOR -> Icons.Default.Piano
    AppDestination.FLASHER -> Icons.Default.Bolt
    AppDestination.UNBRICKER -> Icons.Default.Healing
    AppDestination.BACKUPS -> Icons.Default.VerifiedUser
    AppDestination.LOGS_DOCS -> Icons.Default.MenuBook
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmwareHubApp(
    viewModel: FirmwareHubViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val isShowingFirmwareDetail by viewModel.isShowingFirmwareDetail.collectAsStateWithLifecycle()
    val firmwares by viewModel.filteredFirmwares.collectAsStateWithLifecycle()
    val totalCatalogCount by viewModel.totalCatalogCount.collectAsStateWithLifecycle()
    val downloadedCatalogCount by viewModel.downloadedCatalogCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.onlyFavorites.collectAsStateWithLifecycle()
    val isSyncingHub by viewModel.isSyncingHub.collectAsStateWithLifecycle()
    val isDownloadingFirmware by viewModel.isDownloadingFirmware.collectAsStateWithLifecycle()
    val isDownloadingAll by viewModel.isDownloadingAll.collectAsStateWithLifecycle()
    val batchDownloadStatus by viewModel.batchDownloadStatus.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val useSimulationMode by viewModel.useSimulationMode.collectAsStateWithLifecycle()
    val simulatedProfile by viewModel.simulatedProfile.collectAsStateWithLifecycle()
    val suspectHardBrickCableConnected by viewModel.suspectHardBrickCableConnected.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()

    val selectedFirmware by viewModel.selectedFirmware.collectAsStateWithLifecycle()
    val checksumReport by viewModel.activeChecksumReport.collectAsStateWithLifecycle()
    val customExpectedHash by viewModel.customExpectedHashInput.collectAsStateWithLifecycle()
    val flashState by viewModel.flashSessionState.collectAsStateWithLifecycle()

    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val selectedLogLevel by viewModel.selectedLogLevelFilter.collectAsStateWithLifecycle()
    val selectedDesktopOs by viewModel.selectedDesktopOs.collectAsStateWithLifecycle()
    val emulatorState by viewModel.emulatorUiState.collectAsStateWithLifecycle()

    // Real-time BroadcastReceiver for physical USB OTG attach, detach, and permission grant events
    DisposableEffect(context) {
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED,
                    UsbManager.ACTION_USB_DEVICE_DETACHED,
                    UsbFlashEngine.ACTION_USB_PERMISSION -> {
                        viewModel.refreshUsbConnection(showToastOnScan = true)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbFlashEngine.ACTION_USB_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
        onDispose {
            runCatching { context.unregisterReceiver(usbReceiver) }
        }
    }

    // BackHandler on FirmwareDetailScreen or secondary destinations
    BackHandler(enabled = isShowingFirmwareDetail || currentDestination != AppDestination.CATALOG) {
        if (isShowingFirmwareDetail) {
            viewModel.closeFirmwareDetail()
        } else {
            viewModel.navigateTo(AppDestination.CATALOG)
        }
    }

    // File picker launcher for importing local .bin / .fw / .fwsc / .syx firmware files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var fileName = "custom_fm1_image.bin"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
            }
            viewModel.importLocalFirmwareUri(uri, fileName)
        }
    }

    // SAF CreateDocument launcher for exporting/saving any downloaded .bin firmware to device storage
    val exportBinLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.writePendingExportToUri(uri)
        }
    }

    // SAF CreateDocument launcher for exporting recorded .wav audio from the emulator
    val exportWavLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { uri ->
        if (uri != null) {
            viewModel.writePendingExportToUri(uri)
        }
    }

    val shareText: (String, String) -> Unit = { subject, body ->
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(sendIntent, subject))
    }

    val topStatusColor = when {
        connectedDevice.isReadyToFlash && !connectedDevice.isSimulated -> SynthPhosphor
        connectedDevice.isSimulated -> SynthCyan
        else -> SynthAmberWarn
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "FM1 FirmwareHub & Unbricker",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${connectedDevice.diagnosticVerdict.badgeTitle} (${connectedDevice.vidPidHex}) • Каталог: $totalCatalogCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = topStatusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest) },
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = destinationIcon(dest),
                                        contentDescription = dest.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SynthOrange,
                                    selectedTextColor = SynthOrange,
                                    indicatorColor = SynthOrange.copy(alpha = 0.16f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppDestination.entries.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.navigateTo(dest) },
                                modifier = Modifier.testTag("nav_rail_${dest.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = destinationIcon(dest),
                                        contentDescription = dest.title
                                    )
                                },
                                label = { Text(dest.title) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Status notification toast/banner
                    AnimatedVisibility(visible = statusBanner != null) {
                        statusBanner?.let { msg ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SynthPhosphor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.dismissBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Закрыть уведомление",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentDestination) {
                            AppDestination.CATALOG -> {
                                val detailFw = selectedFirmware
                                if (isShowingFirmwareDetail && detailFw != null) {
                                    FirmwareDetailScreen(
                                        firmware = detailFw,
                                        checksumReport = checksumReport,
                                        isDownloading = isDownloadingFirmware,
                                        onBackToCatalog = viewModel::closeFirmwareDetail,
                                        onToggleFavorite = viewModel::toggleFavoriteFirmware,
                                        onDownloadFirmware = { fw, ver, size ->
                                            viewModel.downloadSingleFirmware(fw, ver, size)
                                        },
                                        onExportFirmwareToDevice = { fw ->
                                            viewModel.prepareExportFirmwareFile(fw) { suggestedName ->
                                                exportBinLauncher.launch(suggestedName)
                                            }
                                        },
                                        onOpenInEmulator = { fw ->
                                            viewModel.openFirmwareInEmulator(fw)
                                        },
                                        onOpenInFlasher = { fw ->
                                            viewModel.selectFirmwareForInspection(
                                                firmware = fw,
                                                navigateToFlasher = true
                                            )
                                        },
                                        onOpenInChecksumInspector = { fw ->
                                            viewModel.selectFirmwareForInspection(
                                                firmware = fw,
                                                navigateToFlasher = false,
                                                navigateToChecksums = true
                                            )
                                        }
                                    )
                                } else {
                                    CatalogScreen(
                                        firmwares = firmwares,
                                        totalCatalogCount = totalCatalogCount,
                                        downloadedCount = downloadedCatalogCount,
                                        searchQuery = searchQuery,
                                        selectedCategory = selectedCategory,
                                        sortOption = sortOption,
                                        onlyFavorites = onlyFavorites,
                                        isSyncingHub = isSyncingHub,
                                        isDownloadingAll = isDownloadingAll,
                                        batchDownloadStatus = batchDownloadStatus,
                                        connectedDevice = connectedDevice,
                                        onSearchQueryChange = viewModel::updateSearchQuery,
                                        onSelectCategory = viewModel::selectCategory,
                                        onSelectSort = viewModel::selectSortOption,
                                        onToggleOnlyFavorites = viewModel::toggleOnlyFavorites,
                                        onResetAllFilters = viewModel::resetAllCatalogFilters,
                                        onToggleFavoriteFirmware = viewModel::toggleFavoriteFirmware,
                                        onSyncHub = viewModel::syncCatalogWithTheFirmwareHub,
                                        onDownloadAllFirmwares = viewModel::downloadAllCatalogFirmwares,
                                        onDownloadSingleFirmware = { fw ->
                                            viewModel.downloadSingleFirmware(fw)
                                        },
                                        onExportFirmwareFile = { fw ->
                                            viewModel.prepareExportFirmwareFile(fw) { suggestedName ->
                                                exportBinLauncher.launch(suggestedName)
                                            }
                                        },
                                        onImportLocalFile = {
                                            filePickerLauncher.launch(arrayOf("*/*"))
                                        },
                                        onOpenFirmwareDetail = { fw ->
                                            viewModel.openFirmwareDetail(fw)
                                        },
                                        onOpenFirmwareInEmulator = { fw ->
                                            viewModel.openFirmwareInEmulator(fw)
                                        },
                                        onSelectFirmwareForFlash = { fw ->
                                            viewModel.selectFirmwareForInspection(fw, navigateToFlasher = true)
                                        },
                                        onOpenUnbricker = {
                                            viewModel.navigateTo(AppDestination.UNBRICKER)
                                        }
                                    )
                                }
                            }

                            AppDestination.EMULATOR -> EmulatorScreen(
                                emulatorState = emulatorState,
                                allFirmwares = firmwares,
                                onSelectActiveSlot = viewModel::selectActiveEmulatorSlot,
                                onAssignFirmwareToSlot = viewModel::assignFirmwareToEmulatorSlot,
                                onSelectPresetIndex = viewModel::selectEmulatorPreset,
                                onSelectEngineType = viewModel::selectEmulatorEngineType,
                                onSelectOledPage = viewModel::selectEmulatorOledPage,
                                onSelectOledDisplayMode = viewModel::selectEmulatorOledDisplayMode,
                                onUpdateKnobParameter = viewModel::updateEmulatorKnob,
                                onToggleSequencerPlay = viewModel::toggleEmulatorSequencerPlay,
                                onSelectPatternIndex = viewModel::selectEmulatorPattern,
                                onToggleStepEditMode = viewModel::toggleEmulatorStepEditMode,
                                onToggleStepActive = viewModel::toggleEmulatorStepActive,
                                onSelectStepForNoteEdit = viewModel::selectEmulatorStepForNoteEdit,
                                onCycleArpMode = viewModel::cycleEmulatorArpMode,
                                onChangeOctave = viewModel::changeEmulatorOctave,
                                onPadNoteOn = viewModel::triggerEmulatorPadOn,
                                onPadNoteOff = viewModel::triggerEmulatorPadOff,
                                onToggleWavRecording = viewModel::toggleEmulatorWavRecording,
                                onExportRecordedWav = {
                                    viewModel.prepareExportRecordedWav { suggestedWavName ->
                                        exportWavLauncher.launch(suggestedWavName)
                                    }
                                },
                                onOpenActiveFirmwareInFlasher = { fw ->
                                    viewModel.selectFirmwareForInspection(fw, navigateToFlasher = true)
                                },
                                onStopAudioOnLeave = viewModel::releaseEmulatorHeldNotes
                            )

                            AppDestination.FLASHER -> FlasherScreen(
                                selectedFirmware = selectedFirmware,
                                checksumReport = checksumReport,
                                connectedDevice = connectedDevice,
                                useSimulationMode = useSimulationMode,
                                simulatedProfile = simulatedProfile,
                                suspectHardBrickCableConnected = suspectHardBrickCableConnected,
                                flashState = flashState,
                                onSetSimulationMode = viewModel::setSimulationMode,
                                onSelectSimulatedProfile = viewModel::selectSimulatedProfile,
                                onSetHardBrickSuspected = viewModel::setHardBrickCableSuspected,
                                onRequestUsbPermission = viewModel::requestActiveUsbPermission,
                                onRefreshUsb = { viewModel.refreshUsbConnection(showToastOnScan = true) },
                                onOpenUnbricker = { viewModel.navigateTo(AppDestination.UNBRICKER) },
                                onToggleAutoBackup = viewModel::toggleAutoBackupBeforeFlash,
                                onToggleVerifyAfterFlash = viewModel::toggleVerifyAfterFlash,
                                onBackupNow = { viewModel.createDeviceBackupNow() },
                                onImportLocalFile = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                onStartFlash = {
                                    viewModel.startAutomatedFlashing(isUnbrickRecovery = false)
                                }
                            )

                            AppDestination.UNBRICKER -> UnbrickerScreen(
                                connectedDevice = connectedDevice,
                                selectedFirmware = selectedFirmware,
                                useSimulationMode = useSimulationMode,
                                simulatedProfile = simulatedProfile,
                                suspectHardBrickCableConnected = suspectHardBrickCableConnected,
                                flashState = flashState,
                                onRefreshUsb = { viewModel.refreshUsbConnection(showToastOnScan = true) },
                                onRequestUsbPermission = viewModel::requestActiveUsbPermission,
                                onSetSimulationMode = viewModel::setSimulationMode,
                                onSelectSimulatedProfile = viewModel::selectSimulatedProfile,
                                onSetHardBrickSuspected = viewModel::setHardBrickCableSuspected,
                                onSelectUnbrickMethod = viewModel::selectUnbrickMethod,
                                onSelectPinShortVector = viewModel::selectPinShortVector,
                                onSetUnbrickUiMode = viewModel::setUnbrickUiMode,
                                onSelectTargetOption = viewModel::selectUnbrickTargetOption,
                                onSetMaxRetries = viewModel::setMaxSectorRetries,
                                onSetBlockSize = viewModel::setTransferBlockSize,
                                onToggleBootSectorRebuild = viewModel::toggleAllowBootSectorRebuild,
                                onInjectWl82LoaderNow = viewModel::injectWl82RamLoaderNow,
                                onExecuteControlTransferReset = viewModel::executeControlTransferResetNow,
                                onRunUsbBusAudit = viewModel::runUsbBusDiagnosticsNow,
                                onToggleBootloopCatcher = viewModel::toggleBootloopAndPinShortCatcher,
                                onRunFwscSurgeon = viewModel::runFwscContainerSurgeonNow,
                                onRepairBootHeader0x0000 = viewModel::repairBootloaderHeader0x0000Now,
                                onExportFull1MbDump = {
                                    viewModel.prepareExportFull1MbSpiDump { suggestedName ->
                                        exportBinLauncher.launch(suggestedName)
                                    }
                                },
                                onImportCustomFile = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                onRunSoftBrickRecovery = viewModel::triggerSoftBrickStockRecovery,
                                onSetWizardStep = viewModel::setUnbrickWizardStep,
                                onOpenLogsAndScripts = {
                                    viewModel.navigateTo(AppDestination.LOGS_DOCS)
                                }
                            )

                            AppDestination.BACKUPS -> BackupsAndChecksumScreen(
                                selectedFirmware = selectedFirmware,
                                checksumReport = checksumReport,
                                customExpectedHash = customExpectedHash,
                                backups = backups,
                                isFlashing = flashState.isFlashing,
                                onUpdateExpectedHash = viewModel::updateCustomExpectedHash,
                                onImportFileForVerify = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                onCreateBackupNow = { viewModel.createDeviceBackupNow() },
                                onRestoreBackup = viewModel::restoreBackupToDevice,
                                onDeleteBackup = viewModel::deleteBackup,
                                onExportBackup = { backup ->
                                    val exportPayload = buildString {
                                        appendLine("# FM1 FirmwareHub — EEPROM & WL82 Boot Backup Dump")
                                        appendLine("Title: ${backup.title}")
                                        appendLine("Device: ${backup.deviceName} (${backup.deviceMode})")
                                        appendLine("Region: ${backup.regionHex}")
                                        appendLine("SHA-256: ${backup.sha256}")
                                        appendLine("CRC32: ${backup.crc32}")
                                        appendLine("MD5: ${backup.md5}")
                                        appendLine("HEX Header: ${backup.rawHexPreview}")
                                        appendLine("Presets JSON: ${backup.presetsJson}")
                                    }
                                    shareText("M-VAVE FM-1 Backup (${backup.crc32})", exportPayload)
                                }
                            )

                            AppDestination.LOGS_DOCS -> LogsAndDocsScreen(
                                logs = logs,
                                selectedLogLevel = selectedLogLevel,
                                selectedDesktopOs = selectedDesktopOs,
                                selectedFirmware = selectedFirmware,
                                onSelectLogLevel = viewModel::selectLogLevelFilter,
                                onSelectDesktopOs = viewModel::selectDesktopOs,
                                onExportLogs = {
                                    val report = viewModel.buildFormattedLogsExport()
                                    shareText("FM1 FirmwareHub Session Log", report)
                                },
                                onShareScript = { script ->
                                    shareText("M-VAVE FM-1 Cross-Platform Flash Script", script)
                                },
                                onClearLogs = viewModel::clearTelemetryLogs
                            )
                        }
                    }
                }
            }
        }
    }
}
