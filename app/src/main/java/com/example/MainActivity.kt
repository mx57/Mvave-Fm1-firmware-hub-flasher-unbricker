package com.example

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppDestination
import com.example.ui.FirmwareHubViewModel
import com.example.ui.screens.BackupsAndChecksumScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.FlasherScreen
import com.example.ui.screens.LogsAndDocsScreen
import com.example.ui.screens.UnbrickerScreen
import com.example.ui.theme.MyApplicationTheme
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
    val firmwares by viewModel.filteredFirmwares.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.onlyFavorites.collectAsStateWithLifecycle()
    val isSyncingHub by viewModel.isSyncingHub.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    val useSimulationMode by viewModel.useSimulationMode.collectAsStateWithLifecycle()
    val simulatedProfile by viewModel.simulatedProfile.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()

    val selectedFirmware by viewModel.selectedFirmware.collectAsStateWithLifecycle()
    val checksumReport by viewModel.activeChecksumReport.collectAsStateWithLifecycle()
    val customExpectedHash by viewModel.customExpectedHashInput.collectAsStateWithLifecycle()
    val flashState by viewModel.flashSessionState.collectAsStateWithLifecycle()

    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val logs by viewModel.filteredLogs.collectAsStateWithLifecycle()
    val selectedLogLevel by viewModel.selectedLogLevelFilter.collectAsStateWithLifecycle()
    val selectedDesktopOs by viewModel.selectedDesktopOs.collectAsStateWithLifecycle()

    // BackHandler on secondary destinations to return to Catalog
    BackHandler(enabled = currentDestination != AppDestination.CATALOG) {
        viewModel.navigateTo(AppDestination.CATALOG)
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

    val shareText: (String, String) -> Unit = { subject, body ->
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        context.startActivity(Intent.createChooser(sendIntent, subject))
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
                                text = "${connectedDevice.vidPidHex} • ${if (useSimulationMode) "SIMULATION READY" else "USB OTG HOST"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (useSimulationMode) SynthCyan else SynthPhosphor
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
                            AppDestination.CATALOG -> CatalogScreen(
                                firmwares = firmwares,
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategory,
                                sortOption = sortOption,
                                onlyFavorites = onlyFavorites,
                                isSyncingHub = isSyncingHub,
                                connectedDevice = connectedDevice,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSelectCategory = viewModel::selectCategory,
                                onSelectSort = viewModel::selectSortOption,
                                onToggleOnlyFavorites = viewModel::toggleOnlyFavorites,
                                onToggleFavoriteFirmware = viewModel::toggleFavoriteFirmware,
                                onSyncHub = viewModel::syncCatalogWithTheFirmwareHub,
                                onImportLocalFile = {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                },
                                onSelectFirmwareForFlash = { fw ->
                                    viewModel.selectFirmwareForInspection(fw, navigateToFlasher = true)
                                },
                                onOpenUnbricker = {
                                    viewModel.navigateTo(AppDestination.UNBRICKER)
                                }
                            )

                            AppDestination.FLASHER -> FlasherScreen(
                                selectedFirmware = selectedFirmware,
                                checksumReport = checksumReport,
                                connectedDevice = connectedDevice,
                                useSimulationMode = useSimulationMode,
                                simulatedProfile = simulatedProfile,
                                flashState = flashState,
                                onSetSimulationMode = viewModel::setSimulationMode,
                                onSelectSimulatedProfile = viewModel::selectSimulatedProfile,
                                onRefreshUsb = viewModel::refreshUsbConnection,
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
                                flashState = flashState,
                                onSelectSimulatedProfile = viewModel::selectSimulatedProfile,
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
