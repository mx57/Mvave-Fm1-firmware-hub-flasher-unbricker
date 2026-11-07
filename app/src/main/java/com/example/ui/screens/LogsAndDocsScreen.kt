package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FirmwareEntity
import com.example.data.FlashLogEntity
import com.example.domain.ChecksumEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SynthAmberWarn
import com.example.ui.theme.SynthCrimsonError
import com.example.ui.theme.SynthCyan
import com.example.ui.theme.SynthOrange
import com.example.ui.theme.SynthPhosphor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsAndDocsScreen(
    logs: List<FlashLogEntity>,
    selectedLogLevel: String,
    selectedDesktopOs: String,
    selectedFirmware: FirmwareEntity?,
    onSelectLogLevel: (String) -> Unit,
    onSelectDesktopOs: (String) -> Unit,
    onExportLogs: () -> Unit,
    onCopyLogs: () -> Unit = {},
    onShareScript: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    val fwName = selectedFirmware?.name ?: "M-VAVE FM-1 Official Stock V16"
    val fwFile = "${selectedFirmware?.id ?: "mvave_fm1_stock_v16"}.bin"
    val fwSha = selectedFirmware?.sha256 ?: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    val fwCrc = selectedFirmware?.crc32 ?: "A1B2C3D4"

    val generatedScript = remember(selectedDesktopOs, fwName, fwSha, fwCrc) {
        ChecksumEngine.generateCrossPlatformScript(
            osPlatform = selectedDesktopOs,
            firmwareName = fwName,
            fileName = fwFile,
            sha256 = fwSha,
            crc32 = fwCrc,
            offsetHex = "0x4000"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("logs_docs_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live USB & Flash Telemetry Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthPhosphor.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Логи",
                                tint = SynthPhosphor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Журнал процесса и USB-телеметрии",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Записей: ${logs.size} • Пакеты SysEx, UBoot и сверки хэшей",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL", "USB_IO", "CHECKSUM", "SUCCESS", "WARN", "ERROR").forEach { level ->
                            FilterChip(
                                selected = selectedLogLevel == level,
                                onClick = { onSelectLogLevel(level) },
                                label = { Text(level, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SynthPhosphor.copy(alpha = 0.22f),
                                    selectedLabelColor = SynthPhosphor
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "[ПУСТО] Событий для выбранного фильтра пока нет.",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                logs.take(40).forEach { log ->
                                    val badgeColor = when (log.level) {
                                        "SUCCESS" -> SynthPhosphor
                                        "USB_IO" -> SynthCyan
                                        "CHECKSUM" -> SynthOrange
                                        "WARN" -> SynthAmberWarn
                                        "ERROR" -> SynthCrimsonError
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Column {
                                        Text(
                                            text = "[${timeFormatter.format(Date(log.timestamp))}] [${log.level}] [${log.tag}] ${log.message}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeColor
                                        )
                                        if (log.hexPayload.isNotBlank()) {
                                            Text(
                                                text = "   ↳ HEX: ${log.hexPayload}",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onExportLogs,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_logs_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SynthPhosphor,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Экспорт логов",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Экспорт (.log)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = onCopyLogs,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_logs_clipboard_button"),
                            border = BorderStroke(1.dp, SynthCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Копировать в буфер",
                                tint = SynthCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Копировать", color = SynthCyan, style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = onClearLogs,
                            modifier = Modifier.weight(0.85f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Очистить",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Очистить", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 2. Cross-Platform Desktop Companion Script Generator (Windows / Linux / macOS / Python)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SynthCyan.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Кроссплатформенный генератор",
                            tint = SynthCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Кроссплатформенный генератор (Windows / macOS / Linux)",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Готовый скрипт прошивки и Unbrick с проверкой SHA-256/CRC32 и защитой 0x4000 для настольных ОС",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val platforms = listOf(
                            "WINDOWS_PS" to "Windows (PowerShell)",
                            "LINUX_MACOS_SH" to "Linux / macOS (Bash)",
                            "PYTHON_CLI" to "Python 3 (Кроссплатф.)"
                        )
                        platforms.forEach { (code, title) ->
                            FilterChip(
                                selected = selectedDesktopOs == code,
                                onClick = { onSelectDesktopOs(code) },
                                label = { Text(title, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SynthCyan.copy(alpha = 0.22f),
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
                        Text(
                            text = generatedScript,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = SynthCyan,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onShareScript(generatedScript) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_desktop_script_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SynthCyan,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Экспортировать скрипт"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ЭКСПОРТИРОВАТЬ СКРИПТ ДЛЯ НАСТОЛЬНОЙ ОС",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Comprehensive End-User Documentation (Установка, Использование, Восстановление)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Документация",
                            tint = SynthOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Документация по установке и использованию",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    DocSectionCard(
                        title = "1. Подключение по USB OTG (Android) и на ПК (Windows / Linux / macOS)",
                        body = "• Android: Подключите синтезатор M-VAVE FM-1 через качественный USB-C OTG переходник. При появлении системного окна нажмите «Разрешить доступ к USB-устройству».\n" +
                            "• Windows 10/11: Для штатной прошивки через USB-MIDI драйверы устанавливаются автоматически. Для режима восстановления WL82 UBOOT1.00 (VID_4C4A:PID_8057) используйте утилиту Zadig для привязки драйвера WinUSB/libusb.\n" +
                            "• Linux: Добавьте правило udev: SUBSYSTEM==\"usb\", ATTR{idVendor}==\"4c4a\", MODE=\"0666\".\n" +
                            "• macOS: Работает из коробки через CoreMIDI и libusb (`brew install libusb python3`)."
                    )

                    DocSectionCard(
                        title = "2. Реальная загрузка прошивок в папку FM1_Firmwares/ и БД Room",
                        body = "• При нажатии «Скачать» или «Скачать все» приложение загружает контейнер .fwsc с thefirmwarehub.com, проверяет контрольную сумму SHA-256 сжатого контейнера, распаковывает чистый бинарный образ (.bin) в отдельную папку FM1_Firmwares/ и повторно верифицирует SHA-256 / CRC32 / MD5.\n" +
                            "• Полное техническое описание прошивки (README / Release Notes) и изображение OLED-экрана (.png в Base64) автоматически сохраняются напрямую в таблицу firmwares базы данных Room для мгновенной офлайн-работы."
                    )

                    DocSectionCard(
                        title = "3. Прошивка по проводу и авто-перехват после погасания экрана FM-1",
                        body = "• Когда синтезатор получает команду входа в загрузчик SysEx (F0 22 24 35 7D F7), его экран гаснет, а чип JieLi AC791N физически перезапускает USB-порт и меняет адрес в системе Android (/dev/bus/usb/...).\n" +
                            "• Приложение автоматически отслеживает отключение старого порта, ловит новое устройство загрузчика (ota-FM-1 или WL82 UBOOT1.00), автоматически запрашивает разрешение USB Host и без пауз продолжает запись по двухпротокольному стеку (USB-MIDI SysEx OTA + JieLi SCSI BOT)."
                    )

                    DocSectionCard(
                        title = "4. 6 проверенных методов реанимации M-VAVE FM-1 с рабочим USB-кодом",
                        body = "• Метод 1 (SysEx OTA + Авто-перехват ota-FM-1): Вход в загрузчик по F0 22 24 35 7D F7 (в 4-байтных USB-MIDI пакетах CIN 0x04..0x07), автоматический перехват нового порта после гашения экрана и запись по USB-MIDI SysEx OTA или SCSI.\n" +
                            "• Метод 2 (SCSI UBOOT1.00 Soft-Brick): Загрузка 4096 байт вторичного драйвера wl82loader.bin в SRAM 0x010000 (CBW 'USBC' 0xFD 0x10), стирание секторов (0xFD 0x20), линейная запись 64×4 КБ блоков (0xFD 0xFC) и аппаратная Read-Back верификация (0xFD 0xFA) с проверкой CSW ('USBS').\n" +
                            "• Метод 3 (Control Transfer EP0 Reset): Снятие зависания пайпов через Endpoint 0 — CLEAR_FEATURE(ENDPOINT_HALT 0x02/0x01), Bulk-Only Mass Storage Reset (0x21/0xFF) и импульс пробуждения JieLi ISP (0x40/0xFD).\n" +
                            "• Метод 4 (Аппаратный MaskROM — замыкание 25Q80 пинцетом): При выключенном питании замкните на чипе флеш-памяти 25Q80 Pin 1 (/CS) на Pin 8 (VCC 3.3V) [или Pin 2 DO на Pin 4 GND / Pin 6 CLK на Pin 4 GND], включите тумблер ON — через 1.5 сек чип войдет в MaskROM UBOOT1.00, уберите пинцет и нажмите кнопку прошивки.\n" +
                            "• Метод 5 (Хирург .fwsc и реконструктор 0x0000–0x3FFF): Распаковка маркеров .fwsc (каждые 0x30 байт в первых 20 блоках), очистка опасных заголовков и реконструкция затертого 16 КБ загрузчика (Golden Boot Header JLWL82BT).\n" +
                            "• Метод 6 (Программатор CH341A 1 МБ / Мост RP2040): Сборка эталонного 1 МБ полного дампа SPI NOR Flash (0x000000–0x0FFFFF) для прищепки SOIC-8 CH341A или захвата через мост RP2040 Transporter."
                    )
                }
            }
        }
    }
}

@Composable
private fun DocSectionCard(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SynthOrange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
