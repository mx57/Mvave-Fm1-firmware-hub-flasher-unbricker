package com.example

import com.example.data.AppDatabase
import com.example.data.FirmwareVersionHistoryCodec
import com.example.domain.ActiveWireProtocol
import com.example.domain.ChecksumEngine
import com.example.domain.SpiPinShortVector
import com.example.domain.UnbrickMethodType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun checksumEngine_verifiesDeterministicFirmwareBinaryAndProtectsBootHeader() {
        val binary = ChecksumEngine.buildDeterministicFirmwareBinary(
            firmwareId = "mvave_fm1_stock_v16_unbrick",
            version = "v1.16",
            targetOffsetHex = "0x00004000",
            declaredSizeBytes = 32768
        )
        val report = ChecksumEngine.analyzeBinary(
            bytes = binary,
            fileName = "mvave_fm1_stock_v16_unbrick.bin",
            requestedOffsetHex = "0x00004000"
        )

        assertEquals(64, report.sha256.length)
        assertEquals(8, report.crc32.length)
        assertEquals(32, report.md5.length)
        assertTrue(report.isValidJieLiPartition)
        assertFalse(report.startsWithBootHeaderHazard)
        assertEquals("0x00004000", report.targetOffsetHex)
    }

    @Test
    fun checksumEngine_unpacksFwsc20MarkerBytesAndAutoProtectsTo0x4000() {
        // Build a synthetic .fwsc with 20 blocks of 0x30 bytes (960 bytes) + 100 tail bytes
        val logicalExpected = ByteArray(20 * 0x2F + 100) { idx -> ((idx * 7) and 0xFF).toByte() }
        val packedFwsc = ByteArray(20 * 0x30 + 100)
        var srcPos = 0
        var dstPos = 0
        for (block in 0 until 20) {
            System.arraycopy(logicalExpected, srcPos, packedFwsc, dstPos, 0x2F)
            packedFwsc[dstPos + 0x2F] = 0xA5.toByte() // .fwsc container marker at index 0x2F
            srcPos += 0x2F
            dstPos += 0x30
        }
        System.arraycopy(logicalExpected, srcPos, packedFwsc, dstPos, 100)

        val unpacked = ChecksumEngine.unpackFwscLogicalImage(packedFwsc)
        assertEquals(logicalExpected.size, unpacked.size)
        assertArrayEquals(logicalExpected, unpacked)

        val raw = "JLWL82FM_RAW_PACKAGE".toByteArray()
        val report = ChecksumEngine.analyzeBinary(
            bytes = raw,
            fileName = "unsafe_package.fwsc",
            requestedOffsetHex = "0x0000"
        )
        assertTrue(report.startsWithBootHeaderHazard)
        assertTrue(report.targetOffsetHex.contains("0x00004000"))
    }

    @Test
    fun defaultCatalog_containsFull160FirmwaresWithVersionHistoryAndHardwareRevs() {
        val catalog = AppDatabase.buildDefaultCatalog()
        assertEquals(160, catalog.size)
        catalog.forEach { fw ->
            assertTrue(fw.releaseNotes.isNotBlank())
            assertTrue(fw.fullDescription.isNotBlank())
            assertTrue(fw.compatibleHardwareRevs.isNotBlank())
            val versions = FirmwareVersionHistoryCodec.decode(fw.versionHistoryJson, fw)
            assertTrue("Expected version history for ${fw.id}", versions.isNotEmpty())
        }
    }

    @Test
    fun unbrickMethods_all6VerifiedVectorsAndDualProtocolsAreValid() {
        assertEquals(6, UnbrickMethodType.entries.size)
        UnbrickMethodType.entries.forEach { method ->
            assertFalse("Method ${method.name} must be no-solder", method.requiresSoldering)
            assertTrue(method.physicsExplanation.isNotBlank())
            assertTrue(method.troubleshootingTips.isNotBlank())
            assertEquals(4, method.steps.size)
        }
        assertEquals(3, SpiPinShortVector.entries.size)
        assertEquals(3, ActiveWireProtocol.entries.size)
        val ramLoader = ChecksumEngine.buildWl82RamLoaderBinary()
        assertEquals(4096, ramLoader.size)
        val goldenBoot = ChecksumEngine.buildGoldenBootloaderHeader16Kb()
        assertEquals(16384, goldenBoot.size)
    }

    @Test
    fun jieliCrc16Ccitt_computesDeterministicChecksumForPageFrames() {
        val testVector = "123456789".toByteArray(Charsets.US_ASCII)
        val crc = com.example.domain.UsbFlashEngine.computeCrc16Ccitt(testVector)
        assertEquals(0x31C3, crc)

        val page512 = ByteArray(512) { 0xFF.toByte() }
        val crcPage = com.example.domain.UsbFlashEngine.computeCrc16Ccitt(page512)
        assertTrue(crcPage in 0..0xFFFF)
    }

    @Test
    fun jieliScsiCbw_buildsValid31ByteBotFrameWithOpcode() {
        val cdb = com.example.domain.UsbFlashEngine.buildJieLiCdb(
            cmd = com.example.domain.UsbFlashEngine.JL_CMD_LOADER_WRITE_FLASH,
            args = byteArrayOf(0x00, 0x00, 0x40, 0x00)
        )
        assertEquals(16, cdb.size)
        assertEquals(0xFB.toByte(), cdb[0])
        assertEquals(0x04.toByte(), cdb[1])

        val cbw = com.example.domain.UsbFlashEngine.buildScsiCbwPacket(
            tag = 100,
            transferLength = 512,
            isDirectionIn = false,
            cdb = cdb
        )
        assertEquals(31, cbw.size)
        val sig = java.nio.ByteBuffer.wrap(cbw, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        assertEquals(com.example.domain.UsbFlashEngine.SCSI_CBW_SIGNATURE, sig)
    }
}
