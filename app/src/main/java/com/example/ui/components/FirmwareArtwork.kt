package com.example.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

private val firmwareScreenBitmapCache = object : LruCache<String, ImageBitmap>(64) {}

/**
 * Returns the exact bundled asset path for a firmware's authentic OLED/TFT display screenshot
 * (`assets/fw_screens/<id>.png`). Every single one of the 160 firmwares has its own dedicated screen file.
 */
fun resolveFirmwareScreenAssetPath(firmware: FirmwareEntity): String {
    val cleanId = firmware.id.lowercase().trim()
    return "fw_screens/$cleanId.png"
}

/**
 * Loads and caches the authentic per-firmware screen PNG from `assets/fw_screens/<id>.png`.
 */
fun loadFirmwareScreenAssetBitmap(context: Context, firmware: FirmwareEntity): ImageBitmap? {
    val assetPath = resolveFirmwareScreenAssetPath(firmware)
    firmwareScreenBitmapCache.get(assetPath)?.let { return it }
    return try {
        context.assets.open(assetPath).use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap?.asImageBitmap()?.also { imageBitmap ->
                firmwareScreenBitmapCache.put(assetPath, imageBitmap)
            }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Per-firmware signature accent color from TheFirmwareHub.com (`LOOK` palette).
 */
fun resolveFirmwareAccentColor(firmware: FirmwareEntity): Color {
    return when (firmware.id.lowercase().trim()) {
        "mvave_fm1_stock_v16_unbrick" -> SynthPhosphor
        "fm1open" -> Color(0xFFF2C230)
        "baudgirl" -> Color(0xFFFF3FA4)
        "dinghy" -> Color(0xFF8F62F0)
        "felucca" -> Color(0xFFAFD9F4)
        "sloop" -> Color(0xFFE6D1B9)
        "sloopdx" -> Color(0xFF42F5F5)
        "zp12" -> Color(0xFFFF6A3D)
        "x0x", "x0x37" -> Color(0xFFF4C0CB)
        "x0xrmx" -> Color(0xFFC9A6E8)
        "fomni", "fomni37" -> Color(0xFFC9B6FF)
        "salt" -> Color(0xFFFF8F7A)
        "choralroot" -> Color(0xFFFFC46B)
        "jangada" -> Color(0xFFFF5FC8)
        "melodee" -> Color(0xFF8FE3B0)
        "hortator" -> Color(0xFFD8DDE6)
        "hortator-bt" -> Color(0xFF9FC4FF)
        "fimba" -> Color(0xFFF0A35E)
        "ghoulbox" -> Color(0xFFFFB000)
        "jiant" -> Color(0xFFFF7A00)
        "bubba" -> Color(0xFFFFC83D)
        "floyd" -> Color(0xFFFFB347)
        "sloopbanks" -> Color(0xFF7FD4FF)
        "waveloop" -> Color(0xFFB58CFF)
        "chord" -> Color(0xFFF2564E)
        "fumi" -> Color(0xFFB08A5A)
        "maku", "maku37" -> Color(0xFFF0806A)
        "rainbow", "rainbow37" -> Color(0xFFFFD23F)
        "obervave" -> Color(0xFF3C82EB)
        "shineon" -> Color(0xFFF5A524)
        "pigeon" -> Color(0xFFB49CFF)
        "cs1" -> Color(0xFFF2A93B)
        "hyp1" -> Color(0xFFA35CFF)
        "tonitor" -> Color(0xFF00D6FF)
        "noizdemons" -> Color(0xFF7DFF3C)
        "coralflower" -> Color(0xFFFF7AA8)
        "mc1" -> Color(0xFFFF8A1E)
        "beetle" -> Color(0xFFFF3AD0)
        "min1" -> Color(0xFF3F7BE0)
        "purplemonkey" -> Color(0xFF9B5CFF)
        "gammawave" -> Color(0xFFFFB800)
        "tuna" -> Color(0xFF3DB7E4)
        "frodhes" -> Color(0xFFE8A33D)
        "fomajam" -> Color(0xFFA3E635)
        "kawa" -> Color(0xFFD9579C)
        "nmsfm" -> Color(0xFF00A632)
        "fm1quest" -> Color(0xFFF2C373)
        "grimm" -> Color(0xFFCD3018)
        "fmini" -> Color(0xFFE0A458)
        "chipbox" -> Color(0xFFA8C61A)
        "clipper" -> Color(0xFF3D8BF2)
        "loom" -> Color(0xFFE3B23C)
        "monk" -> Color(0xFFE0603A)
        "flyfish" -> Color(0xFF3FA9F5)
        "bytemania" -> Color(0xFFFF4F8B)
        "mutable" -> Color(0xFFF0875A)
        "dub" -> Color(0xFF3FB6A8)
        "ketch" -> Color(0xFFF2C94C)
        "fmel" -> Color(0xFFC98A5A)
        "zen" -> Color(0xFFF4A6C0)
        "golana" -> Color(0xFFF5A54A)
        "golanadz" -> Color(0xFFE8873A)
        "modelar" -> Color(0xFFFF3D9A)
        "niagara" -> Color(0xFFFF2A2A)
        "unholy" -> Color(0xFFC0392B)
        "emb" -> Color(0xFFB5651D)
        "cutador" -> Color(0xFFF0A030)
        "hera" -> Color(0xFF1EA7F2)
        "frasil" -> Color(0xFF5CC8F5)
        "tessera" -> Color(0xFFECB848)
        "drift" -> Color(0xFFE6E6E6)
        "delrio" -> Color(0xFFE5733A)
        "carnac" -> Color(0xFFD6963C)
        "sidou" -> Color(0xFFA77BFF)
        "mono2k" -> Color(0xFFF2F1EC)
        "caravel" -> Color(0xFFF7A21B)
        "midimon" -> Color(0xFF39BA62)
        "makohai" -> Color(0xFF5AD1E0)
        "estampie" -> Color(0xFFC4452E)
        "fizz" -> Color(0xFF4A9DFF)
        "matryoshka" -> Color(0xFFF5A300)
        "loomfm1" -> Color(0xFFB040FF)
        "tastywaves" -> Color(0xFF9B7BFF)
        "arctic" -> Color(0xFF7FD6E8)
        "s1vave" -> Color(0xFF2AD4A0)
        "gator" -> Color(0xFFFF2E6E)
        "monohull" -> Color(0xFFF5A623)
        "perf1" -> Color(0xFFF2B84B)
        "j6" -> Color(0xFF2F8FD8)
        "guitchordi" -> Color(0xFFFF8A3D)
        "ravel" -> Color(0xFFE05A6A)
        "berlinan" -> Color(0xFFF28C28)
        "mnm1" -> Color(0xFF90E4F0)
        "mope1" -> Color(0xFF6FA3B8)
        "kitten" -> Color(0xFFA35CF5)
        "nauclerus" -> Color(0xFFE8742A)
        "ptfm" -> Color(0xFF4FD6E0)
        "spampy" -> Color(0xFFFF4FC0)
        "spellbox" -> Color(0xFFE06A2C)
        "dungeontracker" -> Color(0xFF7FD88A)
        "nachtfahrt" -> Color(0xFFC48CFF)
        "daurade" -> Color(0xFF5AB4FF)
        "sill" -> Color(0xFFFF3FA4)
        "kwasub" -> Color(0xFF3FDC5A)
        "luxr" -> Color(0xFFF2A33A)
        "fmdj" -> Color(0xFF9BBC0F)
        "fm1gm" -> Color(0xFF4AA3DF)
        "386dx" -> Color(0xFFF5D63A)
        "opn1" -> Color(0xFFE8303A)
        "sid1" -> Color(0xFF8F7FE0)
        "minitone" -> Color(0xFF22B8A5)
        "fm1sid" -> Color(0xFF6C8CD5)
        "morpheus" -> Color(0xFF3CCF6E)
        "bunker" -> Color(0xFF9D7BE8)
        "amb" -> Color(0xFFF6D79A)
        "ctl" -> Color(0xFFEF4A33)
        "winampfm" -> Color(0xFF5B7BFF)
        "mycelium" -> Color(0xFF9CCCE4)
        "polly" -> Color(0xFF7B93E0)
        "etch" -> Color(0xFFCFCFCF)
        "fm106" -> Color(0xFF2A9FD6)
        "tribe" -> Color(0xFFFC6C54)
        "duell" -> Color(0xFFE6E6E6)
        "skiff" -> Color(0xFFFCB424)
        "fart" -> Color(0xFF84CC9C)
        "orbit" -> Color(0xFF4F7FF0)
        "felucca8", "felucca-smk" -> Color(0xFFAFD9F4)
        "vector37" -> Color(0xFF5FC48A)
        "makkuro37" -> Color(0xFFD0524F)
        "speak37" -> Color(0xFF38BDF8)
        "erik37" -> Color(0xFFF59E0B)
        "snoop37" -> Color(0xFFE6D1B9)
        else -> when {
            firmware.isOfficialStock -> SynthPhosphor
            firmware.category == FirmwareCategory.GROOVEBOX.name -> SynthOrange
            firmware.category == FirmwareCategory.CHIPTUNE.name -> SynthAmberWarn
            firmware.category == FirmwareCategory.AMBIENT.name -> SynthPhosphor
            else -> SynthCyan
        }
    }
}

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
 * Displays the authentic unique interface screenshot of each of the 160 firmwares from TheFirmwareHub.com
 * bundled directly in `assets/fw_screens/<id>.png` for 100% offline work, with tap-to-zoom lightbox inspection.
 */
@Composable
fun FirmwareArtworkBanner(
    firmware: FirmwareEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenBitmap = remember(firmware.id) {
        loadFirmwareScreenAssetBitmap(context, firmware)
    }
    val fallbackDrawableRes = remember(firmware.id, firmware.category) {
        resolveFirmwareInterfaceDrawable(firmware)
    }
    val accentColor = remember(firmware.id, firmware.category, firmware.isOfficialStock) {
        resolveFirmwareAccentColor(firmware)
    }
    var showZoomDialog by remember { mutableStateOf(false) }

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
                                text = "Экран прошивки: ${firmware.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = accentColor
                            )
                            Text(
                                text = "${firmware.version} • Автор: ${firmware.author} • ID: ${firmware.id}",
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
                        if (screenBitmap != null) {
                            Image(
                                bitmap = screenBitmap,
                                contentDescription = "Полный интерфейс прошивки ${firmware.name}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Image(
                                painter = painterResource(id = fallbackDrawableRes),
                                contentDescription = "Полный интерфейс прошивки ${firmware.name}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
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
            .height(184.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showZoomDialog = true },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0E0C0D),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.72f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            Color(0xFF0E0C0D)
                        )
                    )
                )
        ) {
            if (screenBitmap != null) {
                Image(
                    bitmap = screenBitmap,
                    contentDescription = "Экран интерфейса прошивки ${firmware.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = fallbackDrawableRes),
                    contentDescription = "Экран интерфейса прошивки ${firmware.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 8.dp, horizontal = 10.dp),
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
                        tint = if (firmware.isDownloaded) SynthPhosphor else accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (firmware.isDownloaded) {
                            "${firmware.id.uppercase()} • .BIN СКАЧАН"
                        } else {
                            "${firmware.id.uppercase()} • ЭКРАН UI"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (firmware.isDownloaded) SynthPhosphor else accentColor
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
