package com.satis.caregiver

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.satis.caregiver.auth.AuthManager
import com.satis.caregiver.firebase.CaregiverFirebaseManager
import com.satis.caregiver.ui.CaregiverAppScreen
import com.satis.caregiver.ui.CaregiverViewModel
import com.satis.caregiver.ui.theme.CaregiverTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        // Initialize dependencies (In a real app, use Hilt/Dagger for DI)
        val authManager = AuthManager()
        val firebaseManager = CaregiverFirebaseManager()
        val viewModel = CaregiverViewModel(authManager, firebaseManager)

        setContent {
            CaregiverTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CaregiverAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}
