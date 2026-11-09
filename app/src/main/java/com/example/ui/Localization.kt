package com.example.ui

import com.example.data.FirmwareCategory
import com.example.domain.DeviceDiagnosticVerdict
import com.example.domain.UsbProfileType

/**
 * Clean dual-language localization (EN / RU) for FM1 FirmwareHub & Unbricker Studio.
 */
object AppStrings {

    fun navTitle(dest: AppDestination, lang: AppLanguage): String = when (dest) {
        AppDestination.CATALOG -> if (lang == AppLanguage.EN) "Catalog" else "Каталог"
        AppDestination.EMULATOR -> if (lang == AppLanguage.EN) "Emulator" else "Эмулятор"
        AppDestination.FLASHER -> if (lang == AppLanguage.EN) "Flasher" else "Прошивка"
        AppDestination.UNBRICKER -> "Unbricker"
        AppDestination.BACKUPS -> if (lang == AppLanguage.EN) "Backups" else "Бэкапы"
        AppDestination.LOGS_DOCS -> if (lang == AppLanguage.EN) "Logs" else "Логи"
    }

    fun appHeader(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "FM1 FirmwareHub & Unbricker" else "FM1 FirmwareHub & Unbricker"

    fun catalogCount(count: Int, lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Catalog: $count" else "Каталог: $count"

    fun simulationModeBanner(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "SAFETY SIMULATION MODE (NO HARDWARE RISK)" else "РЕЖИМ АППАРАТНОЙ СИМУЛЯЦИИ (БЕЗ РИСКА ДЛЯ ЖЕЛЕЗА)"

    fun usbConnectedReady(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "USB OTG CONNECTED & READY" else "USB OTG ПОДКЛЮЧЕНО И ГОТОВО"

    fun scanUsb(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Scan USB" else "Скан USB"

    fun searchHint(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Search by name, author, chip, tags (SLOOP, Felucca, SID)…"
        else "Поиск по названию, автору, чипу, тегам (SLOOP, Felucca, SID)…"

    fun syncHub(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Sync with TheFirmwareHub" else "Синхронизировать с TheFirmwareHub"

    fun importLocalFile(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Import file (.bin/.fw/.fwsc/.syx)" else "Импорт файла (.bin/.fw/.fwsc/.syx)"

    fun downloadAll(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Download All" else "Скачать все"

    fun startFlashButton(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "START AUTO-FLASH (0x00004000)" else "ЗАПУСТИТЬ АВТО-ПРОШИВКУ (0x00004000)"

    fun bootloaderGuard(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "BOOTLOADER GUARD (0x0000–0x3FFF LOCKED)" else "ЗАЩИТА ЗАГРУЗЧИКА (0x0000–0x3FFF ЗАБЛОКИРОВАН)"

    fun imageIntegrityVerified(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "IMAGE INTEGRITY CONFIRMED" else "ЦЕЛОСТНОСТЬ ОБРАЗА ПОДТВЕРЖДЕНА"

    fun hashReportOk(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "SHA-256 / CRC32 / MD5 OK" else "SHA-256 / CRC32 / MD5 OK"

    fun backupEeprom(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Auto-backup EEPROM & Presets before flash" else "Авто-бэкап EEPROM и пресетов перед записью"

    fun copyLogs(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Copy Log" else "Копировать лог"

    fun fullLogs(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Full Logs" else "Все логи"

    fun selectAndFlash(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Select & Flash" else "Выбрать и прошить"

    fun details(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Details" else "Подробнее"

    fun favoritesOnly(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Favorites only" else "Только избранное"

    fun resetFilters(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Reset filters" else "Сбросить фильтры"

    fun categoryName(category: FirmwareCategory, lang: AppLanguage): String = when (category) {
        FirmwareCategory.ALL -> if (lang == AppLanguage.EN) "All" else "Все"
        FirmwareCategory.FM_SYNTH -> if (lang == AppLanguage.EN) "FM Synth" else "FM Синтезаторы"
        FirmwareCategory.GROOVEBOX -> if (lang == AppLanguage.EN) "Groovebox" else "Грувбоксы"
        FirmwareCategory.VIRTUAL_ANALOG -> if (lang == AppLanguage.EN) "Virtual Analog" else "Аналог"
        FirmwareCategory.CHIPTUNE -> if (lang == AppLanguage.EN) "Chiptune / Retro" else "Чиптюн"
        FirmwareCategory.AMBIENT -> if (lang == AppLanguage.EN) "Ambient / Generative" else "Эмбиент"
        FirmwareCategory.OFFICIAL_STOCK -> if (lang == AppLanguage.EN) "Official Stock" else "Заводские"
    }

    fun sortName(sort: SortOption, lang: AppLanguage): String = when (sort) {
        SortOption.RECOMMENDED -> if (lang == AppLanguage.EN) "Recommended & Stock" else "Рекомендуемые & Сток"
        SortOption.NEWEST -> if (lang == AppLanguage.EN) "Newest First" else "Сначала новые"
        SortOption.NAME_ASC -> if (lang == AppLanguage.EN) "By Name (A–Z)" else "По имени (A–Z)"
        SortOption.SIZE_ASC -> if (lang == AppLanguage.EN) "By Size" else "По размеру"
    }

    fun unbrickerHeader(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "M-VAVE FM-1 Rescue & Unbricker Studio" else "Центр восстановления M-VAVE FM-1"

    fun softBrickHeader(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Soft-Brick Recovery (WL82 UBOOT1.00)" else "Восстановление из Soft-Brick (WL82 UBOOT1.00)"

    fun softBrickDesc(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Synthesizer screen is black. Recovers clean factory Stock V16 at 0x00004000 via USB OTG."
        else "Экран синтезатора черный. Восстанавливает чистую заводскую прошивку Stock V16 по адресу 0x00004000 по USB OTG."

    fun restoreStockButton(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "1-Click Stock V16 Recovery" else "1-Клик Восстановление Stock V16"

    fun hardBrickHeader(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Hard-Brick Recovery (MaskROM / RP2040)" else "Восстановление из Hard-Brick (MaskROM / RP2040)"

    fun createBackupNow(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "Create EEPROM & Preset Dump Now" else "Создать дамп настроек и EEPROM сейчас"

    fun languageToggleLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.EN) "RU" else "EN"
}
