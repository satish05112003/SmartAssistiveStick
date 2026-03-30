package com.satis.caregiver.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth by lazy { FirebaseAuth.getInstance() }

    suspend fun signInAnonymously(): String? {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.uid
            } else {
                val result = auth.signInAnonymously().await()
                result.user?.uid
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
