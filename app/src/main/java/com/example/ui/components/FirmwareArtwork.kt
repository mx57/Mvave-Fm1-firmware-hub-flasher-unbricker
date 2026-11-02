package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.FirmwareCategory
import com.example.data.FirmwareEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor

@DrawableRes
fun resolveFirmwareInterfaceDrawable(firmware: FirmwareEntity): Int {
    val cleanId = firmware.id.lowercase().trim()
    return when {
        cleanId == "sloop" || cleanId == "sloopdx" || cleanId.startsWith("sloop") -> R.drawable.fw_ui_sloop
        cleanId == "felucca" || cleanId == "felucca_salt" || cleanId.startsWith("felucca") -> R.drawable.fw_ui_felucca
        cleanId == "zp12" -> R.drawable.fw_ui_zp12
        cleanId == "x0x" || cleanId.contains("303") || cleanId.contains("acid") -> R.drawable.fw_ui_x0x
        cleanId == "jiant" || cleanId == "bubba" -> R.drawable.fw_ui_jiant
        cleanId == "melodee" || cleanId == "fimba1" -> R.drawable.fw_ui_melodee
        cleanId == "skiff" || cleanId == "jangada" || cleanId == "waveloop" -> R.drawable.fw_ui_skiff
        cleanId == "obervave" || cleanId == "fm106" || cleanId == "hortator" -> R.drawable.fw_ui_obervave
        cleanId == "amb" || cleanId.contains("mutable") -> R.drawable.fw_ui_amb
        cleanId == "ghoulbox" -> R.drawable.fw_ui_ghoulbox
        cleanId == "choralroot" -> R.drawable.fw_ui_choralroot
        cleanId == "fomni" -> R.drawable.fw_ui_fomni
        cleanId == "fm1sid" || cleanId.contains("sid") -> R.drawable.fw_ui_fm1sid
        cleanId == "fm1trax" || cleanId.contains("trax") -> R.drawable.fw_ui_fm1trax
        cleanId == "winampfm" || cleanId.contains("winamp") -> R.drawable.fw_ui_winampfm
        cleanId == "dub" -> R.drawable.fw_ui_dub
        firmware.isOfficialStock -> R.drawable.ic_fw_stock
        firmware.category == FirmwareCategory.GROOVEBOX.name -> R.drawable.fw_ui_sloop
        firmware.category == FirmwareCategory.FM_SYNTH.name -> R.drawable.fw_ui_felucca
        firmware.category == FirmwareCategory.CHIPTUNE.name -> R.drawable.fw_ui_fm1sid
        firmware.category == FirmwareCategory.VIRTUAL_ANALOG.name -> R.drawable.fw_ui_obervave
        firmware.category == FirmwareCategory.AMBIENT.name -> R.drawable.fw_ui_amb
        else -> R.drawable.fw_ui_felucca
    }
}

/**
 * Displays the authentic interface screenshot of each firmware from TheFirmwareHub.com
 * bundled directly in app resources for 100% offline work, with tap-to-zoom lightbox inspection.
 */
@Composable
fun FirmwareArtworkBanner(
    firmware: FirmwareEntity,
    modifier: Modifier = Modifier
) {
    val interfaceDrawableRes = remember(firmware.id, firmware.category) {
        resolveFirmwareInterfaceDrawable(firmware)
    }
    var showZoomDialog by remember { mutableStateOf(false) }

    val accentColor = when {
        firmware.isOfficialStock -> SynthPhosphor
        firmware.category == FirmwareCategory.GROOVEBOX.name -> SynthOrange
        firmware.category == FirmwareCategory.CHIPTUNE.name -> SynthAmberWarn
        firmware.category == FirmwareCategory.AMBIENT.name -> SynthPhosphor
        else -> SynthCyan
    }

    if (showZoomDialog) {
        Dialog(onDismissRequest = { showZoomDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E0C0D)),
                border = BorderStroke(2.dp, accentColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Интерфейс: ${firmware.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = accentColor
                            )
                            Text(
                                text = "${firmware.version} • Автор: ${firmware.author}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showZoomDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть превью",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF07080A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = interfaceDrawableRes),
                            contentDescription = "Полный интерфейс прошивки ${firmware.name}",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = firmware.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showZoomDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0E0C0D),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.16f),
                            Color(0xFF0E0C0D)
                        )
                    )
                )
        ) {
            if (firmware.isOfficialStock) {
                val stockBadge = when {
                    firmware.version.contains("16") -> "V16"
                    firmware.version.contains("14") -> "V14"
                    else -> "V15"
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = Color(0xFF091410),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(2.dp, SynthPhosphor),
                        modifier = Modifier
                            .width(136.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "M-VAVE",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stockBadge,
                                style = MaterialTheme.typography.displayLarge,
                                color = SynthPhosphor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "FACTORY ROM",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthPhosphor
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ШТАТНЫЙ ИНТЕРФЕЙС M-VAVE FM-1 ($stockBadge)",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Заводской OLED-экран: 64 FM-пресета, выбор алгоритма, арпеджиатор и USB-MIDI SysEx загрузчик.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Безопасное смещение: 0x00004000",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                    }
                }
            } else {
                Image(
                    painter = painterResource(id = interfaceDrawableRes),
                    contentDescription = "Экран интерфейса прошивки ${firmware.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Top-Left Offline & Downloaded Status Badge
            Surface(
                color = Color(0xD90A0D12),
                shape = RoundedCornerShape(bottomEnd = 10.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = "Статус образа",
                        tint = if (firmware.isDownloaded) SynthPhosphor else SynthCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (firmware.isDownloaded) {
                            "${firmware.id.uppercase()} • .BIN СКАЧАН"
                        } else {
                            "${firmware.id.uppercase()} • OLED UI"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (firmware.isDownloaded) SynthPhosphor else SynthCyan
                    )
                }
            }

            // Bottom-Left OLED Synth Identity Badge
            Surface(
                color = Color(0xD90A0D12),
                shape = RoundedCornerShape(topEnd = 10.dp),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = "${firmware.name} (${firmware.version}) • ${firmware.author}",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(210.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Bottom-Right Tap-to-Zoom Pill
            Surface(
                color = Color(0xD90A0D12),
                shape = RoundedCornerShape(topStart = 10.dp),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Увеличить интерфейс",
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Увеличить экран",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
