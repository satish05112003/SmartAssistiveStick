package com.example.smartassistivestick.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "caregivers")
data class Caregiver(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String
)
