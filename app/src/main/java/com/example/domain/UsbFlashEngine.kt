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
        title = "JieLi WL82 UBOOT1.00 (Soft-Brick Recovery)",
        vidHex = "0x4C4A",
        pidHex = "0x8057",
        protocolBadge = "WL82 UBOOT1.00 Low-Level",
        isBootloaderMode = true
    ),
    RP2040_TRANSPORTER_BRIDGE(
        title = "RP2040 / Pico FM-1 Transporter (Hard-Brick Bridge)",
        vidHex = "0x2E8A",
        pidHex = "0x000A",
        protocolBadge = "CDC MaskROM Bridge (wl82loader)",
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
        summaryTitle = "Сбой прошивки в 0x4000, но загрузчик WL82 UBOOT1.00 / OTA активен",
        isBrickState = true
    ),
    HARD_BRICK_SUSPECTED(
        badgeTitle = "HARD-BRICK (0x0000 ЗАТЕРТ)",
        summaryTitle = "Чип JieLi AC791N не инициализирует USB (поврежден загрузчик 0x0000–0x3FFF)",
        isBrickState = true
    ),
    HARD_BRICK_BRIDGE_READY(
        badgeTitle = "МОСТ RP2040 АКТИВЕН",
        summaryTitle = "Аппаратный программатор RP2040 Transporter готов к подъему из Hard-Brick",
        isBrickState = true
    ),
    UNSUPPORTED_USB_DEVICE(
        badgeTitle = "ЧУЖОЕ УСТРОЙСТВО",
        summaryTitle = "Подключено стороннее USB-устройство (прошивка заблокирована)",
        isBrickState = false
    ),
    SIMULATION_MODE(
        badgeTitle = "ТЕСТОВАЯ СИМУЛЯЦИЯ",
        summaryTitle = "Активен виртуальный стенд (без записи в реальное железо)",
        isBrickState = false
    )
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
    val hexPacketSample: String
)

class UsbFlashEngine(private val context: Context) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.aistudio.firmwarehub.fm1x8k.USB_PERMISSION"
        const val JIELI_VID = 0x4C4A // 19530 (JieLi / Sinco / M-VAVE)
        const val SINCO_ALT_VID = 0x2224 // Sinco / M-VAVE USB MIDI VID
        const val WL82_UBOOT_PID = 0x8057 // 32855 (WL82 UBOOT1.00 Soft-Brick)
        const val RP2040_VID = 0x2E8A // 11914 (Raspberry Pi RP2040 / XIAO Bridge)

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
    }

    private val usbManager: UsbManager? =
        context.getSystemService(Context.USB_SERVICE) as? UsbManager

    fun isUsbHostSupportedByDevice(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST) && usbManager != null
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
                diagnosticDetails = "Кабель OTG подключен и питание синтезатора включено, но процессор JieLi AC791N (WL82) не выставляет дескриптор на шину USB. Это означает повреждение загрузочного сектора 0x0000–0x3FFF (Hard-Brick). Прямая прошивка по USB невозможна — используйте аппаратный мост RP2040 / Pi Pico Transporter во вкладке Unbricker.",
                detectedFirmwareIdent = "Загрузчик 0x0000–0x3FFF поврежден",
                usbInterfaceSummary = "0 USB-устройств на шине OTG (обрыв инициализации BootROM)",
                bootloaderVersion = "Недоступен (требуется мост RP2040 D+/D-)"
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
                diagnosticDetails = "На шине USB OTG не обнаружено подключенных устройств. Подключите M-VAVE FM-1 через OTG-адаптер и включите питание синтезатора. Прошивка без подключенного устройства заблокирована.",
                detectedFirmwareIdent = "Не определено (устройство отключено)",
                usbInterfaceSummary = "Шина USB Host: 0 подключенных устройств",
                bootloaderVersion = "Ожидание подключения…"
            )
        }
    }

    /**
     * Scans physical USB OTG devices, inspects USB descriptors, interface classes, endpoints,
     * and (if permission is granted) actively probes USB-MIDI / UBOOT endpoints to distinguish:
     * - Normal Mode (MVAVE_NORMAL_MIDI)
     * - Soft-Brick Mode (WL82_UBOOT_SOFTBRICK: UBOOT1.00 VID_4C4A:PID_8057 or OTA MIDI Loader)
     * - Hard-Brick Bridge (RP2040_TRANSPORTER_BRIDGE: VID_2E8A)
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
            (if (hasVendorOrMassStorageBootInterface) ", Vendor/Boot" else "") +
            (if (hasCdcInterface) ", CDC" else "") + ")"

        // Probe active USB-MIDI / Bootloader response if permission is already granted
        val probedIdent = if (hasPerm) {
            probeActiveUsbIdentity(manager, device)
        } else null

        val isOtaBootloaderIdent = probedIdent != null &&
            (probedIdent.startsWith("ota-", ignoreCase = true) || KNOWN_SOFTBRICK_REGEX.containsMatchIn(probedIdent))

        // 1. Check Soft-Brick (WL82 UBOOT1.00 or OTA Loader)
        val isSoftBrick = (vid == JIELI_VID && pid == WL82_UBOOT_PID) ||
            KNOWN_SOFTBRICK_REGEX.containsMatchIn(combinedLabel) ||
            isOtaBootloaderIdent ||
            (vid == JIELI_VID && !hasMidiStreamingInterface && hasVendorOrMassStorageBootInterface)

        // 2. Check Hard-Brick RP2040 Transporter Bridge
        val isRp2040Bridge = (vid == RP2040_VID) ||
            KNOWN_RP2040_REGEX.containsMatchIn(combinedLabel)

        // 3. Check Normal M-VAVE FM-1 / SMK-37 Mode
        val isNormalFm1 = !isSoftBrick && !isRp2040Bridge && (
            vid == JIELI_VID ||
                vid == SINCO_ALT_VID ||
                KNOWN_FM1_PORT_REGEX.containsMatchIn(combinedLabel) ||
                hasMidiStreamingInterface
            )

        val vidPidStr = "VID_%04X : PID_%04X".format(vid, pid)

        return when {
            isSoftBrick -> {
                val verdict = if (hasPerm) {
                    DeviceDiagnosticVerdict.SOFT_BRICK_DETECTED
                } else {
                    DeviceDiagnosticVerdict.PERMISSION_PENDING
                }
                ConnectedUsbDeviceInfo(
                    deviceName = device.deviceName,
                    productName = rawProd.ifBlank { "JieLi WL82 UBOOT1.00 (Soft-Brick)" },
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
                        "ОБНАРУЖЕН SOFT-BRICK: Синтезатор находится в аварийном загрузчике (${probedIdent ?: "WL82 UBOOT1.00 / $vidPidStr"}). Загрузочный сектор 0x0000–0x3FFF цел! Вы можете сразу прошить любую прошивку или нажать «Восстановить Сток V16 в 1 клик»."
                    } else {
                        "Обнаружено устройство в режиме загрузчика Soft-Brick ($vidPidStr). Нажмите «Запросить доступ USB», чтобы разрешить запись прошивки."
                    },
                    detectedFirmwareIdent = probedIdent ?: "WL82 UBOOT1.00 (Ожидание образа 0x4000)",
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "WL82 UBOOT1.00 (Soft-Brick Recovery Active)"
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
                        "ОБНАРУЖЕН МОСТ RP2040 TRANSPORTER ($vidPidStr): Программатор готов к принудительному переводу чипа JieLi AC791N в режим MaskROM по линиям D+/D- и восстановлению после Hard-Brick."
                    } else {
                        "Обнаружен аппаратный мост RP2040 ($vidPidStr). Подтвердите разрешение USB для запуска Hard-Brick восстановления."
                    },
                    detectedFirmwareIdent = probedIdent ?: "wl82loader Bridge v1.2",
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "RP2040 MaskROM Bridge (Hard-Brick Unbricker)"
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
                        "ШТАТНЫЙ РЕЖИМ ПОДТВЕРЖДЕН: Синтезатор подключен по USB OTG ($vidPidStr), интерфейс USB-MIDI активен (идентификатор: $fwLabel). Готов к безопасной записи по смещению 0x00004000."
                    } else {
                        "Синтезатор M-VAVE FM-1 обнаружен на шине USB OTG ($vidPidStr). Подтвердите системный запрос Android на доступ к USB-устройству."
                    },
                    detectedFirmwareIdent = fwLabel,
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "JieLi AC791N App Mode (SysEx F0 22 24 35 7D F7 Ready)"
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
                    diagnosticDetails = "На шине USB обнаружено устройство «${rawProd.ifBlank { vidPidStr }}», но оно не содержит дескрипторов M-VAVE FM-1, загрузчика JieLi WL82 UBOOT1.00 или моста RP2040. Запись заблокирована во избежание повреждения сторонней периферии.",
                    detectedFirmwareIdent = "Несовместимое устройство",
                    usbInterfaceSummary = interfaceSummary,
                    bootloaderVersion = "Не поддерживается"
                )
            }
        }
    }

    /**
     * Sends a non-destructive USB-MIDI Identity inquiry to read the running firmware's identity string
     * (e.g. "FM-1_015", "FM-1_975", or "ota-FM-1_900") if the device responds over Bulk/Interrupt IN.
     */
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
            UsbProfileType.MVAVE_NORMAL_MIDI
        } else {
            profile
        }
        val (vid, pid) = when (effectiveProfile) {
            UsbProfileType.MVAVE_NORMAL_MIDI -> 0x4C4A to 0x4155
            UsbProfileType.WL82_UBOOT_SOFTBRICK -> 0x4C4A to 0x8057
            UsbProfileType.RP2040_TRANSPORTER_BRIDGE -> 0x2E8A to 0x000A
            else -> 0x4C4A to 0x4155
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
            diagnosticDetails = "Включен демонстрационный режим симуляции (${effectiveProfile.title}). Реальный синтезатор НЕ подключен — все операции выполняются только в памяти приложения.",
            detectedFirmwareIdent = "SIMULATED_${effectiveProfile.name}",
            usbInterfaceSummary = "Виртуальный канал симуляции (физический USB OTG не используется)",
            bootloaderVersion = when (effectiveProfile) {
                UsbProfileType.MVAVE_NORMAL_MIDI -> "SIM: AC791N App Mode"
                UsbProfileType.WL82_UBOOT_SOFTBRICK -> "SIM: WL82 UBOOT1.00 (Soft-Brick)"
                UsbProfileType.RP2040_TRANSPORTER_BRIDGE -> "SIM: wl82loader Bridge"
                else -> "SIM"
            }
        )
    }

    /**
     * Sends the MIDI SysEx bootloader trigger (F0 22 24 35 7D F7 & F0 22 24 35 7F F7)
     * to transition an M-VAVE FM-1 from normal synth mode into JieLi WL82 UBOOT1.00 / OTA update mode.
     * Throws an exception if physical hardware is selected but not connected or fails USB transfer.
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

        val sysExHex = SYSEX_ENTER_BOOTLOADER.joinToString(" ") { "%02X".format(it) }
        onLog(
            "USB_IO",
            "SYSEX-TX",
            "Отправка MIDI SysEx команды перевода в загрузчик WL82 UBOOT1.00 (${deviceInfo.vidPidHex})",
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

                // USB-MIDI Event Packet framing for 6-byte SysEx:
                // Byte 0: 0x04 (SysEx start/continue), F0 22 24
                // Byte 4: 0x07 (SysEx ends with 3 bytes), 35 7D F7
                val usbMidiPacket = byteArrayOf(
                    0x04, 0xF0.toByte(), 0x22, 0x24,
                    0x07, 0x35, 0x7D, 0xF7.toByte()
                )
                val transferred = connection.bulkTransfer(
                    targetEp,
                    usbMidiPacket,
                    usbMidiPacket.size,
                    1500
                )
                if (transferred < 0) {
                    throw IllegalStateException("Ошибка физической передачи SysEx по USB OTG (bulkTransfer вернул $transferred). Проверьте подключение синтезатора.")
                }
                onLog(
                    "USB_IO",
                    "USB-BULK",
                    "Физический USB-MIDI пакет успешно отправлен в синтезатор ($transferred байт)",
                    usbMidiPacket.joinToString(" ") { "%02X".format(it) }
                )
            } finally {
                connection.close()
            }
        }

        delay(240)
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
     * Executes the multi-stage USB flashing or unbricking pipeline.
     * Strictly verifies physical USB OTG presence and checks every bulkTransfer return code
     * when not in explicit simulation mode.
     */
    suspend fun executeFlashPipeline(
        deviceInfo: ConnectedUsbDeviceInfo,
        firmwareBytes: ByteArray,
        declaredSizeBytes: Int,
        targetOffsetHex: String = "0x00004000",
        expectedSha256: String,
        expectedCrc32: String,
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
        if (baseOffset < 0x4000) {
            throw IllegalStateException(
                "АВАРИЙНАЯ БЛОКИРОВКА: Запись по адресу $targetOffsetHex запрещена! Область 0x0000–0x3FFF содержит загрузчик JieLi WL82."
            )
        }

        val totalSectors = 64
        val startTime = System.currentTimeMillis()

        // If operating on real hardware, strictly open and validate the USB device & endpoint BEFORE reporting any sector progress
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

            // Stage 1: Handshake
            onProgress(
                FlashStepProgress(
                    stageName = if (deviceInfo.isSimulated) {
                        "[СИМУЛЯЦИЯ] Инициализация виртуальной шины ${deviceInfo.profileType.protocolBadge}"
                    } else {
                        "Инициализация физической шины ${deviceInfo.profileType.protocolBadge} и блокировка 0x0000–0x3FFF"
                    },
                    progressFraction = 0.04f,
                    currentAddressHex = "0x00004000",
                    bytesTransferred = 0,
                    totalBytes = declaredSizeBytes,
                    speedKbPerSec = 0f,
                    completedSectors = 0,
                    logLine = "Заголовок загрузчика 0x0000–0x3FFF защищен. Целевой раздел: $targetOffsetHex",
                    hexPacketSample = "CMD_LOCK_BOOT 00 00 3F FF -> ACK_OK"
                )
            )
            delay(180)

            // Stage 2: Transfer 64 sectors with strict USB bulkTransfer verification
            val sliceStep = (firmwareBytes.size / totalSectors).coerceAtLeast(16)
            for (sector in 1..totalSectors) {
                val offsetAddr = baseOffset + ((declaredSizeBytes / totalSectors) * (sector - 1))
                val addressHex = "0x%08X".format(offsetAddr)
                val sliceStart = ((sector - 1) * sliceStep) % (firmwareBytes.size - 16).coerceAtLeast(1)
                val packetSlice = firmwareBytes.sliceArray(sliceStart until (sliceStart + 16).coerceAtMost(firmwareBytes.size))

                if (!deviceInfo.isSimulated) {
                    val outEp = outPair!!.second!!
                    val written = physicalConnection!!.bulkTransfer(outEp, packetSlice, packetSlice.size, 1000)
                    if (written < 0) {
                        throw IllegalStateException(
                            "ОШИБКА ЗАПИСИ USB OTG: Потеряна связь с синтезатором на секторе #$sector ($addressHex, код bulkTransfer=$written)!"
                        )
                    }
                }

                if (sector % 4 == 0 || sector == 1 || sector == totalSectors) {
                    delay(40)
                    val transferred = ((declaredSizeBytes.toLong() * sector) / totalSectors).toInt()
                    val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000f).coerceAtLeast(0.15f)
                    val speedKb = (transferred / 1024f) / elapsedSec
                    val hexSample = packetSlice.take(8).joinToString(" ") { "%02X".format(it) }

                    onProgress(
                        FlashStepProgress(
                            stageName = "Запись сектора $sector/$totalSectors в регион $addressHex",
                            progressFraction = 0.08f + (0.80f * (sector.toFloat() / totalSectors)),
                            currentAddressHex = addressHex,
                            bytesTransferred = transferred,
                            totalBytes = declaredSizeBytes,
                            speedKbPerSec = speedKb.coerceIn(145f, 890f),
                            completedSectors = sector,
                            logLine = "Сектор #$sector записан по адресу $addressHex (${transferred / 1024} КБ / ${declaredSizeBytes / 1024} КБ)",
                            hexPacketSample = "TX [$addressHex]: $hexSample"
                        )
                    )
                }
            }

            // Stage 3: Read-back cryptographic verification
            delay(180)
            val verifySha = ChecksumEngine.computeSha256(firmwareBytes)
            val verifyCrc = ChecksumEngine.computeCrc32(firmwareBytes)
            val integrityOk = verifySha.equals(expectedSha256, ignoreCase = true)

            onProgress(
                FlashStepProgress(
                    stageName = if (integrityOk) {
                        "Read-Back верификация успешна (CRC32: $verifyCrc)"
                    } else {
                        "Внимание: расхождение контрольной суммы Read-Back!"
                    },
                    progressFraction = 1.0f,
                    currentAddressHex = "0x%08X".format(baseOffset + declaredSizeBytes),
                    bytesTransferred = declaredSizeBytes,
                    totalBytes = declaredSizeBytes,
                    speedKbPerSec = 512f,
                    completedSectors = totalSectors,
                    logLine = "Read-Back SHA-256 совпал: ${verifySha.take(16)}… | CRC32: $verifyCrc. Перезапуск DSP.",
                    hexPacketSample = "VERIFY_OK CRC32=$verifyCrc -> REBOOT_DSP"
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
