package com.example.smartassistivestick.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.smartassistivestick.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocationSyncManager(
    context: Context,
    private val firebaseManager: FirebaseManager,
    private val locationManager: LocationManager,
    private val updateIntervalMs: Long = 5000L
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val retryMutex = Mutex()

    private var syncJob: Job? = null
    private var pendingLocation: Pair<Double, Double>? = null

    fun start(userId: String) {
        if (syncJob?.isActive == true) return

        syncJob = syncScope.launch {
            while (isActive) {
                val latLng = locationManager.getCurrentLatLng()
                if (latLng != null) {
                    val success = runCatching {
                        if (isOnline()) {
                            firebaseManager.updateLocation(userId, latLng.first, latLng.second)
                            flushPendingIfAny(userId)
                            true
                        } else {
                            false
                        }
                    }.getOrElse { false }

                    if (!success) {
                        pendingLocation = latLng
                    }
                }

                delay(updateIntervalMs)
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        syncJob = null
    }

    private suspend fun flushPendingIfAny(userId: String) {
        retryMutex.withLock {
            val pending = pendingLocation ?: return
            runCatching {
                firebaseManager.updateLocation(userId, pending.first, pending.second)
                pendingLocation = null
            }
        }
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
