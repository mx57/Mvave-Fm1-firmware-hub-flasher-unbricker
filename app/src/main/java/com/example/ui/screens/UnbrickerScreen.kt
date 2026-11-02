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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FirmwareEntity
import com.example.domain.ConnectedUsbDeviceInfo
import com.example.domain.DeviceDiagnosticVerdict
import com.example.domain.UnbrickMethodType
import com.example.domain.UnbrickTargetFirmwareOption
import com.example.domain.UnbrickUiMode
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
    onSetUnbrickUiMode: (UnbrickUiMode) -> Unit,
    onSelectTargetOption: (UnbrickTargetFirmwareOption) -> Unit,
    onSetMaxRetries: (Int) -> Unit,
    onSetBlockSize: (Int) -> Unit,
    onToggleBootSectorRebuild: (Boolean) -> Unit,
    onInjectWl82LoaderNow: () -> Unit,
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
                                    text = "Центр реанимации M-VAVE FM-1 (6 методов)",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "SCSI UBOOT1.00 • wl82loader SRAM • Pin-Short MaskROM • FWSC Surgeon • RP2040 • CH341A 1MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRefreshUsb,
                            modifier = Modifier.testTag("unbrick_scan_usb_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Проверить USB",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Скан OTG", style = MaterialTheme.typography.labelSmall)
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
                                        text = "Включите для автоматического переключения на Метод 3 (вход в MaskROM замыканием пина SPI Flash без пайки)",
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

                    // Quick Simulation Stand Switch on Unbricker Screen for testing all 6 methods
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
                                    "Активен: можно протестировать все 6 методов реанимации, инъекцию wl82loader и сборку 1 МБ дампа"
                                } else {
                                    "Выключен (строгий режим реального USB OTG). Включите для теста всех функций без железа"
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

        // 2. Selector for all 6 Unbrick Methods + UI Mode Switcher (Interactive Wizard vs Expert Console)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Выберите метод реанимации (6 векторов Unbrick)",
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

                    // Active Method Description Box
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeMethod.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SynthPhosphor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                text = "Симптомы: ${activeMethod.subtitle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthOrange
                            )
                            Text(
                                text = "Оборудование: ${activeMethod.hardwareRequirement} • Раздел: ${activeMethod.targetOffsetDefault}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = SynthCyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeMethod.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Switcher: Interactive Step-by-Step Wizard vs Manual Expert Engineering Console
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

        // 3. Method-Specific Visual Hardware Guide (Pin-Shorting SOIC-8 Diagram / RP2040 / Bootloop Catcher / FWSC / CH341A)
        item {
            when (activeMethod) {
                UnbrickMethodType.SPI_PIN_SHORT_MASKROM,
                UnbrickMethodType.CH341A_FULL_DUMP_PROGRAMMER -> {
                    SpiFlashPinoutCard(
                        isPinShortMode = activeMethod == UnbrickMethodType.SPI_PIN_SHORT_MASKROM,
                        isCatcherActive = flashState.isBootloopCatcherActive,
                        catcherAttempts = flashState.bootloopCatcherAttempts,
                        onToggleCatcher = onToggleBootloopCatcher,
                        onExportFull1MbDump = onExportFull1MbDump
                    )
                }

                UnbrickMethodType.BOOTLOOP_SYSEX_CATCHER -> {
                    BootloopCatcherCard(
                        isCatcherActive = flashState.isBootloopCatcherActive,
                        catcherAttempts = flashState.bootloopCatcherAttempts,
                        onToggleCatcher = onToggleBootloopCatcher
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

        // 4. Target Firmware Selection + 5-Stage Auto-Verified Pipeline OR Expert Engineering Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, SynthPhosphor.copy(alpha = 0.75f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Целевая прошивка для восстановления",
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
                            text = "Автоматическая проверка этапов реанимации (5 шагов):",
                            style = MaterialTheme.typography.labelLarge,
                            color = SynthCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val stages = listOf(
                            0 to "Этап 1: Захват шины USB OTG и проверка разрешения (${connectedDevice.vidPidHex})",
                            1 to "Этап 2: Инъекция вторичного лоадера wl82loader.bin (4 КБ) в SRAM 0x010000",
                            2 to "Этап 3: Полный резервный дамп SPI Flash + проверка хирурга .fwsc",
                            3 to "Этап 4: Запись 64 секторов по протоколу SCSI CBW/CSW (${flashState.maxSectorRetries}x авто-повтор)",
                            4 to "Этап 5: Криптографическая Read-Back сверка SHA-256 / CRC32 и перезапуск DSP"
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
                            onRunFwscSurgeon = onRunFwscSurgeon,
                            onRepairBootHeader0x0000 = onRepairBootHeader0x0000,
                            onExportFull1MbDump = onExportFull1MbDump
                        )
                    }

                    // Live Progress & SCSI CBW Telemetry
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
                                text = "SCSI CBW (USBC): ${progress.scsiCbwHex} • Повторов секторов: ${progress.retryCount}",
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

@Composable
private fun SpiFlashPinoutCard(
    isPinShortMode: Boolean,
    isCatcherActive: Boolean,
    catcherAttempts: Int,
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
                    "Схема микросхемы 25Q80 SOIC-8: Вход в MaskROM замыканием Pin 6 (CLK) на Pin 4 (GND)"
                } else {
                    "Схема подключения прищепки SOIC-8 программатора CH341A к микросхеме 25Q80 (1 МБ)"
                },
                style = MaterialTheme.typography.titleMedium,
                color = SynthOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isPinShortMode) {
                    "1. Выключите питание M-VAVE FM-1, подключите USB OTG кабель и нажмите кнопку «Запустить радар-перехватчик» ниже.\n" +
                        "2. Пинцетом замкните ножку 6 (CLK) на ножку 4 (GND) флеш-памяти 25Q80 и включите тумблер питания синтезатора.\n" +
                        "3. Чип AC791N не сможет прочитать флеш и мгновенно перейдет в режим WL82 UBOOT1.00 (VID_4C4A:PID_8057). Уберите пинцет и прошейте!"
                } else {
                    "Наденьте прищепку SOIC-8 на чип 25Q80 (ключ Pin 1 /CS отмечен точкой на корпусе). Программатор CH341A запишет полный 1 МБ образ (0x000000–0x0FFFFF) напрямую в обход процессора."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Visual SOIC-8 Chip Pinout Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
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

                // Pin 1 orientation dot
                drawCircle(
                    color = SynthCyan,
                    radius = 4.dp.toPx(),
                    center = Offset(chipX + 12.dp.toPx(), chipY + 12.dp.toPx())
                )

                val pinH = 8.dp.toPx()
                val pinW = 22.dp.toPx()
                val rowStep = chipH / 4f

                for (i in 0 until 4) {
                    val py = chipY + (rowStep * i) + (rowStep - pinH) / 2f
                    // Left pins 1..4 (Pin 4 is GND)
                    val leftPinColor = if (i == 3) SynthPhosphor else SynthCyan
                    drawRoundRect(
                        color = leftPinColor,
                        topLeft = Offset(chipX - pinW, py),
                        size = Size(pinW, pinH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Right pins 8..5 (i=0 -> Pin 8 VCC, i=1 -> Pin 7 /HOLD, i=2 -> Pin 6 CLK, i=3 -> Pin 5 DI)
                    val rightPinColor = if (i == 2) SynthOrange else SynthCyan
                    drawRoundRect(
                        color = rightPinColor,
                        topLeft = Offset(chipX + chipW, py),
                        size = Size(pinW, pinH),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // Draw dashed jumper bridge between Pin 6 (right row i=2) and Pin 4 (left row i=3)
                val pin4Center = Offset(chipX - pinW / 2f, chipY + (rowStep * 3) + rowStep / 2f)
                val pin6Center = Offset(chipX + chipW + pinW / 2f, chipY + (rowStep * 2) + rowStep / 2f)
                drawLine(
                    color = SynthOrange,
                    start = pin4Center,
                    end = pin6Center,
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Распиновка 25Q80: Pin 1(/CS) • Pin 2(DO) • Pin 3(/WP) • Pin 4(GND 🟢)  |  Pin 8(VCC 3.3V) • Pin 7(/HOLD) • Pin 6(CLK 🟠) • Pin 5(DI)",
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
                            "ЗАПУСТИТЬ РАДАР ПЕРЕХВАТА PIN-SHORT MASKROM"
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
private fun BootloopCatcherCard(
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
                text = "Высокочастотный перехватчик окна загрузчика (Bootloop & OTA Catcher)",
                style = MaterialTheme.typography.titleMedium,
                color = SynthAmberWarn
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Если прошивка повреждена так, что синтезатор зависает через долю секунды после включения, включите перехватчик ДО подачи питания на FM-1. Приложение будет каждые 180 мс посылать сдвоенный пакет F0 22 24 35 7D F7 + SCSI запрос и переведет чип в загрузчик раньше, чем запустится сбойный DSP-код.",
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
                    contentDescription = "Перехват бутлупа"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCatcherActive) {
                        "ОСТАНОВИТЬ АТАКУ SYSEX/SCSI (Попытка #$catcherAttempts)…"
                    } else {
                        "ВКЛЮЧИТЬ ПЕРЕХВАТЧИК БУТЛУПА (180 МС POLLING)"
                    },
                    fontWeight = FontWeight.Bold
                )
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
                text = "Перед записью секторов во флеш-память в чип JieLi WL82 отправляется 31-байтный пакет SCSI CBW ('USBC' 0xFD 0x10) со вторичным RAM-загрузчиком wl82loader.bin по адресу SRAM 0x00010000.",
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
                        "wl82loader.bin АКТИВЕН В SRAM 0x010000 (ПОВТОРИТЬ ИНЪЕКЦИЮ)"
                    } else {
                        "ШАГ 1: ИНЪЕКТИРОВАТЬ wl82loader.bin В SRAM 0x010000 СЕЙЧАС"
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
                text = "Мастер подключения моста RP2040 / Pi Pico Transporter",
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
                text = "Инженерная панель низкоуровневых параметров SCSI / SPI:",
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
                    onClick = onRunFwscSurgeon,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Хирург .fwsc", style = MaterialTheme.typography.labelSmall)
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
