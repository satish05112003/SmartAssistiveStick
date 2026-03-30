package com.example.smartassistivestick.location

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class LocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    companion object {
        private const val TAG = "APP_DEBUG"
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): String? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission missing in getCurrentLocation")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val addressText = address.getAddressLine(0) ?: "Latitude ${location.latitude}, Longitude ${location.longitude}"
                        continuation.resume(addressText)
                    } else {
                        continuation.resume("Latitude ${location.latitude}, Longitude ${location.longitude}")
                    }
                } catch (e: Exception) {
                    continuation.resume("Latitude ${location.latitude}, Longitude ${location.longitude}")
                }
            } else {
                continuation.resume(null)
            }
        }.addOnFailureListener {
            Log.e(TAG, "getCurrentLocation failed", it)
            continuation.resume(null)
        }
    }
    
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationMapLink(): String? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission missing in getCurrentLocationMapLink")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume("https://maps.google.com/?q=${location.latitude},${location.longitude}")
            } else {
                continuation.resume(null)
            }
        }.addOnFailureListener {
            Log.e(TAG, "getCurrentLocationMapLink failed", it)
            continuation.resume(null)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLatLng(): Pair<Double, Double>? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission missing in getCurrentLatLng")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume(location.latitude to location.longitude)
            } else {
                continuation.resume(null)
            }
        }.addOnFailureListener {
            Log.e(TAG, "getCurrentLatLng failed", it)
            continuation.resume(null)
        }
    }
}
