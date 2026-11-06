package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FirmwareEntity::class, BackupDumpEntity::class, FlashLogEntity::class],
    version = 5,
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

        /**
         * Returns the complete 160-firmware catalog from TheFirmwareHub.com
         * (148 FM-1 firmwares + 9 SMK-37 firmwares + Baud Girl FM-1+VA + FM1 Open V14 + Official Stock V15/V16).
         */
        fun buildDefaultCatalog(): List<FirmwareEntity> {
            return TheFirmwareHubCatalogData.CACHED_ENTITIES
        }
    }
}
