package com.example.smartassistivestick

import android.app.Application
import android.util.Log
import com.example.smartassistivestick.data.CaregiverDatabase
import com.google.firebase.FirebaseApp

class SmartAssistiveApp : Application() {
    val database by lazy { CaregiverDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d("APP_DEBUG", "Step reached: FirebaseApp initialized")
        } catch (e: Exception) {
            Log.e("APP_DEBUG", "FirebaseApp init failed", e)
        }
    }
}
