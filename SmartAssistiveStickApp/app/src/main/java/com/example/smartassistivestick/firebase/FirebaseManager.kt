package com.example.smartassistivestick.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await

data class UserLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Long = 0L
)

class FirebaseManager(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val usersRef = database.getReference("users")
    private val caregiversRef = database.getReference("caregivers")

    suspend fun createOrUpdateUser(userId: String, name: String = "Smart Assistive User") {
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "name" to name,
            "mode" to "OUTDOOR",
            "lastUpdated" to now
        )
        usersRef.child(userId).updateChildren(updates).await()
    }

    suspend fun saveAccessKey(userId: String, accessKey: String) {
        usersRef.child(userId).child("accessKey").setValue(accessKey).await()
    }

    suspend fun getAccessKey(userId: String): String? {
        val snapshot = usersRef.child(userId).child("accessKey").get().await()
        return snapshot.getValue(String::class.java)
    }

    suspend fun linkCaregiverByAccessKey(caregiverId: String, accessKey: String): Result<String> {
        return try {
            val snapshot = usersRef.orderByChild("accessKey").equalTo(accessKey).get().await()
            val linkedUserId = snapshot.children.firstOrNull()?.key
                ?: return Result.failure(IllegalArgumentException("Invalid access key"))

            caregiversRef.child(caregiverId).child("linkedUserId").setValue(linkedUserId).await()
            Result.success(linkedUserId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeCaregiver(caregiverId: String) {
        caregiversRef.child(caregiverId).removeValue().await()
    }

    suspend fun updateMode(userId: String, mode: String) {
        val updates = mapOf<String, Any>(
            "mode" to mode,
            "lastUpdated" to System.currentTimeMillis()
        )
        usersRef.child(userId).updateChildren(updates).await()
    }

    suspend fun updateLocation(userId: String, latitude: Double, longitude: Double) {
        val now = System.currentTimeMillis()
        val updates = mapOf<String, Any>(
            "latitude" to latitude,
            "longitude" to longitude,
            "lastUpdated" to now,
            "location/latitude" to latitude,
            "location/longitude" to longitude,
            "location/lastUpdated" to now
        )
        usersRef.child(userId).updateChildren(updates).await()
    }

    fun observeLinkedUserId(caregiverId: String, onChange: (String?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.child("linkedUserId").getValue(String::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onChange(null)
            }
        }
        caregiversRef.child(caregiverId).addValueEventListener(listener)
        return listener
    }

    fun observeUserLocation(userId: String, onChange: (UserLocation?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("location").child("latitude").getValue(Double::class.java)
                    ?: snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("location").child("longitude").getValue(Double::class.java)
                    ?: snapshot.child("longitude").getValue(Double::class.java)
                val updated = snapshot.child("location").child("lastUpdated").getValue(Long::class.java)
                    ?: snapshot.child("lastUpdated").getValue(Long::class.java)
                    ?: 0L

                if (lat != null && lng != null) {
                    onChange(UserLocation(latitude = lat, longitude = lng, lastUpdated = updated))
                } else {
                    onChange(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onChange(null)
            }
        }
        usersRef.child(userId).addValueEventListener(listener)
        return listener
    }
}
