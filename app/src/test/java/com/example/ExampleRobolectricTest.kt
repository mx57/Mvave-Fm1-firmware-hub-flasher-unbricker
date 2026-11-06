package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FirmwareRepository
import com.example.domain.ActiveWireProtocol
import com.example.domain.ChecksumEngine
import com.example.domain.DeviceDiagnosticVerdict
import com.example.domain.SpiPinShortVector
import com.example.domain.UnbrickMethodType
import com.example.domain.UsbFlashEngine
import com.example.domain.UsbProfileType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FM1 FirmwareHub", appName)
    }

    @Test
    fun `repository seeds 160 firmwares and downloads into separate FM1_Firmwares folder with hash verification and DB screen and description`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = FirmwareRepository(db.appDao(), context.filesDir, context.assets)

        repo.ensureSeeded()
        val snapshot = db.appDao().getAllFirmwaresSnapshot()
        assertEquals(160, snapshot.size)

        // Stock unbrick firmware is materialized immediately into FM1_Firmwares/
        val stock = snapshot.first { it.isOfficialStock }
        assertTrue(stock.isDownloaded)
        assertTrue(stock.hashVerified)
        assertTrue(stock.localFilePath.contains("FM1_Firmwares"))
        assertTrue(File(stock.localFilePath).exists())
        assertTrue(stock.fullDescription.isNotBlank())
        assertTrue(stock.screenImageBase64.isNotBlank())

        // Download another firmware into the separate FM1_Firmwares/ folder and verify SHA-256 + DB fields
        val target = snapshot.first { it.id == "sloop" }
        val downloaded = repo.downloadFirmwareBinary(target)
        assertTrue(downloaded.isDownloaded)
        assertTrue(downloaded.hashVerified)
        assertTrue(downloaded.localFilePath.contains("FM1_Firmwares"))
        val binFile = File(downloaded.localFilePath)
        assertTrue(binFile.exists())
        assertEquals(downloaded.sha256, ChecksumEngine.computeSha256(binFile.readBytes()))
        assertTrue(downloaded.fullDescription.isNotBlank())
        assertTrue(downloaded.screenImageBase64.isNotBlank())

        // Verify in DB snapshot
        val fromDb = db.appDao().getAllFirmwaresSnapshot().first { it.id == "sloop" }
        assertTrue(fromDb.isDownloaded)
        assertTrue(fromDb.hashVerified)
        assertTrue(fromDb.screenImageBase64.isNotBlank())
        assertTrue(fromDb.fullDescription.isNotBlank())

        db.close()
    }

    @Test
    fun `usbFlashEngine encodes 4-byte USB-MIDI Event Packets and handles bootloader re-enumeration after screen blackout`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val usbEngine = UsbFlashEngine(context)

        // 1. Verify 6-byte raw SysEx F0 22 24 35 7D F7 is framed into 8-byte USB-MIDI Event Packets (CIN 0x04, 0x07)
        val rawTrigger = byteArrayOf(0xF0.toByte(), 0x22, 0x24, 0x35, 0x7D, 0xF7.toByte())
        val usbMidiPackets = usbEngine.encodeSysExToUsbMidiPackets(rawTrigger)
        assertEquals(8, usbMidiPackets.size)
        assertArrayEquals(
            byteArrayOf(
                0x04, 0xF0.toByte(), 0x22, 0x24,
                0x07, 0x35, 0x7D, 0xF7.toByte()
            ),
            usbMidiPackets
        )

        // 2. Verify OTA chunk framing in USB-MIDI Event Packets
        val otaChunk = usbEngine.buildFm1SysExOtaChunkUsbMidiPackets(0x00004000, ByteArray(64) { it.toByte() })
        assertTrue(otaChunk.isNotEmpty())
        assertEquals(0, otaChunk.size % 4)
        assertEquals(0x04.toByte(), otaChunk[0])

        // 3. Verify simulated normal MIDI device transitions to bootloader on awaitBootloaderReenumeration
        val normalMidi = usbEngine.buildSimulatedDevice(UsbProfileType.MVAVE_NORMAL_MIDI)
        assertFalse(normalMidi.profileType.isBootloaderMode)
        usbEngine.sendSysExBootloaderTrigger(normalMidi) { _, _, _, _ -> }
        val reenumerated = usbEngine.awaitBootloaderReenumeration(
            previousDevice = normalMidi,
            onLog = { _, _, _, _ -> },
            onProgress = {}
        )
        assertTrue(reenumerated.isReadyToFlash)
        assertTrue(reenumerated.profileType.isBootloaderMode)

        // 4. Execute dual-protocol flash pipeline on the re-enumerated bootloader device
        val testPayload = ChecksumEngine.buildDeterministicFirmwareBinary("sloop", "v1.0", "0x00004000", 16384)
        val flashOk = usbEngine.executeFlashPipeline(
            deviceInfo = reenumerated,
            firmwareBytes = testPayload,
            declaredSizeBytes = testPayload.size,
            targetOffsetHex = "0x00004000",
            expectedSha256 = ChecksumEngine.computeSha256(testPayload),
            expectedCrc32 = ChecksumEngine.computeCrc32(testPayload),
            preferredProtocol = ActiveWireProtocol.AUTO_DUAL_PROTOCOL
        ) {}
        assertTrue(flashOk)
    }

    @Test
    fun `usbFlashEngine blocks flashing when no physical OTG device is connected and detects Hard-Brick`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val usbEngine = UsbFlashEngine(context)

        val disconnected = usbEngine.buildDisconnectedDevice(suspectHardBrick = false)
        assertFalse(disconnected.isConnected)
        assertFalse(disconnected.isReadyToFlash)
        assertEquals(DeviceDiagnosticVerdict.NO_OTG_DEVICE, disconnected.diagnosticVerdict)

        val hardBrick = usbEngine.buildDisconnectedDevice(suspectHardBrick = true)
        assertFalse(hardBrick.isConnected)
        assertFalse(hardBrick.isReadyToFlash)
        assertEquals(DeviceDiagnosticVerdict.HARD_BRICK_SUSPECTED, hardBrick.diagnosticVerdict)
        assertEquals(UnbrickMethodType.SPI_PIN_SHORT_MASKROM, hardBrick.recommendedUnbrickMethod)

        try {
            usbEngine.executeFlashPipeline(
                deviceInfo = disconnected,
                firmwareBytes = ByteArray(16384),
                declaredSizeBytes = 16384,
                targetOffsetHex = "0x00004000",
                expectedSha256 = "00",
                expectedCrc32 = "00"
            ) {}
            fail("Expected IllegalStateException when attempting to flash without USB OTG device connected")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("USB OTG"))
        }
    }

    @Test
    fun `unbrick suite verifies 6 authentic methods, 3-vector pin shorting, CSW USBS parser, Control Reset, and 1MB dump`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val usbEngine = UsbFlashEngine(context)

        // 1. Verify all 6 authentic Unbrick methods and their 4-step PCB guides exist
        assertEquals(6, UnbrickMethodType.entries.size)
        UnbrickMethodType.entries.forEach { method ->
            assertFalse("Method ${method.name} must not require soldering", method.requiresSoldering)
            assertTrue(method.physicsExplanation.isNotBlank())
            assertTrue(method.troubleshootingTips.isNotBlank())
            assertEquals(4, method.steps.size)
        }
        assertEquals(3, SpiPinShortVector.entries.size)

        // 2. Verify 31-byte USB Mass Storage SCSI CBW packet ("USBC" = 0x43425355) and 13-byte CSW ("USBS" = 0x53425355)
        val cbw = usbEngine.buildJieLiScsiCbwPacket(
            tag = 42,
            transferLength = 4096,
            isDirectionIn = false,
            subCommand = UsbFlashEngine.JL_SUBCMD_WRITE_SPI_PAGE,
            flashAddress = 0x00004000
        )
        assertEquals(31, cbw.size)
        val sig = ByteBuffer.wrap(cbw, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(UsbFlashEngine.SCSI_CBW_SIGNATURE, sig)
        assertEquals(UsbFlashEngine.JL_CDB_VENDOR_GROUP, cbw[15])

        val validCsw = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(UsbFlashEngine.SCSI_CSW_SIGNATURE)
            putInt(42)
            putInt(0)
            put(0x00.toByte())
        }.array()
        val parsedCsw = usbEngine.parseScsiCswResponse(validCsw, 13, 42)
        assertTrue(parsedCsw.first)
        assertTrue(parsedCsw.second.contains("USBS"))

        // 3. Verify wl82loader.bin SRAM injection, Control Transfer EP0 reset, and 5-point USB Bus Audit
        val simSoftBrick = usbEngine.buildSimulatedDevice(UsbProfileType.WL82_UBOOT_SOFTBRICK)
        assertTrue(simSoftBrick.isReadyToFlash)
        val injected = usbEngine.injectWl82LoaderToSram(simSoftBrick) { _, _, _, _ -> }
        assertTrue(injected)
        val ctrlReset = usbEngine.executeUsbControlTransferReset(simSoftBrick) { _, _, _, _ -> }
        assertTrue(ctrlReset)
        val busReport = usbEngine.runUsbBusDiagnostics(simSoftBrick) { _, _, _, _ -> }
        assertTrue(busReport.passedAllChecks)
        assertEquals(5, busReport.detailedLines.size)

        // 4. Verify FWSC Container Surgeon unpacks 20 markers + strips 16KB prefix on .fwsc container
        val dummyFwsc = ByteArray(16384 + 8192 + 20) { (it and 0xFF).toByte() }
        val surgeonReport = ChecksumEngine.extractAndRepairFwscContainer(
            rawBytes = dummyFwsc,
            fileName = "FM-1_Official_Update.fwsc"
        )
        assertTrue(surgeonReport.hadFwscContainerHeader)
        assertEquals(16384 + 20, surgeonReport.strippedHeaderBytes)
        assertEquals(8192, surgeonReport.extractedAppPayload.size)
        assertEquals(16384, surgeonReport.goldenBootHeader16Kb.size)

        // 5. Verify Full 1MB (1,048,576 bytes) SPI NOR Flash Dump Builder for CH341A
        val full1Mb = ChecksumEngine.buildFull1MbSpiFlashDump(
            firmwareId = "mvave_fm1_stock_v16",
            firmwareName = "Official Stock V16",
            firmwareVersion = "V16",
            appPayloadBytes = surgeonReport.extractedAppPayload
        )
        assertEquals(1048576, full1Mb.sizeBytes)
        assertTrue(full1Mb.fileNameSuggested.endsWith("_CH341A.bin"))
    }

    @Test
    fun `all 160 firmwares have unique bundled screen images in assets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val entities = com.example.data.TheFirmwareHubCatalogData.CACHED_ENTITIES
        assertEquals(160, entities.size)

        val uniquePaths = mutableSetOf<String>()
        entities.forEach { fw ->
            val assetPath = com.example.ui.components.resolveFirmwareScreenAssetPath(fw)
            assertTrue("Duplicate asset path $assetPath", uniquePaths.add(assetPath))
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            assertTrue("Empty screen image for ${fw.id}", bytes.size > 100)
        }
        assertEquals(160, uniquePaths.size)
    }
}
