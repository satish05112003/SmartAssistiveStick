package com.example.smartassistivestick.firebase

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class AccessKeyGenerator(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val usersRef = database.getReference("users")

    suspend fun generateUniqueAccessKey(maxAttempts: Int = 50): String {
        repeat(maxAttempts) {
            val key = Random.nextInt(100000, 1000000).toString()
            val snapshot = usersRef.orderByChild("accessKey").equalTo(key).get().await()
            if (!snapshot.exists()) {
                return key
            }
        }
        throw IllegalStateException("Unable to generate unique access key after $maxAttempts attempts")
    }
}
