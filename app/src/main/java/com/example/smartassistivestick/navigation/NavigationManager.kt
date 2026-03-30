package com.example.smartassistivestick.navigation

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Navigation Manager for Google Maps integration
 * Handles route planning, waypoints, and voice turn-by-turn guidance
 */
class NavigationManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("navigation_prefs", Context.MODE_PRIVATE)
    
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _savedLocations = MutableStateFlow<List<SavedLocation>>(emptyList())
    val savedLocations: StateFlow<List<SavedLocation>> = _savedLocations
    
    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute: StateFlow<Route?> = _currentRoute
    
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep
    
    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating

    companion object {
        private const val TAG = "NavigationManager"
        private const val SAVED_LOCATIONS_KEY = "saved_locations"
    }

    data class SavedLocation(
        val id: String = "",
        val name: String = "",
        val lat: Double = 0.0,
        val lng: Double = 0.0,
        val type: String = "custom" // home, hospital, workplace, custom
    )

    data class Route(
        val origin: LatLng,
        val destination: LatLng,
        val steps: List<NavigationStep>,
        val totalDistance: Double = 0.0,
        val totalDuration: Long = 0L,
        val createdAt: Long = 0L
    )

    data class NavigationStep(
        val instruction: String,
        val distance: Double,
        val duration: Long,
        val location: LatLng,
        val turnType: String = "" // left, right, straight, arrive
    )

    /**
     * Save a location for quick access
     */
    suspend fun saveLocation(
        name: String,
        lat: Double,
        lng: Double,
        type: String = "custom"
    ): SavedLocation {
        val location = SavedLocation(
            id = "${name}_${System.currentTimeMillis()}",
            name = name,
            lat = lat,
            lng = lng,
            type = type
        )
        
        try {
            // Save to Firebase
            firestore.collection("saved_locations")
                .document(location.id)
                .set(location)
                .await()
            
            Log.d(TAG, "Location saved: $name at ($lat, $lng)")
            loadSavedLocations()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving location", e)
        }
        
        return location
    }

    /**
     * Load all saved locations
     */
    suspend fun loadSavedLocations() {
        try {
            val docs = firestore.collection("saved_locations").get().await()
            val locations = docs.documents.mapNotNull { doc ->
                try {
                    SavedLocation(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        lat = doc.getDouble("lat") ?: 0.0,
                        lng = doc.getDouble("lng") ?: 0.0,
                        type = doc.getString("type") ?: "custom"
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            _savedLocations.value = locations
            Log.d(TAG, "Loaded ${locations.size} saved locations")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved locations", e)
        }
    }

    /**
     * Start navigation to a location
     */
    fun startNavigation(destination: SavedLocation) {
        try {
            // Create a simple route
            val steps = generateNavigationSteps(destination)
            
            val route = Route(
                origin = LatLng(0.0, 0.0), // Will be set from actual location in ViewModel
                destination = LatLng(destination.lat, destination.lng),
                steps = steps,
                totalDistance = steps.sumOf { it.distance },
                totalDuration = steps.sumOf { it.duration },
                createdAt = System.currentTimeMillis()
            )
            
            _currentRoute.value = route
            _currentStep.value = 0
            _isNavigating.value = true
            
            Log.d(TAG, "Navigation started to ${destination.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting navigation", e)
        }
    }

    /**
     * Get next navigation instruction
     */
    fun getNextInstruction(): String {
        val route = _currentRoute.value ?: return ""
        val step = _currentStep.value
        
        return if (step < route.steps.size) {
            route.steps[step].instruction
        } else {
            "Navigation complete"
        }
    }

    /**
     * Get current turn type
     */
    fun getCurrentTurnType(): String {
        val route = _currentRoute.value ?: return ""
        val step = _currentStep.value
        
        return if (step < route.steps.size) {
            route.steps[step].turnType
        } else {
            ""
        }
    }

    /**
     * Advance to next navigation step
     */
    fun advanceStep() {
        val route = _currentRoute.value ?: return
        
        if (_currentStep.value < route.steps.size - 1) {
            _currentStep.value = _currentStep.value + 1
            Log.d(TAG, "Advanced to step ${_currentStep.value}")
        } else {
            endNavigation()
        }
    }

    /**
     * End navigation
     */
    fun endNavigation() {
        _isNavigating.value = false
        _currentRoute.value = null
        _currentStep.value = 0
        
        Log.d(TAG, "Navigation ended")
    }

    /**
     * Get distance to next waypoint
     */
    fun getDistanceToNext(): Double {
        val route = _currentRoute.value ?: return 0.0
        val step = _currentStep.value
        
        return if (step < route.steps.size) {
            route.steps[step].distance
        } else {
            0.0
        }
    }

    /**
     * Delete saved location
     */
    suspend fun deleteLocation(locationId: String): Boolean {
        return try {
            firestore.collection("saved_locations").document(locationId).delete().await()
            loadSavedLocations()
            Log.d(TAG, "Location deleted: $locationId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting location", e)
            false
        }
    }

    private fun generateNavigationSteps(destination: SavedLocation): List<NavigationStep> {
        // This would normally call Google Maps Directions API
        // For now, return placeholder steps
        return listOf(
            NavigationStep(
                instruction = "Head towards destination",
                distance = 1000.0,
                duration = 300000,
                location = LatLng(destination.lat, destination.lng),
                turnType = "straight"
            ),
            NavigationStep(
                instruction = "Arriving at destination",
                distance = 0.0,
                duration = 0,
                location = LatLng(destination.lat, destination.lng),
                turnType = "arrive"
            )
        )
    }

    fun getHomeLocation(): SavedLocation? = _savedLocations.value.find { it.type == "home" }
    
    fun getHospitalLocation(): SavedLocation? = _savedLocations.value.find { it.type == "hospital" }
    
    fun getWorkplaceLocation(): SavedLocation? = _savedLocations.value.find { it.type == "workplace" }
}
