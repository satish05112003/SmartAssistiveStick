package com.example.smartassistivestick.firebase

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthManager(context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun signInAnonymouslyIfNeeded(): Result<String> {
        return try {
            val current = auth.currentUser
            if (current != null) {
                storeUid(current.uid)
                Result.success(current.uid)
            } else {
                val result = auth.signInAnonymously().await()
                val uid = result.user?.uid ?: return Result.failure(IllegalStateException("Anonymous auth returned null user"))
                storeUid(uid)
                Result.success(uid)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUid(): String? = auth.currentUser?.uid

    fun getStoredUid(): String? = prefs.getString(KEY_UID, null)

    private fun storeUid(uid: String) {
        prefs.edit().putString(KEY_UID, uid).apply()
    }

    companion object {
        private const val PREFS_NAME = "firebase_auth_prefs"
        private const val KEY_UID = "firebase_uid"
    }
}
