package com.example.ui.screens

import com.example.ui.AppLanguage
import com.example.ui.AppStrings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FirmwareEntity
import com.example.data.FlashLogEntity
import com.example.domain.ActiveWireProtocol
import com.example.domain.ChecksumReport
import com.example.domain.ConnectedUsbDeviceInfo
import com.example.domain.DeviceDiagnosticVerdict
import com.example.domain.UsbProfileType
import com.example.ui.FlashSessionUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCrimsonError
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FlasherScreen(
    appLanguage: AppLanguage = AppLanguage.RU,

    selectedFirmware: FirmwareEntity?,
    checksumReport: ChecksumReport?,
    connectedDevice: ConnectedUsbDeviceInfo,
    useSimulationMode: Boolean,
    simulatedProfile: UsbProfileType,
    suspectHardBrickCableConnected: Boolean,
    flashState: FlashSessionUiState,
    recentLogs: List<FlashLogEntity> = emptyList(),
    onSetSimulationMode: (Boolean) -> Unit,
    onSelectSimulatedProfile: (UsbProfileType) -> Unit,
    onSetHardBrickSuspected: (Boolean) -> Unit,
    onRequestUsbPermission: () -> Unit,
    onRefreshUsb: () -> Unit,
    onOpenUnbricker: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onToggleVerifyAfterFlash: (Boolean) -> Unit,
    onSelectWireProtocol: (ActiveWireProtocol) -> Unit = {},
    onBackupNow: () -> Unit,
    onImportLocalFile: () -> Unit,
    onStartFlash: () -> Unit,
    onCopyLogs: () -> Unit = {},
    onOpenLogsScreen: () -> Unit = {}
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    val verdict = connectedDevice.diagnosticVerdict
    val statusAccentColor = when (verdict) {
        DeviceDiagnosticVerdict.NORMAL_MODE_READY -> SynthPhosphor
        DeviceDiagnosticVerdict.SOFT_BRICK_DETECTED,
        DeviceDiagnosticVerdict.HARD_BRICK_BRIDGE_READY,
        DeviceDiagnosticVerdict.CH341A_PROGRAMMER_READY -> SynthOrange
        DeviceDiagnosticVerdict.SIMULATION_MODE -> SynthCyan
        DeviceDiagnosticVerdict.PERMISSION_PENDING -> SynthAmberWarn
        DeviceDiagnosticVerdict.NO_OTG_DEVICE,
        DeviceDiagnosticVerdict.HARD_BRICK_SUSPECTED,
        DeviceDiagnosticVerdict.UNSUPPORTED_USB_DEVICE -> Color(0xFFFF5252)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("flasher_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Real USB OTG Detector & Brick Diagnostic Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, statusAccentColor.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Diagnostic Status Banner
                    Surface(
                        color = statusAccentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, statusAccentColor.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    connectedDevice.isReadyToFlash -> Icons.Default.CheckCircle
                                    verdict == DeviceDiagnosticVerdict.HARD_BRICK_SUSPECTED -> Icons.Default.ErrorOutline
                                    else -> Icons.Default.UsbOff
                                },
                                contentDescription = "Статус подключения",
                                tint = statusAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "СТАТУС ШИНЫ: ${verdict.badgeTitle}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusAccentColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = verdict.summaryTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (connectedDevice.isConnected) Icons.Default.Usb else Icons.Default.UsbOff,
                                contentDescription = "USB OTG",
                                tint = statusAccentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = connectedDevice.productName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${connectedDevice.vidPidHex} • ${connectedDevice.bootloaderVersion}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusAccentColor
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRefreshUsb,
                            modifier = Modifier.testTag("scan_usb_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Сканировать USB OTG",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (appLanguage == AppLanguage.EN) "Scan OTG" else "Скан OTG", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Detailed Hardware Telemetry Box
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Шина USB: ${connectedDevice.usbInterfaceSummary}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan
                            )
                            Text(
                                text = "Ответ загрузчика / SysEx ID: ${connectedDevice.detectedFirmwareIdent}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (connectedDevice.isConnected) SynthPhosphor else SynthAmberWarn
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = connectedDevice.diagnosticDetails,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // If USB device is attached but lacks Android USB permission
                    if (connectedDevice.isConnected && !connectedDevice.isSimulated && !connectedDevice.hasPermission) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRequestUsbPermission,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SynthAmberWarn,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = "Разрешить USB")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ЗАПРОСИТЬ РАЗРЕШЕНИЕ ANDROID USB HOST",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Interactive Hard-Brick Detector when 0 USB devices are found
                    if (!connectedDevice.isConnected && !useSimulationMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (suspectHardBrickCableConnected) Color(0xFFFF5252) else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Детектор Hard-Brick (мертвый USB)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (suspectHardBrickCableConnected) Color(0xFFFF5252) else SynthOrange
                                        )
                                        Text(
                                            text = "Включите, если синтезатор физически подключен кабелем OTG и включен тумблером, но не определяется телефоном",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = suspectHardBrickCableConnected,
                                        onCheckedChange = onSetHardBrickSuspected,
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5252))
                                    )
                                }

                                if (suspectHardBrickCableConnected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = onOpenUnbricker,
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, SynthOrange)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Healing,
                                            contentDescription = "Открыть Unbricker",
                                            tint = SynthOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Открыть инструкцию Hard-Brick (RP2040 Transporter)",
                                            color = SynthOrange
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional Developer Simulation Mode Toggle (OFF by default)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Демо-симуляция без железа (только для теста UI)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (useSimulationMode) {
                                    "ВНИМАНИЕ: Активен виртуальный стенд. Отключите для работы с реальным USB OTG!"
                                } else {
                                    "Выключено (по умолчанию). Прошивка работает только при подключенном USB OTG."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (useSimulationMode) SynthAmberWarn else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useSimulationMode,
                            onCheckedChange = onSetSimulationMode,
                            modifier = Modifier.testTag("simulation_mode_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = SynthCyan)
                        )
                    }

                    if (useSimulationMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Тестовый профиль виртуального устройства:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                UsbProfileType.MVAVE_NORMAL_MIDI,
                                UsbProfileType.WL82_UBOOT_SOFTBRICK,
                                UsbProfileType.RP2040_TRANSPORTER_BRIDGE,
                                UsbProfileType.CH341A_SPI_PROGRAMMER
                            ).forEach { profile ->
                                FilterChip(
                                    selected = simulatedProfile == profile,
                                    onClick = { onSelectSimulatedProfile(profile) },
                                    label = {
                                        Text(
                                            text = "${profile.vidHex}:${profile.pidHex} ${profile.protocolBadge}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SynthCyan.copy(alpha = 0.22f),
                                        selectedLabelColor = SynthCyan
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Selected Firmware & Checksum Verification Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Выбранный образ прошивки",
                            style = MaterialTheme.typography.titleMedium,
                            color = SynthOrange
                        )
                        OutlinedButton(
                            onClick = onImportLocalFile,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileOpen,
                                contentDescription = "Открыть файл",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Другой файл", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedFirmware != null) {
                        Text(
                            text = "${selectedFirmware.name} (${selectedFirmware.version})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Автор: ${selectedFirmware.author} • Размер: ${selectedFirmware.sizeBytes / 1024} КБ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        com.example.ui.components.FirmwareArtworkBanner(
                            firmware = selectedFirmware,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Memory Offset Guard Banner (0x0000-0x3FFF Protected, 0x4000 Target)
                        Surface(
                            color = SynthPhosphor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Защита загрузчика",
                                    tint = SynthPhosphor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ЗАЩИТА ЗАГРУЗЧИКА WL82 АКТИВНА (0x0000–0x3FFF LOCKED)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SynthPhosphor
                                    )
                                    Text(
                                        text = "Запись производится строго по смещению ${selectedFirmware.flashOffsetHex}. Исключено повреждение Boot Head.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (checksumReport != null) {
                            val shaMatches = checksumReport.sha256.equals(selectedFirmware.sha256, ignoreCase = true)
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (shaMatches) SynthPhosphor else SynthAmberWarn
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (shaMatches) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = "Статус хэша",
                                            tint = if (shaMatches) SynthPhosphor else SynthAmberWarn,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (shaMatches) {
                                                "ЦЕЛОСТНОСТЬ ОБРАЗА ПОДТВЕРЖДЕНА (SHA-256 / CRC32 / MD5 OK)"
                                            } else {
                                                "ВНИМАНИЕ: ПОЛЬЗОВАТЕЛЬСКИЙ ХЭШ ОТЛИЧАЕТСЯ"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (shaMatches) SynthPhosphor else SynthAmberWarn
                                        )
                                    }
                                    Text(
                                        text = "SHA-256: ${checksumReport.sha256}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "CRC32:   ${checksumReport.crc32}   |   MD5: ${checksumReport.md5}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SynthCyan
                                    )
                                    Text(
                                        text = "HEADER:  ${checksumReport.headerHexPreview}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Pre-Flash Safety Options & Manual Backup Trigger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Параметры безопасности и резервного копирования",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Авто-бэкап EEPROM и пресетов перед записью",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Сохраняет дамп 0x0000–0xFFFF и все 64 патча в локальную базу",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = flashState.autoBackupBeforeFlash,
                            onCheckedChange = onToggleAutoBackup,
                            colors = SwitchDefaults.colors(checkedThumbColor = SynthPhosphor)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Read-Back верификация после прошивки",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Повторно считывает записанные блоки и сверяет CRC32 / SHA-256",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = flashState.verifyAfterFlash,
                            onCheckedChange = onToggleVerifyAfterFlash,
                            colors = SwitchDefaults.colors(checkedThumbColor = SynthPhosphor)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wire Protocol Selector (Auto Dual Protocol: SysEx OTA + JieLi SCSI BOT)
                    Text(
                        text = "Протокол проводной записи (после погасания экрана FM-1):",
                        style = MaterialTheme.typography.labelLarge,
                        color = SynthCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActiveWireProtocol.entries.forEach { proto ->
                            val selected = flashState.activeWireProtocol == proto
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectWireProtocol(proto) },
                                modifier = Modifier.testTag("flasher_proto_${proto.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = proto.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SynthCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = SynthCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = SynthCyan.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "АВТО-ПЕРЕХВАТ ПОСЛЕ ПОГАСАНИЯ ЭКРАНА FM-1 АКТИВЕН",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Когда после F0 22 24 35 7D F7 экран FM-1 гаснет, чип меняет USB-адрес. Приложение автоматически ждет новое устройство (ota-FM-1 / WL82 UBOOT1.00), запрашивает права и продолжает запись (${flashState.activeWireProtocol.description}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBackupNow,
                        enabled = connectedDevice.isReadyToFlash && !flashState.isFlashing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_backup_button"),
                        border = BorderStroke(
                            1.dp,
                            if (connectedDevice.isReadyToFlash) SynthCyan else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Сделать дамп",
                            tint = if (connectedDevice.isReadyToFlash) SynthCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (connectedDevice.isReadyToFlash) {
                                "Сделать дамп текущих настроек и EEPROM сейчас"
                            } else {
                                "Дамп EEPROM недоступен (синтезатор не подключен по OTG)"
                            },
                            color = if (connectedDevice.isReadyToFlash) SynthCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Live 64-Sector Flash Map & Execution Control
        item {
            val progress = flashState.currentProgress
            val completedSectors = progress?.completedSectors ?: 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthOrange.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Карта секторов флеш-памяти (64 блока)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when {
                                progress != null -> "${(progress.progressFraction * 100).toInt()}% • ${progress.speedKbPerSec.toInt()} KB/s"
                                connectedDevice.isReadyToFlash -> "ГОТОВ К ЗАПИСИ"
                                else -> "ОЖИДАНИЕ USB OTG"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connectedDevice.isReadyToFlash) SynthPhosphor else SynthAmberWarn
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        val cols = 16
                        val rows = 4
                        val spacing = 4.dp.toPx()
                        val cellW = (size.width - spacing * (cols - 1)) / cols
                        val cellH = (size.height - spacing * (rows - 1)) / rows

                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                val index = r * cols + c + 1
                                val color = when {
                                    index <= 4 -> SynthCyan // 0x0000-0x3FFF Protected Bootloader Sectors
                                    index <= completedSectors -> SynthPhosphor
                                    index == completedSectors + 1 && flashState.isFlashing -> SynthOrange
                                    else -> Color(0xFF263040)
                                }
                                drawRoundRect(
                                    color = color,
                                    topLeft = Offset(c * (cellW + spacing), r * (cellH + spacing)),
                                    size = Size(cellW, cellH),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "■ Синие (1–4): Boot 0x0000–0x3FFF (Защищено)",
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                        Text(
                            text = "■ Зеленые: Записано 0x00004000+",
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor
                        )
                    }

                    if (progress != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = SynthPhosphor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = progress.stageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${progress.currentAddressHex} | ${progress.hexPacketSample}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                    }

                    flashState.lastCompletedMessage?.let { doneMsg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (flashState.isError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                SynthPhosphor.copy(alpha = 0.16f)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = doneMsg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (flashState.isError) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        SynthPhosphor
                                    }
                                )
                                if (flashState.isError) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onCopyLogs,
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, SynthAmberWarn)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Копировать лог ошибки",
                                                tint = SynthAmberWarn,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Скопировать лог",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SynthAmberWarn
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                onSelectWireProtocol(ActiveWireProtocol.USB_MIDI_SYSEX_OTA)
                                                onRefreshUsb()
                                            },
                                            modifier = Modifier.weight(1f),
                                            border = BorderStroke(1.dp, SynthCyan)
                                        ) {
                                            Text(
                                                text = "Режим SysEx OTA",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SynthCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onStartFlash,
                        enabled = !flashState.isFlashing && selectedFirmware != null && connectedDevice.isReadyToFlash,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_flash_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SynthOrange,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (connectedDevice.isReadyToFlash) Icons.Default.Bolt else Icons.Default.UsbOff,
                            contentDescription = "Прошить"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                flashState.isFlashing -> if (appLanguage == AppLanguage.EN) "FLASHING & VERIFYING…" else "ИДЕТ ПРОШИВКА И ВЕРИФИКАЦИЯ…"
                                !connectedDevice.isConnected -> if (appLanguage == AppLanguage.EN) "NO USB OTG DEVICE (FLASHING LOCKED)" else "НЕТ USB OTG УСТРОЙСТВА (ПРОШИВКА ЗАБЛОКИРОВАНА)"
                                !connectedDevice.hasPermission && !connectedDevice.isSimulated -> if (appLanguage == AppLanguage.EN) "GRANT USB PERMISSION TO FLASH" else "ПОДТВЕРДИТЕ ДОСТУП К USB ДЛЯ ПРОШИВКИ"
                                connectedDevice.isSimulated -> if (appLanguage == AppLanguage.EN) "START TEST SIMULATION (0x00004000)" else "ЗАПУСТИТЬ ТЕСТОВУЮ СИМУЛЯЦИЮ (0x00004000)"
                                else -> if (appLanguage == AppLanguage.EN) "START AUTO-FLASH (0x00004000)" else "ЗАПУСТИТЬ АВТО-ПРОШИВКУ (0x00004000)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Live Step-by-Step USB OTG & Flash Telemetry Console right on FlasherScreen
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("flasher_live_log_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Живой лог прошивки",
                                tint = SynthCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Живой лог шагов прошивки (USB OTG Trace)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Пакеты SysEx, перехват после гашения экрана, MaxPacketSize и ответы пайпов",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (recentLogs.isEmpty()) {
                                Text(
                                    text = "[ОЖИДАНИЕ] Запустите сканирование USB OTG или прошивку для записи пошаговой телеметрии.",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                recentLogs.take(16).forEach { log ->
                                    val badgeColor = when (log.level) {
                                        "SUCCESS" -> SynthPhosphor
                                        "USB_IO" -> SynthCyan
                                        "CHECKSUM" -> SynthOrange
                                        "WARN" -> SynthAmberWarn
                                        "ERROR" -> SynthCrimsonError
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Column {
                                        Text(
                                            text = "[${timeFormatter.format(Date(log.timestamp))}] [${log.level}] [${log.tag}] ${log.message}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor
                                        )
                                        if (log.hexPayload.isNotBlank()) {
                                            Text(
                                                text = "   ↳ ${log.hexPayload}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCopyLogs,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_flasher_logs_button"),
                            border = BorderStroke(1.dp, SynthCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Копировать лог",
                                tint = SynthCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Копировать лог", color = SynthCyan, style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = onOpenLogsScreen,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_full_logs_button"),
                            border = BorderStroke(1.dp, SynthPhosphor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Полный журнал",
                                tint = SynthPhosphor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Вкладка Логи (${recentLogs.size})", color = SynthPhosphor, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
