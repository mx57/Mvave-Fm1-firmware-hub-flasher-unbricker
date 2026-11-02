// File: app/src/main/java/com/example/domain/Fm1DspEmulatorEngine.kt
package com.example.domain

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.FirmwareCategory
import com.example.data.FirmwareEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Synthesis engine architecture adapted per firmware from the 160-firmware catalog.
 */
enum class EmulatorSynthEngineType(
    val code: String,
    val title: String,
    val chipDescription: String
) {
    FM_4OP(
        code = "4-OP FM",
        title = "4-Operator FM Synthesis (OPN / DX / WL82 FPU)",
        chipDescription = "4 фазовых оператора с матрицей алгоритмов, обратной связью и огибающими"
    ),
    CHIP_SID_PWM(
        code = "SID / 8-BIT",
        title = "C64 SID 6581 / PWM / Bitcrushed Chiptune",
        chipDescription = "ШИМ-осциллятор, кольцевая модуляция, 8-битный квантователь и быстрый арпеджиатор"
    ),
    VA_ACID_RESONANT(
        code = "VA / 303 ACID",
        title = "Virtual Analog Saw/Square + 24dB Resonant Filter",
        chipDescription = "Виртуально-аналоговый генератор с суб-осциллятором, резонансным фильтром и акцентом"
    ),
    GROOVE_DRUM_SYNTH(
        code = "GROOVE / SEQ",
        title = "Hybrid FM Groovebox, Bass & Drum Synthesizer",
        chipDescription = "Гибридный ударно-басовый движок с питч-огибающей бочки, FM-перкуссией и секвенсором"
    ),
    AMBIENT_DRONE_PAD(
        code = "AMBIENT / PAD",
        title = "Generative Harmonic Swarm & Stereo Tape Delay",
        chipDescription = "Многоголосный спектральный кластер с плавной модуляцией и глубоким эхо-пространством"
    )
}

/**
 * Parameter page displayed on the simulated M-VAVE FM-1 128x64 OLED screen and mapped to Knobs K1..K4.
 */
enum class EmulatorOledPage(
    val index: Int,
    val shortCode: String,
    val title: String,
    val knobLabels: List<String>
) {
    OSC_FM(
        index = 0,
        shortCode = "P1:OSC/FM",
        title = "Операторы & Тембр",
        knobLabels = listOf("TIMBRE / FM", "RATIO / DETUNE", "FEEDBACK / PWM", "HARM / SUB")
    ),
    FILTER_ENV(
        index = 1,
        shortCode = "P2:FLT/ENV",
        title = "Фильтр & Огибающая",
        knobLabels = listOf("CUTOFF", "RESONANCE", "ATTACK", "DECAY / REL")
    ),
    LFO_FX(
        index = 2,
        shortCode = "P3:MOD/FX",
        title = "Модуляция & Эффекты",
        knobLabels = listOf("LFO RATE", "LFO DEPTH", "SPACE / DELAY", "CRUSH / DRIVE")
    ),
    SEQ_ARP(
        index = 3,
        shortCode = "P4:SEQ/ARP",
        title = "Секвенсор & Гейт",
        knobLabels = listOf("TEMPO BPM", "GATE LENGTH", "SWING %", "OCT / GLIDE")
    )
}

enum class EmulatorOledDisplayMode(val title: String) {
    AUTHENTIC_SKIN_HUD("Экран прошивки + HUD"),
    LIVE_OSCILLOSCOPE("Живой Осциллограф & Спектр"),
    OPERATOR_MATRIX("Матрица регистров AC791N")
}

enum class EmulatorArpMode(val label: String) {
    OFF("ВЫКЛ"),
    UP("ВВЕРХ ▲"),
    DOWN("ВНИЗ ▼"),
    UP_DOWN("ВВЕРХ-ВНИЗ ◆"),
    CHORD("АККОРД ❖")
}

data class EmulatorPreset(
    val index: Int,
    val name: String,
    val categoryBadge: String,
    val engineType: EmulatorSynthEngineType,
    // 16 normalized parameters [0.0f .. 1.0f], 4 per page (P1..P4)
    val parameters: FloatArray,
    val defaultPatternIndex: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmulatorPreset) return false
        return index == other.index &&
            name == other.name &&
            engineType == other.engineType &&
            parameters.contentEquals(other.parameters)
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + name.hashCode()
        result = 31 * result + engineType.hashCode()
        result = 31 * result + parameters.contentHashCode()
        return result
    }
}

data class SequencerStep(
    val stepIndex: Int,
    val active: Boolean = true,
    val semitoneOffset: Int = 0, // relative to root octave C
    val accent: Boolean = false,
    val gateMultiplier: Float = 1.0f
)

data class EmulatorPattern(
    val index: Int,
    val name: String,
    val bpm: Int,
    val steps: List<SequencerStep>
)

data class EmulatorFirmwareProfile(
    val firmwareId: String,
    val firmwareName: String,
    val firmwareVersion: String,
    val author: String,
    val primaryEngine: EmulatorSynthEngineType,
    val bootBannerText: String,
    val presets: List<EmulatorPreset>,
    val patterns: List<EmulatorPattern>
)

/**
 * Deterministic profiler that builds authentic sound banks, synthesis engine configurations,
 * and 16-step demo patterns for any of the 160 firmwares in TheFirmwareHub catalog.
 */
object Fm1FirmwareSoundProfiler {

    fun buildProfile(firmware: FirmwareEntity): EmulatorFirmwareProfile {
        val id = firmware.id.lowercase().trim()
        val seed = computeSeed(id + firmware.crc32)

        val primaryEngine = resolveEngineForFirmware(id, firmware.category, firmware.tagsCsv)
        val presets = buildPresetsForFirmware(id, firmware.name, primaryEngine, seed)
        val patterns = buildPatternsForFirmware(id, primaryEngine, seed)

        val bootBanner = "AC791N DSP • ${firmware.name.uppercase().take(16)} ${firmware.version} • ${primaryEngine.code}"
        return EmulatorFirmwareProfile(
            firmwareId = firmware.id,
            firmwareName = firmware.name,
            firmwareVersion = firmware.version,
            author = firmware.author,
            primaryEngine = primaryEngine,
            bootBannerText = bootBanner,
            presets = presets,
            patterns = patterns
        )
    }

    private fun computeSeed(input: String): Int {
        var h = 0x811C9DC5.toInt()
        for (ch in input) {
            h = h xor ch.code
            h *= 0x01000193
        }
        return abs(h)
    }

    private fun resolveEngineForFirmware(
        id: String,
        category: String,
        tagsCsv: String
    ): EmulatorSynthEngineType {
        val tags = tagsCsv.lowercase()
        return when {
            id.contains("x0x") || id.contains("acid") || id.contains("obervave") ||
                id.contains("min1") || id.contains("mono2k") || id.contains("j6") ||
                id.contains("s1vave") || id.contains("cs1") || id.contains("modelar") ->
                EmulatorSynthEngineType.VA_ACID_RESONANT

            id.contains("sid") || id.contains("chip") || id.contains("8bit") ||
                id.contains("lsdj") || id.contains("386dx") || id.contains("game") ||
                id.contains("nes") || id.contains("fmdj") || id.contains("bytemania") ->
                EmulatorSynthEngineType.CHIP_SID_PWM

            id.contains("sloop") || id.contains("trax") || id.contains("ghoulbox") ||
                id.contains("drum") || id.contains("beat") || id.contains("jam") ||
                id.contains("bunker") || id.contains("tribe") || id.contains("mc1") ||
                id.contains("perf1") || id.contains("dungeon") ->
                EmulatorSynthEngineType.GROOVE_DRUM_SYNTH

            id.contains("amb") || id.contains("choral") || id.contains("zen") ||
                id.contains("drift") || id.contains("vesper") || id.contains("penumbra") ||
                id.contains("mycelium") || id.contains("cosmos") || id.contains("orbit") ||
                id.contains("clouds") || id.contains("mutable") ->
                EmulatorSynthEngineType.AMBIENT_DRONE_PAD

            category == FirmwareCategory.CHIPTUNE.name -> EmulatorSynthEngineType.CHIP_SID_PWM
            category == FirmwareCategory.VIRTUAL_ANALOG.name -> EmulatorSynthEngineType.VA_ACID_RESONANT
            category == FirmwareCategory.GROOVEBOX.name -> EmulatorSynthEngineType.GROOVE_DRUM_SYNTH
            category == FirmwareCategory.AMBIENT.name -> EmulatorSynthEngineType.AMBIENT_DRONE_PAD
            tags.contains("acid") || tags.contains("analog") -> EmulatorSynthEngineType.VA_ACID_RESONANT
            tags.contains("sid") || tags.contains("chiptune") -> EmulatorSynthEngineType.CHIP_SID_PWM
            tags.contains("ambient") || tags.contains("drone") -> EmulatorSynthEngineType.AMBIENT_DRONE_PAD
            tags.contains("groove") || tags.contains("sequencer") -> EmulatorSynthEngineType.GROOVE_DRUM_SYNTH
            else -> EmulatorSynthEngineType.FM_4OP
        }
    }

    private fun buildPresetsForFirmware(
        id: String,
        fwName: String,
        primaryEngine: EmulatorSynthEngineType,
        seed: Int
    ): List<EmulatorPreset> {
        val customNames: List<String> = when {
            id.contains("x0x") -> listOf(
                "303 Acid Squelch", "DevilFish Overdrive", "Sub-Octave Resin", "Warehouse Saw",
                "Liquid Filter Sweep", "Hard Accent Bass", "Rave Stabs 1993", "Phuture Pulse"
            )
            id.contains("sid") -> listOf(
                "C64 Hubbard Lead", "6581 RingMod Bell", "Galway PWM Sweep", "8-Bit Arp Blitz",
                "Bitcrush Cyber Bass", "SID Noise Snare", "Tracker Chip Pad", "Demoscene Sync"
            )
            id.contains("sloop") -> listOf(
                "Tape Loop Pluck", "Lo-Fi Rhodes Warble", "Dusty Vinyl Keys", "Sub Groove Chop",
                "Sloop DX Bell", "Warm Cassette Pad", "Dub Echo Chord", "Bit-Reduced Organ"
            )
            id.contains("obervave") || id.contains("j6") -> listOf(
                "OB-Xa Brass Poly", "Juno Chorus Strings", "Warm Analog Fifths", "Resonant Sweep Pad",
                "Retro Synthwave Bass", "Blade Runner Lead", "Detuned SuperSaw", "Subtle Analog Pluck"
            )
            id.contains("choral") || id.contains("amb") -> listOf(
                "Cathedral Formant", "Shimmering Ether", "Deep Space Drone", "Bio-Mycelium Pad",
                "Frozen Glass Harmonics", "Sub-Abyssal Choir", "Slow Nebula LFO", "Solar Wind Texture"
            )
            id.contains("ghoulbox") || id.contains("trax") || id.contains("tribe") -> listOf(
                "FM Kick & Sub Bass", "Industrial Metal Hit", "Electro Zap Perc", "Techno Rumble Seq",
                "Glitch Hat Matrix", "Acid Groove Kit", "Dark EBM Pluck", "Distorted Snare Synth"
            )
            id.contains("stock") || id.contains("fm1open") -> listOf(
                "DX7 E.Piano 1", "FM Solid Lately Bass", "Crystal Tubular Bell", "Cyber Brass Ensemble",
                "Metallic Clavinet", "Warm Pad Algorithm 4", "Harmonic Pluck Lead", "WL82 Reference Tone"
            )
            else -> {
                val prefix = fwName.split(" ").firstOrNull()?.take(10) ?: "FM1"
                listOf(
                    "$prefix Prime Lead",
                    "$prefix Deep Bass",
                    "$prefix Crystal Pluck",
                    "$prefix Resonant Sweep",
                    "$prefix Mod Matrix",
                    "$prefix Sub Groove",
                    "$prefix Space Choir",
                    "$prefix Overdrive"
                )
            }
        }

        val secondaryEngines = listOf(
            primaryEngine,
            primaryEngine,
            when (primaryEngine) {
                EmulatorSynthEngineType.FM_4OP -> EmulatorSynthEngineType.VA_ACID_RESONANT
                EmulatorSynthEngineType.VA_ACID_RESONANT -> EmulatorSynthEngineType.FM_4OP
                EmulatorSynthEngineType.CHIP_SID_PWM -> EmulatorSynthEngineType.FM_4OP
                EmulatorSynthEngineType.GROOVE_DRUM_SYNTH -> EmulatorSynthEngineType.VA_ACID_RESONANT
                EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> EmulatorSynthEngineType.FM_4OP
            },
            primaryEngine,
            EmulatorSynthEngineType.FM_4OP,
            primaryEngine,
            EmulatorSynthEngineType.AMBIENT_DRONE_PAD,
            primaryEngine
        )

        return customNames.mapIndexed { idx, presetName ->
            val pSeed = seed + idx * 7919
            fun randNorm(offset: Int, minVal: Float, maxVal: Float): Float {
                val raw = ((pSeed ushr (offset * 3)) xor (pSeed * (offset + 5))) and 0xFFFF
                val norm = (raw.toFloat() / 65535f).coerceIn(0f, 1f)
                return minVal + norm * (maxVal - minVal)
            }

            val eng = secondaryEngines[idx % secondaryEngines.size]
            val params = FloatArray(16)
            when (eng) {
                EmulatorSynthEngineType.FM_4OP -> {
                    params[0] = randNorm(1, 0.28f, 0.82f) // FM Mod Index
                    params[1] = randNorm(2, 0.15f, 0.85f) // Harmonic Ratio
                    params[2] = randNorm(3, 0.10f, 0.65f) // Feedback
                    params[3] = randNorm(4, 0.25f, 0.75f) // Op3/Op4 Mix
                    params[4] = randNorm(5, 0.55f, 0.95f) // Cutoff
                    params[5] = randNorm(6, 0.15f, 0.55f) // Resonance
                    params[6] = if (idx == 5 || idx == 6) 0.35f else 0.03f // Attack
                    params[7] = randNorm(8, 0.35f, 0.80f) // Decay/Release
                }
                EmulatorSynthEngineType.VA_ACID_RESONANT -> {
                    params[0] = randNorm(1, 0.40f, 0.90f) // Wave Morph Saw/Square
                    params[1] = randNorm(2, 0.20f, 0.60f) // Detune Spread
                    params[2] = randNorm(3, 0.35f, 0.85f) // Env Mod Depth
                    params[3] = randNorm(4, 0.40f, 0.85f) // Sub Oscillator
                    params[4] = randNorm(5, 0.25f, 0.75f) // Filter Cutoff
                    params[5] = randNorm(6, 0.55f, 0.92f) // High Acid Resonance
                    params[6] = 0.01f // Snappy Attack
                    params[7] = randNorm(8, 0.25f, 0.65f) // Decay
                }
                EmulatorSynthEngineType.CHIP_SID_PWM -> {
                    params[0] = randNorm(1, 0.30f, 0.85f) // Pulse Width
                    params[1] = randNorm(2, 0.20f, 0.80f) // RingMod / Coarse Tune
                    params[2] = randNorm(3, 0.35f, 0.90f) // PWM Sweep Depth
                    params[3] = randNorm(4, 0.15f, 0.65f) // Noise / Sub Mix
                    params[4] = randNorm(5, 0.60f, 0.98f) // Cutoff
                    params[5] = randNorm(6, 0.20f, 0.60f) // Resonance
                    params[6] = 0.01f // Instant 8-bit Attack
                    params[7] = randNorm(8, 0.20f, 0.60f) // Decay
                }
                EmulatorSynthEngineType.GROOVE_DRUM_SYNTH -> {
                    params[0] = randNorm(1, 0.45f, 0.90f) // Punch / Pitch Env
                    params[1] = randNorm(2, 0.25f, 0.75f) // FM Metal Ratio
                    params[2] = randNorm(3, 0.20f, 0.75f) // Drive / Transient
                    params[3] = randNorm(4, 0.50f, 0.90f) // Sub Bass Weight
                    params[4] = randNorm(5, 0.45f, 0.88f) // Cutoff
                    params[5] = randNorm(6, 0.30f, 0.70f) // Resonance
                    params[6] = 0.005f // Percussive Attack
                    params[7] = randNorm(8, 0.20f, 0.55f) // Snappy Decay
                }
                EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> {
                    params[0] = randNorm(1, 0.35f, 0.75f) // Shimmer Harmonics
                    params[1] = randNorm(2, 0.40f, 0.85f) // Swarm Detune
                    params[2] = randNorm(3, 0.25f, 0.70f) // Warmth / Fold
                    params[3] = randNorm(4, 0.50f, 0.85f) // Fifth / Octave Layer
                    params[4] = randNorm(5, 0.40f, 0.80f) // Soft Cutoff
                    params[5] = randNorm(6, 0.20f, 0.55f) // Gentle Resonance
                    params[6] = randNorm(7, 0.35f, 0.72f) // Lush Swell Attack
                    params[7] = randNorm(8, 0.65f, 0.95f) // Long Release
                }
            }

            // Page 3: LFO & Effects (indices 8..11)
            params[8] = randNorm(9, 0.20f, 0.65f) // LFO Rate
            params[9] = randNorm(10, 0.12f, 0.50f) // LFO Depth
            params[10] = if (eng == EmulatorSynthEngineType.AMBIENT_DRONE_PAD) 0.72f else randNorm(11, 0.22f, 0.58f) // Space/Delay
            params[11] = if (eng == EmulatorSynthEngineType.CHIP_SID_PWM) 0.55f else randNorm(12, 0.05f, 0.35f) // Crush/Drive

            // Page 4: Sequencer & Gate (indices 12..15)
            params[12] = when (eng) {
                EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> 0.25f // ~90 BPM
                EmulatorSynthEngineType.CHIP_SID_PWM -> 0.65f // ~138 BPM
                else -> 0.52f // ~122 BPM
            }
            params[13] = if (eng == EmulatorSynthEngineType.AMBIENT_DRONE_PAD) 0.85f else 0.55f // Gate Length
            params[14] = 0.18f // Swing
            params[15] = 0.50f // Octave / Glide

            EmulatorPreset(
                index = idx,
                name = presetName,
                categoryBadge = eng.code,
                engineType = eng,
                parameters = params,
                defaultPatternIndex = idx % 4
            )
        }
    }

    private fun buildPatternsForFirmware(
        id: String,
        engine: EmulatorSynthEngineType,
        seed: Int
    ): List<EmulatorPattern> {
        // Minor pentatonic / Dorian / Acid scales relative to C
        val acidNotes = intArrayOf(0, 0, 12, 0, 3, 7, 0, 10, 12, 0, 7, 3, 0, 15, 12, 7)
        val fmMelodyNotes = intArrayOf(0, 7, 12, 15, 7, 10, 12, 19, 0, 5, 10, 12, 7, 3, 5, 12)
        val chipArpNotes = intArrayOf(0, 4, 7, 12, 16, 12, 7, 4, 0, 3, 7, 10, 15, 10, 7, 3)
        val ambientNotes = intArrayOf(0, 7, 12, 14, 19, 12, 7, 5, 0, 10, 14, 17, 12, 7, 3, 7)

        val rot = seed % 16
        fun rotate(arr: IntArray, shift: Int): List<Int> =
            List(16) { i -> arr[(i + shift) % arr.size] }

        val p1Notes = rotate(
            when (engine) {
                EmulatorSynthEngineType.VA_ACID_RESONANT -> acidNotes
                EmulatorSynthEngineType.CHIP_SID_PWM -> chipArpNotes
                EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> ambientNotes
                else -> fmMelodyNotes
            },
            rot
        )

        val p1 = EmulatorPattern(
            index = 0,
            name = "Фирменный паттерн ${id.uppercase().take(8)}",
            bpm = when (engine) {
                EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> 92
                EmulatorSynthEngineType.CHIP_SID_PWM -> 136
                else -> 124
            },
            steps = List(16) { idx ->
                val active = when (engine) {
                    EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> idx % 2 == 0
                    else -> idx != 5 && idx != 13
                }
                SequencerStep(
                    stepIndex = idx,
                    active = active,
                    semitoneOffset = p1Notes[idx],
                    accent = (idx % 4 == 0) || (idx == 10)
                )
            }
        )

        val p2 = EmulatorPattern(
            index = 1,
            name = "303 Acid Bassline / Octave Drive",
            bpm = 128,
            steps = List(16) { idx ->
                SequencerStep(
                    stepIndex = idx,
                    active = idx != 3 && idx != 11,
                    semitoneOffset = acidNotes[(idx + rot) % 16],
                    accent = idx == 0 || idx == 2 || idx == 6 || idx == 12
                )
            }
        )

        val p3 = EmulatorPattern(
            index = 2,
            name = "Chiptune / Demoscene Fast Arp",
            bpm = 140,
            steps = List(16) { idx ->
                SequencerStep(
                    stepIndex = idx,
                    active = true,
                    semitoneOffset = chipArpNotes[(idx + rot) % 16],
                    accent = idx % 4 == 0
                )
            }
        )

        val p4 = EmulatorPattern(
            index = 3,
            name = "Generative Poly-Harmonic Sequence",
            bpm = 98,
            steps = List(16) { idx ->
                SequencerStep(
                    stepIndex = idx,
                    active = idx % 2 == 0 || idx == 7 || idx == 15,
                    semitoneOffset = ambientNotes[(idx + rot) % 16],
                    accent = idx % 8 == 0
                )
            }
        )

        return listOf(p1, p2, p3, p4)
    }
}

/**
 * Polyphonic voice state inside the real-time 44.1 kHz synthesis loop.
 */
private class SynthVoice {
    var active: Boolean = false
    var midiNote: Int = 60
    var frequency: Double = 261.63
    var targetFrequency: Double = 261.63
    var velocity: Float = 0.9f
    var isReleased: Boolean = false
    var envLevel: Float = 0f
    var envStage: Int = 0 // 0=Off, 1=Attack, 2=Decay/Sustain, 3=Release
    var ageSamples: Long = 0L

    var phase1: Double = 0.0
    var phase2: Double = 0.0
    var phase3: Double = 0.0
    var phase4: Double = 0.0
    var prevOp1Out: Double = 0.0

    // State-variable resonant filter state per voice
    var svfLow: Double = 0.0
    var svfBand: Double = 0.0
}

/**
 * Real-time low-latency PCM synthesizer & 16-step hardware sequencer emulating the M-VAVE FM-1
 * (JieLi AC791N / WL82 Dual-Core RISC + FPU audio subsystem).
 */
class Fm1DspEmulatorEngine(
    private val externalScope: CoroutineScope,
    private val onStepChanged: (stepIndex: Int, midiNote: Int) -> Unit,
    private val onScopeUpdated: (samples: FloatArray, peakLevel: Float, recordedDurationSec: Float) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_FRAMES = 512
        private const val MAX_VOICES = 6
    }

    private val voices = Array(MAX_VOICES) { SynthVoice() }
    private val voiceLock = Any()

    @Volatile
    private var engineType: EmulatorSynthEngineType = EmulatorSynthEngineType.FM_4OP

    // 16 normalized parameters [0..1]
    private val params = FloatArray(16) { 0.5f }

    @Volatile
    private var isSequencerPlaying: Boolean = false

    @Volatile
    private var arpMode: EmulatorArpMode = EmulatorArpMode.OFF

    @Volatile
    private var baseOctave: Int = 4 // C4 = MIDI 60

    @Volatile
    private var masterVolume: Float = 0.85f

    private var steps: Array<SequencerStep> = Array(16) {
        SequencerStep(stepIndex = it, active = true, semitoneOffset = 0)
    }

    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null

    // Master delay line (up to 450 ms)
    private val delayBuffer = FloatArray(20000)
    private var delayWritePos = 0

    // LFO & Sequencer clock state
    private var lfoPhase: Double = 0.0
    private var sampleCounter: Long = 0L
    private var nextStepSample: Long = 0L
    private var currentStepIndex: Int = 0
    private var currentStepGateOffSample: Long = 0L
    private var activeSequencerMidiNote: Int = -1

    // Live Held Pad Notes for Arpeggiator / Live Play
    private val heldNotes = mutableListOf<Int>()
    private var arpStepCursor = 0

    // WAV Recording state
    @Volatile
    private var isRecordingWav: Boolean = false
    private val wavPcmStream = ByteArrayOutputStream()
    private val wavLock = Any()

    @Volatile
    private var recordedSampleCount: Long = 0L

    fun startEngineIfNeeded() {
        if (audioJob?.isActive == true) return
        audioJob = externalScope.launch(Dispatchers.Default) {
            runAudioLoop()
        }
    }

    fun stopEngine() {
        isSequencerPlaying = false
        allNotesOff()
        audioJob?.cancel()
        audioJob = null
    }

    fun applyPreset(preset: EmulatorPreset) {
        engineType = preset.engineType
        synchronized(voiceLock) {
            for (i in 0 until min(16, preset.parameters.size)) {
                params[i] = preset.parameters[i].coerceIn(0f, 1f)
            }
        }
    }

    fun setEngineType(type: EmulatorSynthEngineType) {
        engineType = type
    }

    fun setParameter(paramIndex: Int, normalizedValue: Float) {
        if (paramIndex in 0..15) {
            params[paramIndex] = normalizedValue.coerceIn(0f, 1f)
        }
    }

    fun getParametersSnapshot(): FloatArray = params.clone()

    fun setSequencerPattern(patternSteps: List<SequencerStep>, bpm: Int? = null) {
        synchronized(voiceLock) {
            for (i in 0 until 16) {
                steps[i] = patternSteps.getOrNull(i) ?: SequencerStep(i, active = false)
            }
            if (bpm != null) {
                val normBpm = ((bpm - 60f) / 120f).coerceIn(0f, 1f)
                params[12] = normBpm
            }
        }
    }

    fun updateSingleStep(stepIndex: Int, step: SequencerStep) {
        if (stepIndex in 0..15) {
            synchronized(voiceLock) {
                steps[stepIndex] = step
            }
        }
    }

    fun setSequencerPlaying(playing: Boolean) {
        startEngineIfNeeded()
        synchronized(voiceLock) {
            isSequencerPlaying = playing
            if (playing) {
                currentStepIndex = 0
                nextStepSample = sampleCounter
            } else {
                if (activeSequencerMidiNote >= 0) {
                    noteOffInternal(activeSequencerMidiNote)
                    activeSequencerMidiNote = -1
                }
            }
        }
    }

    fun setArpMode(mode: EmulatorArpMode) {
        arpMode = mode
    }

    fun setBaseOctave(octave: Int) {
        baseOctave = octave.coerceIn(2, 6)
    }

    fun triggerPadNoteOn(semitoneOffsetFromC: Int, velocity: Float = 0.92f): Int {
        startEngineIfNeeded()
        val midiNote = (baseOctave * 12 + 12 + semitoneOffsetFromC).coerceIn(24, 108)
        synchronized(voiceLock) {
            if (!heldNotes.contains(midiNote)) {
                heldNotes.add(midiNote)
            }
            if (arpMode == EmulatorArpMode.CHORD) {
                noteOnInternal(midiNote, velocity)
                noteOnInternal((midiNote + 4).coerceAtMost(108), velocity * 0.78f)
                noteOnInternal((midiNote + 7).coerceAtMost(108), velocity * 0.78f)
            } else {
                noteOnInternal(midiNote, velocity)
            }
        }
        return midiNote
    }

    fun triggerPadNoteOff(semitoneOffsetFromC: Int) {
        val midiNote = (baseOctave * 12 + 12 + semitoneOffsetFromC).coerceIn(24, 108)
        synchronized(voiceLock) {
            heldNotes.remove(midiNote)
            if (arpMode == EmulatorArpMode.CHORD) {
                noteOffInternal(midiNote)
                noteOffInternal((midiNote + 4).coerceAtMost(108))
                noteOffInternal((midiNote + 7).coerceAtMost(108))
            } else {
                noteOffInternal(midiNote)
            }
        }
    }

    fun allNotesOff() {
        synchronized(voiceLock) {
            heldNotes.clear()
            for (v in voices) {
                v.isReleased = true
                v.envStage = 3
            }
        }
    }

    fun startWavRecording() {
        startEngineIfNeeded()
        synchronized(wavLock) {
            wavPcmStream.reset()
            recordedSampleCount = 0L
            isRecordingWav = true
        }
    }

    fun stopWavRecordingAndBuildFile(): ByteArray {
        val rawPcm: ByteArray
        synchronized(wavLock) {
            isRecordingWav = false
            rawPcm = wavPcmStream.toByteArray()
        }
        return wrapPcmInRiffWavHeader(rawPcm, SAMPLE_RATE, channels = 1, bitsPerSample = 16)
    }

    fun getRecordedDurationSeconds(): Float {
        return recordedSampleCount.toFloat() / SAMPLE_RATE.toFloat()
    }

    private fun noteOnInternal(midiNote: Int, velocity: Float) {
        val targetFreq = 440.0 * 2.0.pow((midiNote - 69.0) / 12.0)
        // Find existing voice with same note, or free voice, or oldest voice
        val existing = voices.firstOrNull { it.active && it.midiNote == midiNote }
        val targetVoice = existing
            ?: voices.firstOrNull { !it.active }
            ?: voices.maxByOrNull { it.ageSamples }
            ?: voices[0]

        val glideNorm = params[15]
        val shouldGlide = glideNorm > 0.58f && targetVoice.active
        targetVoice.midiNote = midiNote
        targetVoice.targetFrequency = targetFreq
        if (!shouldGlide || targetVoice.frequency <= 20.0) {
            targetVoice.frequency = targetFreq
        }
        targetVoice.velocity = velocity.coerceIn(0.1f, 1.0f)
        targetVoice.active = true
        targetVoice.isReleased = false
        targetVoice.envStage = 1
        targetVoice.ageSamples = 0L
        if (!shouldGlide) {
            targetVoice.phase1 = 0.0
            targetVoice.phase2 = 0.0
        }
    }

    private fun noteOffInternal(midiNote: Int) {
        for (v in voices) {
            if (v.active && v.midiNote == midiNote && !v.isReleased) {
                v.isReleased = true
                v.envStage = 3
            }
        }
    }

    private fun runAudioLoop() {
        val minBufSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(BUFFER_FRAMES * 2)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            null
        }

        audioTrack = track
        runCatching { track?.play() }

        val pcmBuffer = ShortArray(BUFFER_FRAMES)
        val scopeSnapshot = FloatArray(128)
        var uiNotifyCounter = 0

        try {
            while (externalScope.isActive) {
                var peak = 0f
                synchronized(voiceLock) {
                    val bpm = 60.0 + params[12] * 120.0
                    val samplesPerStep = ((60.0 / bpm / 4.0) * SAMPLE_RATE).toLong().coerceAtLeast(1200L)
                    val gateFraction = (0.18 + params[13] * 0.74).coerceIn(0.15, 0.95)
                    val swingAmount = (params[14] * 0.32).coerceIn(0.0, 0.35)

                    for (i in 0 until BUFFER_FRAMES) {
                        // Sequencer / Arpeggiator clock tick
                        if (isSequencerPlaying && sampleCounter >= nextStepSample) {
                            val stepIdx = currentStepIndex % 16
                            val step = steps[stepIdx]

                            if (activeSequencerMidiNote >= 0) {
                                noteOffInternal(activeSequencerMidiNote)
                                activeSequencerMidiNote = -1
                            }

                            val stepDuration = if (stepIdx % 2 == 0) {
                                (samplesPerStep * (1.0 + swingAmount)).toLong()
                            } else {
                                (samplesPerStep * (1.0 - swingAmount)).toLong()
                            }

                            if (step.active) {
                                val noteToPlay = resolveStepOrArpNote(step)
                                val vel = if (step.accent) 1.0f else 0.76f
                                noteOnInternal(noteToPlay, vel)
                                activeSequencerMidiNote = noteToPlay
                                currentStepGateOffSample =
                                    sampleCounter + (stepDuration * gateFraction * step.gateMultiplier).toLong()
                                onStepChanged(stepIdx, noteToPlay)
                            } else {
                                onStepChanged(stepIdx, -1)
                            }

                            nextStepSample = sampleCounter + stepDuration
                            currentStepIndex = (currentStepIndex + 1) % 16
                        }

                        if (isSequencerPlaying && activeSequencerMidiNote >= 0 && sampleCounter >= currentStepGateOffSample) {
                            noteOffInternal(activeSequencerMidiNote)
                            activeSequencerMidiNote = -1
                        }

                        val sampleOut = synthesizeSingleSample()
                        val absVal = abs(sampleOut)
                        if (absVal > peak) peak = absVal

                        if (i % (BUFFER_FRAMES / 128) == 0) {
                            val scopeIdx = (i / (BUFFER_FRAMES / 128)).coerceIn(0, 127)
                            scopeSnapshot[scopeIdx] = sampleOut
                        }

                        val pcmShort = (sampleOut * 32760f).roundToInt().coerceIn(-32767, 32767).toShort()
                        pcmBuffer[i] = pcmShort
                        sampleCounter++
                    }
                }

                if (isRecordingWav) {
                    synchronized(wavLock) {
                        if (isRecordingWav && recordedSampleCount < SAMPLE_RATE * 60) {
                            val byteBuf = ByteBuffer.allocate(BUFFER_FRAMES * 2).order(ByteOrder.LITTLE_ENDIAN)
                            for (s in pcmBuffer) {
                                byteBuf.putShort(s)
                            }
                            wavPcmStream.write(byteBuf.array())
                            recordedSampleCount += BUFFER_FRAMES
                        }
                    }
                }

                uiNotifyCounter++
                if (uiNotifyCounter % 3 == 0) {
                    onScopeUpdated(scopeSnapshot.clone(), peak, getRecordedDurationSeconds())
                }

                if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.write(pcmBuffer, 0, BUFFER_FRAMES)
                } else {
                    Thread.sleep(10)
                }
            }
        } catch (_: InterruptedException) {
            // Normal shutdown
        } finally {
            runCatching {
                track?.stop()
                track?.release()
            }
            audioTrack = null
        }
    }

    private fun resolveStepOrArpNote(step: SequencerStep): Int {
        if (arpMode != EmulatorArpMode.OFF && heldNotes.isNotEmpty()) {
            val sorted = heldNotes.sorted()
            val idx = when (arpMode) {
                EmulatorArpMode.UP -> arpStepCursor % sorted.size
                EmulatorArpMode.DOWN -> (sorted.size - 1) - (arpStepCursor % sorted.size)
                EmulatorArpMode.UP_DOWN -> {
                    val cycle = max(1, sorted.size * 2 - 2)
                    val pos = arpStepCursor % cycle
                    if (pos < sorted.size) pos else cycle - pos
                }
                else -> arpStepCursor % sorted.size
            }
            arpStepCursor++
            return sorted[idx.coerceIn(0, sorted.lastIndex)]
        }
        return (baseOctave * 12 + 12 + step.semitoneOffset).coerceIn(24, 108)
    }

    private fun synthesizeSingleSample(): Float {
        val twoPi = 2.0 * PI
        val invSr = 1.0 / SAMPLE_RATE

        // LFO update (0.2 Hz .. 16 Hz)
        val lfoRateHz = 0.25 + (params[8] * params[8]) * 16.0
        lfoPhase += twoPi * lfoRateHz * invSr
        if (lfoPhase >= twoPi) lfoPhase -= twoPi
        val lfoVal = sin(lfoPhase) * params[9].toDouble()

        val timbre = params[0].toDouble()
        val ratioParam = params[1].toDouble()
        val feedbackParam = params[2].toDouble()
        val harmMix = params[3].toDouble()

        val baseCutoff = params[4].toDouble()
        val resonance = params[5].toDouble()
        val attackParam = params[6].coerceIn(0.002f, 0.99f)
        val decayReleaseParam = params[7].coerceIn(0.02f, 0.99f)

        val spaceMix = params[10].toDouble()
        val crushDrive = params[11].toDouble()

        val attackStep = 1.0f / (SAMPLE_RATE * (0.002f + attackParam * attackParam * 0.85f))
        val decayStep = 1.0f / (SAMPLE_RATE * (0.04f + decayReleaseParam * decayReleaseParam * 1.6f))
        val releaseStep = 1.0f / (SAMPLE_RATE * (0.03f + decayReleaseParam * 1.1f))
        val sustainLevel = when (engineType) {
            EmulatorSynthEngineType.GROOVE_DRUM_SYNTH -> 0.12f
            EmulatorSynthEngineType.VA_ACID_RESONANT -> 0.35f
            EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> 0.82f
            else -> 0.55f
        }

        var mix = 0.0
        var activeVoicesCount = 0

        for (v in voices) {
            if (!v.active) continue
            v.ageSamples++

            // Envelope progression
            when (v.envStage) {
                1 -> {
                    v.envLevel += attackStep
                    if (v.envLevel >= 1.0f) {
                        v.envLevel = 1.0f
                        v.envStage = 2
                    }
                }
                2 -> {
                    if (v.envLevel > sustainLevel) {
                        v.envLevel = max(sustainLevel, v.envLevel - decayStep)
                    }
                    if (engineType == EmulatorSynthEngineType.GROOVE_DRUM_SYNTH && v.envLevel <= sustainLevel + 0.01f) {
                        v.envStage = 3
                    }
                }
                3 -> {
                    v.envLevel -= releaseStep
                    if (v.envLevel <= 0.0005f) {
                        v.envLevel = 0f
                        v.active = false
                        v.envStage = 0
                        continue
                    }
                }
            }

            // Portamento / Glide
            v.frequency += (v.targetFrequency - v.frequency) * 0.0035

            // Pitch modulation from LFO
            val lfoPitchFactor = 1.0 + lfoVal * 0.025
            val freq = v.frequency * lfoPitchFactor

            val rawSignal: Double = when (engineType) {
                EmulatorSynthEngineType.FM_4OP -> {
                    val ratios = doubleArrayOf(0.5, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 7.0)
                    val op2Ratio = ratios[(ratioParam * (ratios.size - 1)).roundToInt().coerceIn(0, ratios.lastIndex)]
                    val op3Ratio = if (harmMix > 0.5) 3.0 else 2.0

                    val inc1 = twoPi * freq * invSr
                    val inc2 = twoPi * (freq * op2Ratio) * invSr
                    val inc3 = twoPi * (freq * op3Ratio) * invSr

                    v.phase1 = (v.phase1 + inc1) % twoPi
                    v.phase2 = (v.phase2 + inc2) % twoPi
                    v.phase3 = (v.phase3 + inc3) % twoPi

                    val modIndex = (timbre * 5.5) * (0.35 + 0.65 * v.envLevel)
                    val fb = feedbackParam * 1.8 * v.prevOp1Out
                    val op3 = sin(v.phase3) * harmMix * 1.6
                    val op2 = sin(v.phase2 + op3) * modIndex
                    val op1 = sin(v.phase1 + op2 + fb)
                    v.prevOp1Out = op1
                    op1
                }

                EmulatorSynthEngineType.CHIP_SID_PWM -> {
                    val inc1 = freq * invSr
                    val inc2 = (freq * (1.0 + ratioParam * 0.5)) * invSr
                    v.phase1 = (v.phase1 + inc1) % 1.0
                    v.phase2 = (v.phase2 + inc2) % 1.0

                    val pw = (0.12 + timbre * 0.76 + lfoVal * 0.22).coerceIn(0.08, 0.92)
                    val pulse = if (v.phase1 < pw) 1.0 else -1.0
                    val tri = 1.0 - 4.0 * abs(v.phase2 - 0.5)
                    val ring = pulse * tri
                    val combined = pulse * (1.0 - feedbackParam * 0.55) + ring * (feedbackParam * 0.55)
                    // 8-bit quantization
                    val stepsQuant = (16.0 - crushDrive * 11.0).coerceAtLeast(4.0)
                    (combined * stepsQuant).roundToInt() / stepsQuant
                }

                EmulatorSynthEngineType.VA_ACID_RESONANT -> {
                    val detune = 1.0 + ratioParam * 0.015
                    val inc1 = freq * invSr
                    val inc2 = (freq * detune) * invSr
                    val incSub = (freq * 0.5) * invSr

                    v.phase1 = (v.phase1 + inc1) % 1.0
                    v.phase2 = (v.phase2 + inc2) % 1.0
                    v.phase3 = (v.phase3 + incSub) % 1.0

                    val saw = 2.0 * v.phase1 - 1.0
                    val sq = if (v.phase2 < 0.5) 1.0 else -1.0
                    val sub = if (v.phase3 < 0.5) 0.8 else -0.8
                    val mainWave = saw * (1.0 - timbre * 0.75) + sq * (timbre * 0.75)
                    mainWave * 0.72 + sub * (harmMix * 0.55)
                }

                EmulatorSynthEngineType.GROOVE_DRUM_SYNTH -> {
                    // Pitch envelope kick + metallic FM percussion + bass
                    val pitchDrop = 1.0 + (timbre * 3.8) * exp(-v.ageSamples.toDouble() / (SAMPLE_RATE * 0.028))
                    val inc1 = twoPi * (freq * pitchDrop) * invSr
                    val inc2 = twoPi * (freq * (1.5 + ratioParam * 3.2)) * invSr
                    v.phase1 = (v.phase1 + inc1) % twoPi
                    v.phase2 = (v.phase2 + inc2) % twoPi

                    val fmSnap = sin(v.phase2) * (feedbackParam * 3.0) * exp(-v.ageSamples.toDouble() / (SAMPLE_RATE * 0.06))
                    val body = sin(v.phase1 + fmSnap)
                    val sub = sin(v.phase1 * 0.5) * harmMix
                    tanh((body + sub * 0.7) * (1.2 + feedbackParam * 1.4))
                }

                EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> {
                    val spread = 0.004 + ratioParam * 0.014
                    val inc1 = twoPi * freq * invSr
                    val inc2 = twoPi * (freq * (1.0 + spread)) * invSr
                    val inc3 = twoPi * (freq * (1.0 - spread)) * invSr
                    val inc4 = twoPi * (freq * 1.4983) * invSr // Fifth harmonic

                    v.phase1 = (v.phase1 + inc1) % twoPi
                    v.phase2 = (v.phase2 + inc2) % twoPi
                    v.phase3 = (v.phase3 + inc3) % twoPi
                    v.phase4 = (v.phase4 + inc4) % twoPi

                    val fold = 1.0 + timbre * 2.2
                    val swarm = (sin(v.phase1 * fold) + sin(v.phase2) + cos(v.phase3)) * 0.35
                    val fifth = sin(v.phase4) * (harmMix * 0.40)
                    tanh(swarm + fifth)
                }
            }

            // Per-voice 2-pole State Variable Filter (Chamberlin SVF)
            val envMod = if (engineType == EmulatorSynthEngineType.VA_ACID_RESONANT) {
                v.envLevel * (0.25 + feedbackParam * 0.65)
            } else {
                v.envLevel * 0.25
            }
            val cutoffNorm = (baseCutoff + envMod + lfoVal * 0.18).coerceIn(0.05, 0.96)
            val cutoffHz = 90.0 * (110.0).pow(cutoffNorm)
            val f = (2.0 * sin(PI * min(0.42, cutoffHz * invSr))).coerceIn(0.01, 0.82)
            val q = (1.0 - resonance * 0.86).coerceIn(0.12, 1.0)

            v.svfLow += f * v.svfBand
            val high = rawSignal - v.svfLow - q * v.svfBand
            v.svfBand += f * high

            val filtered = v.svfLow + (if (resonance > 0.6) v.svfBand * 0.28 else 0.0)
            mix += filtered * v.envLevel * v.velocity
            activeVoicesCount++
        }

        var master = if (activeVoicesCount > 0) {
            mix * 0.36
        } else {
            0.0
        }

        // Drive / Bitcrush saturation
        if (crushDrive > 0.06) {
            master = tanh(master * (1.0 + crushDrive * 3.2))
        }

        // Stereo-style tape delay / space feedback loop
        val delaySamples = when (engineType) {
            EmulatorSynthEngineType.AMBIENT_DRONE_PAD -> 14800
            else -> 11025
        }
        val readPos = (delayWritePos - delaySamples + delayBuffer.size) % delayBuffer.size
        val delayed = delayBuffer[readPos].toDouble()
        val combined = master + delayed * (spaceMix * 0.52)
        delayBuffer[delayWritePos] = (master + delayed * (spaceMix * 0.45)).toFloat().coerceIn(-0.98f, 0.98f)
        delayWritePos = (delayWritePos + 1) % delayBuffer.size

        return (combined * masterVolume).toFloat().coerceIn(-0.98f, 0.98f)
    }

    private fun wrapPcmInRiffWavHeader(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = pcmData.size + 36

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1.toShort()) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcmData.size)

        val out = ByteArray(44 + pcmData.size)
        System.arraycopy(header.array(), 0, out, 0, 44)
        System.arraycopy(pcmData, 0, out, 44, pcmData.size)
        return out
    }
}
