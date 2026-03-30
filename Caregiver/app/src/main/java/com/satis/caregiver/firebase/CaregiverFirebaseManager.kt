package com.satis.caregiver.firebase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.satis.caregiver.model.AlertEvent
import com.satis.caregiver.model.UserData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CaregiverFirebaseManager {
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    private val caregiversRef = database.getReference("caregivers")

    suspend fun getLinkedUserId(caregiverId: String): String? {
        return try {
            val snapshot = caregiversRef.child(caregiverId).child("linkedUserId").get().await()
            snapshot.getValue(String::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching linked user", e)
            null
        }
    }

    suspend fun linkCaregiver(caregiverId: String, accessKey: String): String? {
        return try {
            val snapshot = usersRef.orderByChild("accessKey").equalTo(accessKey).get().await()
            if (snapshot.exists()) {
                val linkedUserId = snapshot.children.first().key
                if (linkedUserId != null) {
                    caregiversRef.child(caregiverId).child("linkedUserId").setValue(linkedUserId).await()
                    linkedUserId
                } else null
            } else null
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to link caregiver using key: $accessKey", e)
            null
        }
    }

    suspend fun unlinkCaregiver(caregiverId: String) {
        try {
            caregiversRef.child(caregiverId).child("linkedUserId").removeValue().await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to unlink caregiver: $caregiverId", e)
        }
    }

    fun getUserDataFlow(userId: String): Flow<UserData?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val data = snapshot.getValue(UserData::class.java)
                        trySend(data)
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "Database parse error", e)
                        trySend(null)
                    }
                } else {
                    Log.e("FirebaseManager", "No user data found")
                    close(Exception("No user data found"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseManager", "Database cancelled: ${error.message}")
                close(Exception(error.message))
            }
        }
        val ref = usersRef.child(userId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAlertsFlow(userId: String): Flow<List<AlertEvent>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alerts = mutableListOf<AlertEvent>()
                for (child in snapshot.children) {
                    try {
                        child.getValue(AlertEvent::class.java)?.let { alerts.add(it) }
                    } catch (e: Exception) {
                        Log.e("FirebaseManager", "Failed parsing alert", e)
                    }
                }
                alerts.sortByDescending { it.timestamp }
                trySend(alerts)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseManager", "Alerts cancelled: ${error.message}")
            }
        }
        val ref = database.getReference("alerts").child(userId)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
