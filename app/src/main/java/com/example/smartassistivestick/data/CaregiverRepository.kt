package com.example.smartassistivestick.data

import kotlinx.coroutines.flow.Flow

class CaregiverRepository(private val caregiverDao: CaregiverDao) {
    val allCaregivers: Flow<List<Caregiver>> = caregiverDao.getAllCaregivers()

    suspend fun getAllCaregiversSync(): List<Caregiver> {
        return caregiverDao.getAllCaregiversSync()
    }

    suspend fun insert(caregiver: Caregiver) {
        caregiverDao.insert(caregiver)
    }

    suspend fun delete(caregiver: Caregiver) {
        caregiverDao.delete(caregiver)
    }
}
