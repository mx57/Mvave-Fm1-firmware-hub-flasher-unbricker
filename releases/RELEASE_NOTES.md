# FM1 FirmwareHub & M-VAVE FM-1 Unbricker — v1.0 Release Package

## 📦 Included APK Files / Собранные APK-файлы

- `FM1-FirmwareHub-Unbricker-v1.0-debug.apk` — Ready-to-install Android package (Debug-signed for immediate installation on Android 7.0+ devices with USB OTG support & Hardware Simulation Mode).
- `SHA256SUMS.txt` — Cryptographic SHA-256 & CRC32 checksums of the compiled APK binary.

## ✨ What's Included in v1.0 / Что входит в релиз v1.0

1. **Offline-Ready Firmware Artwork & Catalog (`TheFirmwareHub.com`)**:
   - Every firmware card includes bundled offline vector artwork and procedural OLED synth waveform previews (`SLOOP Groovebox`, `sloopDX 6-Op FM`, `Felucca`, `OberVave`, `FM-1 SID`, `FM1-TRAX`, `Mutable Dreams`, `Baud Girl`, `AcidVave 303`, `Official Stock V16 & V15`).
2. **Safe 0x00004000 USB Flasher & Bootloader Guard**:
   - Native Android USB Host OTG (`MIDI SysEx F0 22 24 35 7D F7` + `JieLi WL82 UBOOT1.00`) + interactive **Safety Simulation Mode**.
   - Hardware bootloader lock for `0x0000–0x3FFF` and 64-sector visual flash map.
3. **M-VAVE FM-1 Unbricker (Soft-Brick & Hard-Brick)**:
   - 1-Click Soft-Brick recovery (`VID_4C4A : PID_8057`) and 4-step Hard-Brick `RP2040 / Pi Pico Transporter` hardware bridge wizard.
4. **Triple Checksum Verifier & EEPROM Backup Manager**:
   - Real-time `SHA-256`, `CRC32`, and `MD5` verification before and after flashing, plus 1-click EEPROM preset dump & restore.
