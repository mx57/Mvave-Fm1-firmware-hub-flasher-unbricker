// File: app/src/main/java/com/example/ui/screens/EmulatorScreen.kt
package com.example.ui.screens

import com.example.ui.AppLanguage
import com.example.ui.AppStrings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirmwareEntity
import com.example.domain.EmulatorArpMode
import com.example.domain.EmulatorOledDisplayMode
import com.example.domain.EmulatorOledPage
import com.example.domain.EmulatorSynthEngineType
import com.example.domain.SequencerStep
import com.example.ui.EmulatorSlot
import com.example.ui.EmulatorUiState
import com.example.ui.components.loadFirmwareScreenAssetBitmap
import com.example.ui.components.resolveFirmwareAccentColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCrimsonError
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val NOTE_NAMES_16 = listOf(
    "C", "C#", "D", "D#", "E", "F", "F#", "G",
    "G#", "A", "A#", "B", "C+", "C#+", "D+", "D#+"
)

private val BLACK_KEY_INDICES = setOf(1, 3, 6, 8, 10, 13, 15)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmulatorScreen(
    appLanguage: AppLanguage = AppLanguage.RU,

    emulatorState: EmulatorUiState,
    allFirmwares: List<FirmwareEntity>,
    onSelectActiveSlot: (EmulatorSlot) -> Unit,
    onAssignFirmwareToSlot: (EmulatorSlot, FirmwareEntity) -> Unit,
    onSelectPresetIndex: (Int) -> Unit,
    onSelectEngineType: (EmulatorSynthEngineType) -> Unit,
    onSelectOledPage: (EmulatorOledPage) -> Unit,
    onSelectOledDisplayMode: (EmulatorOledDisplayMode) -> Unit,
    onUpdateKnobParameter: (knobIndexOnPage: Int, normalizedValue: Float) -> Unit,
    onToggleSequencerPlay: () -> Unit,
    onSelectPatternIndex: (Int) -> Unit,
    onToggleStepEditMode: () -> Unit,
    onToggleStepActive: (Int) -> Unit,
    onSelectStepForNoteEdit: (Int) -> Unit,
    onCycleArpMode: () -> Unit,
    onChangeOctave: (Int) -> Unit,
    onPadNoteOn: (Int) -> Unit,
    onPadNoteOff: (Int) -> Unit,
    onToggleWavRecording: () -> Unit,
    onExportRecordedWav: () -> Unit,
    onOpenActiveFirmwareInFlasher: (FirmwareEntity) -> Unit,
    onStopAudioOnLeave: () -> Unit
) {
    val activeFirmware = emulatorState.activeFirmware
    val activeProfile = emulatorState.activeProfile
    val accentColor = remember(activeFirmware?.id) {
        activeFirmware?.let { resolveFirmwareAccentColor(it) } ?: SynthCyan
    }

    var pickerTargetSlot by remember { mutableStateOf<EmulatorSlot?>(null) }
    var pickerSearchQuery by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            // Release held notes when leaving screen so notes never hang
            onStopAudioOnLeave()
        }
    }

    if (pickerTargetSlot != null) {
        val slot = pickerTargetSlot!!
        val filteredList = remember(allFirmwares, pickerSearchQuery) {
            val q = pickerSearchQuery.trim().lowercase()
            if (q.isEmpty()) allFirmwares
            else allFirmwares.filter {
                it.name.lowercase().contains(q) ||
                    it.id.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.tagsCsv.lowercase().contains(q)
            }
        }
        AlertDialog(
            onDismissRequest = { pickerTargetSlot = null },
            title = {
                Text(
                    text = "Выбор прошивки для ${slot.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SynthCyan
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pickerSearchQuery,
                        onValueChange = { pickerSearchQuery = it },
                        placeholder = { Text("Поиск из ${allFirmwares.size} прошивок (Sloop, SID, X0X…)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList, key = { it.id }) { fw ->
                            val fwColor = resolveFirmwareAccentColor(fw)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, fwColor.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAssignFirmwareToSlot(slot, fw)
                                        pickerTargetSlot = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${fw.name} (${fw.version})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${fw.category} • ${fw.author}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = fwColor
                                        )
                                    }
                                    Text(
                                        text = fw.crc32,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SynthPhosphor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerTargetSlot = null }) {
                    Text("Закрыть")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emulator_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Hardware Header & A/B Firmware Slot Switcher + WAV Recorder
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = accentColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "M-VAVE FM-1 HARDWARE & DSP EMULATOR • 44.1 KHZ PCM",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activeFirmware?.let { "${it.name} (${it.version})" } ?: "Загрузка прошивки…",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = activeProfile?.let {
                                    "Движок: ${emulatorState.activeEngineType.title} • Автор: ${it.author}"
                                } ?: "Выберите прошивку для эмуляции",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (activeFirmware != null) {
                            Button(
                                onClick = { onOpenActiveFirmwareInFlasher(activeFirmware) },
                                modifier = Modifier.testTag("emulator_flash_active_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SynthOrange,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "В прошивку",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Прошить",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // A/B Comparison Deck
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AbSlotChip(
                            slot = EmulatorSlot.SLOT_A,
                            firmware = emulatorState.slotAFirmware,
                            isSelected = emulatorState.activeSlot == EmulatorSlot.SLOT_A,
                            onSelectSlot = { onSelectActiveSlot(EmulatorSlot.SLOT_A) },
                            onChangeFirmware = {
                                pickerSearchQuery = ""
                                pickerTargetSlot = EmulatorSlot.SLOT_A
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("emulator_slot_a")
                        )

                        IconButton(
                            onClick = {
                                val next = if (emulatorState.activeSlot == EmulatorSlot.SLOT_A) {
                                    EmulatorSlot.SLOT_B
                                } else {
                                    EmulatorSlot.SLOT_A
                                }
                                onSelectActiveSlot(next)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("emulator_ab_swap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompareArrows,
                                contentDescription = "A/B Сравнение",
                                tint = SynthCyan
                            )
                        }

                        AbSlotChip(
                            slot = EmulatorSlot.SLOT_B,
                            firmware = emulatorState.slotBFirmware,
                            isSelected = emulatorState.activeSlot == EmulatorSlot.SLOT_B,
                            onSelectSlot = { onSelectActiveSlot(EmulatorSlot.SLOT_B) },
                            onChangeFirmware = {
                                pickerSearchQuery = ""
                                pickerTargetSlot = EmulatorSlot.SLOT_B
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("emulator_slot_b")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // WAV Audio Recorder & Export Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggleWavRecording,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("emulator_record_wav_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (emulatorState.isRecordingWav) {
                                    SynthCrimsonError
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (emulatorState.isRecordingWav) {
                                    Color.White
                                } else {
                                    SynthCrimsonError
                                }
                            )
                        ) {
                            Icon(
                                imageVector = if (emulatorState.isRecordingWav) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = "Запись WAV",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (emulatorState.isRecordingWav) {
                                    String.format(Locale.US, "СТОП ЗАПИСЬ (%.1f c)", emulatorState.recordedDurationSec)
                                } else {
                                    "ЗАПИСЬ ЗВУКА (.WAV)"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (emulatorState.lastRecordedWavBytes != null && !emulatorState.isRecordingWav) {
                            OutlinedButton(
                                onClick = onExportRecordedWav,
                                modifier = Modifier.testTag("emulator_export_wav_button"),
                                border = BorderStroke(1.dp, SynthPhosphor)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SaveAlt,
                                    contentDescription = "Сохранить WAV",
                                    tint = SynthPhosphor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Скачать .WAV (${(emulatorState.lastRecordedWavBytes.size / 1024).coerceAtLeast(1)} KB)",
                                    color = SynthPhosphor,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Authentic M-VAVE FM-1 Chassis: Interactive 128x64 OLED Display + Preset & Engine Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0D12)),
                border = BorderStroke(1.5.dp, Color(0xFF263142))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top Bezel: Preset Selector & OLED Display Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val count = activeProfile?.presets?.size ?: 8
                                    val prev = (emulatorState.selectedPresetIndex - 1 + count) % count
                                    onSelectPresetIndex(prev)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF151C28), RoundedCornerShape(6.dp))
                                    .testTag("emulator_prev_preset")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Пред. пресет",
                                    tint = accentColor
                                )
                            }

                            Surface(
                                color = Color(0xFF141B26),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                            ) {
                                val preset = activeProfile?.presets?.getOrNull(emulatorState.selectedPresetIndex)
                                Text(
                                    text = "PATCH P0${emulatorState.selectedPresetIndex + 1}: ${preset?.name ?: "Init Patch"}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val count = activeProfile?.presets?.size ?: 8
                                    val next = (emulatorState.selectedPresetIndex + 1) % count
                                    onSelectPresetIndex(next)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFF151C28), RoundedCornerShape(6.dp))
                                    .testTag("emulator_next_preset")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "След. пресет",
                                    tint = accentColor
                                )
                            }
                        }

                        // OLED Display Mode Cycle Button
                        OutlinedButton(
                            onClick = {
                                val modes = EmulatorOledDisplayMode.entries
                                val nextMode = modes[(emulatorState.oledDisplayMode.ordinal + 1) % modes.size]
                                onSelectOledDisplayMode(nextMode)
                            },
                            modifier = Modifier.testTag("emulator_oled_mode_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Режим OLED",
                                tint = SynthCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = emulatorState.oledDisplayMode.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive 128x64 Hardware OLED Screen
                    Fm1InteractiveOledScreen(
                        firmware = activeFirmware,
                        emulatorState = emulatorState,
                        accentColor = accentColor,
                        onCycleDisplayMode = {
                            val modes = EmulatorOledDisplayMode.entries
                            val nextMode = modes[(emulatorState.oledDisplayMode.ordinal + 1) % modes.size]
                            onSelectOledDisplayMode(nextMode)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // OLED Parameter Page Selector Tabs (P1:OSC/FM, P2:FLT/ENV, P3:MOD/FX, P4:SEQ/ARP)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EmulatorOledPage.entries.forEach { page ->
                            val selected = emulatorState.selectedOledPage == page
                            Surface(
                                color = if (selected) accentColor.copy(alpha = 0.22f) else Color(0xFF141A24),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) accentColor else Color(0xFF283244)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectOledPage(page) }
                                    .testTag("emulator_page_${page.index}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = page.shortCode,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4 Hardware Rotary Encoders (Knobs K1 - K4)
                    val pageOffset = emulatorState.selectedOledPage.index * 4
                    val knobLabels = emulatorState.selectedOledPage.knobLabels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (k in 0 until 4) {
                            val paramIdx = pageOffset + k
                            val paramValue = emulatorState.parameters.getOrElse(paramIdx) { 0.5f }
                            val formattedVal = formatKnobDisplayValue(
                                page = emulatorState.selectedOledPage,
                                knobIndex = k,
                                norm = paramValue
                            )
                            HardwareRotaryEncoderKnob(
                                knobNumber = k + 1,
                                label = knobLabels.getOrElse(k) { "PARAM ${k + 1}" },
                                formattedValue = formattedVal,
                                normalizedValue = paramValue,
                                accentColor = accentColor,
                                onValueChange = { newVal ->
                                    onUpdateKnobParameter(k, newVal)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("emulator_knob_${k + 1}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Synthesis Engine Architecture Quick Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DSP Ядро:",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        EmulatorSynthEngineType.entries.forEach { eng ->
                            val isSelected = emulatorState.activeEngineType == eng
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectEngineType(eng) },
                                label = {
                                    Text(
                                        text = eng.code,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.22f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Hardware Transport, 16-Step Sequencer & Pattern Deck + 16 Chromatic Performance Pads
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E131B)),
                border = BorderStroke(1.5.dp, Color(0xFF283447))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Transport Buttons Row: PLAY/STOP, DEMO PATTERN, STEP EDIT, ARP, OCTAVE
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PLAY / STOP Sequencer Button
                        Button(
                            onClick = onToggleSequencerPlay,
                            modifier = Modifier.testTag("emulator_play_stop_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (emulatorState.isSequencerPlaying) SynthPhosphor else SynthOrange,
                                contentColor = Color.Black
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (emulatorState.isSequencerPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = "Старт/Стоп секвенсора",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (emulatorState.isSequencerPlaying) "STOP SEQ" else "PLAY DEMO SEQ",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Demo Pattern Cycle Button
                        OutlinedButton(
                            onClick = {
                                val totalPatterns = activeProfile?.patterns?.size ?: 4
                                val nextPat = (emulatorState.selectedPatternIndex + 1) % totalPatterns
                                onSelectPatternIndex(nextPat)
                            },
                            modifier = Modifier.testTag("emulator_cycle_pattern_button"),
                            border = BorderStroke(1.dp, SynthCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Паттерн",
                                tint = SynthCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PAT ${emulatorState.selectedPatternIndex + 1}/4",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = SynthCyan
                            )
                        }

                        // STEP EDIT vs LIVE PLAY Mode Button
                        OutlinedButton(
                            onClick = onToggleStepEditMode,
                            modifier = Modifier.testTag("emulator_step_edit_toggle"),
                            border = BorderStroke(
                                1.dp,
                                if (emulatorState.isStepEditMode) SynthAmberWarn else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (emulatorState.isStepEditMode) {
                                    SynthAmberWarn.copy(alpha = 0.18f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Режим шагов",
                                tint = if (emulatorState.isStepEditMode) SynthAmberWarn else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (emulatorState.isStepEditMode) "РЕД. ШАГОВ [ON]" else "LIVE КЛАВИШИ",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (emulatorState.isStepEditMode) SynthAmberWarn else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Arpeggiator / Chord Mode Button
                        OutlinedButton(
                            onClick = onCycleArpMode,
                            modifier = Modifier.testTag("emulator_arp_button"),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "ARP: ${emulatorState.arpMode.label}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor
                            )
                        }

                        // Octave - / + Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF161E2B), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF2A364B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            TextButton(
                                onClick = { onChangeOctave(emulatorState.baseOctave - 1) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.size(width = 40.dp, height = 32.dp)
                            ) {
                                Text("OCT-", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = SynthCyan)
                            }
                            Text(
                                text = "C${emulatorState.baseOctave}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = SynthPhosphor,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            TextButton(
                                onClick = { onChangeOctave(emulatorState.baseOctave + 1) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.size(width = 40.dp, height = 32.dp)
                            ) {
                                Text("OCT+", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = SynthCyan)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 16-Step Sequencer LED Strip (Interactive)
                    Text(
                        text = if (emulatorState.isStepEditMode) {
                            "16-ШАГОВЫЙ СЕКВЕНСОР (Нажмите на шаг 1–16 для вкл/выкл, затем выберите ноту на клавишах ниже • Выбран шаг #${emulatorState.selectedStepForEdit + 1})"
                        } else {
                            "16-ШАГОВАЯ ДОРОЖКА СЕКВЕНСОРА (Нажмите на любой шаг для быстрого Mute/Unmute шага)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (emulatorState.isStepEditMode) SynthAmberWarn else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Sequencer16StepStrip(
                        steps = emulatorState.sequencerSteps,
                        currentPlayingStep = if (emulatorState.isSequencerPlaying) emulatorState.currentStepIndex else -1,
                        selectedEditStep = if (emulatorState.isStepEditMode) emulatorState.selectedStepForEdit else -1,
                        accentColor = accentColor,
                        onStepTap = { stepIdx ->
                            if (emulatorState.isStepEditMode) {
                                onSelectStepForNoteEdit(stepIdx)
                            } else {
                                onToggleStepActive(stepIdx)
                            }
                        },
                        onStepDoubleTapOrLongPress = { stepIdx ->
                            onToggleStepActive(stepIdx)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(color = Color(0xFF243044))

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "КЛАВИАТУРА / 16 ТРИГГЕР-ПЭДОВ M-VAVE FM-1 (C${emulatorState.baseOctave} – D#${emulatorState.baseOctave + 1})",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
                        )
                        Text(
                            text = if (emulatorState.lastTriggeredNoteMidi > 0) {
                                "MIDI NOTE: #${emulatorState.lastTriggeredNoteMidi}"
                            } else {
                                "ГОТОВ К ИГРЕ"
                            },
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 16 Hardware Pads in 2 rows of 8 (Authentic FM-1 Pad Matrix)
                    Fm1SixteenHardwarePadsGrid(
                        baseOctave = emulatorState.baseOctave,
                        activeNotes = emulatorState.activePadSemitones,
                        accentColor = accentColor,
                        isStepEditMode = emulatorState.isStepEditMode,
                        onPadPress = { semitone ->
                            onPadNoteOn(semitone)
                        },
                        onPadRelease = { semitone ->
                            onPadNoteOff(semitone)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AbSlotChip(
    slot: EmulatorSlot,
    firmware: FirmwareEntity?,
    isSelected: Boolean,
    onSelectSlot: () -> Unit,
    onChangeFirmware: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slotColor = if (slot == EmulatorSlot.SLOT_A) SynthCyan else SynthOrange
    Surface(
        color = if (isSelected) slotColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) slotColor else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.clickable { onSelectSlot() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${slot.title} ${if (isSelected) "• АКТИВЕН" else "• СРАВНИТЬ"}",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) slotColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = firmware?.name ?: "Выбрать…",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onChangeFirmware,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Сменить прошивку в слоте",
                    tint = slotColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun Fm1InteractiveOledScreen(
    firmware: FirmwareEntity?,
    emulatorState: EmulatorUiState,
    accentColor: Color,
    onCycleDisplayMode: () -> Unit
) {
    val context = LocalContext.current
    val screenBitmap = remember(firmware?.id) {
        firmware?.let { loadFirmwareScreenAssetBitmap(context, it) }
    }
    val bpm = (60 + emulatorState.parameters.getOrElse(12) { 0.5f } * 120).roundToInt()
    val presetName = emulatorState.activeProfile?.presets
        ?.getOrNull(emulatorState.selectedPresetIndex)?.name ?: "Init Patch"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(195.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF04070B))
            .border(2.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
            .clickable { onCycleDisplayMode() }
            .testTag("emulator_oled_display")
    ) {
        when (emulatorState.oledDisplayMode) {
            EmulatorOledDisplayMode.AUTHENTIC_SKIN_HUD -> {
                if (screenBitmap != null) {
                    Image(
                        bitmap = screenBitmap,
                        contentDescription = "OLED экран прошивки ${firmware?.name}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        contentScale = ContentScale.Crop,
                        alpha = 0.42f
                    )
                }

                // Live Oscilloscope Overlay + Authentic 128x64 Telemetry HUD
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Subtle OLED scanlines
                    val scanStep = 4.dp.toPx()
                    var y = 0f
                    while (y < h) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.28f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                        y += scanStep
                    }

                    // Live waveform in the center-bottom area
                    val samples = emulatorState.waveformSamples
                    if (samples.isNotEmpty()) {
                        val path = Path()
                        val midY = h * 0.60f
                        val amp = h * 0.24f
                        val stepX = w / (samples.size - 1).coerceAtLeast(1)
                        for (i in samples.indices) {
                            val px = i * stepX
                            val py = (midY - samples[i] * amp).coerceIn(8f, h - 8f)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(
                            path = path,
                            color = accentColor,
                            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Top & Bottom OLED Pixel Telemetry Overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xCC060A10),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "OLED 128x64 • ${firmware?.name?.uppercase()?.take(14) ?: "FM-1"} ${firmware?.version ?: ""}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = Color(0xCC060A10),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "${emulatorState.activeEngineType.code} • $bpm BPM",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthPhosphor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bottom parameter bar inside OLED
                    Surface(
                        color = Color(0xD905080E),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF223042)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val pageIdx = emulatorState.selectedOledPage.index * 4
                        val labels = emulatorState.selectedOledPage.knobLabels
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PRESET: $presetName",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (emulatorState.isSequencerPlaying) {
                                        "SEQ STEP [${(emulatorState.currentStepIndex + 1).toString().padStart(2, '0')}/16]"
                                    } else {
                                        "PEAK ${(emulatorState.peakLevel * 100).roundToInt()}%"
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SynthCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (k in 0 until 4) {
                                    val v = (emulatorState.parameters.getOrElse(pageIdx + k) { 0.5f } * 100).roundToInt()
                                    Text(
                                        text = "K${k + 1}:${labels[k].take(6)} $v%",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            EmulatorOledDisplayMode.LIVE_OSCILLOSCOPE -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    for (gx in 1..7) {
                        val x = w * (gx / 8f)
                        drawLine(
                            color = Color(0xFF142234),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }
                    for (gy in 1..3) {
                        val y = h * (gy / 4f)
                        drawLine(
                            color = Color(0xFF142234),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    val samples = emulatorState.waveformSamples
                    if (samples.isNotEmpty()) {
                        val path = Path()
                        val midY = h * 0.50f
                        val amp = h * 0.38f
                        val stepX = w / (samples.size - 1).coerceAtLeast(1)
                        for (i in samples.indices) {
                            val px = i * stepX
                            val py = (midY - samples[i] * amp).coerceIn(6f, h - 6f)
                            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(
                            path = path,
                            color = SynthPhosphor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Bottom spectrum / harmonic bars
                    val barCount = 24
                    val barW = (w / barCount) * 0.68f
                    for (b in 0 until barCount) {
                        val sIdx = (b * samples.size / barCount).coerceIn(0, samples.lastIndex.coerceAtLeast(0))
                        val mag = kotlin.math.abs(samples.getOrElse(sIdx) { 0f })
                        val barH = (mag * h * 0.42f + 4f).coerceAtMost(h * 0.45f)
                        val bx = b * (w / barCount) + 4f
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.55f),
                            topLeft = Offset(bx, h - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LIVE PCM OSCILLOSCOPE (44.1 kHz / 16-BIT)",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = SynthPhosphor
                    )
                    Text(
                        text = "VU: ${(emulatorState.peakLevel * 100).roundToInt()}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor
                    )
                }
            }

            EmulatorOledDisplayMode.OPERATOR_MATRIX -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AC791N / WL82 DSP REGISTER MATRIX",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "OFFSET: ${firmware?.flashOffsetHex ?: "0x00004000"} • CRC32: ${firmware?.crc32 ?: "N/A"}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor
                        )
                    }

                    val p = emulatorState.parameters
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MatrixRegisterRow(
                            "OP1/OSC1 [TIMBRE/MOD]",
                            p[0],
                            "OP2/OSC2 [RATIO/DET]",
                            p[1],
                            accentColor
                        )
                        MatrixRegisterRow(
                            "OP3/FB   [FEEDBACK]",
                            p[2],
                            "OP4/SUB  [HARM/SUB]",
                            p[3],
                            accentColor
                        )
                        MatrixRegisterRow(
                            "SVF_CUT  [CUTOFF HZ]",
                            p[4],
                            "SVF_RES  [RESONANCE]",
                            p[5],
                            SynthCyan
                        )
                        MatrixRegisterRow(
                            "ENV_ATK  [ATTACK]",
                            p[6],
                            "ENV_DEC  [DECAY/REL]",
                            p[7],
                            SynthPhosphor
                        )
                        MatrixRegisterRow(
                            "LFO_MOD  [RATE/DEPTH]",
                            (p[8] + p[9]) * 0.5f,
                            "DSP_FX   [DELAY/DRV]",
                            (p[10] + p[11]) * 0.5f,
                            SynthOrange
                        )
                    }

                    Text(
                        text = "Нажмите на экран OLED для переключения режима отображения",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MatrixRegisterRow(
    leftLabel: String,
    leftVal: Float,
    rightLabel: String,
    rightVal: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RegisterBarCell(leftLabel, leftVal, color, Modifier.weight(1f))
        RegisterBarCell(rightLabel, rightVal, color, Modifier.weight(1f))
    }
}

@Composable
private fun RegisterBarCell(
    label: String,
    norm: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val hexByte = (norm * 255).roundToInt().coerceIn(0, 255)
        .toString(16).uppercase().padStart(2, '0')
    Row(
        modifier = modifier
            .background(Color(0xFF0E1520), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "0x$hexByte (${(norm * 100).roundToInt()}%)",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HardwareRotaryEncoderKnob(
    knobNumber: Int,
    label: String,
    formattedValue: String,
    normalizedValue: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValState by rememberUpdatedState(normalizedValue)
    val currentCallback by rememberUpdatedState(onValueChange)

    Surface(
        color = Color(0xFF121822),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF263246)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "K$knobNumber • $label",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Rotary Encoder Canvas (Vertical/Horizontal Drag)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(62.dp)
                    .pointerInput(knobNumber) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = (-dragAmount.y + dragAmount.x * 0.5f) / 140f
                            val updated = (currentValState + delta).coerceIn(0f, 1f)
                            currentCallback(updated)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 5.dp.toPx()
                    val radius = (size.minDimension - strokeW * 2) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Background track arc (135 deg to 405 deg = 270 deg sweep)
                    drawArc(
                        color = Color(0xFF253042),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Active value arc
                    drawArc(
                        color = accentColor,
                        startAngle = 135f,
                        sweepAngle = 270f * normalizedValue.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Inner metallic knob cap
                    drawCircle(
                        color = Color(0xFF1B2433),
                        radius = radius * 0.72f,
                        center = center
                    )

                    // Indicator pointer line
                    val angleDeg = 135.0 + 270.0 * normalizedValue.coerceIn(0f, 1f)
                    val angleRad = angleDeg * PI / 180.0
                    val lineStart = Offset(
                        x = (center.x + cos(angleRad) * radius * 0.22f).toFloat(),
                        y = (center.y + sin(angleRad) * radius * 0.22f).toFloat()
                    )
                    val lineEnd = Offset(
                        x = (center.x + cos(angleRad) * radius * 0.68f).toFloat(),
                        y = (center.y + sin(angleRad) * radius * 0.68f).toFloat()
                    )
                    drawLine(
                        color = accentColor,
                        start = lineStart,
                        end = lineEnd,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formattedValue,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Fine Step Buttons (- / +) for precise touch adjustment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Surface(
                    color = Color(0xFF1A2332),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .size(width = 28.dp, height = 22.dp)
                        .clickable {
                            onValueChange((normalizedValue - 0.05f).coerceIn(0f, 1f))
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = SynthCyan)
                    }
                }
                Surface(
                    color = Color(0xFF1A2332),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .size(width = 28.dp, height = 22.dp)
                        .clickable {
                            onValueChange((normalizedValue + 0.05f).coerceIn(0f, 1f))
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = SynthCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun Sequencer16StepStrip(
    steps: List<SequencerStep>,
    currentPlayingStep: Int,
    selectedEditStep: Int,
    accentColor: Color,
    onStepTap: (Int) -> Unit,
    onStepDoubleTapOrLongPress: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (col in 0..7) {
                    val idx = row * 8 + col
                    val step = steps.getOrNull(idx) ?: SequencerStep(idx, active = false)
                    val isPlayingHere = currentPlayingStep == idx
                    val isEditTarget = selectedEditStep == idx
                    val noteName = NOTE_NAMES_16[step.semitoneOffset.coerceIn(0, 15)]

                    val bgColor = when {
                        isPlayingHere -> SynthPhosphor.copy(alpha = 0.32f)
                        isEditTarget -> SynthAmberWarn.copy(alpha = 0.28f)
                        step.active -> accentColor.copy(alpha = 0.18f)
                        else -> Color(0xFF121822)
                    }
                    val borderColor = when {
                        isPlayingHere -> SynthPhosphor
                        isEditTarget -> SynthAmberWarn
                        step.active -> accentColor.copy(alpha = 0.7f)
                        else -> Color(0xFF232E40)
                    }

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(if (isPlayingHere || isEditTarget) 1.5.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { onStepTap(idx) }
                            .testTag("emulator_seq_step_${idx + 1}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 3.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            when {
                                                isPlayingHere -> SynthPhosphor
                                                step.active -> accentColor
                                                else -> Color(0xFF38465E)
                                            },
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = "${idx + 1}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (step.active) noteName else "---",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (step.active) MaterialTheme.colorScheme.onSurface else Color(0xFF4D5C75)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Fm1SixteenHardwarePadsGrid(
    baseOctave: Int,
    activeNotes: Set<Int>,
    accentColor: Color,
    isStepEditMode: Boolean,
    onPadPress: (Int) -> Unit,
    onPadRelease: (Int) -> Unit
) {
    val currentPress by rememberUpdatedState(onPadPress)
    val currentRelease by rememberUpdatedState(onPadRelease)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0..7) {
                    val padIndex = row * 8 + col
                    val noteLabel = NOTE_NAMES_16[padIndex]
                    val isSharp = BLACK_KEY_INDICES.contains(padIndex)
                    val isPressed = activeNotes.contains(padIndex)

                    val padBg = when {
                        isPressed -> accentColor.copy(alpha = 0.38f)
                        isSharp -> Color(0xFF141B26)
                        else -> Color(0xFF1D2738)
                    }
                    val padBorder = when {
                        isPressed -> SynthPhosphor
                        isStepEditMode -> SynthAmberWarn.copy(alpha = 0.65f)
                        isSharp -> accentColor.copy(alpha = 0.55f)
                        else -> Color(0xFF33445E)
                    }

                    Surface(
                        color = padBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(if (isPressed) 2.dp else 1.dp, padBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .pointerInput(padIndex) {
                                detectTapGestures(
                                    onPress = {
                                        currentPress(padIndex)
                                        tryAwaitRelease()
                                        currentRelease(padIndex)
                                    }
                                )
                            }
                            .testTag("emulator_pad_${padIndex + 1}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "P${padIndex + 1}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (isPressed) SynthPhosphor else accentColor.copy(alpha = 0.35f),
                                            CircleShape
                                        )
                                )
                            }

                            Text(
                                text = noteLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isPressed) SynthPhosphor else if (isSharp) accentColor else Color.White,
                                textAlign = TextAlign.Center
                            )

                            val octNum = if (padIndex >= 12) baseOctave + 1 else baseOctave
                            Text(
                                text = "OCT $octNum",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatKnobDisplayValue(
    page: EmulatorOledPage,
    knobIndex: Int,
    norm: Float
): String {
    val pct = (norm * 100).roundToInt()
    return when (page) {
        EmulatorOledPage.OSC_FM -> when (knobIndex) {
            1 -> {
                val ratios = listOf("0.5x", "1.0x", "1.5x", "2.0x", "3.0x", "4.0x", "5.0x", "7.0x")
                ratios[(norm * (ratios.size - 1)).roundToInt().coerceIn(0, ratios.lastIndex)]
            }
            else -> "$pct%"
        }
        EmulatorOledPage.FILTER_ENV -> when (knobIndex) {
            0 -> {
                val hz = (90.0 * Math.pow(110.0, norm.toDouble())).roundToInt()
                if (hz >= 1000) String.format(Locale.US, "%.1fkHz", hz / 1000f) else "${hz}Hz"
            }
            2, 3 -> "${(5 + norm * 1450).roundToInt()}ms"
            else -> "$pct%"
        }
        EmulatorOledPage.LFO_FX -> when (knobIndex) {
            0 -> String.format(Locale.US, "%.1fHz", 0.25f + norm * norm * 16f)
            else -> "$pct%"
        }
        EmulatorOledPage.SEQ_ARP -> when (knobIndex) {
            0 -> "${(60 + norm * 120).roundToInt()} BPM"
            1 -> "${(15 + norm * 80).roundToInt()}% GATE"
            2 -> "${(norm * 35).roundToInt()}% SWG"
            else -> if (norm > 0.58f) "GLIDE ${pct}%" else "PORT OFF"
        }
    }
}
