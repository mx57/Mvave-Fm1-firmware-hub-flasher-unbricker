# FM1 FirmwareHub & M-VAVE FM-1 Unbricker — Full 160-Firmware Release Package

## 📦 Included APK Files / Собранные APK-файлы

- `FM1-FirmwareHub-Unbricker-v1.0-debug.apk` — Актуальная сборка APK с полным каталогом из **160 прошивок** (Debug-signed для быстрой установки на Android 7.0+ с поддержкой USB OTG и режима аппаратной симуляции).
- `FM1-FirmwareHub-Unbricker-v2.0-160fw-debug.apk` — Дубликат релиза v2.0 (160 прошивок TheFirmwareHub.com).
- `SHA256SUMS.txt` — Криптографические хэш-суммы SHA-256 и CRC32 собранных APK-файлов.

## ✨ Что входит в релиз (v2.0 — 160 Firmwares Edition)

1. **Полный каталог 160 прошивок (`thefirmwarehub.com/catalog.json` + офлайн-архив)**:
   - Все **148 прошивок M-VAVE FM-1** из официального `catalog.json` (`zp12`, `SLOOP`, `sloopDX`, `Felucca`, `SALT`, `Jangada`, `GHOULBOX`, `Melodee`, `X0X`, `FoMni`, `ChoralRoot`, `WaveLoop`, `OberVave`, `Hortator`, `Hortator Bluetooth`, `JIANT`, `DUB-1`, `Skiff`, `AMB-1`, `winampFM`, `FM-1 SID`, `FM1-TRAX`, `FOMAJAM`, `Frodhes`, `ORBIT`, `Clipper`, `Bubba`, `Floyd` и др.).
   - **9 эксклюзивных прошивок M-VAVE SMK-37 / FM-1** (`Felucca SMK`, `Makkuro37`, `Speak37`, `Erik37`, `Snoop37`, `X0X-37`, `FoMni-37`, `MAKU-37`, `Rainbow37`).
   - **3 специальные и официальные сборки**: `M-VAVE FM-1 Official Stock V15/V16 (Rescue)`, `M-VAVE FM1 Open V14 (Official SDK)` и `Baud Girl (FM-1+VA)`.
2. **Полноэкранная страница деталей прошивки и раскрываемое превью**:
   - Для каждой из 160 прошивок доступны **Release Notes**, **совместимые ревизии железа (`PCB Rev A/B/C`, `JieLi AC791N / WL82`, `UBOOT1.00 / V15 / V16`)** и **интерактивная история версий** с переключением и скачиванием любой ревизии.
3. **Скачивание `.bin` / `.fwsc`, экспорт и открытие в 1 клик**:
   - Прямая загрузка пакетов `.fwsc` с `thefirmwarehub.com` с распаковкой логического образа (`logicalImage`) и офлайн-генерацией проверенных образов `.bin` в память (`filesDir/firmwares/`).
   - Пакетное скачивание всех 160 прошивок кнопкой **«Скачать все (160)»**, экспорт `.bin` через системный диалог Android и мгновенная передача в **Прошивальщик** или **Инспектор хэшей**.
4. **Безопасная прошивка по смещению `0x00004000` и Unbricker (`WL82 UBOOT1.00`)**:
   - Аппаратная защита загрузчика `0x0000–0x3FFF`, проверка `SHA-256 / CRC32 / MD5` до и после записи, вывод из Soft-Brick (`VID_4C4A : PID_8057`) и пошаговый мастер Hard-Brick (`RP2040 / Pi Pico Transporter`).
