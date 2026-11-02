package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.ConnectedUsbDeviceInfo
import com.example.domain.UsbProfileType
import com.example.ui.FlashSessionUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor

@Composable
fun UnbrickerScreen(
    connectedDevice: ConnectedUsbDeviceInfo,
    flashState: FlashSessionUiState,
    onSelectSimulatedProfile: (UsbProfileType) -> Unit,
    onRunSoftBrickRecovery: () -> Unit,
    onSetWizardStep: (Int) -> Unit,
    onOpenLogsAndScripts: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("unbricker_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Banner: JieLi AC791N / WL82 Unbrick Center
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, SynthOrange)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Healing,
                            contentDescription = "Unbricker",
                            tint = SynthOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "M-VAVE FM-1 Unbricker Studio",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Восстановление процессора JieLi AC791N / WL82 (Soft-Brick & Hard-Brick)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ЗОЛОТОЕ ПРАВИЛО ВОССТАНОВЛЕНИЯ M-VAVE FM-1:",
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthAmberWarn
                            )
                            Text(
                                text = "Никогда не записывайте сырые .fwsc пакеты по адресу 0x0000! Область 0x0000–0x3FFF содержит загрузочный заголовок JieLi WL82. Этот модуль автоматически сохраняет полный дамп памяти и прошивает чистый стоковый образ V16 строго по смещению 0x00004000.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Mode 1: Soft-Brick Direct USB Recovery (WL82 UBOOT1.00 VID_4C4A:PID_8057)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SynthPhosphor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "СЦЕНАРИЙ 1 • БЕЗ ПАЙКИ И ДОП. ПЛАТ",
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthPhosphor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "VID_4C4A : PID_8057",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Soft-Brick: Черный экран / WL82 UBOOT1.00",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Если после неудачной прошивки синтезатор не включает OLED-экран, но при подключении по USB определяется как «WL82 UBOOT1.00», аппаратный загрузчик цел и готов к прямой перезаписи региона 0x4000.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Конвейер автоматического Soft-Brick Unbrick:",
                            style = MaterialTheme.typography.labelLarge,
                            color = SynthCyan
                        )
                        Text(
                            text = "1. Захват интерфейса загрузчика WL82 UBOOT1.00 (или посылка SysEx F0 22 24 35 7D F7)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "2. Полный дамп текущей флеш-памяти (0x00000–0xFFFFF) в раздел «Бэкапы»",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "3. Блокировка сектора загрузчика 0x0000–0x3FFF от перезаписи",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "4. Запись проверенной заводской прошивки Official Stock V16 по адресу 0x00004000 и сверка SHA-256",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRunSoftBrickRecovery,
                        enabled = !flashState.isFlashing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("soft_unbrick_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SynthPhosphor,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Запустить Unbrick"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (flashState.isFlashing) {
                                "ВЫПОЛНЯЕТСЯ ВОССТАНОВЛЕНИЕ WL82…"
                            } else {
                                "ВОССТАНОВИТЬ СТОК V16 В 1 КЛИК (0x4000)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Mode 2: Interactive Hard-Brick Hardware Wizard (RP2040 / Pi Pico "FM-1 Transporter")
        item {
            val step = flashState.activeUnbrickStep
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SynthCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "СЦЕНАРИЙ 2 • HARD-BRICK TRANSPORTER BRIDGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "RP2040 + wl82loader",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Мастер Hard-Brick: Мост RP2040 / Pi Pico Transporter",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Используется, если был затерт сектор 0x0000 и синтезатор вообще не определяется по USB. Плата Seeed XIAO RP2040 или Raspberry Pi Pico с прошивкой wl82loader.bin принудительно переводит чип JieLi в режим MaskROM по линиям USB D+/D-.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Step selector tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val stepLabels = listOf("1. Плата", "2. Линии D+/D-", "3. Захват ROM", "4. Прошивка")
                        stepLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = step == index,
                                onClick = { onSetWizardStep(index) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SynthCyan.copy(alpha = 0.24f),
                                    selectedLabelColor = SynthCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                            when (step) {
                                0 -> {
                                    Text(
                                        text = "Шаг 1: Подготовка RP2040 / Raspberry Pi Pico",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SynthCyan
                                    )
                                    Text(
                                        text = "• Зажмите кнопку BOOTSEL на плате Seeed Studio XIAO RP2040 или Raspberry Pi Pico и подключите ее по USB.\n• Скопируйте образ моста `wl82loader.uf2` на появившийся диск RPI-RP2.\n• После перезагрузки плата определится как CDC-мост (VID_2E8A : PID_000A).",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                1 -> {
                                    Text(
                                        text = "Шаг 2: Схема подключения линий данных к M-VAVE FM-1",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SynthCyan
                                    )
                                    Text(
                                        text = "Подключите 3 линии от RP2040 к USB-порту (или тест-поинтам на плате) выключенного синтезатора M-VAVE FM-1:\n• GPIO 0 (RP2040)  ──►  USB D+ (Зеленый провод FM-1)\n• GPIO 1 (RP2040)  ──►  USB D- (Белый провод FM-1)\n• GND (RP2040)     ──►  GND (Черный провод / корпус FM-1)",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SynthPhosphor
                                    )
                                }
                                2 -> {
                                    Text(
                                        text = "Шаг 3: Принудительный вход в JieLi MaskROM",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SynthCyan
                                    )
                                    Text(
                                        text = "• Подайте питание на M-VAVE FM-1 одновременно с активным мостом RP2040.\n• Мост автоматически пошлет высокочастотный ключ синхронизации по D+/D- и переведет процессор AC791N/WL82 в режим низкоуровневого программирования `WL82 UBOOT1.00`.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                else -> {
                                    Text(
                                        text = "Шаг 4: Запись полного дампа (Boot 0x0000 + App 0x4000)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SynthCyan
                                    )
                                    Text(
                                        text = "• Нажмите кнопку ниже, чтобы переключить профиль шины на RP2040 Transporter Bridge и записать чистый заводской образ вместе с восстановлением заголовка.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
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
                            onClick = {
                                onSelectSimulatedProfile(UsbProfileType.RP2040_TRANSPORTER_BRIDGE)
                                onRunSoftBrickRecovery()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, SynthCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "Мост RP2040",
                                tint = SynthCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Прошить через RP2040", color = SynthCyan)
                        }

                        OutlinedButton(
                            onClick = onOpenLogsAndScripts,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, SynthOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Скрипты для ПК",
                                tint = SynthOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Скрипты для ПК", color = SynthOrange)
                        }
                    }
                }
            }
        }
    }
}
