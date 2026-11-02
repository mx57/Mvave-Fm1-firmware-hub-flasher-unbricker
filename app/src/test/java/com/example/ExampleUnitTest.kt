package com.example

import com.example.domain.ChecksumEngine
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
    fun checksumEngine_detectsRawFwscAtZeroOffsetAndAutoProtectsTo0x4000() {
        val raw = "JLWL82FM_RAW_PACKAGE".toByteArray()
        val report = ChecksumEngine.analyzeBinary(
            bytes = raw,
            fileName = "unsafe_package.fwsc",
            requestedOffsetHex = "0x0000"
        )

        assertTrue(report.startsWithBootHeaderHazard)
        assertTrue(report.targetOffsetHex.contains("0x00004000"))
    }
}
