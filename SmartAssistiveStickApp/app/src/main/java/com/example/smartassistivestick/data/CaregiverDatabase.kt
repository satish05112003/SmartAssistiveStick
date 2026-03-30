package com.example.smartassistivestick.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Caregiver::class], version = 1, exportSchema = false)
abstract class CaregiverDatabase : RoomDatabase() {
    abstract fun caregiverDao(): CaregiverDao

    companion object {
        @Volatile
        private var INSTANCE: CaregiverDatabase? = null

        fun getDatabase(context: Context): CaregiverDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaregiverDatabase::class.java,
                    "caregiver_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
