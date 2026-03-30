package com.satis.caregiver.model

data class UserData(
    val accessKey: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val mode: String = "INDOOR", // "INDOOR" or "OUTDOOR"
    val sosActive: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val batteryLevel: Int = 100
)

data class AlertEvent(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "", // "OBSTACLE", "MODE_CHANGE", "SOS", "GEOFENCE"
    val severity: String = "INFO", // "INFO", "WARNING", "DANGER"
    val message: String = ""
)
