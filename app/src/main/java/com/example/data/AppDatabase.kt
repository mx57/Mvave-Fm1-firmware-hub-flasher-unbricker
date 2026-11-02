package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.ChecksumEngine

@Database(
    entities = [FirmwareEntity::class, BackupDumpEntity::class, FlashLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fm1_firmwarehub_studio.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun buildDefaultCatalog(): List<FirmwareEntity> {
            data class RawSpec(
                val id: String,
                val name: String,
                val version: String,
                val author: String,
                val category: FirmwareCategory,
                val summary: String,
                val changelog: String,
                val sizeBytes: Int,
                val tags: String,
                val isStock: Boolean,
                val releaseDate: String,
                val isFav: Boolean = false
            )

            val specs = listOf(
                RawSpec(
                    id = "mvave_fm1_stock_v16_unbrick",
                    name = "M-VAVE FM-1 Official Stock V16",
                    version = "v1.16 (Factory Rescue)",
                    author = "M-VAVE Official / Unbrick Team",
                    category = FirmwareCategory.OFFICIAL_STOCK,
                    summary = "Официальная заводская прошивка V16 с чистой разметкой региона 0x4000. Предназначена для восстановления из состояния WL82 UBOOT1.00 (Soft-Brick) и возврата к заводскому состоянию.",
                    changelog = "• Восстановление заводской таблицы векторов прерываний по адресу 0x4000\n• Стабильный USB-MIDI стек и корректная обработка SysEx F0 22 24 35 7D F7\n• Эталонный образ для вывода из Soft-Brick",
                    sizeBytes = 524288,
                    tags = "Stock,Unbrick,WL82,Factory,V16,Recovery,MIDI",
                    isStock = true,
                    releaseDate = "2026-10-15",
                    isFav = true
                ),
                RawSpec(
                    id = "sloop",
                    name = "SLOOP Groovebox",
                    version = "v2.8.4",
                    author = "isod89",
                    category = FirmwareCategory.GROOVEBOX,
                    summary = "Полноценный лайв-грувбокс на базе M-VAVE FM-1: 4 дорожки (3 синта + драм-машина), слои, степ-секвенсор, режим Song Mode, punch-in эффекты в реальном времени и поддержка сэмплов.",
                    changelog = "• Реальный пиксельный интерфейс SLOOP с 4 дорожками и индикацией шагов\n• Punch-in эффекты и лупер в реальном времени\n• Более 7100 установок через TheFirmwareHub.com",
                    sizeBytes = 610052,
                    tags = "SLOOP,Groovebox,Sequencer,Live,Samples,SongMode",
                    isStock = false,
                    releaseDate = "2026-10-30",
                    isFav = true
                ),
                RawSpec(
                    id = "felucca",
                    name = "Felucca Workstation",
                    version = "v1.9.0",
                    author = "Leo Kuroshita (Hügelton Instruments)",
                    category = FirmwareCategory.FM_SYNTH,
                    summary = "Фундаментальная открытая прошивка для FM-1: расширенные движки синтеза (6-Op FM/DX7, Virtual Analog, Phase Distortion, Granular), секвенсор, эффекты и графический интерфейс.",
                    changelog = "• Обновленная панель управления и браузерный редактор патчей\n• Двухъядерный аудио-движок с поддержкой MIDI Learn\n• Совместимость с банками DX7",
                    sizeBytes = 610286,
                    tags = "Felucca,Hugelton,6-Op,FM,DX7,Granular,Sequencer",
                    isStock = false,
                    releaseDate = "2026-11-01",
                    isFav = true
                ),
                RawSpec(
                    id = "zp12",
                    name = "zp12 (SP-1200 12-Bit Sampler)",
                    version = "v2.5",
                    author = "zvenson",
                    category = FirmwareCategory.GROOVEBOX,
                    summary = "12-битный сэмплер и драм-машина в духе легендарного E-mu SP-1200: сэмплирование прямо на FM-1 по USB или через микрофон/линейный вход, фирменное пробивное Lo-Fi звучание, лупы и паттерны.",
                    changelog = "• Аутентичный интерфейс и тракт 12-bit / 26.04 kHz E-mu SP-1200\n• Прямой захват сэмплов по USB Audio\n• Секвенсор с классическим свингом и режимом Song",
                    sizeBytes = 612200,
                    tags = "zp12,SP1200,Sampler,12-bit,LoFi,HipHop,Drums",
                    isStock = false,
                    releaseDate = "2026-10-24",
                    isFav = true
                ),
                RawSpec(
                    id = "obervave",
                    name = "OberVave Polysynth",
                    version = "v3.3.2",
                    author = "zvenson",
                    category = FirmwareCategory.VIRTUAL_ANALOG,
                    summary = "8-голосный виртуально-аналоговый полифонический синтезатор в духе классических американских полисинтов 1979–1984 гг. 4 мультитембральные дорожки, микшер с хорусом, дилеем, плейт-ревером и DJ-фильтром.",
                    changelog = "• 4 независимые дорожки со своим звуком, арпеджиатором и секвенцией\n• Мастер-секция с компрессором и DJ-фильтром\n• Фирменный интерфейс Oberheim-стиля со светлыми полосами",
                    sizeBytes = 612223,
                    tags = "OberVave,Analog,Polysynth,8-Voice,Oberheim,Chorus",
                    isStock = false,
                    releaseDate = "2026-11-02",
                    isFav = true
                ),
                RawSpec(
                    id = "x0x",
                    name = "X0X Acid & 808/909 Groovebox",
                    version = "v1.2.0",
                    author = "Charles Vestal",
                    category = FirmwareCategory.GROOVEBOX,
                    summary = "Кислотный грувбокс: драм-машины TR-909 и TR-808, две линии баса TB-303 с генератором паттернов TB-3PO, генератор брейкбитов и полноценный режим Song Mode.",
                    changelog = "• Двойной движок 303 с акцентами, слайдами и дисторшном\n• Классический интерфейс степ-редактора X0X\n• Генератор сбивок и брейкбитов на лету",
                    sizeBytes = 609684,
                    tags = "X0X,303,808,909,Acid,Breakbeat,Groovebox",
                    isStock = false,
                    releaseDate = "2026-11-01"
                ),
                RawSpec(
                    id = "jiant",
                    name = "JIANT FM Bio-Synth",
                    version = "V10.46",
                    author = "juanjiant-bit",
                    category = FirmwareCategory.FM_SYNTH,
                    summary = "Био-синтетический грувбокс: 6 дорожек с уникальным тепловизионным интерфейсом, 8 звуковых движков (FM6 с совместимостью Dexed, аналог, фазовый, формантный) и мутирующие макросы.",
                    changelog = "• Уникальная тепловая визуализация живых организмов на экране\n• 8 движков синтеза + Song Mode из SLOOP\n• Генеративные мутации тембров",
                    sizeBytes = 671320,
                    tags = "JIANT,FM6,Dexed,ThermalUI,BioSynth,Groovebox",
                    isStock = false,
                    releaseDate = "2026-11-02"
                ),
                RawSpec(
                    id = "melodee",
                    name = "Melodee (CZ-1 & Dexed)",
                    version = "v1.6.0",
                    author = "keremimo",
                    category = FirmwareCategory.FM_SYNTH,
                    summary = "Инструмент для живого исполнения: запись без привязки к сетке (unquantized), микротональные лады, эмуляция фазового синтеза Casio CZ-1 и побитово точный движок Dexed.",
                    changelog = "• Эмуляция Casio CZ-1 Phase Distortion + Yamaha DX7 Dexed\n• Специальные ладовые режимы и микротональность\n• Наглядный многооконный графический интерфейс",
                    sizeBytes = 668692,
                    tags = "Melodee,CZ1,Dexed,Microtonal,PhaseDistortion,FM",
                    isStock = false,
                    releaseDate = "2026-11-02"
                ),
                RawSpec(
                    id = "skiff",
                    name = "Skiff 6-Track Workstation",
                    version = "v2.1.1",
                    author = "illuminus5",
                    category = FirmwareCategory.GROOVEBOX,
                    summary = "6 дорожек синтезаторов и ударных с воркфлоу модульного грувбокса: 14 движков, 64-шаговый секвенсор с аккордами, рэтчетами, условиями срабатывания (trig conditions) и 6 цветовыми темами.",
                    changelog = "• Режим PERFORM со сценами и полной рулежкой FM6 с лицевой панели\n• 64-шаговый секвенсор с parameter locks\n• 6 встроенных визуальных тем оформления",
                    sizeBytes = 610429,
                    tags = "Skiff,6-Track,14-Engines,TrigConditions,Perform,FM6",
                    isStock = false,
                    releaseDate = "2026-10-31"
                ),
                RawSpec(
                    id = "fm1sid",
                    name = "FM-1 SID (C64 reSID & GT2)",
                    version = "v0.3",
                    author = "Joakim Eriksson",
                    category = FirmwareCategory.CHIPTUNE,
                    summary = "Музыкальная станция Commodore 64 на FM-1: эмулятор процессора 6510 и чипа reSID, встроенный трекерный редактор GoatTracker 2 с 10 демо-песнями и прямой запуск любых .SID файлов.",
                    changelog = "• Аутентичный синий экран Commodore 64 / GoatTracker 2\n• Полноценный синтезатор SID с секвенсором и эффектами\n• Воспроизведение PSID и RSID композиций",
                    sizeBytes = 610110,
                    tags = "SID,C64,reSID,GoatTracker,Chiptune,6581,8580",
                    isStock = false,
                    releaseDate = "2026-10-25"
                ),
                RawSpec(
                    id = "fm1trax",
                    name = "FM1-TRAX MegaDrive Tracker",
                    version = "v0.9.3",
                    author = "jartificer",
                    category = FirmwareCategory.CHIPTUNE,
                    summary = "Трекер Sega Mega Drive / Genesis: эмуляция чипов YM2612 и SN76489 (6 каналов FM, 3 канала меандра и шум), эффекты в стиле DefleMask, 128 пресетов FM, ударные на ЦАП и экспорт VGM.",
                    changelog = "• Трекерный интерфейс с шестнадцатеричными колонками команд\n• 128 банков инструментов FM + PCM ударные\n• Экспорт готовых треков в формат .VGM",
                    sizeBytes = 609661,
                    tags = "FM1-TRAX,YM2612,SN76489,Genesis,Tracker,DefleMask",
                    isStock = false,
                    releaseDate = "2026-11-01"
                ),
                RawSpec(
                    id = "winampfm",
                    name = "winampFM Synth 2.x",
                    version = "v0.12",
                    author = "Hericdk",
                    category = FirmwareCategory.GROOVEBOX,
                    summary = "Легендарный интерфейс плеера Winamp 2.x прямо на экране M-VAVE FM-1 (построен на базе SLOOP): ваши звуки в виде плейлиста, 4 дорожки лупов, эквалайзер-эффекты и линейный визуализатор.",
                    changelog = "• Классический скин Winamp 2.x со спектроанализатором\n• Запись 4 луп-дорожек и управление эффектами через ползунки\n• Полная совместимость с проектами SLOOP",
                    sizeBytes = 610019,
                    tags = "Winamp,winampFM,SLOOP,RetroUI,Visualizer,Loops",
                    isStock = false,
                    releaseDate = "2026-10-14"
                ),
                RawSpec(
                    id = "ghoulbox",
                    name = "GHOULBOX Dungeon Synth",
                    version = "v2.4",
                    author = "Jason Persinger",
                    category = FirmwareCategory.AMBIENT,
                    summary = "Атмосферный Dungeon Synth с интерфейсом ретро-RPG подземелья с горящими факелами: кафедральный ревербератор, пленочный сатуратор, колесная лира (hurdy-gurdy), хоры, органы и гусли.",
                    changelog = "• Анимированный ретро-RPG экран с каменными сводами и факелами\n• Движки Hurdy-Gurdy, средневековых хоров, арфы и органа\n• Эффекты старой кассеты и биткрашера",
                    sizeBytes = 610086,
                    tags = "GHOULBOX,DungeonSynth,RPG,Cathedral,Tape,Ambient",
                    isStock = false,
                    releaseDate = "2026-10-24"
                ),
                RawSpec(
                    id = "amb",
                    name = "AMB-1 Generative Ambient",
                    version = "v1.3",
                    author = "Charles Vestal",
                    category = FirmwareCategory.AMBIENT,
                    summary = "Генеративная эмбиент-машина в духе «Music for Airports» Брайана Ино: дышащие голоса, колокольчики на магнитофонных петлях разной длины и плавающий саб-бас.",
                    changelog = "• Асинхронные ленточные лупы разной длины (Tape Loops)\n• Автономная генерация медитативных звуковых полотен\n• Минималистичный графический монитор петель",
                    sizeBytes = 610108,
                    tags = "AMB-1,Ambient,Generative,TapeLoops,Eno,Drone",
                    isStock = false,
                    releaseDate = "2026-10-17"
                ),
                RawSpec(
                    id = "dub",
                    name = "DUB-1 Generative Dub Techno",
                    version = "v1.4.3",
                    author = "sbegueria",
                    category = FirmwareCategory.AMBIENT,
                    summary = "Автономная даб-техно станция: минорные аккордовые стабы через ленточный дилей (Tape Echo), FM-пинги, глубокий саб-бас, сайдчейн-бочка, виниловый кракл и 8-тактовая аранжировка.",
                    changelog = "• Фирменный зеленый осциллографический интерфейс DUB-1\n• Автоматические брейки и дабовые сбивки каждые 8 тактов\n• Глубокий Tape Delay с обратной связью",
                    sizeBytes = 609666,
                    tags = "DUB-1,DubTechno,TapeEcho,ChordStabs,Generative",
                    isStock = false,
                    releaseDate = "2026-10-30"
                ),
                RawSpec(
                    id = "choralroot",
                    name = "ChoralRoot Chord Instrument",
                    version = "v0.29",
                    author = "Quixotic7",
                    category = FirmwareCategory.VIRTUAL_ANALOG,
                    summary = "Аккордовый инструмент в стиле Telepathic Orchid: одна рука берет основные тоны, вторая формирует сложные джазовые обращения аккордов (voicings), басовую линию и лупер.",
                    changelog = "• Индикация аккордовых обращений и нотного стана на экране\n• Перформанс-режимы арпеджио и страмминга\n• Встроенный многослойный лупер",
                    sizeBytes = 610313,
                    tags = "ChoralRoot,Orchid,Chords,Voicings,Looper,Synth",
                    isStock = false,
                    releaseDate = "2026-11-01"
                ),
                RawSpec(
                    id = "fomni",
                    name = "FoMni Chord Harp",
                    version = "v0.4",
                    author = "Charles Vestal",
                    category = FirmwareCategory.VIRTUAL_ANALOG,
                    summary = "Электронная аккордовая арфа, вдохновленная классическим Suzuki Omnichord: выбирайте мажорные, минорные и септаккорды на черных клавишах и делайте глиссандо (strum) по белым.",
                    changelog = "• Интерфейс выбора аккордовых кнопок в стиле Omnichord\n• Чувствительный струнный страмминг по клавиатуре FM-1\n• Встроенные ритм-паттерны автоаккомпанемента",
                    sizeBytes = 609661,
                    tags = "FoMni,Omnichord,Harp,Strum,Chords,Vintage",
                    isStock = false,
                    releaseDate = "2026-10-15"
                ),
                RawSpec(
                    id = "mvave_fm1_stock_v15_legacy",
                    name = "M-VAVE FM-1 Official Factory V15",
                    version = "v1.15 (Official Stock)",
                    author = "M-VAVE Official",
                    category = FirmwareCategory.OFFICIAL_STOCK,
                    summary = "Официальная заводская прошивка V15 с сайта TheFirmwareHub / M-VAVE для полного возврата синтезатора к штатному заводскому интерфейсу и стандартным 64 пресетам.",
                    changelog = "• Штатный заводской экран M-VAVE V15\n• Оригинальный заводской банк из 64 FM-патчей\n• Полная совместимость с утилитой M-UPGRADE",
                    sizeBytes = 524288,
                    tags = "Stock,V15,Factory,Official,M-VAVE",
                    isStock = true,
                    releaseDate = "2026-05-20"
                )
            )

            return specs.map { spec ->
                val rawBytes = ChecksumEngine.buildDeterministicFirmwareBinary(
                    firmwareId = spec.id,
                    version = spec.version,
                    targetOffsetHex = "0x00004000",
                    declaredSizeBytes = spec.sizeBytes
                )
                val report = ChecksumEngine.analyzeBinary(
                    bytes = rawBytes,
                    fileName = "${spec.id}.bin",
                    requestedOffsetHex = "0x00004000"
                )
                FirmwareEntity(
                    id = spec.id,
                    name = spec.name,
                    version = spec.version,
                    author = spec.author,
                    category = spec.category.name,
                    summary = spec.summary,
                    changelog = spec.changelog,
                    targetDevice = "M-VAVE FM-1",
                    chipArchitecture = "JieLi AC791N / WL82",
                    flashOffsetHex = "0x00004000",
                    sizeBytes = spec.sizeBytes,
                    sha256 = report.sha256,
                    crc32 = report.crc32,
                    md5 = report.md5,
                    downloadUrl = "https://thefirmwarehub.com/img/${spec.id}.png",
                    tagsCsv = spec.tags,
                    isOfficialStock = spec.isStock,
                    isVerified = true,
                    isOfflineBundled = true,
                    releaseDate = spec.releaseDate,
                    isFavorite = spec.isFav
                )
            }
        }
    }
}
