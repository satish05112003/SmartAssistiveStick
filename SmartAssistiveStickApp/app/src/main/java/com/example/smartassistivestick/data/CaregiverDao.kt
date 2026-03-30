package com.example.smartassistivestick.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaregiverDao {
    @Query("SELECT * FROM caregivers")
    fun getAllCaregivers(): Flow<List<Caregiver>>

    @Query("SELECT * FROM caregivers")
    suspend fun getAllCaregiversSync(): List<Caregiver>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(caregiver: Caregiver)

    @Delete
    suspend fun delete(caregiver: Caregiver)
}
