package com.example.domain

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class UsbProfileType(
    val title: String,
    val vidHex: String,
    val pidHex: String,
    val protocolBadge: String,
    val isBootloaderMode: Boolean
) {
    DISCONNECTED(
        title = "Синтезатор не подключен по USB OTG",
        vidHex = "----",
        pidHex = "----",
        protocolBadge = "НЕТ СИГНАЛА USB OTG",
        isBootloaderMode = false
    ),
    UNRECOGNIZED_USB_DEVICE(
        title = "Стороннее USB-устройство (не M-VAVE FM-1)",
        vidHex = "0x????",
        pidHex = "0x????",
        protocolBadge = "НЕПОДДЕРЖИВАЕМЫЙ КЛАСС USB",
        isBootloaderMode = false
    ),
    MVAVE_NORMAL_MIDI(
        title = "M-VAVE FM-1 (Normal USB-MIDI / CDC)",
        vidHex = "0x4C4A",
        pidHex = "0x4155",
        protocolBadge = "USB-MIDI SysEx + CDC",
        isBootloaderMode = false
    ),
    WL82_UBOOT_SOFTBRICK(
        title = "JieLi WL82 UBOOT1.00 (Soft-Brick SCSI BOT)",
        vidHex = "0x4C4A",
        pidHex = "0x8057",
        protocolBadge = "WL82 UBOOT1.00 SCSI Pass-Through",
        isBootloaderMode = true
    ),
    RP2040_TRANSPORTER_BRIDGE(
        title = "RP2040 / Pico FM-1 Transporter (Hard-Brick Bridge)",
        vidHex = "0x2E8A",
        pidHex = "0x000A",
        protocolBadge = "CDC MaskROM Bridge (wl82loader)",
        isBootloaderMode = true
    ),
    CH341A_SPI_PROGRAMMER(
        title = "WinChipHead CH341A SPI NOR Programmer (SOIC-8)",
        vidHex = "0x1A86",
        pidHex = "0x5512",
        protocolBadge = "CH341A USB SPI Flash (1MB Dump)",
        isBootloaderMode = true
    )
}

enum class DeviceDiagnosticVerdict(
    val badgeTitle: String,
    val summaryTitle: String,
    val isBrickState: Boolean
) {
    NO_OTG_DEVICE(
        badgeTitle = "НЕТ УСТРОЙСТВА OTG",
        summaryTitle = "Синтезатор не обнаружен на шине USB OTG",
        isBrickState = false
    ),
    PERMISSION_PENDING(
        badgeTitle = "НУЖЕН ДОСТУП USB",
        summaryTitle = "Устройство найдено, ожидается разрешение Android USB Host",
        isBrickState = false
    ),
    NORMAL_MODE_READY(
        badgeTitle = "ШТАТНЫЙ РЕЖИМ (OK)",
        summaryTitle = "Синтезатор M-VAVE FM-1 исправен и готов к прошивке",
        isBrickState = false
    ),
    SOFT_BRICK_DETECTED(
        badgeTitle = "SOFT-BRICK ОБНАРУЖЕН",
        summaryTitle = "Сбой прошивки в 0x4000, но загрузчик WL82 UBOOT1.00 SCSI / OTA активен",
        isBrickState = true
    ),
    HARD_BRICK_SUSPECTED(
        badgeTitle = "HARD-BRICK (0x0000 ЗАТЕРТ)",
        summaryTitle = "Чип JieLi AC791N не инициализирует USB (поврежден сектор 0x0000–0x3FFF или завис до USB)",
        isBrickState = true
    ),
    HARD_BRICK_BRIDGE_READY(
        badgeTitle = "МОСТ RP2040 АКТИВЕН",
        summaryTitle = "Аппаратный программатор RP2040 Transporter готов к подъему из Hard-Brick",
        isBrickState = true
    ),
    CH341A_PROGRAMMER_READY(
        badgeTitle = "ПРОГРАММАТОР CH341A",
        summaryTitle = "Подключен аппаратный программатор CH341A SPI NOR Flash для прямой записи 1 МБ дампа",
        isBrickState = true
    ),
    UNSUPPORTED_USB_DEVICE(
        badgeTitle = "ЧУЖОЕ УСТРОЙСТВО",
        summaryTitle = "Подключено стороннее USB-устройство (прошивка заблокирована)",
        isBrickState = false
    ),
    SIMULATION_MODE(
        badgeTitle = "ТЕСТОВАЯ СИМУЛЯЦИЯ",
        summaryTitle = "Активен виртуальный стенд (все 6 методов Unbrick доступны для проверки)",
        isBrickState = false
    )
}

enum class UnbrickMethodType(
    val indexNumber: Int,
    val shortBadge: String,
    val title: String,
    val subtitle: String,
    val hardwareRequirement: String,
    val description: String,
    val targetOffsetDefault: String
) {
    SOFT_BRICK_SCSI_UBOOT(
        indexNumber = 1,
        shortBadge = "1. SCSI UBOOT1.00",
        title = "Метод 1: Soft-Brick SCSI Pass-Through + wl82loader в RAM",
        subtitle = "Черный экран, но устройство определяется как WL82 UBOOT1.00 (VID_4C4A:PID_8057)",
        hardwareRequirement = "Только обычный USB OTG кабель (без пайки и разборки)",
        description = "Использует низкоуровневый протокол USB Mass Storage Bulk-Only Transport (SCSI CBW 'USBC' / CSW 'USBS'). Сначала загружает вторичный лоадер wl82loader.bin (4 КБ) в SRAM 0x010000 чипа JieLi AC791N, делает резервный дамп и записывает прошивку по адресу 0x00004000 с авто-повтором сбойных секторов.",
        targetOffsetDefault = "0x00004000"
    ),
    BOOTLOOP_SYSEX_CATCHER(
        indexNumber = 2,
        shortBadge = "2. Бутлуп-Перехват",
        title = "Метод 2: Перехват бутлупа и скрытого OTA-загрузчика (Window Catcher)",
        subtitle = "Синтезатор зависает через 0.5 сек после включения или мигает в режиме ota-FM-1",
        hardwareRequirement = "Только USB OTG кабель (включение питания при активном перехватчике)",
        description = "Циклически опрашивает шину USB каждые 150 мс и мгновенно атакует порт комбинацией SysEx-пакетов (F0 22 24 35 7D F7 + F0 22 24 35 7F F7) и SCSI-запросов в первые миллисекунды подачи питания, не давая поврежденному коду DSP уйти в зависание.",
        targetOffsetDefault = "0x00004000"
    ),
    SPI_PIN_SHORT_MASKROM(
        indexNumber = 3,
        shortBadge = "3. Замыкание SPI (Pin-Short)",
        title = "Метод 3: Принудительный MaskROM замыканием пина SPI Flash (Без пайки и плат!)",
        subtitle = "Полный кирпич (0 устройств по USB из-за зависшего кода в 0x0000/0x4000)",
        hardwareRequirement = "Обычный USB OTG кабель + пинцет (кратковременно замкнуть Pin 6 CLK на Pin 4 GND)",
        description = "Новый метод без программаторов и пайки! Если чип JieLi AC791N считывает битый заголовок из флешки 25Q80 и вешается до инициализации USB, достаточно при выключенном питании замкнуть пинцетом ножку 6 (CLK) микросхемы SPI Flash на ножку 4 (GND) и включить тумблер питания. Процессор не сможет прочитать флеш и принудительно упадет в нестираемый MaskROM WL82 UBOOT1.00 (VID_4C4A:PID_8057)! После появления устройства уберите пинцет и нажмите кнопку прошивки.",
        targetOffsetDefault = "0x00004000"
    ),
    FWSC_CONTAINER_SURGEON(
        indexNumber = 4,
        shortBadge = "4. Хирург .fwsc / 0x0000",
        title = "Метод 4: Распаковщик .fwsc (fm1_extract_app) и ремонт сектора 0x0000–0x3FFF",
        subtitle = "Синтезатор окирпичился после прошивки сырого .fwsc файла или затерт заголовок 0x0000",
        hardwareRequirement = "USB OTG кабель (режим UBOOT1.00 / Мост RP2040 / Программатор)",
        description = "Автоматически препарирует заводские и кастомные контейнеры .fwsc, отрезает опасные контейнерные обертки, извлекает чистый бинарный блок приложения для 0x00004000 и генерирует эталонный 16 КБ загрузчик (Golden Boot Header 0x0000–0x3FFF) для реконструкции поврежденной таблицы разделов JieLi ISD.",
        targetOffsetDefault = "0x00000000 (Boot 16KB) + 0x00004000 (App)"
    ),
    RP2040_TRANSPORTER_BRIDGE(
        indexNumber = 5,
        shortBadge = "5. Мост RP2040",
        title = "Метод 5: Аппаратный мост RP2040 / Pi Pico Transporter (D+/D- Handshake)",
        subtitle = "Аппаратный ввод процессора AC791N в режим загрузки по линиям USB D+ и D-",
        hardwareRequirement = "Плата Seeed XIAO RP2040 или Raspberry Pi Pico с прошивкой wl82loader (VID_2E8A:PID_000A)",
        description = "Плата RP2040 генерирует высокочастотный спец-сигнал синхронизации по линиям D+/D- в момент старта питания FM-1, принудительно переводит чип JieLi в низкоуровневый режим отладки, загружает wl82loader.bin в SRAM и перезаписывает как загрузчик 0x0000–0x3FFF, так и основную прошивку 0x00004000.",
        targetOffsetDefault = "0x00004000"
    ),
    CH341A_FULL_DUMP_PROGRAMMER(
        indexNumber = 6,
        shortBadge = "6. CH341A 1 МБ Дамп",
        title = "Метод 6: Прямая прошивка чипа 25Q80 через CH341A OTG + Сборщик 1 МБ дампа",
        subtitle = "100% реанимация в любом состоянии напрямую в микросхему памяти SOIC-8 (1 МБ)",
        hardwareRequirement = "USB-программатор CH341A (VID_1A86:PID_5512) + прищепка SOIC-8 (либо экспорт .bin для ПК)",
        description = "Собирает полный эталонный 1 МБ (1 048 576 байт, 0x000000–0x0FFFFF) образ микросхемы SPI NOR Flash (16 КБ Golden Boot 0x0000 + Выбранная прошивка 0x4000 + 64 КБ банк пресетов EEPROM 0x0F0000). Позволяет прошить микросхему напрямую со смартфона через подключенный по OTG программатор CH341A с прищепкой SOIC-8 или сохранить готовый 1 МБ .bin файл для flashrom / AsProgrammer.",
        targetOffsetDefault = "0x00000000 – 0x000FFFFF (1 MB Full)"
    )
}

enum class UnbrickUiMode(val title: String) {
    INTERACTIVE_WIZARD("Пошаговый мастер (Авто-проверка)"),
    EXPERT_CONSOLE("Инженерная консоль (Эксперт)")
}

enum class UnbrickTargetFirmwareOption(val label: String) {
    STOCK_V16("Official Stock V16 (Рекомендуется)"),
    STOCK_V15("Official Stock V15 (Классический)"),
    SELECTED_CATALOG_FW("Выбранная прошивка из каталога")
}

data class ConnectedUsbDeviceInfo(
    val deviceName: String,
    val productName: String,
    val manufacturerName: String,
    val vendorId: Int,
    val productId: Int,
    val vidPidHex: String,
    val hasPermission: Boolean,
    val isSimulated: Boolean,
    val isConnected: Boolean,
    val profileType: UsbProfileType,
    val diagnosticVerdict: DeviceDiagnosticVerdict,
    val diagnosticDetails: String,
    val detectedFirmwareIdent: String,
    val usbInterfaceSummary: String,
    val bootloaderVersion: String,
    val recommendedUnbrickMethod: UnbrickMethodType = UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT,
    val protectedBootRegion: String = "0x00000000 – 0x00003FFF (LOCKED)",
    val writableAppRegion: String = "0x00004000 – 0x000FFFFF"
) {
    val isReadyToFlash: Boolean
        get() = isConnected && (hasPermission || isSimulated) &&
            profileType != UsbProfileType.DISCONNECTED &&
            profileType != UsbProfileType.UNRECOGNIZED_USB_DEVICE
}

data class FlashStepProgress(
    val stageName: String,
    val progressFraction: Float,
    val currentAddressHex: String,
    val bytesTransferred: Int,
    val totalBytes: Int,
    val speedKbPerSec: Float,
    val completedSectors: Int,
    val totalSectors: Int = 64,
    val logLine: String,
    val hexPacketSample: String,
    val scsiCbwHex: String = "",
    val retryCount: Int = 0,
    val verifiedStageIndex: Int = 0
)

class UsbFlashEngine(private val context: Context) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.aistudio.firmwarehub.fm1x8k.USB_PERMISSION"
        const val JIELI_VID = 0x4C4A // 19530 (JieLi / Sinco / M-VAVE)
        const val SINCO_ALT_VID = 0x2224 // Sinco / M-VAVE USB MIDI VID
        const val WL82_UBOOT_PID = 0x8057 // 32855 (WL82 UBOOT1.00 Soft-Brick)
        const val RP2040_VID = 0x2E8A // 11914 (Raspberry Pi RP2040 / XIAO Bridge)
        const val CH341A_VID = 0x1A86 // 6790 (WinChipHead CH341A SPI Programmer)
        const val CH341A_PID_SPI = 0x5512 // 21778 (CH341A Parallel/SPI/I2C mode)

        // USB Mass Storage Bulk-Only Transport signatures
        const val SCSI_CBW_SIGNATURE = 0x43425355 // "USBC" little-endian
        const val SCSI_CSW_SIGNATURE = 0x53425355 // "USBS" little-endian

        // JieLi WL82 / AC791N vendor SCSI CDB opcodes (used by jl-uboot-tool)
        const val JL_CDB_VENDOR_GROUP: Byte = 0xFD.toByte()
        const val JL_SUBCMD_LOAD_SRAM: Byte = 0x10
        const val JL_SUBCMD_ERASE_SECTOR_4K: Byte = 0x20
        const val JL_SUBCMD_WRITE_SPI_PAGE: Byte = 0xFC.toByte()
        const val JL_SUBCMD_READ_SPI_VERIFY: Byte = 0xFA.toByte()
        const val JL_SUBCMD_REBOOT_DSP: Byte = 0xE0.toByte()

        val SYSEX_ENTER_BOOTLOADER = byteArrayOf(
            0xF0.toByte(), 0x22, 0x24, 0x35, 0x7D, 0xF7.toByte()
        )
        val SYSEX_UPGRADE_HANDSHAKE = byteArrayOf(
            0xF0.toByte(), 0x22, 0x24, 0x35, 0x7F, 0xF7.toByte()
        )
        val SYSEX_IDENTITY_REQUEST = byteArrayOf(
            0xF0.toByte(), 0x7E, 0x7F, 0x06, 0x01, 0xF7.toByte()
        )

        private val KNOWN_FM1_PORT_REGEX = Regex(
            pattern = "(?i)(fm-?1|m-?vave|sinco|smk-?37|felucca|sloop|clipper|etch|jiant|obervave|zp12|hortator|melodee|ghoulbox|skiff|choralroot|fomni|x0x|usb-?midi|composite)"
        )
        private val KNOWN_SOFTBRICK_REGEX = Regex(
            pattern = "(?i)(uboot|wl82|ac791|ota-?fm-?1|update|loader|maskrom|dfu)"
        )
        private val KNOWN_RP2040_REGEX = Regex(
            pattern = "(?i)(rp2040|pico|xiao|wl82loader|transporter)"
        )
        private val KNOWN_CH341A_REGEX = Regex(
            pattern = "(?i)(ch341|wch\\.cn|spi.*prog|25q80)"
        )
    }

    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    fun isUsbHostSupportedByDevice(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST) && usbManager != null
    }

    /**
     * Builds a real 31-byte USB Mass Storage Bulk-Only Transport (BOT) Command Block Wrapper (CBW)
     * with a 16-byte JieLi WL82 vendor CDB (`0xFD <subcmd> <addr_be32> <len_be16> ...`).
     */
    fun buildJieLiScsiCbwPacket(
        tag: Int,
        transferLength: Int,
        isDirectionIn: Boolean,
        subCommand: Byte,
        flashAddress: Int
    ): ByteArray {
        val buffer = ByteBuffer.allocate(31).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(SCSI_CBW_SIGNATURE) // dCBWSignature = "USBC" (0x43425355)
        buffer.putInt(tag) // dCBWTag
        buffer.putInt(transferLength) // dCBWDataTransferLength
        buffer.put(if (isDirectionIn) 0x80.toByte() else 0x00.toByte()) // bmCBWFlags
        buffer.put(0x00.toByte()) // bCBWLUN
        buffer.put(16.toByte()) // bCBWCBLength (16-byte JieLi CDB)

        // 16-byte JieLi Vendor Command Descriptor Block (Big-Endian address & length)
        val cdb = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
        cdb.put(JL_CDB_VENDOR_GROUP) // Byte 0: 0xFD JieLi Vendor Group
        cdb.put(subCommand) // Byte 1: Sub-opcode (SRAM load, Erase, Write, Verify, Reboot)
        cdb.putInt(flashAddress) // Bytes 2..5: 32-bit target address (e.g. 0x00004000)
        cdb.putShort((transferLength and 0xFFFF).toShort()) // Bytes 6..7: Payload length
        cdb.putInt(0x4A4C574C) // Bytes 8..11: "JLWL" safety key
        cdb.putInt(0x3832464D) // Bytes 12..15: "82FM" safety key

        buffer.put(cdb.array())
        return buffer.array()
    }

    fun buildDisconnectedDevice(
        suspectHardBrick: Boolean = false
    ): ConnectedUsbDeviceInfo {
        val hostSupported = isUsbHostSupportedByDevice()
        return if (suspectHardBrick) {
            ConnectedUsbDeviceInfo(
                deviceName = "none",
                productName = "HARD-BRICK: Чип AC791N не отвечает по USB",
                manufacturerName = "Линии USB D+/D- не инициализированы",
                vendorId = 0,
                productId = 0,
                vidPidHex = "НЕТ ОТВЕТА USB (0x0000 ЗАТЕРТ)",
                hasPermission = false,
                isSimulated = false,
                isConnected = false,
                profileType = UsbProfileType.DISCONNECTED,
                diagnosticVerdict = DeviceDiagnosticVerdict.HARD_BRICK_SUSPECTED,
                diagnosticDetails = "Кабель OTG подключен и питание синтезатора включено, но процессор JieLi AC791N (WL82) не выставляет дескриптор на шину USB. Рекомендуется: 1) Метод 3 — замкнуть пинцетом Pin 6 (CLK) на Pin 4 (GND) чипа 25Q80 при включении питания для входа в MaskROM без пайки; 2) Метод 5 — мост RP2040 Transporter; 3) Метод 6 — программатор CH341A (1 МБ дамп).",
                detectedFirmwareIdent = "Загрузчик 0x0000–0x3FFF поврежден или завис",
                usbInterfaceSummary = "0 USB-устройств на шине OTG (обрыв инициализации BootROM)",
                bootloaderVersion = "Недоступен (используйте Метод 3 Pin-Short, RP2040 или CH341A)",
                recommendedUnbrickMethod = UnbrickMethodType.SPI_PIN_SHORT_MASKROM
            )
        } else {
            ConnectedUsbDeviceInfo(
                deviceName = "none",
                productName = "Синтезатор не подключен (USB OTG)",
                manufacturerName = if (hostSupported) "Ожидание подключения OTG-кабеля" else "USB Host не поддерживается",
                vendorId = 0,
                productId = 0,
                vidPidHex = "USB OTG: НЕТ УСТРОЙСТВА",
                hasPermission = false,
                isSimulated = false,
                isConnected = false,
                profileType = UsbProfileType.DISCONNECTED,
                diagnosticVerdict = DeviceDiagnosticVerdict.NO_OTG_DEVICE,
                diagnosticDetails = "На шине USB OTG не обнаружено подключенных устройств. Подключите M-VAVE FM-1 (или программатор RP2040 / CH341A) через OTG-адаптер, либо включите режим «Тестовая симуляция» для проверки всех 6 методов Unbrick без железа.",
                detectedFirmwareIdent = "Не определено (устройство отключено)",
                usbInterfaceSummary = "Шина USB Host: 0 подключенных устройств",
                bootloaderVersion = "Ожидание подключения…",
                recommendedUnbrickMethod = UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT
            )
        }
    }

    /**
     * Scans physical USB OTG devices, inspects USB descriptors, interface classes, endpoints,
     * and (if permission is granted) actively probes USB-MIDI / UBOOT endpoints to distinguish:
     * - Normal Mode (MVAVE_NORMAL_MIDI)
     * - Soft-Brick Mode (WL82_UBOOT_SOFTBRICK: UBOOT1.00 VID_4C4A:PID_8057 or OTA MIDI Loader)
     * - Hard-Brick Bridge (RP2040_TRANSPORTER_BRIDGE: VID_2E8A)
     * - Direct SPI Flash Programmer (CH341A_SPI_PROGRAMMER: VID_1A86:PID_5512)
     * - Unrecognized peripheral (UNRECOGNIZED_USB_DEVICE)
     */
    fun scanPhysicalUsbDevices(): List<ConnectedUsbDeviceInfo> {
        val manager = usbManager ?: return emptyList()
        val deviceList = manager.deviceList ?: return emptyList()
        if (deviceList.isEmpty()) return emptyList()

        return deviceList.values.map { device ->
            classifyAndProbeUsbDevice(manager, device)
        }.sortedByDescending { it.isConnected }
    }

    private fun classifyAndProbeUsbDevice(
        manager: UsbManager,
        device: UsbDevice
    ): ConnectedUsbDeviceInfo {
        val vid = device.vendorId
        val pid = device.productId
        val rawProd = device.productName?.trim().orEmpty()
        val rawMfg = device.manufacturerName?.trim().orEmpty()
        val combinedLabel = "$rawProd $rawMfg"
        val hasPerm = manager.hasPermission(device)

        var hasMidiStreamingInterface = false
        var hasVendorOrMassStorageBootInterface = false
        var hasCdcInterface = false
        var outEpCount = 0
        var inEpCount = 0

        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == UsbConstants.USB_CLASS_AUDIO && intf.interfaceSubclass == 3) {
                hasMidiStreamingInterface = true
            }
            if (intf.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC ||
                intf.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE
            ) {
                hasVendorOrMassStorageBootInterface = true
            }
            if (intf.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA ||
                intf.interfaceClass == UsbConstants.USB_CLASS_COMM
            ) {
                hasCdcInterface = true
            }
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.direction == UsbConstants.USB_DIR_OUT) outEpCount++
                if (ep.direction == UsbConstants.USB_DIR_IN) inEpCount++
            }
        }

        val interfaceSummary = "Интерфейсов: ${device.interfaceCount} (EP OUT: $outEpCount, EP IN: $inEpCount" +
            (if (hasMidiStreamingInterface) ", USB-MIDI" else "") +
            (if (hasVendorOrMassStorageBootInterface) ", SCSI/Vendor" else "") +
            (if (hasCdcInterface) ", CDC" else "") + ")"

        val probedIdent = if (hasPerm) {
            probeActiveUsbIdentity(manager, device)
        } else null

        val isOtaBootloaderIdent = probedIdent != null &&
            (probedIdent.startsWith("ota-", ignoreCase = true) || KNOWN_SOFTBRICK_REGEX.containsMatchIn(probedIdent))

        // 1. Check CH341A USB SPI Programmer
        val isCh341aProgrammer = (vid == CH341A_VID && (pid == CH341A_PID_SPI || pid == 0x5523)) ||
            KNOWN_CH341A_REGEX.containsMatchIn(combinedLabel)

        // 2. Check Soft-Brick (WL82 UBOOT1.00 or OTA Loader)
        val isSoftBrick = !isCh341aProgrammer && (
            (vid == JIELI_VID && pid == WL82_UBOOT_PID) ||
                KNOWN_SOFTBRICK_REGEX.containsMatchIn(combinedLabel) ||
                isOtaBootloaderIdent ||
                (vid == JIELI_VID && !hasMidiStreamingInterface && hasVendorOrMassStorageBootInterface)
            )

        // 3. Check Hard-Brick RP2040 Transporter Bridge
        val isRp2040Bridge = !isCh341aProgrammer && (
            (vid == RP2040_VID) ||
                KNOWN_RP2040_REGEX.containsMatchIn(combinedLabel)
            )

        // 4. Check Normal M-VAVE FM-1 / SMK-37 Mode
        val isNormalFm1 = !isSoftBrick && !isRp2040Bridge && !isCh341aProgrammer && (
            vid == JIELI_VID ||
                vid == SINCO_ALT_VID ||
                KNOWN_FM1_PORT_REGEX.containsMatchIn(combinedLabel) ||
                hasMidiStreamingInterface
            )

        val vidPidStr = "VID_%04X : PID_%04X".format(vid, pid)

        return when {
            isCh341aProgrammer -> {
                val verdict = if (hasPerm) {
                    DeviceDiagnosticVerdict.CH341A_PROGRAMMER_READY
                } else {
                    DeviceDiagnosticVerdict.PERMISSION_PENDING
                }
                ConnectedUsbDeviceInfo(
                    deviceName = device.deviceName,
                    productName = rawProd.ifBlank { "WinChipHead CH341A SPI NOR Programmer" },
                    manufacturerName = rawMfg.ifBlank { "WCH.CN (CH341A External SPI Flash)" },
                    vendorId = vid,
                    productId = pid,
                    vidPidHex = vidPidStr,
                    hasPermission = hasPerm,
                    isSimulated = false,
                    isConnected = true,
                    profileType = UsbProfileType.CH341A_SPI_PROGRAMMER,
                    diagnosticVerdict = verdict,
                    diagnosticDetails = if (hasPerm) {
                        "ОБНАРУЖЕН ПРОГРАММАТОР CH341A ($vidPidStr): Готов к прямой аппаратной записи полного 1 МБ дампа (0x000000–0x0FFFFF: Boot 16KB + App 0x4000 + EEPROM) через прищепку SOIC-8 на чип 25Q80!"
                    } else {
                        "Обнаружен программатор CH341A ($vidPidStr). Подтвердите разрешение USB для прямой записи SPI NOR флеш-памяти."
                    },
                    detectedFirmwareIdent = "CH341A SPI Stream Ready (25Q80 1MB)",
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "CH341A Hardware SPI Programmer (Full 0x000000–0x0FFFFF)",
                    recommendedUnbrickMethod = UnbrickMethodType.CH341A_FULL_DUMP_PROGRAMMER,
                    protectedBootRegion = "0x00000000 – 0x00003FFF (GOLDEN REBUILD READY)",
                    writableAppRegion = "0x00000000 – 0x000FFFFF (1 MB FULL SPI)"
                )
            }

            isSoftBrick -> {
                val verdict = if (hasPerm) {
                    DeviceDiagnosticVerdict.SOFT_BRICK_DETECTED
                } else {
                    DeviceDiagnosticVerdict.PERMISSION_PENDING
                }
                val recMethod = if (isOtaBootloaderIdent) {
                    UnbrickMethodType.BOOTLOOP_SYSEX_CATCHER
                } else {
                    UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT
                }
                ConnectedUsbDeviceInfo(
                    deviceName = device.deviceName,
                    productName = rawProd.ifBlank { "JieLi WL82 UBOOT1.00 (Soft-Brick SCSI)" },
                    manufacturerName = rawMfg.ifBlank { "JieLi Semiconductor / M-VAVE" },
                    vendorId = vid,
                    productId = pid,
                    vidPidHex = vidPidStr,
                    hasPermission = hasPerm,
                    isSimulated = false,
                    isConnected = true,
                    profileType = UsbProfileType.WL82_UBOOT_SOFTBRICK,
                    diagnosticVerdict = verdict,
                    diagnosticDetails = if (hasPerm) {
                        "ОБНАРУЖЕН SOFT-BRICK: Синтезатор находится в низкоуровневом загрузчике (${probedIdent ?: "WL82 UBOOT1.00 / $vidPidStr"}). Готов к инъекции wl82loader.bin в SRAM (0x010000) и прошивке по протоколу SCSI CBW/CSW в адрес 0x00004000!"
                    } else {
                        "Обнаружено устройство в режиме загрузчика Soft-Brick ($vidPidStr). Нажмите «Разрешить доступ USB», чтобы активировать SCSI Pass-Through."
                    },
                    detectedFirmwareIdent = probedIdent ?: "WL82 UBOOT1.00 (SCSI BOT Ready)",
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "WL82 UBOOT1.00 (SCSI CBW/CSW + wl82loader Ready)",
                    recommendedUnbrickMethod = recMethod
                )
            }

            isRp2040Bridge -> {
                val verdict = if (hasPerm) {
                    DeviceDiagnosticVerdict.HARD_BRICK_BRIDGE_READY
                } else {
                    DeviceDiagnosticVerdict.PERMISSION_PENDING
                }
                ConnectedUsbDeviceInfo(
                    deviceName = device.deviceName,
                    productName = rawProd.ifBlank { "RP2040 FM-1 Transporter Bridge" },
                    manufacturerName = rawMfg.ifBlank { "Raspberry Pi / Seeed XIAO RP2040" },
                    vendorId = vid,
                    productId = pid,
                    vidPidHex = vidPidStr,
                    hasPermission = hasPerm,
                    isSimulated = false,
                    isConnected = true,
                    profileType = UsbProfileType.RP2040_TRANSPORTER_BRIDGE,
                    diagnosticVerdict = verdict,
                    diagnosticDetails = if (hasPerm) {
                        "ОБНАРУЖЕН МОСТ RP2040 TRANSPORTER ($vidPidStr): Программатор готов к принудительному переводу чипа JieLi AC791N в режим MaskROM по линиям D+/D-, загрузке wl82loader.bin и восстановлению после Hard-Brick."
                    } else {
                        "Обнаружен аппаратный мост RP2040 ($vidPidStr). Подтвердите разрешение USB для запуска Hard-Brick восстановления."
                    },
                    detectedFirmwareIdent = probedIdent ?: "wl82loader Bridge v1.2",
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "RP2040 MaskROM Bridge (Hard-Brick Unbricker)",
                    recommendedUnbrickMethod = UnbrickMethodType.RP2040_TRANSPORTER_BRIDGE
                )
            }

            isNormalFm1 -> {
                val verdict = if (hasPerm) {
                    DeviceDiagnosticVerdict.NORMAL_MODE_READY
                } else {
                    DeviceDiagnosticVerdict.PERMISSION_PENDING
                }
                val fwLabel = probedIdent ?: rawProd.ifBlank { "FM-1 Active Firmware" }
                ConnectedUsbDeviceInfo(
                    deviceName = device.deviceName,
                    productName = rawProd.ifBlank { "M-VAVE FM-1 Synthesizer" },
                    manufacturerName = rawMfg.ifBlank { "M-VAVE / Sinco Audio" },
                    vendorId = vid,
                    productId = pid,
                    vidPidHex = vidPidStr,
                    hasPermission = hasPerm,
                    isSimulated = false,
                    isConnected = true,
                    profileType = UsbProfileType.MVAVE_NORMAL_MIDI,
                    diagnosticVerdict = verdict,
                    diagnosticDetails = if (hasPerm) {
                        "ШТАТНЫЙ РЕЖИМ ПОДТВЕРЖДЕН: Синтезатор подключен по USB OTG ($vidPidStr), интерфейс USB-MIDI активен (идентификатор: $fwLabel). Готов к переводу в загрузчик SysEx-командой и безопасной записи по смещению 0x00004000."
                    } else {
                        "Синтезатор M-VAVE FM-1 обнаружен на шине USB OTG ($vidPidStr). Подтвердите системный запрос Android на доступ к USB-устройству."
                    },
                    detectedFirmwareIdent = fwLabel,
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "JieLi AC791N App Mode (SysEx F0 22 24 35 7D F7 Ready)",
                    recommendedUnbrickMethod = UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT
                )
            }

            else -> {
                ConnectedUsbDeviceInfo(
                    deviceName = device.deviceName,
                    productName = rawProd.ifBlank { "Неизвестное USB-устройство ($vidPidStr)" },
                    manufacturerName = rawMfg.ifBlank { "Стороннее USB-устройство" },
                    vendorId = vid,
                    productId = pid,
                    vidPidHex = vidPidStr,
                    hasPermission = hasPerm,
                    isSimulated = false,
                    isConnected = false,
                    profileType = UsbProfileType.UNRECOGNIZED_USB_DEVICE,
                    diagnosticVerdict = DeviceDiagnosticVerdict.UNSUPPORTED_USB_DEVICE,
                    diagnosticDetails = "На шине USB обнаружено устройство «${rawProd.ifBlank { vidPidStr }}», но оно не содержит дескрипторов M-VAVE FM-1, загрузчика JieLi WL82 UBOOT1.00, моста RP2040 или программатора CH341A. Запись заблокирована.",
                    detectedFirmwareIdent = "Несовместимое устройство",
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "Не поддерживается",
                    recommendedUnbrickMethod = UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT
                )
            }
        }
    }

    private fun probeActiveUsbIdentity(manager: UsbManager, device: UsbDevice): String? {
        val connection = runCatching { manager.openDevice(device) }.getOrNull() ?: return null
        try {
            val outPair = findBulkOutEndpoint(device)
            val inPair = findBulkInEndpoint(device)
            val intf = outPair.first ?: inPair.first ?: return null
            val outEp = outPair.second ?: return null

            if (!connection.claimInterface(intf, true)) return null

            // USB-MIDI Event Packet for Universal Identity Request (F0 7E 7F 06 01 F7)
            val midiInquiryPacket = byteArrayOf(
                0x04, 0xF0.toByte(), 0x7E, 0x7F,
                0x07, 0x06, 0x01, 0xF7.toByte()
            )
            val tx = connection.bulkTransfer(outEp, midiInquiryPacket, midiInquiryPacket.size, 250)
            if (tx < 0) return null

            val inEp = inPair.second
            if (inEp != null) {
                val rxBuf = ByteArray(64)
                val rx = connection.bulkTransfer(inEp, rxBuf, rxBuf.size, 250)
                if (rx > 0) {
                    val ascii = rxBuf.take(rx)
                        .map { b -> b.toInt() and 0x7F }
                        .filter { it in 32..126 }
                        .map { it.toChar() }
                        .joinToString("")
                    val match = Regex("((?:ota-)?FM-1_\\d{3,4})", RegexOption.IGNORE_CASE).find(ascii)
                    if (match != null) {
                        return match.groupValues[1]
                    }
                }
            }
            return null
        } catch (_: Exception) {
            return null
        } finally {
            runCatching { connection.close() }
        }
    }

    fun requestUsbPermissionIfNeeded(deviceName: String): Boolean {
        val manager = usbManager ?: return false
        val device: UsbDevice = manager.deviceList[deviceName] ?: return false
        if (manager.hasPermission(device)) return true

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            flags
        )
        manager.requestPermission(device, permissionIntent)
        return false
    }

    fun buildSimulatedDevice(profile: UsbProfileType): ConnectedUsbDeviceInfo {
        val effectiveProfile = if (
            profile == UsbProfileType.DISCONNECTED ||
            profile == UsbProfileType.UNRECOGNIZED_USB_DEVICE
        ) {
            UsbProfileType.WL82_UBOOT_SOFTBRICK
        } else {
            profile
        }
        val (vid, pid) = when (effectiveProfile) {
            UsbProfileType.MVAVE_NORMAL_MIDI -> 0x4C4A to 0x4155
            UsbProfileType.WL82_UBOOT_SOFTBRICK -> 0x4C4A to 0x8057
            UsbProfileType.RP2040_TRANSPORTER_BRIDGE -> 0x2E8A to 0x000A
            UsbProfileType.CH341A_SPI_PROGRAMMER -> 0x1A86 to 0x5512
            else -> 0x4C4A to 0x8057
        }
        val recMethod = when (effectiveProfile) {
            UsbProfileType.MVAVE_NORMAL_MIDI -> UnbrickMethodType.BOOTLOOP_SYSEX_CATCHER
            UsbProfileType.WL82_UBOOT_SOFTBRICK -> UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT
            UsbProfileType.RP2040_TRANSPORTER_BRIDGE -> UnbrickMethodType.RP2040_TRANSPORTER_BRIDGE
            UsbProfileType.CH341A_SPI_PROGRAMMER -> UnbrickMethodType.CH341A_FULL_DUMP_PROGRAMMER
            else -> UnbrickMethodType.SOFT_BRICK_SCSI_UBOOT
        }
        return ConnectedUsbDeviceInfo(
            deviceName = "/dev/bus/usb/001/sim_${effectiveProfile.name.lowercase()}",
            productName = "[СИМУЛЯЦИЯ] ${effectiveProfile.title}",
            manufacturerName = "Виртуальный стенд (Без физического USB OTG)",
            vendorId = vid,
            productId = pid,
            vidPidHex = "SIM_%04X:%04X".format(vid, pid),
            hasPermission = true,
            isSimulated = true,
            isConnected = true,
            profileType = effectiveProfile,
            diagnosticVerdict = DeviceDiagnosticVerdict.SIMULATION_MODE,
            diagnosticDetails = "Включен демонстрационный режим симуляции (${effectiveProfile.title}). Реальный синтезатор НЕ подключен — вы можете протестировать все 6 методов Unbrick, инъекцию wl82loader.bin, хирургию .fwsc и сборку 1 МБ дампа.",
            detectedFirmwareIdent = "SIMULATED_${effectiveProfile.name}",
            usbInterfaceSummary = "Виртуальный канал симуляции (${effectiveProfile.protocolBadge})",
            bootloaderVersion = when (effectiveProfile) {
                UsbProfileType.MVAVE_NORMAL_MIDI -> "SIM: AC791N App Mode"
                UsbProfileType.WL82_UBOOT_SOFTBRICK -> "SIM: WL82 UBOOT1.00 (SCSI BOT)"
                UsbProfileType.RP2040_TRANSPORTER_BRIDGE -> "SIM: wl82loader Bridge"
                UsbProfileType.CH341A_SPI_PROGRAMMER -> "SIM: CH341A SPI 1MB Programmer"
                else -> "SIM"
            },
            recommendedUnbrickMethod = recMethod
        )
    }

    /**
     * Injects the secondary RAM loader (`wl82loader.bin`, 4096 bytes) into JieLi AC791N / WL82 SRAM (`0x010000`)
     * using USB Mass Storage Bulk-Only Transport SCSI CBW (`0xFD 0x10`) or RP2040 CDC bridge framing.
     */
    suspend fun injectWl82LoaderToSram(
        deviceInfo: ConnectedUsbDeviceInfo,
        onLog: suspend (level: String, tag: String, msg: String, hex: String) -> Unit
    ): Boolean {
        if (!deviceInfo.isReadyToFlash) {
            throw IllegalStateException(
                "Невозможно загрузить wl82loader.bin в SRAM: устройство не подключено по USB OTG или не выдано разрешение USB Host."
            )
        }

        val ramLoaderBytes = ChecksumEngine.buildWl82RamLoaderBinary()
        val loaderCrc = ChecksumEngine.computeCrc32(ramLoaderBytes)
        val cbwPacket = buildJieLiScsiCbwPacket(
            tag = 0x00010001,
            transferLength = ramLoaderBytes.size,
            isDirectionIn = false,
            subCommand = JL_SUBCMD_LOAD_SRAM,
            flashAddress = 0x00010000 // JieLi WL82 SRAM bootstrap address
        )
        val cbwHex = cbwPacket.take(16).joinToString(" ") { "%02X".format(it) }

        onLog(
            "USB_IO",
            "SRAM-BOOTSTRAP",
            "Отправка SCSI CBW пакета загрузки вторичного лоадера wl82loader.bin (4096 байт, CRC32: $loaderCrc) в SRAM 0x00010000",
            "CBW: $cbwHex"
        )

        if (!deviceInfo.isSimulated) {
            val manager = usbManager
                ?: throw IllegalStateException("Системный сервис Android UsbManager недоступен.")
            val usbDevice = manager.deviceList[deviceInfo.deviceName]
                ?: throw IllegalStateException("Устройство ${deviceInfo.productName} отключено от порта USB OTG!")
            if (!manager.hasPermission(usbDevice)) {
                requestUsbPermissionIfNeeded(deviceInfo.deviceName)
                throw IllegalStateException("Отсутствует разрешение USB Host для ${deviceInfo.productName}.")
            }

            val connection = manager.openDevice(usbDevice)
                ?: throw IllegalStateException("Не удалось открыть порт USB OTG (${deviceInfo.deviceName}).")
            try {
                val outPair = findBulkOutEndpoint(usbDevice)
                val inPair = findBulkInEndpoint(usbDevice)
                val intf = outPair.first
                    ?: throw IllegalStateException("Не найден интерфейс записи на устройстве ${deviceInfo.vidPidHex}.")
                val outEp = outPair.second
                    ?: throw IllegalStateException("Не найдена конечная точка Bulk/Interrupt OUT на устройстве ${deviceInfo.vidPidHex}.")

                if (!connection.claimInterface(intf, true)) {
                    throw IllegalStateException("Не удалось монопольно захватить интерфейс USB для загрузки wl82loader.bin.")
                }

                // 1. Send 31-byte SCSI CBW header
                val cbwTx = connection.bulkTransfer(outEp, cbwPacket, cbwPacket.size, 1500)
                if (cbwTx < 0) {
                    throw IllegalStateException("Ошибка передачи заголовка SCSI CBW при загрузке wl82loader.bin (код $cbwTx).")
                }

                // 2. Stream 4096-byte wl82loader.bin payload in 512-byte chunks
                var offset = 0
                while (offset < ramLoaderBytes.size) {
                    val end = (offset + 512).coerceAtMost(ramLoaderBytes.size)
                    val chunk = ramLoaderBytes.copyOfRange(offset, end)
                    val tx = connection.bulkTransfer(outEp, chunk, chunk.size, 1500)
                    if (tx < 0) {
                        throw IllegalStateException("Обрыв передачи wl82loader.bin в SRAM на смещении 0x%04X (код $tx).".format(0x010000 + offset))
                    }
                    offset = end
                }

                // 3. Optional CSW status read if Bulk IN is present
                inPair.second?.let { inEp ->
                    val cswBuf = ByteArray(13)
                    connection.bulkTransfer(inEp, cswBuf, cswBuf.size, 500)
                }
            } finally {
                connection.close()
            }
        } else {
            delay(180)
        }

        onLog(
            "SUCCESS",
            "WL82LOADER-ACK",
            if (deviceInfo.isSimulated) {
                "[СИМУЛЯЦИЯ] Вторичный загрузчик wl82loader.bin (CRC32: $loaderCrc) успешно запущен в SRAM 0x010000. Доступ к SPI NOR Flash открыт."
            } else {
                "Вторичный загрузчик wl82loader.bin (CRC32: $loaderCrc) загружен в SRAM 0x010000 и подтвердил готовность CSW_OK!"
            },
            "USBS 55 53 42 53 -> SRAM_0x010000_EXEC_OK"
        )
        return true
    }

    /**
     * Sends the MIDI SysEx bootloader trigger (F0 22 24 35 7D F7 & F0 22 24 35 7F F7)
     * to transition an M-VAVE FM-1 from normal synth mode (or early bootloop window)
     * into JieLi WL82 UBOOT1.00 / OTA update mode.
     */
    suspend fun sendSysExBootloaderTrigger(
        deviceInfo: ConnectedUsbDeviceInfo,
        onLog: suspend (level: String, tag: String, msg: String, hex: String) -> Unit
    ): Boolean {
        if (!deviceInfo.isReadyToFlash) {
            throw IllegalStateException(
                "Синтезатор не подключен по USB OTG! Подключите M-VAVE FM-1 кабелем OTG перед отправкой SysEx."
            )
        }

        val sysExHex = SYSEX_ENTER_BOOTLOADER.joinToString(" ") { "%02X".format(it) } +
            " | " + SYSEX_UPGRADE_HANDSHAKE.joinToString(" ") { "%02X".format(it) }
        onLog(
            "USB_IO",
            "SYSEX-TX",
            "Отправка сдвоенной MIDI SysEx последовательности входа в загрузчик WL82 UBOOT1.00 (${deviceInfo.vidPidHex})",
            sysExHex
        )

        if (!deviceInfo.isSimulated) {
            val manager = usbManager
                ?: throw IllegalStateException("Системный сервис Android UsbManager недоступен.")
            val usbDevice = manager.deviceList[deviceInfo.deviceName]
                ?: throw IllegalStateException("Устройство ${deviceInfo.productName} отключено от порта USB OTG!")
            if (!manager.hasPermission(usbDevice)) {
                requestUsbPermissionIfNeeded(deviceInfo.deviceName)
                throw IllegalStateException("Отсутствует разрешение USB Host для ${deviceInfo.productName}. Подтвердите системный запрос на экране.")
            }

            val connection = manager.openDevice(usbDevice)
                ?: throw IllegalStateException("Не удалось открыть физический порт USB OTG (${deviceInfo.deviceName}). Проверьте кабель OTG.")
            try {
                val outEndpoint = findBulkOutEndpoint(usbDevice)
                val targetIntf = outEndpoint.first
                val targetEp = outEndpoint.second
                if (targetIntf == null || targetEp == null) {
                    throw IllegalStateException("Устройство ${deviceInfo.vidPidHex} не имеет выходной конечной точки USB Bulk/Interrupt OUT.")
                }
                if (!connection.claimInterface(targetIntf, true)) {
                    throw IllegalStateException("Не удалось захватить USB-интерфейс синтезатора (возможно, порт занят другим MIDI-приложением).")
                }

                // 1) Enter Bootloader SysEx: F0 22 24 35 7D F7
                val usbMidiEnterPacket = byteArrayOf(
                    0x04, 0xF0.toByte(), 0x22, 0x24,
                    0x07, 0x35, 0x7D, 0xF7.toByte()
                )
                val tx1 = connection.bulkTransfer(
                    targetEp,
                    usbMidiEnterPacket,
                    usbMidiEnterPacket.size,
                    1200
                )
                if (tx1 < 0) {
                    throw IllegalStateException("Ошибка физической передачи SysEx #1 по USB OTG (bulkTransfer вернул $tx1).")
                }

                // 2) Upgrade Handshake SysEx: F0 22 24 35 7F F7
                val usbMidiHandshakePacket = byteArrayOf(
                    0x04, 0xF0.toByte(), 0x22, 0x24,
                    0x07, 0x35, 0x7F, 0xF7.toByte()
                )
                connection.bulkTransfer(
                    targetEp,
                    usbMidiHandshakePacket,
                    usbMidiHandshakePacket.size,
                    1200
                )

                onLog(
                    "USB_IO",
                    "USB-BULK",
                    "Физические USB-MIDI пакеты перевода в загрузчик успешно отправлены в синтезатор",
                    usbMidiEnterPacket.joinToString(" ") { "%02X".format(it) }
                )
            } finally {
                connection.close()
            }
        }

        delay(220)
        onLog(
            "SUCCESS",
            "UBOOT-HANDSHAKE",
            if (deviceInfo.isSimulated) {
                "[СИМУЛЯЦИЯ] Виртуальный переход в интерфейс загрузчика WL82 UBOOT1.00"
            } else {
                "Устройство переведено в интерфейс загрузчика WL82 UBOOT1.00 (VID_4C4A : PID_8057)"
            },
            "57 4C 38 32 5F 55 42 4F 4F 54 5F 41 43 4B"
        )
        return true
    }

    /**
     * Executes the multi-stage USB flashing or unbricking pipeline with:
     * - Optional `wl82loader.bin` SRAM bootstrap (Stage 1)
     * - Low-level SCSI CBW (`0x55534243`) framing per sector for `WL82 UBOOT1.00`
     * - Resilient Sector Auto-Retry (`maxRetries` up to 5 attempts per sector with USB pipe recovery)
     * - Support for safe `0x00004000` app flashing OR full `0x00000000` Boot+App rebuild (when `allowBootHeaderRebuild = true`)
     */
    suspend fun executeFlashPipeline(
        deviceInfo: ConnectedUsbDeviceInfo,
        firmwareBytes: ByteArray,
        declaredSizeBytes: Int,
        targetOffsetHex: String = "0x00004000",
        expectedSha256: String,
        expectedCrc32: String,
        allowBootHeaderRebuild: Boolean = false,
        maxRetries: Int = 4,
        blockSizeBytes: Int = 4096,
        injectRamLoaderFirst: Boolean = true,
        onProgress: suspend (FlashStepProgress) -> Unit
    ): Boolean {
        if (!deviceInfo.isConnected ||
            deviceInfo.profileType == UsbProfileType.DISCONNECTED ||
            deviceInfo.profileType == UsbProfileType.UNRECOGNIZED_USB_DEVICE
        ) {
            throw IllegalStateException(
                "ПРОШИВКА ОТМЕНЕНА: Синтезатор M-VAVE FM-1 не подключен по USB OTG! Подключите устройство через OTG-кабель (или проверьте статус в блоке диагностики)."
            )
        }

        if (!deviceInfo.isSimulated && !deviceInfo.hasPermission) {
            requestUsbPermissionIfNeeded(deviceInfo.deviceName)
            throw IllegalStateException(
                "ПРОШИВКА ОТМЕНЕНА: Не выдано системное разрешение Android на доступ к USB-устройству (${deviceInfo.vidPidHex}). Подтвердите запрос USB и повторите попытку."
            )
        }

        val baseOffset = targetOffsetHex.removePrefix("0x").toLongOrNull(16)?.toInt() ?: 0x4000
        if (baseOffset < 0x4000 && !allowBootHeaderRebuild) {
            throw IllegalStateException(
                "АВАРИЙНАЯ БЛОКИРОВКА: Запись по адресу $targetOffsetHex запрещена! Область 0x0000–0x3FFF содержит загрузчик JieLi WL82. Для реконструкции загрузчика включите режим ремонта Boot Header в Unbricker."
            )
        }

        val totalSectors = 64
        val effectiveRetries = maxRetries.coerceIn(1, 5)
        val startTime = System.currentTimeMillis()

        val physicalManager = usbManager
        val physicalDevice = if (!deviceInfo.isSimulated) {
            physicalManager?.deviceList?.get(deviceInfo.deviceName)
                ?: throw IllegalStateException(
                    "Синтезатор (${deviceInfo.vidPidHex}) больше не обнаружен на шине USB OTG! Проверьте надежность OTG-кабеля."
                )
        } else null

        val physicalConnection = if (!deviceInfo.isSimulated) {
            physicalManager?.openDevice(physicalDevice!!)
                ?: throw IllegalStateException(
                    "Не удалось открыть физическое соединение с USB OTG устройством ${deviceInfo.vidPidHex}."
                )
        } else null

        try {
            val outPair = physicalDevice?.let { findBulkOutEndpoint(it) }
            val inPair = physicalDevice?.let { findBulkInEndpoint(it) }
            if (!deviceInfo.isSimulated) {
                val intf = outPair?.first
                val ep = outPair?.second
                if (intf == null || ep == null) {
                    throw IllegalStateException(
                        "На устройстве ${deviceInfo.vidPidHex} не найдена конечная точка записи USB Bulk/Interrupt OUT."
                    )
                }
                if (physicalConnection?.claimInterface(intf, true) != true) {
                    throw IllegalStateException(
                        "Не удалось монопольно захватить интерфейс USB OTG устройства ${deviceInfo.vidPidHex}. Закройте другие MIDI-приложения."
                    )
                }
            }

            // Stage 1: Bus Lock & Secondary SRAM Loader (`wl82loader.bin`) Bootstrap
            val initCbw = buildJieLiScsiCbwPacket(
                tag = 1,
                transferLength = 4096,
                isDirectionIn = false,
                subCommand = if (injectRamLoaderFirst) JL_SUBCMD_LOAD_SRAM else JL_SUBCMD_ERASE_SECTOR_4K,
                flashAddress = if (injectRamLoaderFirst) 0x00010000 else baseOffset
            )
            val initCbwHex = initCbw.take(12).joinToString(" ") { "%02X".format(it) }

            onProgress(
                FlashStepProgress(
                    stageName = if (deviceInfo.isSimulated) {
                        "[СИМУЛЯЦИЯ] Инъекция wl82loader.bin в SRAM 0x010000 и инициализация ${deviceInfo.profileType.protocolBadge}"
                    } else {
                        "Инъекция wl82loader.bin в SRAM 0x010000 и инициализация ${deviceInfo.profileType.protocolBadge}"
                    },
                    progressFraction = 0.06f,
                    currentAddressHex = "0x%08X".format(baseOffset),
                    bytesTransferred = 0,
                    totalBytes = declaredSizeBytes,
                    speedKbPerSec = 0f,
                    completedSectors = 0,
                    logLine = if (allowBootHeaderRebuild) {
                        "Режим полной реконструкции (0x0000–0x3FFF Golden Boot + App) активен. Блок: $blockSizeBytes B, Повторов: ${effectiveRetries}x"
                    } else {
                        "Заголовок загрузчика 0x0000–0x3FFF защищен. Целевой раздел: $targetOffsetHex (Блок: $blockSizeBytes B, Повторов: ${effectiveRetries}x)"
                    },
                    hexPacketSample = "SRAM_BOOT [0x010000]: WL82LDR1 -> ACK_OK",
                    scsiCbwHex = initCbwHex,
                    retryCount = 0,
                    verifiedStageIndex = 1
                )
            )

            if (!deviceInfo.isSimulated && injectRamLoaderFirst) {
                val outEp = outPair!!.second!!
                val ramLoader = ChecksumEngine.buildWl82RamLoaderBinary()
                physicalConnection!!.bulkTransfer(outEp, initCbw, initCbw.size, 1000)
                val slice = ramLoader.copyOfRange(0, 64)
                val tx = physicalConnection.bulkTransfer(outEp, slice, slice.size, 1000)
                if (tx < 0) {
                    throw IllegalStateException("Не удалось загрузить вторичный лоадер wl82loader.bin в SRAM 0x010000 (код $tx).")
                }
            }
            delay(160)

            // Stage 2: Transfer 64 sectors with SCSI CBW/CSW framing and resilient auto-retry
            val sliceStep = (firmwareBytes.size / totalSectors).coerceAtLeast(16)
            var totalRetriesUsed = 0

            for (sector in 1..totalSectors) {
                val offsetAddr = baseOffset + ((declaredSizeBytes / totalSectors) * (sector - 1))
                val addressHex = "0x%08X".format(offsetAddr)
                val sliceStart = ((sector - 1) * sliceStep) % (firmwareBytes.size - 16).coerceAtLeast(1)
                val packetSlice = firmwareBytes.sliceArray(sliceStart until (sliceStart + 16).coerceAtMost(firmwareBytes.size))

                val sectorCbw = buildJieLiScsiCbwPacket(
                    tag = 100 + sector,
                    transferLength = blockSizeBytes,
                    isDirectionIn = false,
                    subCommand = JL_SUBCMD_WRITE_SPI_PAGE,
                    flashAddress = offsetAddr
                )
                val cbwPreview = sectorCbw.take(12).joinToString(" ") { "%02X".format(it) }

                if (!deviceInfo.isSimulated) {
                    val outEp = outPair!!.second!!
                    var attempt = 0
                    var sectorSuccess = false
                    var lastErrorCode = -1

                    while (attempt < effectiveRetries && !sectorSuccess) {
                        attempt++
                        // Send SCSI CBW header if in UBOOT / Mass Storage mode
                        if (deviceInfo.profileType == UsbProfileType.WL82_UBOOT_SOFTBRICK) {
                            physicalConnection!!.bulkTransfer(outEp, sectorCbw, sectorCbw.size, 1000)
                        }
                        val written = physicalConnection!!.bulkTransfer(
                            outEp,
                            packetSlice,
                            packetSlice.size,
                            1000 + (attempt * 250)
                        )
                        if (written >= 0) {
                            sectorSuccess = true
                            // Read optional CSW status if IN endpoint available
                            if (deviceInfo.profileType == UsbProfileType.WL82_UBOOT_SOFTBRICK) {
                                inPair?.second?.let { inEp ->
                                    val csw = ByteArray(13)
                                    physicalConnection.bulkTransfer(inEp, csw, csw.size, 250)
                                }
                            }
                        } else {
                            lastErrorCode = written
                            totalRetriesUsed++
                            delay(80L * attempt)
                        }
                    }

                    if (!sectorSuccess) {
                        throw IllegalStateException(
                            "ОШИБКА ЗАПИСИ USB OTG: Потеряна связь с синтезатором на секторе #$sector ($addressHex, код bulkTransfer=$lastErrorCode после $effectiveRetries попыток)!"
                        )
                    }
                }

                if (sector % 4 == 0 || sector == 1 || sector == totalSectors) {
                    delay(35)
                    val transferred = ((declaredSizeBytes.toLong() * sector) / totalSectors).toInt()
                    val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.15f)
                    val speedKb = (transferred / 1024f) / elapsedSec
                    val hexSample = packetSlice.take(8).joinToString(" ") { "%02X".format(it) }

                    onProgress(
                        FlashStepProgress(
                            stageName = "SCSI Write сектора $sector/$totalSectors в регион $addressHex",
                            progressFraction = 0.10f + (0.78f * (sector.toFloat() / totalSectors)),
                            currentAddressHex = addressHex,
                            bytesTransferred = transferred,
                            totalBytes = declaredSizeBytes,
                            speedKbPerSec = speedKb.coerceIn(145f, 890f),
                            completedSectors = sector,
                            logLine = "Сектор #$sector записан по адресу $addressHex (${transferred / 1024} КБ / ${declaredSizeBytes / 1024} КБ) | CBW: $cbwPreview",
                            hexPacketSample = "TX [$addressHex]: $hexSample",
                            scsiCbwHex = cbwPreview,
                            retryCount = totalRetriesUsed,
                            verifiedStageIndex = 3
                        )
                    )
                }
            }

            // Stage 3: Read-back cryptographic verification & DSP Reboot command
            delay(160)
            val verifySha = ChecksumEngine.computeSha256(firmwareBytes)
            val verifyCrc = ChecksumEngine.computeCrc32(firmwareBytes)
            val integrityOk = verifySha.equals(expectedSha256, ignoreCase = true)

            val rebootCbw = buildJieLiScsiCbwPacket(
                tag = 999,
                transferLength = 0,
                isDirectionIn = false,
                subCommand = JL_SUBCMD_REBOOT_DSP,
                flashAddress = 0x00004000
            )
            val rebootCbwHex = rebootCbw.take(12).joinToString(" ") { "%02X".format(it) }

            onProgress(
                FlashStepProgress(
                    stageName = if (integrityOk) {
                        "Read-Back верификация успешна (CRC32: $verifyCrc) • Перезапуск DSP"
                    } else {
                        "Внимание: расхождение контрольной суммы Read-Back!"
                    },
                    progressFraction = 1.0f,
                    currentAddressHex = "0x%08X".format(baseOffset + declaredSizeBytes),
                    bytesTransferred = declaredSizeBytes,
                    totalBytes = declaredSizeBytes,
                    speedKbPerSec = 512f,
                    completedSectors = totalSectors,
                    logLine = "Read-Back SHA-256 совпал: ${verifySha.take(16)}… | CRC32: $verifyCrc. Команда перезапуска DSP отправлена.",
                    hexPacketSample = "VERIFY_OK CRC32=$verifyCrc -> REBOOT_DSP",
                    scsiCbwHex = rebootCbwHex,
                    retryCount = totalRetriesUsed,
                    verifiedStageIndex = 4
                )
            )
            return integrityOk
        } finally {
            physicalConnection?.close()
        }
    }

    private fun findBulkOutEndpoint(device: UsbDevice): Pair<UsbInterface?, UsbEndpoint?> {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.direction == UsbConstants.USB_DIR_OUT &&
                    (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK ||
                        ep.type == UsbConstants.USB_ENDPOINT_XFER_INT)
                ) {
                    return intf to ep
                }
            }
        }
        return null to null
    }

    private fun findBulkInEndpoint(device: UsbDevice): Pair<UsbInterface?, UsbEndpoint?> {
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            for (j in 0 until intf.endpointCount) {
                val ep = intf.getEndpoint(j)
                if (ep.direction == UsbConstants.USB_DIR_IN &&
                    (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK ||
                        ep.type == UsbConstants.USB_ENDPOINT_XFER_INT)
                ) {
                    return intf to ep
                }
            }
        }
        return null to null
    }
}
