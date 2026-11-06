package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FirmwareEntity
import com.example.data.FirmwareVersionEntry
import com.example.data.FirmwareVersionHistoryCodec
import com.example.domain.ChecksumReport
import com.example.ui.components.FirmwareArtworkBanner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirmwareDetailScreen(
    firmware: FirmwareEntity,
    checksumReport: ChecksumReport?,
    isDownloading: Boolean,
    firmwaresFolderPath: String = "FM1_Firmwares",
    onBackToCatalog: () -> Unit,
    onToggleFavorite: (FirmwareEntity) -> Unit,
    onDownloadFirmware: (FirmwareEntity, String, Int) -> Unit,
    onExportFirmwareToDevice: (FirmwareEntity) -> Unit,
    onOpenInEmulator: (FirmwareEntity) -> Unit,
    onOpenInFlasher: (FirmwareEntity) -> Unit,
    onOpenInChecksumInspector: (FirmwareEntity) -> Unit
) {
    val versionHistory = remember(firmware.id, firmware.versionHistoryJson, firmware.version) {
        FirmwareVersionHistoryCodec.decode(firmware.versionHistoryJson, firmware)
    }

    val hardwareRevPills = remember(firmware.compatibleHardwareRevs) {
        firmware.compatibleHardwareRevs.split("•").map { it.trim() }.filter { it.isNotEmpty() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("firmware_detail_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Navigation & Title Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (firmware.isOfficialStock) SynthPhosphor.copy(alpha = 0.7f) else SynthOrange.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onBackToCatalog,
                            modifier = Modifier.testTag("detail_back_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад в каталог",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Назад в каталог", style = MaterialTheme.typography.labelLarge)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = if (firmware.isDownloaded) {
                                    SynthPhosphor.copy(alpha = 0.18f)
                                } else {
                                    SynthCyan.copy(alpha = 0.16f)
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (firmware.isDownloaded) SynthPhosphor.copy(alpha = 0.6f) else SynthCyan.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = if (firmware.isDownloaded) "СКАЧАНО В ПАМЯТЬ (.BIN)" else "ДОСТУПНО ОНЛАЙН/АРХИВ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (firmware.isDownloaded) SynthPhosphor else SynthCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(onClick = { onToggleFavorite(firmware) }) {
                                Icon(
                                    imageVector = if (firmware.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Избранное",
                                    tint = if (firmware.isFavorite) SynthOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (firmware.isOfficialStock) SynthPhosphor.copy(alpha = 0.18f) else SynthOrange.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (firmware.isOfficialStock) "OFFICIAL STOCK / UNBRICK" else firmware.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (firmware.isOfficialStock) SynthPhosphor else SynthOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = firmware.version,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${firmware.sizeBytes / 1024} KB",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthPhosphor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = firmware.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Разработчик: ${firmware.author} • Дата релиза: ${firmware.releaseDate} • ID: ${firmware.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FirmwareArtworkBanner(
                        firmware = firmware,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = firmware.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        firmware.tagsCsv.split(",").filter { it.isNotBlank() }.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Download, Export & Open Action Center Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Управление бинарным файлом прошивки (.BIN)",
                            style = MaterialTheme.typography.titleMedium,
                            color = SynthCyan
                        )
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SynthCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (firmware.isDownloaded && firmware.localFilePath.isNotBlank()) {
                                    "Папка прошивок (FM1_Firmwares): ${firmware.localFilePath}"
                                } else {
                                    "Целевая папка загрузки: $firmwaresFolderPath (${firmware.downloadUrl})"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (firmware.isDownloaded) SynthPhosphor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (firmware.rawFwscFilePath.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Исходный контейнер .fwsc: ${firmware.rawFwscFilePath}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (firmware.hashVerified) {
                                    "Хеш SHA-256 и CRC32 проверены • Полное описание и PNG-экран сохранены в БД Room"
                                } else {
                                    "При скачивании проверяется SHA-256, описание и картинка экрана сохраняются в БД, а .bin — в папку FM1_Firmwares/"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (firmware.hashVerified) SynthPhosphor else SynthCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Смещение записи: ${firmware.flashOffsetHex} (Загрузчик 0x0000–0x3FFF защищен)",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Button: Open & Flash
                    Button(
                        onClick = { onOpenInFlasher(firmware) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("detail_open_flasher_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (firmware.isOfficialStock) SynthPhosphor else SynthOrange,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (firmware.isOfficialStock) Icons.Default.Security else Icons.Default.Bolt,
                            contentDescription = "Открыть во флешере"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (firmware.isDownloaded) {
                                "ОТКРЫТЬ ОБРАЗ И ПРОШИТЬ (0x00004000)"
                            } else {
                                "СКАЧАТЬ, ОТКРЫТЬ И ПРОШИТЬ (0x00004000)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Launch Hardware & DSP Emulator Button
                    Button(
                        onClick = { onOpenInEmulator(firmware) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("detail_open_emulator_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SynthCyan,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Piano,
                            contentDescription = "Открыть в эмуляторе"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ТЕСТИРОВАТЬ ЗВУК И OLED В ЭМУЛЯТОРЕ FM-1",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDownloadFirmware(firmware, firmware.version, firmware.sizeBytes)
                            },
                            enabled = !isDownloading,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_download_button"),
                            border = BorderStroke(1.dp, SynthPhosphor)
                        ) {
                            Icon(
                                imageVector = if (firmware.isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                contentDescription = "Скачать в память",
                                tint = SynthPhosphor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (firmware.isDownloaded) "Обновить .bin" else "Скачать .bin",
                                color = SynthPhosphor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { onExportFirmwareToDevice(firmware) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("detail_export_button"),
                            border = BorderStroke(1.dp, SynthCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SaveAlt,
                                contentDescription = "Экспорт файла",
                                tint = SynthCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Экспорт .bin",
                                color = SynthCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onOpenInChecksumInspector(firmware) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_inspect_checksum_button"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Инспектор хэшей",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Открыть в HEX-инспекторе и верификаторе SHA-256 / CRC32",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Compatible Hardware Revisions Matrix Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Совместимые ревизии железа",
                            tint = SynthCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Совместимые ревизии железа (Hardware Compatibility)",
                            style = MaterialTheme.typography.titleMedium,
                            color = SynthCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hardwareRevPills.forEach { rev ->
                            Surface(
                                color = SynthCyan.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SynthPhosphor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = rev,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HardwareSpecRow("Целевое устройство:", firmware.targetDevice)
                            HardwareSpecRow("Процессор / SoC:", "${firmware.chipArchitecture} (Dual-Core 320 MHz RISC + FPU)")
                            HardwareSpecRow("Разметка NOR Flash:", "8 MB (W25Q64) • Раздел приложения ${firmware.flashOffsetHex}")
                            HardwareSpecRow("Защита загрузчика:", "Сектора 0x0000–0x3FFF (WL82 Boot Header) защищены от перезаписи")
                            HardwareSpecRow("Протоколы прошивки:", "USB-MIDI SysEx (F0 22 24 35 7D F7) + JieLi WL82 UBOOT1.00 DFU")
                        }
                    }
                }
            }
        }

        // 4. Extended Release Notes & Changelog Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Полное описание из базы данных и архитектура движка",
                        style = MaterialTheme.typography.titleMedium,
                        color = SynthOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = firmware.fullDescription.ifBlank { firmware.releaseNotes.ifBlank { firmware.summary } },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Ключевые изменения в текущей сборке (${firmware.version}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = SynthPhosphor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = firmware.changelog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 5. Interactive Version History Timeline Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "История версий",
                    tint = SynthOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "История версий и архив релизов (${versionHistory.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Вы можете скачать и установить любую из предыдущих или актуальных версий прошивки",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 6. Version History Items
        itemsIndexed(versionHistory) { index, verEntry ->
            val isCurrentSelectedVersion = verEntry.version == firmware.version
            VersionHistoryCard(
                entry = verEntry,
                isLatest = index == 0,
                isCurrentActive = isCurrentSelectedVersion,
                onSelectAndDownloadVersion = {
                    onDownloadFirmware(firmware, verEntry.version, verEntry.sizeBytes)
                }
            )
        }

        // 7. Cryptographic Hashes & HEX Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Криптографический паспорт и HEX-заголовок",
                        style = MaterialTheme.typography.titleMedium,
                        color = SynthPhosphor
                    )
                    Text(
                        text = "SHA-256: ${firmware.sha256}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CRC32:   ${firmware.crc32}   |   MD5: ${firmware.md5}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = SynthCyan
                    )
                    if (checksumReport != null) {
                        Text(
                            text = "HEX (0x00..0x1F): ${checksumReport.headerHexPreview}",
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

@Composable
private fun HardwareSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun VersionHistoryCard(
    entry: FirmwareVersionEntry,
    isLatest: Boolean,
    isCurrentActive: Boolean,
    onSelectAndDownloadVersion: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentActive) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (isCurrentActive) 1.5.dp else 1.dp,
            color = if (isCurrentActive) SynthPhosphor else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isCurrentActive) SynthPhosphor else SynthCyan,
                                CircleShape
                            )
                    )
                    Text(
                        text = entry.version,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (isLatest) {
                        Surface(
                            color = SynthOrange.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LATEST",
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthOrange,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isCurrentActive) {
                        Surface(
                            color = SynthPhosphor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ВЫБРАНА",
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthPhosphor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${entry.releaseDate} • ${entry.sizeBytes / 1024} KB",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRC32: ${entry.crc32} • SHA-256: ${entry.sha256.take(16)}…",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = SynthCyan
                )

                OutlinedButton(
                    onClick = onSelectAndDownloadVersion,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentActive) SynthPhosphor else SynthAmberWarn
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Выбрать и скачать версию",
                        tint = if (isCurrentActive) SynthPhosphor else SynthAmberWarn,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCurrentActive) "Перекачать .bin" else "Выбрать ${entry.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentActive) SynthPhosphor else SynthAmberWarn
                    )
                }
            }
        }
    }
}
