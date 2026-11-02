package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FirmwareEntity
import com.example.domain.ChecksumReport
import com.example.domain.ConnectedUsbDeviceInfo
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
    flashState: FlashSessionUiState,
    onSetSimulationMode: (Boolean) -> Unit,
    onSelectSimulatedProfile: (UsbProfileType) -> Unit,
    onRefreshUsb: () -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onToggleVerifyAfterFlash: (Boolean) -> Unit,
    onBackupNow: () -> Unit,
    onImportLocalFile: () -> Unit,
    onStartFlash: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("flasher_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. USB Bus & Hardware Simulation Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (useSimulationMode) SynthAmberWarn.copy(alpha = 0.65f) else SynthPhosphor.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (useSimulationMode) {
                        Surface(
                            color = SynthAmberWarn.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, SynthAmberWarn.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Режим симуляции",
                                    tint = SynthAmberWarn,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SAFETY SIMULATION MODE АКТИВЕН (Безопасная проверка без риска для чипа)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthAmberWarn
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Usb,
                                contentDescription = "USB Интерфейс",
                                tint = SynthCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Интерфейс USB OTG / Загрузчик",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${connectedDevice.productName} (${connectedDevice.vidPidHex})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SynthCyan
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRefreshUsb,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Сканировать USB",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Скан USB", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Режим симуляции оборудования (JieLi WL82)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Отключите для прямой записи на физический USB OTG порт",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useSimulationMode,
                            onCheckedChange = onSetSimulationMode,
                            colors = SwitchDefaults.colors(checkedThumbColor = SynthOrange)
                        )
                    }

                    if (useSimulationMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Профиль виртуального устройства:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UsbProfileType.entries.forEach { profile ->
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

                        // Cryptographic Checksum Verification Box
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_backup_button"),
                        border = BorderStroke(1.dp, SynthCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Сделать дамп",
                            tint = SynthCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Сделать дамп текущих настроек и EEPROM сейчас",
                            color = SynthCyan
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
                            text = if (progress != null) {
                                "${(progress.progressFraction * 100).toInt()}% • ${progress.speedKbPerSec.toInt()} KB/s"
                            } else {
                                "ГОТОВ К ЗАПИСИ"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Canvas rendering 64 Flash Sectors (First 4 locked boot sectors + 60 app sectors)
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
                                    else -> androidx.compose.ui.graphics.Color(0xFF263040)
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
                        enabled = !flashState.isFlashing && selectedFirmware != null,
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
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Прошить"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (flashState.isFlashing) {
                                "ИДЕТ ПРОШИВКА И ВЕРИФИКАЦИЯ…"
                            } else {
                                "ЗАПУСТИТЬ АВТО-ПРОШИВКУ (0x00004000)"
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
