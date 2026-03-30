package com.example.smartassistivestick.caregiver

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/**
 * Manages caregiver access codes and linking system
 * Provides secure linking between users and caregivers
 */
class CaregiverManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _accessCode = MutableStateFlow("")
    val accessCode: StateFlow<String> = _accessCode
    
    private val _linkedCaregivers = MutableStateFlow<List<CaregiverAccess>>(emptyList())
    val linkedCaregivers: StateFlow<List<CaregiverAccess>> = _linkedCaregivers
    
    private val _accessRequests = MutableStateFlow<List<AccessRequest>>(emptyList())
    val accessRequests: StateFlow<List<AccessRequest>> = _accessRequests

    companion object {
        private const val TAG = "CaregiverManager"
        private const val USERS_COLLECTION = "users"
        private const val CAREGIVERS_COLLECTION = "caregivers"
        private const val ACCESS_CODES_COLLECTION = "access_codes"
        private const val ACCESS_REQUESTS_COLLECTION = "access_requests"
        private const val ACCESS_CODE_EXPIRY_HOURS = 24L
    }

    data class CaregiverAccess(
        val caregiverId: String = "",
        val caregiverName: String = "",
        val caregiverPhone: String = "",
        val linkedAt: Long = 0L,
        val expiresAt: Long? = null,
        val isActive: Boolean = true
    )

    data class AccessRequest(
        val caregiverId: String = "",
        val caregiverName: String = "",
        val caregiverEmail: String = "",
        val requestedAt: Long = 0L,
        val status: String = "pending" // pending, accepted, rejected
    )

    /**
     * Generate a unique 6-digit access code
     */
    suspend fun generateAccessCode(): String {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        
        var code = ""
        var isUnique = false
        
        while (!isUnique) {
            code = generateRandomCode()
            
            // Check if code is unique in Firestore
            val existing = firestore.collection(ACCESS_CODES_COLLECTION)
                .whereEqualTo("code", code)
                .get()
                .await()
            
            isUnique = existing.isEmpty
        }
        
        // Save the code with expiry
        val expiryTime = System.currentTimeMillis() + (ACCESS_CODE_EXPIRY_HOURS * 60 * 60 * 1000)
        
        firestore.collection(ACCESS_CODES_COLLECTION).document(code).set(
            mapOf(
                "code" to code,
                "userId" to userId,
                "createdAt" to System.currentTimeMillis(),
                "expiresAt" to expiryTime,
                "isUsed" to false
            )
        ).await()
        
        _accessCode.value = code
        Log.d(TAG, "Access code generated: $code")
        
        return code
    }

    /**
     * Caregiver links using access code
     */
    suspend fun caregiverLinkWithCode(
        code: String,
        caregiverName: String,
        caregiverEmail: String,
        caregiverPhone: String
    ): Boolean {
        return try {
            // Verify code exists and is not expired
            val codeDoc = firestore.collection(ACCESS_CODES_COLLECTION).document(code).get().await()
            
            if (!codeDoc.exists()) {
                Log.e(TAG, "Access code not found: $code")
                return false
            }
            
            val isUsed = codeDoc.getBoolean("isUsed") ?: false
            if (isUsed) {
                Log.e(TAG, "Access code already used: $code")
                return false
            }
            
            val expiresAt = codeDoc.getLong("expiresAt") ?: 0L
            if (System.currentTimeMillis() > expiresAt) {
                Log.e(TAG, "Access code expired: $code")
                return false
            }
            
            val userId = codeDoc.getString("userId") ?: throw Exception("Invalid code data")
            val caregiverId = auth.currentUser?.uid ?: throw Exception("Caregiver not authenticated")
            
            // Add caregiver to user's caregivers list
            firestore.collection(USERS_COLLECTION).document(userId)
                .collection("caregivers").document(caregiverId).set(
                    mapOf(
                        "caregiverId" to caregiverId,
                        "caregiverName" to caregiverName,
                        "caregiverEmail" to caregiverEmail,
                        "caregiverPhone" to caregiverPhone,
                        "linkedAt" to System.currentTimeMillis(),
                        "isActive" to true
                    )
                ).await()
            
            // Add user to caregiver's users list
            firestore.collection(CAREGIVERS_COLLECTION).document(caregiverId)
                .collection("users").document(userId).set(
                    mapOf(
                        "userId" to userId,
                        "linkedAt" to System.currentTimeMillis(),
                        "isActive" to true
                    )
                ).await()
            
            // Mark code as used
            firestore.collection(ACCESS_CODES_COLLECTION).document(code).update(
                mapOf("isUsed" to true, "usedBy" to caregiverId, "usedAt" to System.currentTimeMillis())
            ).await()
            
            Log.d(TAG, "Caregiver linked successfully with code: $code")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error linking caregiver", e)
            false
        }
    }

    /**
     * Load all linked caregivers for current user
     */
    suspend fun loadLinkedCaregivers() {
        try {
            val userId = auth.currentUser?.uid ?: return
            
            val caregiverDocs = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection("caregivers")
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val caregivers = caregiverDocs.documents.map { doc ->
                CaregiverAccess(
                    caregiverId = doc.getString("caregiverId") ?: "",
                    caregiverName = doc.getString("caregiverName") ?: "",
                    caregiverPhone = doc.getString("caregiverPhone") ?: "",
                    linkedAt = doc.getLong("linkedAt") ?: 0L,
                    expiresAt = doc.getLong("expiresAt"),
                    isActive = doc.getBoolean("isActive") ?: true
                )
            }
            
            _linkedCaregivers.value = caregivers
            Log.d(TAG, "Loaded ${caregivers.size} caregivers")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading caregivers", e)
        }
    }

    /**
     * Revoke caregiver access
     */
    suspend fun revokeCaregiverAccess(caregiverId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            // Mark caregiver as inactive
            firestore.collection(USERS_COLLECTION).document(userId)
                .collection("caregivers").document(caregiverId)
                .update(mapOf("isActive" to false))
                .await()
            
            // Mark user as inactive in caregiver's list
            firestore.collection(CAREGIVERS_COLLECTION).document(caregiverId)
                .collection("users").document(userId)
                .update(mapOf("isActive" to false))
                .await()
            
            Log.d(TAG, "Access revoked for caregiver: $caregiverId")
            
            // Reload caregivers list
            loadLinkedCaregivers()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking access", e)
            false
        }
    }

    /**
     * Revoke all caregiver access
     */
    suspend fun revokeAllAccess(): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            
            _linkedCaregivers.value.forEach { caregiver ->
                firestore.collection(USERS_COLLECTION).document(userId)
                    .collection("caregivers").document(caregiver.caregiverId)
                    .update(mapOf("isActive" to false))
                    .await()
            }
            
            Log.d(TAG, "All caregiver access revoked")
            loadLinkedCaregivers()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking all access", e)
            false
        }
    }

    /**
     * Set temporary access with expiry time
     */
    suspend fun setTemporaryAccess(caregiverId: String, expiryHours: Long): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val expiryTime = System.currentTimeMillis() + (expiryHours * 60 * 60 * 1000)
            
            firestore.collection(USERS_COLLECTION).document(userId)
                .collection("caregivers").document(caregiverId)
                .update(mapOf("expiresAt" to expiryTime))
                .await()
            
            Log.d(TAG, "Temporary access set for caregiver: $caregiverId, expires in $expiryHours hours")
            loadLinkedCaregivers()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting temporary access", e)
            false
        }
    }

    /**
     * Check if access is still valid
     */
    fun isAccessValid(caregiver: CaregiverAccess): Boolean {
        if (!caregiver.isActive) return false
        
        caregiver.expiresAt?.let {
            if (System.currentTimeMillis() > it) {
                return false
            }
        }
        
        return true
    }

    /**
     * Get caregiver by ID
     */
    fun getCaregiverById(caregiverId: String): CaregiverAccess? {
        return _linkedCaregivers.value.find { it.caregiverId == caregiverId }
    }

    private fun generateRandomCode(): String {
        return (100000..999999).random().toString()
    }
}
