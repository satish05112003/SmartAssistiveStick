package com.example.smartassistivestick.location

import android.app.Service
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Background service for continuous location tracking
 * Updates location to Firebase in real-time for caregiver access
 */
class LocationTrackingService : Service() {

    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    private val binder = LocationBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation
    
    private val _lastUpdateTime = MutableStateFlow(0L)
    val lastUpdateTime: StateFlow<Long> = _lastUpdateTime

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val NOTIFICATION_ID = 1002
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
        private const val FASTEST_LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationTrackingService created")
        
        fusedLocationClient = com.google.android.gms.location.LocationServices
            .getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            Log.d(TAG, "LocationTrackingService started")
            startForegroundService()

            val hasLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                startLocationUpdates()
            } else {
                Log.e(TAG, "Location permission missing. Service will run without updates.")
            }

            START_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "LocationTrackingService start failed", e)
            START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationTrackingService destroyed")
        
        stopLocationUpdates()
    }

    /**
     * Start receiving location updates
     */
    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission) {
            Log.e(TAG, "Cannot request updates: location permission missing")
            return
        }

        val locationRequest = LocationRequest.Builder(LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_LOCATION_UPDATE_INTERVAL)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                
                for (location in locationResult.locations) {
                    _currentLocation.value = Pair(location.latitude, location.longitude)
                    _lastUpdateTime.value = System.currentTimeMillis()
                    
                    Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
                    
                    // Update location in Firebase
                    updateLocationInFirebase(location.latitude, location.longitude)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location updates", e)
        }
    }

    /**
     * Stop receiving location updates
     */
    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        Log.d(TAG, "Location updates stopped")
    }

    /**
     * Update location in Firebase for caregiver access
     */
    private fun updateLocationInFirebase(latitude: Double, longitude: Double) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            firestore.collection("users").document(userId)
                .collection("location").document("current")
                .set(
                    mapOf(
                        "lat" to latitude,
                        "lng" to longitude,
                        "timestamp" to System.currentTimeMillis(),
                        "accuracy" to "high"
                    )
                )
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating location in Firebase", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating location", e)
        }
    }

    /**
     * Get last known location
     */
    @Suppress("MissingPermission")
    fun getLastLocation(callback: (Double, Double) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                callback(location.latitude, location.longitude)
            }
        }
    }

    /**
     * Start foreground service with notification
     */
    private fun startForegroundService() {
        val channelId = "location_tracking_channel"
        
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Location Tracking",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        
        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Smart Assistive Stick")
            .setContentText("Location tracking enabled")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
}
