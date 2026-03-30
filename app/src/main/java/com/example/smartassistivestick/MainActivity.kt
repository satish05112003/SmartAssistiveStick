package com.example.smartassistivestick

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.smartassistivestick.ui.MainScreen
import com.example.smartassistivestick.ui.theme.SmartAssistiveStickTheme
import com.example.smartassistivestick.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasLocationPermission by mutableStateOf(false)

    companion object {
        private const val TAG = "APP_DEBUG"
    }

    private val requestMultiPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
            val smsGranted = permissions[Manifest.permission.SEND_SMS] == true
            val btConnectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
            } else {
                true
            }
            val btScanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true
            } else {
                true
            }

            hasLocationPermission = fineGranted
            val allRequiredGranted = fineGranted && audioGranted && smsGranted && btConnectGranted && btScanGranted

            Log.d(TAG, "Step reached: permissions callback, allRequiredGranted=$allRequiredGranted")
            viewModel.onRuntimePermissionsUpdated(
                allGranted = allRequiredGranted,
                locationGranted = fineGranted,
                audioGranted = audioGranted,
                bluetoothGranted = btConnectGranted && btScanGranted
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "Step reached: MainActivity.onCreate")

            val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val smsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
            val btConnectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            val btScanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            hasLocationPermission = fineGranted
            val allRequiredGranted = fineGranted && audioGranted && smsGranted && btConnectGranted && btScanGranted

            viewModel.onRuntimePermissionsUpdated(
                allGranted = allRequiredGranted,
                locationGranted = fineGranted,
                audioGranted = audioGranted,
                bluetoothGranted = btConnectGranted && btScanGranted
            )

            if (!allRequiredGranted) {
                requestPermissions()
            }

            setContent {
                SmartAssistiveStickTheme {
                    MainScreen(
                        viewModel = viewModel,
                        hasLocationPermission = hasLocationPermission,
                        onConnectClicked = {
                            viewModel.connectToBluetooth("00:14:22:01:23:45")
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity onCreate crash guarded", e)
        }
    }

    override fun onResume() {
        try {
            super.onResume()
            Log.d(TAG, "Step reached: MainActivity.onResume")
            viewModel.fetchLocation()
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity onResume crash guarded", e)
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        Log.d(TAG, "Step reached: requesting runtime permissions")
        requestMultiPermissionLauncher.launch(permissions.toTypedArray())
    }
}
