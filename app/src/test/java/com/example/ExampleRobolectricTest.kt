package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FirmwareRepository
import com.example.domain.DeviceDiagnosticVerdict
import com.example.domain.UsbFlashEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

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
    fun `repository seeds and materializes all 160 firmwares locally`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = FirmwareRepository(db.appDao(), context.filesDir)

        repo.ensureSeeded()
        val snapshot = db.appDao().getAllFirmwaresSnapshot()
        assertEquals(160, snapshot.size)

        snapshot.forEach { fw ->
            assertTrue(fw.isDownloaded)
            assertTrue(fw.localFilePath.isNotBlank())
            assertTrue(File(fw.localFilePath).exists())
        }
        db.close()
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

