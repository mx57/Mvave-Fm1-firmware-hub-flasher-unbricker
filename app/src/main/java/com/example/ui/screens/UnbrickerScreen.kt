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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UsbOff
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FirmwareEntity
import com.example.domain.ConnectedUsbDeviceInfo
import com.example.domain.DeviceDiagnosticVerdict
import com.example.domain.PcbHighlightZone
import com.example.domain.SpiPinShortVector
import com.example.domain.UnbrickMethodType
import com.example.domain.UnbrickTargetFirmwareOption
import com.example.domain.UnbrickUiMode
import com.example.domain.UsbBusDiagnosticReport
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
    selectedFirmware: FirmwareEntity?,
    useSimulationMode: Boolean,
    simulatedProfile: UsbProfileType,
    suspectHardBrickCableConnected: Boolean,
    flashState: FlashSessionUiState,
    onRefreshUsb: () -> Unit,
    onRequestUsbPermission: () -> Unit,
    onSetSimulationMode: (Boolean) -> Unit,
    onSelectSimulatedProfile: (UsbProfileType) -> Unit,
    onSetHardBrickSuspected: (Boolean) -> Unit,
    onSelectUnbrickMethod: (UnbrickMethodType) -> Unit,
    onSelectPinShortVector: (SpiPinShortVector) -> Unit,
    onSetUnbrickUiMode: (UnbrickUiMode) -> Unit,
    onSelectTargetOption: (UnbrickTargetFirmwareOption) -> Unit,
    onSetMaxRetries: (Int) -> Unit,
    onSetBlockSize: (Int) -> Unit,
    onToggleBootSectorRebuild: (Boolean) -> Unit,
    onInjectWl82LoaderNow: () -> Unit,
    onExecuteControlTransferReset: () -> Unit,
    onRunUsbBusAudit: () -> Unit,
    onToggleBootloopCatcher: () -> Unit,
    onRunFwscSurgeon: () -> Unit,
    onRepairBootHeader0x0000: () -> Unit,
    onExportFull1MbDump: () -> Unit,
    onImportCustomFile: () -> Unit,
    onRunSoftBrickRecovery: () -> Unit,
    onSetWizardStep: (Int) -> Unit,
    onOpenLogsAndScripts: () -> Unit
) {
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

    val activeMethod = flashState.selectedUnbrickMethod
    val progress = flashState.currentProgress

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("unbricker_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Hardware Brick Radar & Simulation Stand Control
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, statusAccentColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Healing,
                                contentDescription = "Диагностика кирпича",
                                tint = statusAccentColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Центр реанимации M-VAVE FM-1 (10 методов без пайки)",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "SCSI UBOOT1.00 • VBUS Глитч • Control Reset EP0 • 3-Vector 25Q80 • Сброс АКБ • FWSC • RP2040 • CH341A",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onRunUsbBusAudit,
                                modifier = Modifier.testTag("unbrick_bus_audit_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, SynthCyan)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = "Аудит шины USB",
                                    tint = SynthCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Тест шины", style = MaterialTheme.typography.labelSmall, color = SynthCyan)
                            }

                            OutlinedButton(
                                onClick = onRefreshUsb,
                                modifier = Modifier.testTag("unbrick_scan_usb_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Проверить USB",
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Скан OTG", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = statusAccentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, statusAccentColor.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        connectedDevice.isReadyToFlash -> Icons.Default.CheckCircle
                                        verdict == DeviceDiagnosticVerdict.HARD_BRICK_SUSPECTED -> Icons.Default.ErrorOutline
                                        else -> Icons.Default.UsbOff
                                    },
                                    contentDescription = "Вердикт диагностики",
                                    tint = statusAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ДИАГНОЗ: ${verdict.badgeTitle}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = statusAccentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Устройство: ${connectedDevice.productName} (${connectedDevice.vidPidHex})",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Интерфейсы: ${connectedDevice.usbInterfaceSummary}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan
                            )
                            Text(
                                text = "Рекомендуемый вектор: ${connectedDevice.recommendedUnbrickMethod.title}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthPhosphor
                            )
                            Text(
                                text = connectedDevice.diagnosticDetails,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

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
                                text = "РАЗРЕШИТЬ ДОСТУП ANDROID USB HOST",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!connectedDevice.isConnected && !useSimulationMode) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (suspectHardBrickCableConnected) Color(0xFFFF5252) else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Кабель OTG подключен, но устройство не определяется (0 USB)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (suspectHardBrickCableConnected) Color(0xFFFF5252) else SynthOrange
                                    )
                                    Text(
                                        text = "Включите для активации гида по Hard-Brick без пайки (VBUS-Глитч, 3 вектора замыкания 25Q80 пинцетом или сброс АКБ)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = suspectHardBrickCableConnected,
                                    onCheckedChange = onSetHardBrickSuspected,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF5252))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Simulation Stand Switch on Unbricker Screen for testing all 10 methods
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Виртуальный стенд проверки Unbrick (без физического кабеля)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (useSimulationMode) {
                                    "Активен: доступны все 10 методов реанимации, тест шины OTG, сброс EP0, инъекция wl82loader и сборка 1 МБ дампа"
                                } else {
                                    "Выключен (строгий режим реального USB OTG). Включите для проверки всех функций без железа"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (useSimulationMode) SynthCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useSimulationMode,
                            onCheckedChange = onSetSimulationMode,
                            modifier = Modifier.testTag("unbrick_sim_switch"),
                            colors = SwitchDefaults.colors(checkedThumbColor = SynthCyan)
                        )
                    }

                    if (useSimulationMode) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                UsbProfileType.WL82_UBOOT_SOFTBRICK,
                                UsbProfileType.MVAVE_NORMAL_MIDI,
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

        // 2. Selector for all 10 Unbrick Methods + Expanded Engineering Description & Guides
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Арсенал реанимации M-VAVE FM-1 (10 методов без паяльника):",
                        style = MaterialTheme.typography.titleMedium,
                        color = SynthCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnbrickMethodType.entries.forEach { method ->
                            val isSelected = activeMethod == method
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectUnbrickMethod(method) },
                                modifier = Modifier.testTag("unbrick_method_${method.indexNumber}"),
                                label = {
                                    Text(
                                        text = method.shortBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SynthPhosphor.copy(alpha = 0.22f),
                                    selectedLabelColor = SynthPhosphor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Method Full Guide & Physics Box
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = activeMethod.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = SynthPhosphor
                            )

                            // Status badges: No-Solder, Case Open/Closed, Timing Window
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MethodBadgePill(
                                    text = "БЕЗ ПАЙКИ (100%)",
                                    color = SynthPhosphor
                                )
                                MethodBadgePill(
                                    text = if (activeMethod.requiresCaseOpen) {
                                        "КРЫШКА СНЯТА (ДОСТУП К ПЛАТЕ)"
                                    } else {
                                        "БЕЗ РАЗБОРКИ КОРПУСА"
                                    },
                                    color = if (activeMethod.requiresCaseOpen) SynthOrange else SynthCyan
                                )
                                MethodBadgePill(
                                    text = activeMethod.timingWindowSpec,
                                    color = SynthAmberWarn
                                )
                            }

                            Text(
                                text = "Симптомы кирпича: ${activeMethod.subtitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthOrange
                            )
                            Text(
                                text = "Инструменты: ${activeMethod.hardwareRequirement} • Раздел: ${activeMethod.targetOffsetDefault}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                            Text(
                                text = activeMethod.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Deep Physics & Hardware Mechanism Section
                            Surface(
                                color = SynthCyan.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Физика работы с чипом JieLi AC791N (WL82) и памятью 25Q80:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SynthCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeMethod.physicsExplanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Troubleshooting & Safety Tips
                            Surface(
                                color = SynthOrange.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, SynthOrange.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Важные нюансы и устранение сбоев:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SynthOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeMethod.troubleshootingTips,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Switcher: Interactive Step-by-Step Master + PCB Visualizer vs Manual Expert Engineering Console
                    Text(
                        text = "Формат управления процессом:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnbrickUiMode.entries.forEach { mode ->
                            val selected = flashState.unbrickUiMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onSetUnbrickUiMode(mode) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("unbrick_mode_${mode.name.lowercase()}"),
                                label = {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SynthOrange.copy(alpha = 0.22f),
                                    selectedLabelColor = SynthOrange
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Interactive Step-by-Step Guide Master with M-VAVE FM-1 Motherboard (PCB) & Contact Point Visualizer
        item {
            InteractiveFm1PcbMasterCard(
                activeMethod = activeMethod,
                activeStepIndex = flashState.activeUnbrickStep,
                selectedPinShortVector = flashState.selectedPinShortVector,
                onSelectStep = onSetWizardStep
            )
        }

        // 4. Method-Specific Action & Hardware Tool Card
        item {
            when (activeMethod) {
                UnbrickMethodType.SPI_PIN_SHORT_MASKROM,
                UnbrickMethodType.CH341A_FULL_DUMP_PROGRAMMER -> {
                    SpiFlashPinoutCard(
                        isPinShortMode = activeMethod == UnbrickMethodType.SPI_PIN_SHORT_MASKROM,
                        selectedVector = flashState.selectedPinShortVector,
                        isCatcherActive = flashState.isBootloopCatcherActive,
                        catcherAttempts = flashState.bootloopCatcherAttempts,
                        onSelectVector = onSelectPinShortVector,
                        onToggleCatcher = onToggleBootloopCatcher,
                        onExportFull1MbDump = onExportFull1MbDump
                    )
                }

                UnbrickMethodType.USB_VBUS_GLITCH_HANDSHAKE,
                UnbrickMethodType.BOOTLOOP_SYSEX_CATCHER -> {
                    VbusGlitchAndBootloopCatcherCard(
                        isVbusGlitchMode = activeMethod == UnbrickMethodType.USB_VBUS_GLITCH_HANDSHAKE,
                        isCatcherActive = flashState.isBootloopCatcherActive,
                        catcherAttempts = flashState.bootloopCatcherAttempts,
                        onToggleCatcher = onToggleBootloopCatcher
                    )
                }

                UnbrickMethodType.USB_CONTROL_TRANSFER_RESET -> {
                    ControlTransferResetCard(
                        resetCompleted = flashState.controlTransferResetDone,
                        isReadyToFlash = connectedDevice.isReadyToFlash,
                        isFlashing = flashState.isFlashing,
                        onExecuteReset = onExecuteControlTransferReset
                    )
                }

                UnbrickMethodType.BROWNOUT_PMU_RELEASE -> {
                    BrownoutPmuReleaseCard(
                        isCatcherActive = flashState.isBootloopCatcherActive,
                        catcherAttempts = flashState.bootloopCatcherAttempts,
                        onToggleCatcher = onToggleBootloopCatcher,
                        onRefreshUsb = onRefreshUsb
                    )
                }

                UnbrickMethodType.FWSC_CONTAINER_SURGEON -> {
                    FwscSurgeonCard(
                        lastReport = flashState.lastFwscSurgeonReport,
                        bootRepaired = flashState.bootHeader0x0000Repaired,
                        isFlashing = flashState.isFlashing,
                        isReadyToFlash = connectedDevice.isReadyToFlash,
                        onRunSurgeon = onRunFwscSurgeon,
                        onImportFile = onImportCustomFile,
                        onRepairBoot0x0000 = onRepairBootHeader0x0000
                    )
                }

                UnbrickMethodType.RP2040_TRANSPORTER_BRIDGE -> {
                    Rp2040BridgeWizardCard(
                        step = flashState.activeUnbrickStep,
                        onSetWizardStep = onSetWizardStep
                    )
                }

                UnbrickMethodType.USB_BUS_DIAGNOSTIC_ANALYZER -> {
                    UsbBusDiagnosticCard(
                        report = flashState.lastBusDiagnosticReport,
                        onRunAudit = onRunUsbBusAudit,
                        onExecuteControlReset = onExecuteControlTransferReset
                    )
                }

                UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT -> {
                    ScsiUbootProtocolCard(
                        wl82LoaderInjected = flashState.wl82LoaderInjected,
                        isReadyToFlash = connectedDevice.isReadyToFlash,
                        isFlashing = flashState.isFlashing,
                        onInjectWl82Loader = onInjectWl82LoaderNow
                    )
                }
            }
        }

        // 5. Target Firmware Selection + 5-Stage Auto-Verified Pipeline OR Expert Engineering Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, SynthPhosphor.copy(alpha = 0.75f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Целевая прошивка и конвейер записи M-VAVE FM-1",
                        style = MaterialTheme.typography.titleMedium,
                        color = SynthPhosphor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnbrickTargetFirmwareOption.entries.forEach { option ->
                            val selected = flashState.unbrickTargetOption == option
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectTargetOption(option) },
                                label = {
                                    Text(option.label, style = MaterialTheme.typography.labelSmall)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SynthPhosphor.copy(alpha = 0.22f),
                                    selectedLabelColor = SynthPhosphor
                                )
                            )
                        }
                    }

                    if (selectedFirmware != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Активный образ: ${selectedFirmware.name} (${selectedFirmware.version}) • CRC32: ${selectedFirmware.crc32} • SHA-256: ${selectedFirmware.sha256.take(16)}…",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (flashState.unbrickUiMode == UnbrickUiMode.INTERACTIVE_WIZARD) {
                        // 5-Stage Auto-Verified Interactive Checklist
                        Text(
                            text = "Автоматический контроль 5 этапов реанимации:",
                            style = MaterialTheme.typography.labelLarge,
                            color = SynthCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val stages = listOf(
                            0 to "Этап 1: Захват шины USB OTG и проверка дескрипторов (${connectedDevice.vidPidHex})",
                            1 to "Этап 2: Полная инъекция wl82loader.bin (4096 байт) в SRAM 0x010000 + CSW USBS",
                            2 to "Этап 3: Резервный дамп SPI Flash + проверка хирурга контейнеров .fwsc",
                            3 to "Этап 4: Честное стирание (0xFD 0x20) и запись 64×4 КБ секторов (${flashState.maxSectorRetries}x повтор)",
                            4 to "Этап 5: Аппаратная Read-Back сверка (0xFD 0xFA) и перезапуск DSP (0xFD 0xE0)"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stages.forEach { (idx, label) ->
                                val isDone = flashState.verifiedWizardStages.contains(idx)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Radar,
                                        contentDescription = label,
                                        tint = if (isDone) SynthPhosphor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDone) SynthPhosphor else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    } else {
                        // Manual Expert Engineering Console
                        ExpertEngineeringConsoleSection(
                            flashState = flashState,
                            isReadyToFlash = connectedDevice.isReadyToFlash,
                            onSetMaxRetries = onSetMaxRetries,
                            onSetBlockSize = onSetBlockSize,
                            onToggleBootSectorRebuild = onToggleBootSectorRebuild,
                            onInjectWl82LoaderNow = onInjectWl82LoaderNow,
                            onExecuteControlReset = onExecuteControlTransferReset,
                            onRunUsbBusAudit = onRunUsbBusAudit,
                            onRunFwscSurgeon = onRunFwscSurgeon,
                            onRepairBootHeader0x0000 = onRepairBootHeader0x0000,
                            onExportFull1MbDump = onExportFull1MbDump
                        )
                    }

                    // Live Progress & SCSI CBW / CSW ('USBS') Telemetry
                    if (progress != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = SynthPhosphor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${progress.stageName} (${(progress.progressFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SynthPhosphor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${progress.currentAddressHex} | ${progress.hexPacketSample}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                        if (progress.scsiCbwHex.isNotBlank()) {
                            Text(
                                text = "CBW (USBC): ${progress.scsiCbwHex} • ${progress.cswStatusHex} • Повторов: ${progress.retryCount}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthOrange
                            )
                        }
                    }

                    flashState.lastCompletedMessage?.let { msg ->
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
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
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

                    // Primary Execution Button
                    Button(
                        onClick = onRunSoftBrickRecovery,
                        enabled = !flashState.isFlashing && connectedDevice.isReadyToFlash,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("soft_unbrick_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SynthPhosphor,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(
                            imageVector = if (connectedDevice.isReadyToFlash) Icons.Default.Security else Icons.Default.UsbOff,
                            contentDescription = "Запустить Unbrick"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                flashState.isFlashing -> "ВЫПОЛНЯЕТСЯ КОНВЕЙЕР UNBRICK (WL82)…"
                                !connectedDevice.isConnected -> "ПОДКЛЮЧИТЕ OTG (ИЛИ ВКЛЮЧИТЕ СТЕНД СИМУЛЯЦИИ)"
                                !connectedDevice.hasPermission && !connectedDevice.isSimulated -> "ПОДТВЕРДИТЕ ДОСТУП К USB OTG"
                                flashState.allowBootSectorRebuild -> "ЗАПУСТИТЬ ПОЛНЫЙ UNBRICK (0x0000 BOOT + 0x4000 APP)"
                                else -> "ЗАПУСТИТЬ UNBRICK (${activeMethod.shortBadge} -> 0x4000)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportFull1MbDump,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_1mb_dump_button"),
                            border = BorderStroke(1.dp, SynthCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Скачать 1 МБ дамп",
                                tint = SynthCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Дамп 1 МБ (.bin)", color = SynthCyan)
                        }

                        OutlinedButton(
                            onClick = onOpenLogsAndScripts,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, SynthOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Скрипты и гайды",
                                tint = SynthOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Логи и гайды ПК", color = SynthOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodBadgePill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Interactive Step-by-Step Master with M-VAVE FM-1 Motherboard (PCB) Topology & Contact Point Visualizer.
 */
@Composable
private fun InteractiveFm1PcbMasterCard(
    activeMethod: UnbrickMethodType,
    activeStepIndex: Int,
    selectedPinShortVector: SpiPinShortVector,
    onSelectStep: (Int) -> Unit
) {
    val steps = activeMethod.steps
    val safeStepIdx = activeStepIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
    val currentStep = steps.getOrNull(safeStepIdx) ?: return
    val highlightZone = currentStep.pcbHighlightZone

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pcb_visualizer_master_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Пошаговый мастер и карта платы M-VAVE FM-1 (PCB Rev A/B/C)",
                        style = MaterialTheme.typography.titleMedium,
                        color = SynthPhosphor
                    )
                    Text(
                        text = "Активная точка контакта: ${highlightZone.label}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = SynthCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Step selector chips (Steps 1..4)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                steps.forEachIndexed { idx, stepGuide ->
                    val isSelected = idx == safeStepIdx
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStep(idx) },
                        modifier = Modifier.testTag("wizard_step_chip_$idx"),
                        label = {
                            Text(
                                text = stepGuide.stepBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SynthPhosphor.copy(alpha = 0.24f),
                            selectedLabelColor = SynthPhosphor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Interactive M-VAVE FM-1 PCB Layout Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
                    .background(Color(0xFF0A1412), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                val w = size.width
                val h = size.height

                // Outer PCB board outline (dark emerald solder mask)
                drawRoundRect(
                    color = Color(0xFF0F241D),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    color = SynthPhosphor.copy(alpha = 0.35f),
                    topLeft = Offset.Zero,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 1. USB Type-C Port (Top-Left)
                val usbSelected = highlightZone == PcbHighlightZone.USB_C_PORT
                val usbRectTopLeft = Offset(w * 0.04f, h * 0.08f)
                val usbRectSize = Size(w * 0.22f, h * 0.26f)
                drawRoundRect(
                    color = if (usbSelected) SynthCyan.copy(alpha = 0.35f) else Color(0xFF263238),
                    topLeft = usbRectTopLeft,
                    size = usbRectSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = if (usbSelected) SynthCyan else Color(0xFF78909C),
                    topLeft = usbRectTopLeft,
                    size = usbRectSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = if (usbSelected) 2.5.dp.toPx() else 1.dp.toPx())
                )

                // 2. Power Slide Switch & 5V VBUS Rail (Top-Right)
                val pwrSelected = highlightZone == PcbHighlightZone.POWER_SWITCH_AND_VBUS
                val pwrTopLeft = Offset(w * 0.72f, h * 0.08f)
                val pwrSize = Size(w * 0.24f, h * 0.24f)
                drawRoundRect(
                    color = if (pwrSelected) SynthOrange.copy(alpha = 0.35f) else Color(0xFF263238),
                    topLeft = pwrTopLeft,
                    size = pwrSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = if (pwrSelected) SynthOrange else Color(0xFF78909C),
                    topLeft = pwrTopLeft,
                    size = pwrSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = if (pwrSelected) 2.5.dp.toPx() else 1.dp.toPx())
                )

                // 3. JST-PH 2.0 Battery Connector (Bottom-Left, 2-pin Red/Black)
                val batSelected = highlightZone == PcbHighlightZone.JST_BATTERY_CONNECTOR
                val batTopLeft = Offset(w * 0.04f, h * 0.64f)
                val batSize = Size(w * 0.22f, h * 0.28f)
                drawRoundRect(
                    color = if (batSelected) SynthAmberWarn.copy(alpha = 0.35f) else Color(0xFF2E353B),
                    topLeft = batTopLeft,
                    size = batSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = if (batSelected) SynthAmberWarn else Color(0xFF90A4AE),
                    topLeft = batTopLeft,
                    size = batSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = if (batSelected) 2.5.dp.toPx() else 1.dp.toPx())
                )
                // Red & Black battery pins inside JST-PH2
                drawCircle(
                    color = Color(0xFFFF5252),
                    radius = 4.dp.toPx(),
                    center = Offset(batTopLeft.x + batSize.width * 0.35f, batTopLeft.y + batSize.height * 0.5f)
                )
                drawCircle(
                    color = Color(0xFFB0BEC5),
                    radius = 4.dp.toPx(),
                    center = Offset(batTopLeft.x + batSize.width * 0.65f, batTopLeft.y + batSize.height * 0.5f)
                )

                // 4. Center QFN-48 JieLi AC791N / WL82 SoC
                val socSelected = highlightZone == PcbHighlightZone.AC791N_SOC_CORE
                val socSize = Size(w * 0.26f, h * 0.56f)
                val socTopLeft = Offset(w * 0.35f, h * 0.22f)
                drawRoundRect(
                    color = if (socSelected) SynthPhosphor.copy(alpha = 0.28f) else Color(0xFF1A202C),
                    topLeft = socTopLeft,
                    size = socSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = if (socSelected) SynthPhosphor else Color(0xFF607D8B),
                    topLeft = socTopLeft,
                    size = socSize,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = if (socSelected) 2.5.dp.toPx() else 1.2.dp.toPx())
                )
                // Pin 1 dot on AC791N
                drawCircle(
                    color = SynthPhosphor,
                    radius = 3.dp.toPx(),
                    center = Offset(socTopLeft.x + 8.dp.toPx(), socTopLeft.y + 8.dp.toPx())
                )

                // 5. 25Q80 SOIC-8 SPI NOR Flash (Right of SoC)
                val spiSelected = highlightZone == PcbHighlightZone.SPI_FLASH_25Q80
                val spiSize = Size(w * 0.18f, h * 0.44f)
                val spiTopLeft = Offset(w * 0.72f, h * 0.44f)
                drawRoundRect(
                    color = if (spiSelected) SynthOrange.copy(alpha = 0.32f) else Color(0xFF1E2636),
                    topLeft = spiTopLeft,
                    size = spiSize,
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRoundRect(
                    color = if (spiSelected) SynthOrange else Color(0xFF78909C),
                    topLeft = spiTopLeft,
                    size = spiSize,
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    style = Stroke(width = if (spiSelected) 2.5.dp.toPx() else 1.2.dp.toPx())
                )

                // Draw 8 pins on 25Q80 SOIC-8 and highlight active pin-short vector
                val pinRowStep = spiSize.height / 4f
                for (row in 0 until 4) {
                    val leftPinNum = row + 1 // Pins 1..4
                    val rightPinNum = 8 - row // Pins 8..5
                    val py = spiTopLeft.y + row * pinRowStep + pinRowStep * 0.25f
                    val leftActive = spiSelected && (
                        selectedPinShortVector.fromPinNumber == leftPinNum ||
                            selectedPinShortVector.toPinNumber == leftPinNum
                        )
                    val rightActive = spiSelected && (
                        selectedPinShortVector.fromPinNumber == rightPinNum ||
                            selectedPinShortVector.toPinNumber == rightPinNum
                        )
                    drawRect(
                        color = if (leftActive) SynthOrange else Color(0xFFB0BEC5),
                        topLeft = Offset(spiTopLeft.x - 6.dp.toPx(), py),
                        size = Size(6.dp.toPx(), pinRowStep * 0.5f)
                    )
                    drawRect(
                        color = if (rightActive) SynthPhosphor else Color(0xFFB0BEC5),
                        topLeft = Offset(spiTopLeft.x + spiSize.width, py),
                        size = Size(6.dp.toPx(), pinRowStep * 0.5f)
                    )
                }

                // 6. Test Pads (TP_DP, TP_DM, TP_GND) between USB-C and SoC
                val padsSelected = highlightZone == PcbHighlightZone.PCB_TEST_PADS_DP_DM
                val padX = w * 0.295f
                listOf(h * 0.35f, h * 0.50f, h * 0.65f).forEachIndexed { idx, py ->
                    drawCircle(
                        color = when {
                            padsSelected -> SynthCyan
                            idx == 0 -> SynthPhosphor.copy(alpha = 0.7f)
                            idx == 1 -> SynthCyan.copy(alpha = 0.7f)
                            else -> Color(0xFF90A4AE)
                        },
                        radius = if (padsSelected) 5.5.dp.toPx() else 3.5.dp.toPx(),
                        center = Offset(padX, py)
                    )
                }

                // Draw copper bus traces connecting USB-C -> Test Pads -> AC791N -> 25Q80
                drawLine(
                    color = if (usbSelected || padsSelected) SynthCyan else SynthPhosphor.copy(alpha = 0.4f),
                    start = Offset(usbRectTopLeft.x + usbRectSize.width, usbRectTopLeft.y + usbRectSize.height * 0.5f),
                    end = Offset(socTopLeft.x, socTopLeft.y + socSize.height * 0.35f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = if (spiSelected || socSelected) SynthOrange else SynthPhosphor.copy(alpha = 0.4f),
                    start = Offset(socTopLeft.x + socSize.width, socTopLeft.y + socSize.height * 0.55f),
                    end = Offset(spiTopLeft.x, spiTopLeft.y + spiSize.height * 0.5f),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Legend for PCB zones
            Text(
                text = "Топология платы: [Слева вверху] USB-C OTG • [Слева внизу] Штекер АКБ JST-PH2 • [Центр] Чип JieLi AC791N • [Справа вверху] Тумблер ON/OFF • [Справа внизу] Флеш 25Q80 (1 МБ)",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Active Step Instruction Box
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentStep.stepBadge} — ${currentStep.stepTitle}",
                            style = MaterialTheme.typography.labelLarge,
                            color = SynthCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        MethodBadgePill(text = currentStep.timingBadge, color = SynthOrange)
                    }

                    Text(
                        text = currentStep.detailedInstruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "✓ ${currentStep.verificationHint}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = SynthPhosphor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { onSelectStep((safeStepIdx - 1).coerceAtLeast(0)) },
                            enabled = safeStepIdx > 0,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("← Предыдущий шаг", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onSelectStep((safeStepIdx + 1).coerceAtMost(steps.lastIndex)) },
                            enabled = safeStepIdx < steps.lastIndex,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, SynthPhosphor)
                        ) {
                            Text("Следующий шаг →", style = MaterialTheme.typography.labelSmall, color = SynthPhosphor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpiFlashPinoutCard(
    isPinShortMode: Boolean,
    selectedVector: SpiPinShortVector,
    isCatcherActive: Boolean,
    catcherAttempts: Int,
    onSelectVector: (SpiPinShortVector) -> Unit,
    onToggleCatcher: () -> Unit,
    onExportFull1MbDump: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthOrange)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isPinShortMode) {
                    "Микросхема 25Q80 SOIC-8: 3 безопасных вектора входа в MaskROM без пайки"
                } else {
                    "Схема подключения прищепки SOIC-8 программатора CH341A к микросхеме 25Q80 (1 МБ)"
                },
                style = MaterialTheme.typography.titleMedium,
                color = SynthOrange
            )

            if (isPinShortMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpiPinShortVector.entries.forEach { vector ->
                        val selected = selectedVector == vector
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectVector(vector) },
                            modifier = Modifier.testTag("pin_vector_${vector.name.lowercase()}"),
                            label = {
                                Text(
                                    text = vector.shortTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SynthOrange.copy(alpha = 0.24f),
                                selectedLabelColor = SynthOrange
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${selectedVector.pinPairLabel} • ${selectedVector.safetyRating}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthPhosphor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedVector.technicalReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Наденьте прищепку SOIC-8 на чип 25Q80 (ключ Pin 1 /CS отмечен точкой на корпусе). Программатор CH341A запишет полный 1 МБ образ (0x000000–0x0FFFFF) напрямую в обход процессора.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual SOIC-8 Chip Pinout Canvas with dynamic jumper line for the selected vector
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                val chipW = size.width * 0.42f
                val chipH = size.height * 0.78f
                val chipX = (size.width - chipW) / 2f
                val chipY = (size.height - chipH) / 2f

                // Draw SOIC-8 IC body
                drawRoundRect(
                    color = Color(0xFF1E2636),
                    topLeft = Offset(chipX, chipY),
                    size = Size(chipW, chipH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Pin 1 index dot (top-left)
                drawCircle(
                    color = SynthCyan,
                    radius = 5.dp.toPx(),
                    center = Offset(chipX + 14.dp.toPx(), chipY + 14.dp.toPx())
                )

                val pinW = 18.dp.toPx()
                val pinH = 8.dp.toPx()
                val rowStep = chipH / 4f

                fun pinCenter(pinNumber: Int): Offset {
                    return if (pinNumber in 1..4) {
                        val row = pinNumber - 1
                        Offset(chipX - pinW / 2f, chipY + (rowStep * row) + rowStep / 2f)
                    } else {
                        val row = 8 - pinNumber
                        Offset(chipX + chipW + pinW / 2f, chipY + (rowStep * row) + rowStep / 2f)
                    }
                }

                for (i in 0 until 4) {
                    val leftPin = i + 1
                    val rightPin = 8 - i
                    val py = chipY + (rowStep * i) + (rowStep - pinH) / 2f

                    val leftHighlighted = isPinShortMode &&
                        (selectedVector.fromPinNumber == leftPin || selectedVector.toPinNumber == leftPin)
                    val rightHighlighted = isPinShortMode &&
                        (selectedVector.fromPinNumber == rightPin || selectedVector.toPinNumber == rightPin)

                    drawRoundRect(
                        color = if (leftHighlighted) SynthPhosphor else Color(0xFF90A4AE),
                        topLeft = Offset(chipX - pinW, py),
                        size = Size(pinW, pinH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    drawRoundRect(
                        color = if (rightHighlighted) SynthOrange else Color(0xFF90A4AE),
                        topLeft = Offset(chipX + chipW, py),
                        size = Size(pinW, pinH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                if (isPinShortMode) {
                    val startCenter = pinCenter(selectedVector.fromPinNumber)
                    val endCenter = pinCenter(selectedVector.toPinNumber)
                    drawLine(
                        color = SynthOrange,
                        start = startCenter,
                        end = endCenter,
                        strokeWidth = 3.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Распиновка 25Q80: Pin 1(/CS) • Pin 2(DO) • Pin 3(/WP) • Pin 4(GND)  |  Pin 8(VCC 3.3V) • Pin 7(/HOLD) • Pin 6(CLK) • Pin 5(DI)",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = SynthCyan
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (isPinShortMode) {
                Button(
                    onClick = onToggleCatcher,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_short_catcher_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCatcherActive) Color(0xFFFF5252) else SynthOrange,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (isCatcherActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Перехватчик Pin-Short"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCatcherActive) {
                            "ОСТАНОВИТЬ РАДАР ПЕРЕХВАТА (Цикл #$catcherAttempts)…"
                        } else {
                            "ЗАПУСТИТЬ РАДАР ПЕРЕХВАТА (${selectedVector.shortTitle})"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onExportFull1MbDump,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SynthCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Собрать 1 МБ дамп")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "СОБРАТЬ И СОХРАНИТЬ ПОЛНЫЙ 1 МБ ДАМП (0x000000–0x0FFFFF)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VbusGlitchAndBootloopCatcherCard(
    isVbusGlitchMode: Boolean,
    isCatcherActive: Boolean,
    catcherAttempts: Int,
    onToggleCatcher: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthAmberWarn)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isVbusGlitchMode) {
                    "Генератор VBUS-Глитча и захватчика аппаратного BootROM (Окно 0..50 мс)"
                } else {
                    "Высокочастотный перехватчик окна загрузчика (Bootloop & OTA Catcher 120 мс)"
                },
                style = MaterialTheme.typography.titleMedium,
                color = SynthAmberWarn
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isVbusGlitchMode) {
                    "1. Выключите тумблер питания M-VAVE FM-1 и выньте кабель USB-C из синтезатора на 5 секунд.\n" +
                        "2. Нажмите кнопку запуска радара ниже — смартфон начнет непрерывно генерировать пакеты пробуждения JieLi ISP (0x40/0xFD + USBC 0xFD 0x10).\n" +
                        "3. Включите тумблер FM-1 и резко вставьте штекер USB-C (или нажмите кнопку на USB-хабе), чтобы поймать первые 50 мс старта BootROM без разборки корпуса!"
                } else {
                    "Если прошивка повреждена так, что синтезатор зависает через долю секунды после включения, включите радар ДО подачи питания на FM-1. Приложение будет каждые 120 мс посылать сдвоенный пакет F0 22 24 35 7D F7 + Control ISP Wakeup + wl82loader.bin."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onToggleCatcher,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bootloop_catcher_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCatcherActive) Color(0xFFFF5252) else SynthAmberWarn,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = if (isCatcherActive) Icons.Default.Stop else Icons.Default.Radar,
                    contentDescription = "Перехват VBUS/Bootloop"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCatcherActive) {
                        "ОСТАНОВИТЬ РАДАР АТАКИ USBKEY/SYSEX (Цикл #$catcherAttempts)…"
                    } else if (isVbusGlitchMode) {
                        "ЗАПУСТИТЬ VBUS-ГЛИТЧ И ПЕРЕХВАТЧИК USBKEY (120 МС)"
                    } else {
                        "ВКЛЮЧИТЬ ПЕРЕХВАТЧИК БУТЛУПА (120 МС POLLING)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ControlTransferResetCard(
    resetCompleted: Boolean,
    isReadyToFlash: Boolean,
    isFlashing: Boolean,
    onExecuteReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthCyan)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Сброс EP0",
                    tint = SynthCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Вывод из дескрипторного ступора через Endpoint 0 Control Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    color = SynthCyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Отправляет по управляющему каналу Endpoint 0 команды CLEAR_FEATURE (ENDPOINT_HALT 0x02/0x01), Bulk-Only Mass Storage Reset (0x21/0xFF) и вендорный импульс пробуждения JieLi ISP (0x40/0xFD), после чего автоматически загружает 4 КБ wl82loader.bin в SRAM.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onExecuteReset,
                enabled = isReadyToFlash && !isFlashing,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("control_transfer_reset_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (resetCompleted) SynthPhosphor else SynthCyan,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = if (resetCompleted) Icons.Default.CheckCircle else Icons.Default.ElectricBolt,
                    contentDescription = "Control Reset"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (resetCompleted) {
                        "ПАЙПЫ USB EP0 ОЧИЩЕНЫ И WL82LOADER В SRAM (ПОВТОРИТЬ СБРОС)"
                    } else {
                        "ВЫПОЛНИТЬ CONTROL TRANSFER СБРОС ПАЙПОВ USB + ИНЪЕКЦИЮ SRAM"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BrownoutPmuReleaseCard(
    isCatcherActive: Boolean,
    catcherAttempts: Int,
    onToggleCatcher: () -> Unit,
    onRefreshUsb: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthOrange)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = "Сброс защиты АКБ",
                    tint = SynthOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Снятие защиты контроллера питания (PMU Brownout Latch) без пайки",
                    style = MaterialTheme.typography.titleMedium,
                    color = SynthOrange
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• Способ 1 (Без вскрытия корпуса): Выключите FM-1 тумблером OFF, подключите кабель питания на 20 секунд, затем включите радар ниже и переключите тумблер ON.\n" +
                    "• Способ 2 (При глубокой блокировке PMU): Снимите нижнюю крышку, пальцами или пинцетом выньте белый 2-контактный штекер аккумулятора JST-PH 2.0 на 10 секунд, включите тумблер для разряда конденсаторов и подключите USB OTG без батареи!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onToggleCatcher,
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCatcherActive) Color(0xFFFF5252) else SynthOrange,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (isCatcherActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Холодный старт VBUS"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCatcherActive) "РАДАР АКТИВЕН (#$catcherAttempts)" else "РАДАР ХОЛОДНОГО СТАРТА VBUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onRefreshUsb,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SynthCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Проверить шину",
                        tint = SynthCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Проверить USB", style = MaterialTheme.typography.labelSmall, color = SynthCyan)
                }
            }
        }
    }
}

@Composable
private fun UsbBusDiagnosticCard(
    report: UsbBusDiagnosticReport?,
    onRunAudit: () -> Unit,
    onExecuteControlReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthCyan)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Аудит шины USB OTG",
                    tint = SynthCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "5-уровневый диагностический стенд шины USB OTG и пакетов USBC/USBS",
                    style = MaterialTheme.typography.titleMedium,
                    color = SynthCyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Проверяет поддержку USB Host, наличие линий данных D+/D- в кабеле, чтение дескрипторов через Endpoint 0, целостность пайпов Bulk IN/OUT и прохождение команд SCSI CBW/CSW.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (report.passedAllChecks) SynthPhosphor else SynthOrange
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = report.summaryTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (report.passedAllChecks) SynthPhosphor else SynthOrange,
                            fontWeight = FontWeight.Bold
                        )
                        report.detailedLines.forEach { line ->
                            Text(
                                text = "• $line",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "HEX: ${report.rawHexTelemetry}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRunAudit,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("run_usb_bus_audit_card_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SynthCyan,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(imageVector = Icons.Default.Radar, contentDescription = "Запустить аудит")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ЗАПУСТИТЬ АУДИТ ШИНЫ USB", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onExecuteControlReset,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SynthOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Сброс EP0",
                        tint = SynthOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сброс EP0 Halt", style = MaterialTheme.typography.labelSmall, color = SynthOrange)
                }
            }
        }
    }
}

@Composable
private fun FwscSurgeonCard(
    lastReport: String?,
    bootRepaired: Boolean,
    isFlashing: Boolean,
    isReadyToFlash: Boolean,
    onRunSurgeon: () -> Unit,
    onImportFile: () -> Unit,
    onRepairBoot0x0000: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthCyan)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Хирург контейнеров .fwsc (fm1_extract_app) и реконструктор 0x0000–0x3FFF",
                style = MaterialTheme.typography.titleMedium,
                color = SynthCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Автоматически очищает файлы .fwsc от контейнерных заголовков, извлекает чистый образ для 0x00004000 и позволяет реконструировать затертый 16 КБ сектор загрузчика (Golden Boot Header 0x0000–0x3FFF).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (lastReport != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastReport,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = SynthPhosphor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRunSurgeon,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("run_fwsc_surgeon_button"),
                    border = BorderStroke(1.dp, SynthCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Проверить FWSC",
                        tint = SynthCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Препарировать образ", color = SynthCyan, style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onImportFile,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, SynthOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Открыть .fwsc",
                        tint = SynthOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Открыть свой .fwsc", color = SynthOrange, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRepairBoot0x0000,
                enabled = isReadyToFlash && !isFlashing,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repair_boot_0x0000_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SynthOrange,
                    contentColor = Color.Black
                )
            ) {
                Icon(imageVector = Icons.Default.Memory, contentDescription = "Ремонт 0x0000")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (bootRepaired) {
                        "ЗАГРУЗЧИК 0x0000–0x3FFF ВОССТАНОВЛЕН (ПОВТОРИТЬ ЗАПИСЬ)"
                    } else {
                        "РЕКОНСТРУИРОВАТЬ GOLDEN BOOT 0x0000–0x3FFF + APP 0x4000"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScsiUbootProtocolCard(
    wl82LoaderInjected: Boolean,
    isReadyToFlash: Boolean,
    isFlashing: Boolean,
    onInjectWl82Loader: () -> Unit
) {
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
                Text(
                    text = "Стек USB Mass Storage SCSI Pass-Through (VID_4C4A:PID_8057)",
                    style = MaterialTheme.typography.titleMedium,
                    color = SynthPhosphor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Перед записью секторов во флеш-память в чип JieLi WL82 отправляется 31-байтный пакет SCSI CBW ('USBC' 0xFD 0x10), за которым следуют все 4096 байт вторичного RAM-драйвера wl82loader.bin по адресу SRAM 0x00010000 с проверкой 13-байтного ответа CSW ('USBS').",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onInjectWl82Loader,
                enabled = isReadyToFlash && !isFlashing,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inject_wl82loader_button"),
                border = BorderStroke(1.dp, if (wl82LoaderInjected) SynthPhosphor else SynthCyan)
            ) {
                Icon(
                    imageVector = if (wl82LoaderInjected) Icons.Default.CheckCircle else Icons.Default.Memory,
                    contentDescription = "Инъекция wl82loader.bin",
                    tint = if (wl82LoaderInjected) SynthPhosphor else SynthCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (wl82LoaderInjected) {
                        "wl82loader.bin (4 КБ) АКТИВЕН В SRAM 0x010000 (ПОВТОРИТЬ ИНЪЕКЦИЮ)"
                    } else {
                        "ШАГ 1: ИНЪЕКТИРОВАТЬ wl82loader.bin (4096 БАЙТ) В SRAM 0x010000 СЕЙЧАС"
                    },
                    color = if (wl82LoaderInjected) SynthPhosphor else SynthCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun Rp2040BridgeWizardCard(
    step: Int,
    onSetWizardStep: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SynthCyan)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Быстрая памятка распиновки моста RP2040 / Pi Pico Transporter",
                style = MaterialTheme.typography.titleMedium,
                color = SynthCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val stepLabels = listOf("1. Плата UF2", "2. Линии D+/D-", "3. Захват ROM", "4. Прошивка")
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
                Column(modifier = Modifier.padding(12.dp)) {
                    when (step) {
                        0 -> Text(
                            text = "• Зажмите кнопку BOOTSEL на плате Seeed XIAO RP2040 или Raspberry Pi Pico и подключите по USB.\n• Скопируйте образ моста `wl82loader.uf2` на диск RPI-RP2 (VID_2E8A : PID_000A).",
                            style = MaterialTheme.typography.bodySmall
                        )
                        1 -> Text(
                            text = "Подключите 3 линии от RP2040 к USB-разъему выключенного M-VAVE FM-1:\n• GPIO 0 (RP2040) ──► USB D+ (Зеленый)\n• GPIO 1 (RP2040) ──► USB D- (Белый)\n• GND (RP2040)    ──► GND (Черный)",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodySmall,
                            color = SynthPhosphor
                        )
                        2 -> Text(
                            text = "• Включите питание FM-1 при активном мосте RP2040.\n• Мост пошлет спец-последовательность синхронизации по D+/D- и переведет AC791N в режим приема wl82loader.bin.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        else -> Text(
                            text = "• Нажмите главную зеленую кнопку ниже для инъекции wl82loader.bin и полной перезаписи разделов 0x0000 + 0x4000.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpertEngineeringConsoleSection(
    flashState: FlashSessionUiState,
    isReadyToFlash: Boolean,
    onSetMaxRetries: (Int) -> Unit,
    onSetBlockSize: (Int) -> Unit,
    onToggleBootSectorRebuild: (Boolean) -> Unit,
    onInjectWl82LoaderNow: () -> Unit,
    onExecuteControlReset: () -> Unit,
    onRunUsbBusAudit: () -> Unit,
    onRunFwscSurgeon: () -> Unit,
    onRepairBootHeader0x0000: () -> Unit,
    onExportFull1MbDump: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SynthOrange.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Инженерная панель низкоуровневых параметров SCSI / SPI / EP0:",
                style = MaterialTheme.typography.labelLarge,
                color = SynthOrange
            )

            // Block size selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Размер страницы записи:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(512, 2048, 4096).forEach { size ->
                        FilterChip(
                            selected = flashState.transferBlockSize == size,
                            onClick = { onSetBlockSize(size) },
                            label = { Text("$size B", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Max retries selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Авто-повтор сбойных секторов:", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 3, 5).forEach { retries ->
                        FilterChip(
                            selected = flashState.maxSectorRetries == retries,
                            onClick = { onSetMaxRetries(retries) },
                            label = { Text("${retries}x", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Boot sector 0x0000-0x3FFF Golden Rebuild switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Полная перезапись 0x0000–0x3FFF (Golden Boot Rebuild)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (flashState.allowBootSectorRebuild) SynthOrange else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Включайте при затертом загрузчике 0x0000 или записи через CH341A / RP2040",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = flashState.allowBootSectorRebuild,
                    onCheckedChange = onToggleBootSectorRebuild,
                    colors = SwitchDefaults.colors(checkedThumbColor = SynthOrange)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            // Direct atomic expert operations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInjectWl82LoaderNow,
                    enabled = isReadyToFlash && !flashState.isFlashing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SRAM wl82loader", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onExecuteControlReset,
                    enabled = isReadyToFlash && !flashState.isFlashing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сброс EP0 Halt", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRunFwscSurgeon,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Хирург .fwsc", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onRunUsbBusAudit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Аудит шины OTG", style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRepairBootHeader0x0000,
                    enabled = isReadyToFlash && !flashState.isFlashing,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ремонт 0x0000", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onExportFull1MbDump,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сборка 1MB SPI", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
