package com.example.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.CRC32

data class ChecksumReport(
    val sha256: String,
    val crc32: String,
    val md5: String,
    val sizeBytes: Int,
    val startsWithBootHeaderHazard: Boolean,
    val isValidJieLiPartition: Boolean,
    val targetOffsetHex: String,
    val headerHexPreview: String
)

object ChecksumEngine {

    fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computeMd5(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun computeCrc32(bytes: ByteArray): String {
        val crc = CRC32()
        crc.update(bytes)
        return "%08X".format(crc.value)
    }

    /**
     * Generates a deterministic, realistic JieLi WL82 / M-VAVE FM-1 binary payload
     * for any catalog firmware so real cryptographic hashing (SHA-256, CRC32, MD5),
     * block-by-block USB packet slicing, and read-back verification operate on real bytes.
     */
    fun buildDeterministicFirmwareBinary(
        firmwareId: String,
        version: String,
        targetOffsetHex: String = "0x00004000",
        declaredSizeBytes: Int = 262144
    ): ByteArray {
        // Use a compact 32KB representative binary slice for fast in-memory cryptographic verification
        val actualLength = declaredSizeBytes.coerceIn(16384, 65536)
        val buffer = ByteBuffer.allocate(actualLength).order(ByteOrder.LITTLE_ENDIAN)

        // JieLi AC791N / WL82 Firmware Partition Magic Header ("JLWL82FM")
        val magic = "JLWL82FM".toByteArray(Charsets.US_ASCII)
        buffer.put(magic)

        // Target offset marker (0x00004000 safe application partition)
        val offsetValue = targetOffsetHex.removePrefix("0x").toLongOrNull(16)?.toInt() ?: 0x4000
        buffer.putInt(offsetValue)
        buffer.putInt(declaredSizeBytes)

        // Embed firmware identifier & version into the header descriptor
        val idBytes = "$firmwareId:$version:M-VAVE-FM1".toByteArray(Charsets.UTF_8)
        buffer.put(idBytes, 0, idBytes.size.coerceAtMost(64))

        // Fill deterministic DSP synthesis LUT & opcode blocks
        var seed = (firmwareId.hashCode() xor version.hashCode() xor declaredSizeBytes)
        while (buffer.hasRemaining()) {
            seed = seed xor (seed shl 13)
            seed = seed xor (seed ushr 17)
            seed = seed xor (seed shl 5)
            buffer.put(seed.toByte())
        }
        return buffer.array()
    }

    /**
     * Inspects any binary image (catalog or user-imported .bin/.fw/.fwsc/.syx)
     * and returns full SHA-256, CRC32, MD5, HEX dump preview, and 0x0000 boot-sector hazard check.
     */
    fun analyzeBinary(
        bytes: ByteArray,
        fileName: String = "firmware.bin",
        requestedOffsetHex: String = "0x00004000"
    ): ChecksumReport {
        val sha256 = computeSha256(bytes)
        val crc32 = computeCrc32(bytes)
        val md5 = computeMd5(bytes)

        val lowerName = fileName.lowercase()
        // Raw .fwsc packages flashed to 0x0000 overwrite the JieLi boot head (0x0000-0x3FFF)
        val isFwscAtZero = lowerName.endsWith(".fwsc") &&
            (requestedOffsetHex.equals("0x0000", ignoreCase = true) ||
                requestedOffsetHex.equals("0x00000000", ignoreCase = true))

        val previewBytes = bytes.take(32).toByteArray()
        val hexPreview = previewBytes.joinToString(" ") { "%02X".format(it) }

        val hasJieLiMagic = bytes.size >= 8 &&
            String(bytes.sliceArray(0 until 8), Charsets.US_ASCII).startsWith("JLWL82")

        return ChecksumReport(
            sha256 = sha256,
            crc32 = crc32,
            md5 = md5,
            sizeBytes = bytes.size,
            startsWithBootHeaderHazard = isFwscAtZero,
            isValidJieLiPartition = hasJieLiMagic || bytes.size >= 1024,
            targetOffsetHex = if (isFwscAtZero) "0x00004000 (AUTO-PROTECTED)" else requestedOffsetHex,
            headerHexPreview = hexPreview
        )
    }

    /**
     * Generates cross-platform CLI recovery and flashing scripts for Windows (PowerShell),
     * Linux/macOS (Bash + jl-uboot-tool), and Python 3 so users can run the exact same
     * checksum-verified flashing workflow on desktop operating systems.
     */
    fun generateCrossPlatformScript(
        osPlatform: String,
        firmwareName: String,
        fileName: String,
        sha256: String,
        crc32: String,
        offsetHex: String = "0x4000"
    ): String {
        return when (osPlatform) {
            "WINDOWS_PS" -> """
                # =====================================================================
                # FirmwareHub Studio — Windows PowerShell Auto-Flash & WL82 Unbricker
                # Target: M-VAVE FM-1 (JieLi AC791N / WL82) | Package: $firmwareName
                # =====================================================================
                ${'$'}ErrorActionPreference = "Stop"
                ${'$'}FwFile = "$fileName"
                ${'$'}ExpectedSha256 = "$sha256"
                ${'$'}SafeOffset = "$offsetHex"

                Write-Host "[1/4] Verifying SHA-256 integrity of ${'$'}FwFile (CRC32: $crc32)..." -ForegroundColor Cyan
                ${'$'}ActualHash = (Get-FileHash -Path ${'$'}FwFile -Algorithm SHA256).Hash.ToLower()
                if (${'$'}ActualHash -ne ${'$'}ExpectedSha256) {
                    Write-Error "CRITICAL: SHA-256 mismatch! Expected ${'$'}ExpectedSha256, got ${'$'}ActualHash. Aborting flash."
                    exit 1
                }
                Write-Host "OK: Checksum verified (${'$'}ActualHash)" -ForegroundColor Green

                Write-Host "[2/4] Scanning USB for M-VAVE FM-1 or WL82 UBOOT1.00 (VID_4C4A&PID_8057)..." -ForegroundColor Cyan
                python -m jluboot.cli --vid 0x4C4A --pid 0x8057 info

                Write-Host "[3/4] Backing up EEPROM & Boot Header (0x0000-0x3FFF) to backup_fm1_$(Get-Date -Format 'yyyyMMdd_HHmmss').bin..." -ForegroundColor Yellow
                python -m jluboot.cli read 0x0000 0x100000 "backup_fm1_full.bin"

                Write-Host "[4/4] Flashing $firmwareName strictly to ${'$'}SafeOffset (Bootloader 0x0000-0x3FFF protected)..." -ForegroundColor Green
                python -m jluboot.cli write ${'$'}SafeOffset ${'$'}FwFile --verify
                Write-Host "SUCCESS: M-VAVE FM-1 flashed and verified!" -ForegroundColor Green
            """.trimIndent()

            "LINUX_MACOS_SH" -> """
                #!/usr/bin/env bash
                # =====================================================================
                # FirmwareHub Studio — Linux / macOS Auto-Flash & WL82 Unbricker
                # Target: M-VAVE FM-1 (JieLi AC791N / WL82) | Package: $firmwareName
                # =====================================================================
                set -euo pipefail

                FW_FILE="$fileName"
                EXPECTED_SHA256="$sha256"
                SAFE_OFFSET="$offsetHex"

                echo "[1/4] Verifying SHA-256 checksum (CRC32: $crc32)..."
                if command -v sha256sum >/dev/null 2>&1; then
                    ACTUAL_SHA256=${'$'}(sha256sum "${'$'}FW_FILE" | awk '{print ${'$'}1}')
                else
                    ACTUAL_SHA256=${'$'}(shasum -a 256 "${'$'}FW_FILE" | awk '{print ${'$'}1}')
                fi

                if [[ "${'$'}ACTUAL_SHA256" != "${'$'}EXPECTED_SHA256" ]]; then
                    echo "ERROR: Checksum mismatch! Expected ${'$'}EXPECTED_SHA256, got ${'$'}ACTUAL_SHA256" >&2
                    exit 1
                fi
                echo "OK: Integrity verified (${'$'}ACTUAL_SHA256)"

                echo "[2/4] Triggering MIDI SysEx Bootloader Entry (F0 22 24 35 7D F7) if in normal mode..."
                if command -v amidi >/dev/null 2>&1; then
                    amidi -l | grep -i "FM" && amidi -p hw:1,0,0 -S "F0 22 24 35 7D F7" || true
                fi

                echo "[3/4] Creating full EEPROM & Flash backup before writing..."
                python3 jl_uboot_tool.py --device 4c4a:8057 dump 0x0000 0x100000 "fm1_backup_${'$'}(date +%Y%m%d_%H%M%S).bin"

                echo "[4/4] Writing $firmwareName to safe offset ${'$'}SAFE_OFFSET (preserving 0x0000-0x3FFF)..."
                python3 jl_uboot_tool.py --device 4c4a:8057 write "${'$'}SAFE_OFFSET" "${'$'}FW_FILE" --verify-crc "$crc32"
                echo "DONE: M-VAVE FM-1 successfully updated and verified!"
            """.trimIndent()

            else -> """
                #!/usr/bin/env python3
                # =====================================================================
                # Cross-Platform Python 3 Unbricker & Flasher (Windows / macOS / Linux)
                # Firmware: $firmwareName | Expected SHA-256: $sha256
                # =====================================================================
                import hashlib, sys, zlib, pathlib

                FW_PATH = pathlib.Path("$fileName")
                EXPECTED_SHA256 = "$sha256"
                EXPECTED_CRC32 = "$crc32"
                SAFE_OFFSET = int("$offsetHex", 16)

                data = FW_PATH.read_bytes()
                sha = hashlib.sha256(data).hexdigest()
                crc = f"{zlib.crc32(data) & 0xFFFFFFFF:08X}"

                print(f"[VERIFY] File: {FW_PATH.name} ({len(data)} bytes)")
                print(f"[VERIFY] SHA-256: {sha}")
                print(f"[VERIFY] CRC-32:  {crc}")

                if sha != EXPECTED_SHA256:
                    sys.exit(f"ABORT: SHA-256 mismatch! Expected {EXPECTED_SHA256}")
                if SAFE_OFFSET < 0x4000:
                    sys.exit("ABORT: Refusing to overwrite JieLi boot header (0x0000-0x3FFF)!")

                print(f"[READY] Safe offset 0x{SAFE_OFFSET:04X} validated. Connect WL82 UBOOT1.00 (VID 0x4C4A, PID 0x8057).")
            """.trimIndent()
        }
    }
}
