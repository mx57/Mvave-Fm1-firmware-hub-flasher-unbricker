package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
import com.example.domain.ChecksumReport
import com.example.domain.ConnectedUsbDeviceInfo
import com.example.domain.DeviceDiagnosticVerdict
import com.example.domain.UsbProfileType
import com.example.ui.FlashSessionUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor

@Composable
fun FlasherScreen(
    selectedFirmware: FirmwareEntity?,
    checksumReport: ChecksumReport?,
    connectedDevice: ConnectedUsbDeviceInfo,
    useSimulationMode: Boolean,
    simulatedProfile: UsbProfileType,
    suspectHardBrickCableConnected: Boolean,
    flashState: FlashSessionUiState,
    onSetSimulationMode: (Boolean) -> Unit,
    onSelectSimulatedProfile: (UsbProfileType) -> Unit,
    onSetHardBrickSuspected: (Boolean) -> Unit,
    onRequestUsbPermission: () -> Unit,
    onRefreshUsb: () -> Unit,
    onOpenUnbricker: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onToggleVerifyAfterFlash: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onImportLocalFile: () -> Unit,
    onStartFlash: () -> Unit
) {
    val verdict = connectedDevice.diagnosticVerdict
    val statusAccentColor = when (verdict) {
        DeviceDiagnosticVerdict.NORMAL_MODE_READY -> SynthPhosphor
        DeviceDiagnosticVerdict.SOFT_BRICK_DETECTED,
        DeviceDiagnosticVerdict.HARD_BRICK_BRIDGE_READY -> SynthOrange
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
                            Text("Скан OTG", style = MaterialTheme.typography.labelSmall)
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
                                UsbProfileType.RP2040_TRANSPORTER_BRIDGE
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
                            Text(
                                text = doneMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (flashState.isError) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    SynthPhosphor
                                },
                                modifier = Modifier.padding(10.dp)
                            )
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
                                flashState.isFlashing -> "ИДЕТ ПРОШИВКА И ВЕРИФИКАЦИЯ…"
                                !connectedDevice.isConnected -> "НЕТ USB OTG УСТРОЙСТВА (ПРОШИВКА ЗАБЛОКИРОВАНА)"
                                !connectedDevice.hasPermission && !connectedDevice.isSimulated -> "ПОДТВЕРДИТЕ ДОСТУП К USB ДЛЯ ПРОШИВКИ"
                                connectedDevice.isSimulated -> "ЗАПУСТИТЬ ТЕСТОВУЮ СИМУЛЯЦИЮ (0x00004000)"
                                else -> "ЗАПУСТИТЬ АВТО-ПРОШИВКУ (0x00004000)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
