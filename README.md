<div align="center">

# 🎛️ FM1 FirmwareHub & M-VAVE FM-1 Unbricker Studio

**Cross-Platform Firmware Catalog (`TheFirmwareHub.com`), USB OTG Flasher, Cryptographic Checksum Verifier, EEPROM Preset Backup Manager & JieLi WL82 Unbricker for the M-VAVE FM-1 Synthesizer**

[![Platform](https://img.shields.io/badge/Platform-Android%20USB%20OTG%20%7C%20Windows%20%7C%20Linux%20%7C%20macOS-FF6B2B?style=for-the-badge)](#)
[![Chipset](https://img.shields.io/badge/Target%20DSP-JieLi%20AC791N%20%2F%20WL82-00E5A3?style=for-the-badge)](#)
[![Bootloader Guard](https://img.shields.io/badge/Boot%20Guard-0x00004000%20Protected-38BDF8?style=for-the-badge)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose%20M3-7F52FF?style=for-the-badge)](#)

<img src="docs/assets/hero_banner.svg" alt="FM1 FirmwareHub & M-VAVE FM-1 Unbricker Hero Banner" width="100%" />

<br />

**[🇬🇧 English Documentation](#-english-documentation)** • **[🇷🇺 Документация на русском языке](#-документация-на-русском-языке)**

</div>

---

# 🇬🇧 English Documentation

## 📖 Overview

**FM1 FirmwareHub & M-VAVE FM-1 Unbricker Studio** is a specialized engineering utility and firmware manager built for the **M-VAVE FM-1 (M-Wave FM1)** pocket synthesizer powered by the **JieLi AC791N / WL82** processor architecture.

The community around **[TheFirmwareHub.com](https://thefirmwarehub.com/)** has created dozens of transformative custom firmwares (*SLOOP Groovebox*, *sloopDX*, *Felucca*, *OberVave*, *FM-1 SID*, *FM1-TRAX*, *Mutable Dreams*, *Baud Girl*). However, interrupted USB transfers or accidentally flashing raw `.fwsc` packages to address `0x0000` can leave the synthesizer in a **Soft-Brick (`WL82 UBOOT1.00`)** or **Hard-Brick** state.

This project solves that problem by combining:
1. **Rich Firmware Catalog & Search Engine**: Online synchronization with `thefirmwarehub.com`, bundled offline catalog, category/tag filtering, and local file import (`.bin`, `.fw`, `.fwsc`, `.syx`).
2. **Hardware Bootloader Guard (`0x0000–0x3FFF`)**: Enforces writing firmware strictly to offset **`0x00004000`**, protecting the 16 KB JieLi WL82 boot header.
3. **Triple Cryptographic Integrity Verification**: Computes and verifies **SHA-256**, **CRC32**, and **MD5** before writing and performs a post-flash **Read-Back Verification**.
4. **Automatic EEPROM & Preset Backup**: Captures full snapshots of your 64 user presets and boot header (`0x0000–0xFFFF`) prior to flashing, with 1-click restoration and cloud/file export.
5. **M-VAVE FM-1 Unbricker (Soft-Brick & Hard-Brick)**:
   - **Soft-Brick 1-Click Recovery**: Communicates directly with `WL82 UBOOT1.00` (`VID_4C4A : PID_8057`) over USB OTG to restore clean **Official Stock V16** at `0x00004000`.
   - **Hard-Brick RP2040 Transporter Wizard**: Interactive 4-step guide for wiring a **Seeed Studio XIAO RP2040** or **Raspberry Pi Pico** (`wl82loader`) to USB `D+ / D-` lines to force MaskROM recovery.
6. **Cross-Platform Desktop Script Generator**: Generates ready-to-run, checksum-verified scripts for **Windows (PowerShell)**, **Linux / macOS (Bash + `jl-uboot-tool`)**, and **Python 3**.

---

## 🖼️ Interface & Architecture Preview

<div align="center">
  <img src="docs/assets/ui_preview.svg" alt="UI Preview - Catalog, 64-Sector Flasher, and Unbricker" width="100%" />
</div>

<br />

<div align="center">
  <img src="docs/assets/memory_map_diagram.svg" alt="JieLi WL82 Memory Map & Unbrick Pathways" width="100%" />
</div>

---

## 🎹 Bundled & Supported Firmwares (`TheFirmwareHub.com`)

| Firmware Name | Category | Version | Target Offset | Highlights |
| :--- | :--- | :--- | :--- | :--- |
| **M-VAVE FM-1 Official Stock V16** | `OFFICIAL_STOCK` | `v1.16` | `0x00004000` | Clean factory recovery image for Soft-Brick (`WL82 UBOOT1.00`) |
| **SLOOP Groovebox Workstation** | `GROOVEBOX` | `v2.4.1` | `0x00004000` | 4-track polyphonic step sequencer, parameter locks, song mode |
| **sloopDX 6-Operator FM** | `FM_SYNTH` | `v1.9.0` | `0x00004000` | Classic 6-op DX7 engine with USB-MIDI `.SYX` patch bank import |
| **Felucca FM & Arpeggiator** | `FM_SYNTH` | `v3.1.2` | `0x00004000` | TX81Z multi-waveform 4-op FM with Euclidean arpeggiator |
| **OberVave Virtual Analog** | `VIRTUAL_ANALOG` | `v1.5.4` | `0x00004000` | 80s virtual analog polysynth with unison detune & BBD delay |
| **FM-1 SID (C64 6581/8580)** | `CHIPTUNE` | `v2.0.3` | `0x00004000` | Cycle-accurate Commodore 64 SID emulation with wavetable arp |
| **FM1-TRAX MegaDrive YM2612** | `CHIPTUNE` | `v1.4.0` | `0x00004000` | Sega Genesis OPN2 synth with DAC ladder effect & PCM drums |
| **Mutable Dreams Generative** | `AMBIENT` | `v1.8.2` | `0x00004000` | Modal resonator, granular cloud & shimmer reverb engine |
| **Baud Girl Glitch & Modem DSP** | `FM_SYNTH` | `v1.2.5` | `0x00004000` | Phase distortion, LPC formant matrix & glitch percussion |
| **AcidVave 303 & Sub-Bass** | `GROOVEBOX` | `v1.3.0` | `0x00004000` | Acid bassline synth with resonant diode ladder filter & slides |

---

## 🛠️ Unbricking Guide (Soft-Brick & Hard-Brick)

### 1. Soft-Brick Recovery (`WL82 UBOOT1.00` — Black Screen)
If a firmware update fails and the M-VAVE FM-1 shows a black OLED screen:
1. Connect the synthesizer via USB-C cable. Check if it enumerates as **`WL82 UBOOT1.00`** (`VID: 0x4C4A`, `PID: 0x8057`).
2. Open the **Unbricker** tab in the app (or export the desktop script from the **Logs & Guides** tab).
3. Click **"1-Click Stock V16 Recovery (0x4000)"**.
4. The app will automatically back up the existing flash memory, lock `0x0000–0x3FFF`, write the clean Stock V16 image to **`0x00004000`**, and verify its `SHA-256` and `CRC32`.

> ⚠️ **CRITICAL WARNING**: Never flash raw `.fwsc` update packages to address `0x0000`. Address range `0x0000–0x3FFF` holds the JieLi boot header. Overwriting it causes a Hard-Brick.

### 2. Hard-Brick Recovery (`RP2040 / Pi Pico Transporter Bridge`)
If `0x0000` was overwritten and the device shows zero USB activity:
1. Flash `wl82loader.uf2` onto a **Seeed Studio XIAO RP2040** or **Raspberry Pi Pico**.
2. Wire the RP2040 bridge to the powered-off M-VAVE FM-1 USB lines:
   - `GPIO 0 (RP2040)` ──► `USB D+` (Green)
   - `GPIO 1 (RP2040)` ──► `USB D-` (White)
   - `GND (RP2040)` ──► `GND` (Black / Shield)
3. Power on the FM-1 while the RP2040 bridge is active to force the JieLi chip into MaskROM download mode, then run the recovery flash.

---

## 💻 Installation & Cross-Platform Usage

* **Pre-built Android APK (`/releases` folder)**: Ready-to-install APK is available directly in the repository under [`/releases/FM1-FirmwareHub-Unbricker-v1.0-debug.apk`](releases/FM1-FirmwareHub-Unbricker-v1.0-debug.apk) (along with [`/releases/SHA256SUMS.txt`](releases/SHA256SUMS.txt)).
* **Android (USB OTG)**: Install the APK, connect the M-VAVE FM-1 using a USB-C OTG adapter, and accept the USB permission dialog. Use **Safety Simulation Mode** anytime to test workflows without hardware attached.
* **Windows 10 / 11**: Standard USB-MIDI works out of the box. For `WL82 UBOOT1.00` (`VID_4C4A&PID_8057`), use **Zadig** to bind the `WinUSB` driver, then run the generated **PowerShell** script from the **Logs & Guides** tab.
* **Linux**: Add udev rule `SUBSYSTEM=="usb", ATTR{idVendor}=="4c4a", MODE="0666"`, reload `udevadm`, and run the exported Bash script.
* **macOS**: Install `libusb` (`brew install libusb python3`) and run the exported Bash / Python CLI script.

---
---

# 🇷🇺 Документация на русском языке

## 📖 Описание проекта

**FM1 FirmwareHub & M-VAVE FM-1 Unbricker Studio** — это специализированный инженерный инструмент, каталог прошивок и система аварийного восстановления для карманного синтезатора **M-VAVE FM-1 (M-Wave FM1)** на базе процессора **JieLi AC791N / WL82**.

Сообщество **[TheFirmwareHub.com](https://thefirmwarehub.com/)** создало десятки открытых альтернативных прошивок (*SLOOP Groovebox*, *sloopDX*, *Felucca*, *OberVave*, *FM-1 SID*, *FM1-TRAX*, *Mutable Dreams*, *Baud Girl*), полностью меняющих звуковой движок синтезатора. Однако обрыв связи по USB или ошибочная запись сырого `.fwsc` файла по нулевому смещению `0x0000` может привести к состоянию **Soft-Brick (`WL82 UBOOT1.00`)** или **Hard-Brick**.

### Ключевые возможности приложения:
1. **Каталог TheFirmwareHub с фильтрацией и поиском**:
   - Онлайн-синхронизация с репозиторием `thefirmwarehub.com` + встроенный офлайн-каталог проверенных сборок.
   - Поиск по названию, автору, тегам, архитектуре чипа и хэшам (`SHA-256` / `CRC32`), фильтрация по категориям синтеза и импорт локальных файлов (`.bin`, `.fw`, `.fwsc`, `.syx`).
2. **Аппаратная защита загрузчика (`0x0000–0x3FFF LOCKED`)**:
   - Автоматически блокирует перезапись загрузочного сектора `0x0000–0x3FFF` и записывает прошивки строго по безопасному смещению **`0x00004000`**.
3. **Тройная проверка контрольных сумм (`SHA-256`, `CRC32`, `MD5`)**:
   - Вычисляет и сверяет криптографические хэши до начала записи, а также выполняет **Read-Back верификацию** записанных секторов после завершения.
4. **Резервное копирование EEPROM и пресетов**:
   - Автоматическое и ручное сохранение полного дампа памяти (заголовок загрузчика + 64 пользовательских патча) с возможностью восстановления в 1 клик и выгрузки файла в облако или на диск.
5. **Центр восстановления M-VAVE FM-1 (`Unbricker`)**:
   - **Режим Soft-Brick (`WL82 UBOOT1.00`, `VID_4C4A : PID_8057`)**: Восстановление синтезатора с «черным экраном» по стандартному USB-кабелю без пайки в 1 клик.
   - **Режим Hard-Brick (`RP2040 Transporter Bridge`)**: Интерактивный 4-шаговый мастер подключения платы *Seeed Studio XIAO RP2040* или *Raspberry Pi Pico* (`wl82loader`) к линиям `USB D+ / D-` для принудительного входа в MaskROM.
6. **Логирование и кроссплатформенность (Android, Windows, Linux, macOS)**:
   - Живая консоль USB-телеметрии с фильтрацией пакетов (`USB_IO`, `CHECKSUM`, `SUCCESS`, `WARN`, `ERROR`) и экспортом `.log` отчетов.
   - Встроенный генератор готовых скриптов авто-прошивки и восстановления для **Windows (PowerShell)**, **Linux / macOS (Bash + `jl-uboot-tool`)** и **Python 3**.

---

## 🚀 Руководство по установке и использованию

### Шаг 1. Подключение устройства
* **На Android (через USB OTG)**: Подключите синтезатор M-VAVE FM-1 кабелем USB-C через OTG-адаптер. Подтвердите системный запрос доступа к USB-устройству. Если вы хотите протестировать интерфейс без синтезатора, оставьте включенным **Режим аппаратной симуляции (Safety Simulation Mode)**.
* **На Windows 10 / 11**: В обычном режиме синтезатор определяется как стандартное USB-MIDI устройство. Если синтезатор находится в режиме восстановления `WL82 UBOOT1.00` (`VID_4C4A : PID_8057`), откройте утилиту **Zadig** и установите для него драйвер `WinUSB`.
* **На Linux**: Создайте правило `/etc/udev/rules.d/99-mvave-fm1.rules`:
  ```bash
  SUBSYSTEM=="usb", ATTR{idVendor}=="4c4a", MODE="0666", GROUP="plugdev"
  ```
* **На macOS**: Установите `libusb` и `python3` через Homebrew:
  ```bash
  brew install libusb python3
  ```

### Шаг 2. Выбор прошивки и проверка целостности
1. Откройте вкладку **«Каталог»**, выберите нужную прошивку (например, *SLOOP v2.4* или *Official Stock V16*) и нажмите **«Выбрать и прошить»**.
2. Во вкладке **«Прошивка»** убедитесь, что индикатор целостности горит зеленым (`ЦЕЛОСТНОСТЬ ОБРАЗА ПОДТВЕРЖДЕНА: SHA-256 / CRC32 / MD5 OK`), а защита загрузчика активна (`0x0000–0x3FFF LOCKED`).

### Шаг 3. Резервное копирование и прошивка
1. Убедитесь, что переключатель **«Авто-бэкап EEPROM и пресетов перед записью»** активен (или нажмите **«Сделать дамп текущих настроек и EEPROM сейчас»**).
2. Нажмите **«ЗАПУСТИТЬ АВТО-ПРОШИВКУ (0x00004000)»**.
3. Приложение автоматически отправит команду входа в загрузчик (`SysEx F0 22 24 35 7D F7`), запишет 64 сектора памяти, проверит контрольную сумму и перезапустит синтезатор.

### Шаг 4. Сборка проекта из исходного кода
```bash
# Сборка отладочного APK
gradle :app:assembleDebug

# Запуск модульных тестов верификатора контрольных сумм и защиты смещения 0x4000
gradle :app:testDebugUnitTest
```

---

## 📄 License & Credits

* Community firmware ecosystem powered by **[TheFirmwareHub.com](https://thefirmwarehub.com/)** and open-source contributors (*aroum*, *kagaimiq*, *Quixotic7*, *Felucca DSP*, *SynthForge*).
* Hardware recovery protocols adapted for the **JieLi AC791N / WL82** processor architecture.
