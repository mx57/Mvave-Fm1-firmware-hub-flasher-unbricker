package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.FirmwareCategory
import com.example.data.FirmwareEntity
import com.example.data.FirmwareVersionHistoryCodec
import com.example.domain.ConnectedUsbDeviceInfo
import com.example.ui.SortOption
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    firmwares: List<FirmwareEntity>,
    totalCatalogCount: Int,
    downloadedCount: Int,
    searchQuery: String,
    selectedCategory: FirmwareCategory,
    sortOption: SortOption,
    onlyFavorites: Boolean,
    isSyncingHub: Boolean,
    isDownloadingAll: Boolean,
    batchDownloadStatus: String?,
    connectedDevice: ConnectedUsbDeviceInfo,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (FirmwareCategory) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onToggleOnlyFavorites: () -> Unit,
    onResetAllFilters: () -> Unit,
    onToggleFavoriteFirmware: (FirmwareEntity) -> Unit,
    onSyncHub: () -> Unit,
    onDownloadAllFirmwares: () -> Unit,
    onDownloadSingleFirmware: (FirmwareEntity) -> Unit,
    onExportFirmwareFile: (FirmwareEntity) -> Unit,
    onImportLocalFile: () -> Unit,
    onOpenFirmwareDetail: (FirmwareEntity) -> Unit,
    onOpenFirmwareInEmulator: (FirmwareEntity) -> Unit,
    onSelectFirmwareForFlash: (FirmwareEntity) -> Unit,
    onOpenUnbricker: () -> Unit
) {
    val isAnyFilterActive = selectedCategory != FirmwareCategory.ALL ||
        searchQuery.isNotBlank() ||
        onlyFavorites

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catalog_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Hardware Hero Banner & Quick Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = SynthOrange.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SynthOrange.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "THEFIRMWAREHUB.COM • ПОЛНЫЙ АРХИВ ($totalCatalogCount ПРОШИВОК)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Каталог прошивок M-VAVE FM-1",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Все кастомные FM-движки, грувбоксы, эмуляторы чипов и заводские образы (0x00004000) с историей версий и скачиванием .bin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Image(
                        painter = painterResource(id = R.drawable.ic_synth_hero_banner),
                        contentDescription = "Иллюстрация синтезатора M-VAVE FM-1",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.FillWidth
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active USB Status Bar inside Hero
                    val usbStatusColor = when {
                        connectedDevice.isReadyToFlash && !connectedDevice.isSimulated -> SynthPhosphor
                        connectedDevice.isSimulated -> SynthCyan
                        else -> SynthOrange
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, usbStatusColor.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = "Статус USB",
                                    tint = usbStatusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${connectedDevice.diagnosticVerdict.badgeTitle}: ${connectedDevice.productName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = usbStatusColor
                                    )
                                    Text(
                                        text = "${connectedDevice.vidPidHex} • ${connectedDevice.bootloaderVersion}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AssistChip(
                                onClick = onOpenUnbricker,
                                label = {
                                    Text(
                                        text = "Диагностика / Unbrick",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons Row 1: Sync Hub & Download All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncHub,
                            enabled = !isSyncingHub,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sync_hub_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSyncingHub) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Синхронизация",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Синхр. Hub",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onDownloadAllFirmwares,
                            enabled = !isDownloadingAll,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_all_firmwares_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SynthPhosphor,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isDownloadingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Скачать все прошивки",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Скачать все ($totalCatalogCount)",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Button Row 2: Import Local .bin/.fw/.syx
                    OutlinedButton(
                        onClick = onImportLocalFile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_firmware_button"),
                        border = BorderStroke(1.dp, SynthCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Импорт файла",
                            tint = SynthCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Импортировать локальный файл (.bin / .fw / .fwsc / .syx)",
                            color = SynthCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (batchDownloadStatus != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = SynthPhosphor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = batchDownloadStatus,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor
                        )
                    }
                }
            }
        }

        // 2. Search & Multi-Filter Controls
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_input"),
                    placeholder = {
                        Text(
                            text = "Поиск по названию, автору, ревизии железа, тегам, SHA-256…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить поиск"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SynthOrange,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Horizontal Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FirmwareCategory.entries.forEach { category ->
                        val selected = category == selectedCategory
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            label = {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SynthOrange.copy(alpha = 0.22f),
                                selectedLabelColor = SynthOrange
                            )
                        )
                    }
                }

                // Sort & Favorites Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = onToggleOnlyFavorites,
                        leadingIcon = {
                            Icon(
                                imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Избранные",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Избранные") }
                    )

                    SortOption.entries.forEach { option ->
                        FilterChip(
                            selected = sortOption == option,
                            onClick = { onSelectSort(option) },
                            label = { Text(option.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Показано: ${firmwares.size} из $totalCatalogCount • Скачано .bin: $downloadedCount/$totalCatalogCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isAnyFilterActive) {
                        AssistChip(
                            onClick = onResetAllFilters,
                            modifier = Modifier.testTag("reset_filters_chip"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterAltOff,
                                    contentDescription = "Сбросить фильтры",
                                    tint = SynthOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "Показать все ($totalCatalogCount)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthOrange
                                )
                            }
                        )
                    }
                }
            }
        }

        // 3. Firmware Catalog Items
        if (firmwares.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "По вашему запросу прошивки не найдены",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "В полном каталоге доступно $totalCatalogCount прошивок. Сбросьте активные фильтры, чтобы показать весь список.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onResetAllFilters,
                            colors = ButtonDefaults.buttonColors(containerColor = SynthOrange)
                        ) {
                            Text("Показать все $totalCatalogCount прошивок")
                        }
                    }
                }
            }
        } else {
            items(firmwares, key = { it.id }) { firmware ->
                FirmwareCatalogCard(
                    firmware = firmware,
                    onToggleFavorite = { onToggleFavoriteFirmware(firmware) },
                    onDownloadBinary = { onDownloadSingleFirmware(firmware) },
                    onExportBinary = { onExportFirmwareFile(firmware) },
                    onOpenDetailScreen = { onOpenFirmwareDetail(firmware) },
                    onOpenInEmulator = { onOpenFirmwareInEmulator(firmware) },
                    onSelectForFlash = { onSelectFirmwareForFlash(firmware) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FirmwareCatalogCard(
    firmware: FirmwareEntity,
    onToggleFavorite: () -> Unit,
    onDownloadBinary: () -> Unit,
    onExportBinary: () -> Unit,
    onOpenDetailScreen: () -> Unit,
    onOpenInEmulator: () -> Unit,
    onSelectForFlash: () -> Unit
) {
    var expandedDetails by remember { mutableStateOf(false) }
    val versionHistory = remember(firmware.id, firmware.versionHistoryJson, firmware.version) {
        FirmwareVersionHistoryCodec.decode(firmware.versionHistoryJson, firmware)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetailScreen() }
            .testTag("firmware_card_${firmware.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (firmware.isOfficialStock) 1.5.dp else 1.dp,
            color = if (firmware.isOfficialStock) SynthPhosphor.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (firmware.isOfficialStock) {
                                SynthPhosphor.copy(alpha = 0.18f)
                            } else {
                                SynthOrange.copy(alpha = 0.18f)
                            },
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
                            color = if (firmware.isDownloaded) {
                                SynthPhosphor.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (firmware.isDownloaded) "СКАЧАНО .BIN" else "ГОТОВО К ЗАГРУЗКЕ",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (firmware.isDownloaded) SynthPhosphor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = firmware.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Автор: ${firmware.author} • Дата: ${firmware.releaseDate} • Версий: ${versionHistory.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (firmware.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "В избранное",
                        tint = if (firmware.isFavorite) SynthOrange else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Offline-ready Firmware Visual Preview & OLED Synth Artwork
            com.example.ui.components.FirmwareArtworkBanner(
                firmware = firmware,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = firmware.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Technical Metadata, Hardware Compatibility & Checksum Strip
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Проверено",
                                tint = SynthPhosphor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CRC32: ${firmware.crc32} • Offset: ${firmware.flashOffsetHex}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthPhosphor
                            )
                        }
                        Text(
                            text = "${firmware.sizeBytes / 1024} KB",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                    }
                    Text(
                        text = "Ревизии железа: ${firmware.compatibleHardwareRevs}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SynthCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SHA-256: ${firmware.sha256.take(32)}…",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tags FlowRow
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
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expandable Inline Details: Release Notes, Hardware Revisions & Version History
            AnimatedVisibility(visible = expandedDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = SynthCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Совместимые ревизии железа:",
                            style = MaterialTheme.typography.labelLarge,
                            color = SynthCyan
                        )
                    }
                    Text(
                        text = firmware.compatibleHardwareRevs,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SynthOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Примечания к релизу (Release Notes):",
                            style = MaterialTheme.typography.labelLarge,
                            color = SynthOrange
                        )
                    }
                    Text(
                        text = firmware.releaseNotes.ifBlank { firmware.changelog },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SynthPhosphor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "История версий (${versionHistory.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            color = SynthPhosphor
                        )
                    }

                    versionHistory.forEach { ver ->
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${ver.version} (${ver.releaseDate})",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SynthCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${ver.sizeBytes / 1024} KB • CRC32: ${ver.crc32}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SynthPhosphor
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ver.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Text(
                        text = "Полный SHA-256: ${firmware.sha256}\nMD5: ${firmware.md5} • Чип: ${firmware.chipArchitecture}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onOpenDetailScreen,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, SynthCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Открыть полный экран",
                            tint = SynthCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Открыть полный экран деталей и выбор версий",
                            color = SynthCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Expand Toggle + Open Full Detail Screen Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expandedDetails = !expandedDetails }
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                        .testTag("expand_fw_${firmware.id}")
                ) {
                    Icon(
                        imageVector = if (expandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Раскрыть карточку",
                        tint = SynthCyan
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (expandedDetails) "Свернуть превью" else "История версий и ревизии",
                        style = MaterialTheme.typography.labelLarge,
                        color = SynthCyan
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenDetailScreen() }
                        .padding(vertical = 6.dp, horizontal = 8.dp)
                        .testTag("open_detail_${firmware.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInFull,
                        contentDescription = "Экран деталей",
                        tint = SynthOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Экран деталей",
                        style = MaterialTheme.typography.labelLarge,
                        color = SynthOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row: Download/Export .bin + Emulator + Flash
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = if (firmware.isDownloaded) onExportBinary else onDownloadBinary,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("download_fw_${firmware.id}"),
                    border = BorderStroke(
                        1.dp,
                        if (firmware.isDownloaded) SynthCyan else SynthPhosphor
                    )
                ) {
                    Icon(
                        imageVector = if (firmware.isDownloaded) Icons.Default.SaveAlt else Icons.Default.Download,
                        contentDescription = if (firmware.isDownloaded) "Экспорт .bin" else "Скачать .bin",
                        tint = if (firmware.isDownloaded) SynthCyan else SynthPhosphor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (firmware.isDownloaded) "Экспорт" else "Скачать",
                        color = if (firmware.isDownloaded) SynthCyan else SynthPhosphor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenInEmulator,
                    modifier = Modifier
                        .weight(1.05f)
                        .testTag("emulate_fw_${firmware.id}"),
                    border = BorderStroke(1.dp, SynthCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Piano,
                        contentDescription = "Тест в эмуляторе",
                        tint = SynthCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Эмулятор",
                        color = SynthCyan,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSelectForFlash,
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("select_fw_${firmware.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (firmware.isOfficialStock) SynthPhosphor else SynthOrange,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (firmware.isOfficialStock) Icons.Default.Security else Icons.Default.Bolt,
                        contentDescription = "Открыть и прошить",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (firmware.isOfficialStock) "Unbrick" else "Прошить",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
