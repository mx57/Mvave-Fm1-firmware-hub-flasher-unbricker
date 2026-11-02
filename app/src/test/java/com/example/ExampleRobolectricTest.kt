package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.FirmwareRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
}
