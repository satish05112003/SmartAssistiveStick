package com.satis.caregiver.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satis.caregiver.auth.AuthManager
import com.satis.caregiver.firebase.CaregiverFirebaseManager
import com.satis.caregiver.model.AlertEvent
import com.satis.caregiver.model.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class AppState {
    object Idle : AppState()
    object Loading : AppState()
    data class Linked(val linkedUserId: String) : AppState()
    data class Error(val message: String) : AppState()
}

class CaregiverViewModel(
    private val authManager: AuthManager,
    private val firebaseManager: CaregiverFirebaseManager
) : ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Idle)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    private val _alerts = MutableStateFlow<List<AlertEvent>>(emptyList())
    val alerts: StateFlow<List<AlertEvent>> = _alerts.asStateFlow()

    init {
        initializeCaregiver()
    }

    private fun initializeCaregiver() {
        viewModelScope.launch {
            _appState.value = AppState.Loading

            // Log state
            android.util.Log.d("CARE", "State = ${_appState.value}")

            // Add fallback timeout
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                if (_appState.value is AppState.Loading) {
                    _appState.value = AppState.Error("No data found. Please link account.")
                }
            }

            val uid = authManager.signInAnonymously()
            if (uid != null) {
                // Force test userId
                val userId = "testUser123"
                _appState.value = AppState.Linked(userId)
                startObservingUser(userId)
            } else {
                _appState.value = AppState.Error("Failed to authenticate.")
            }
        }
    }

    private suspend fun checkExistingLink(caregiverId: String) {
        val linkedUserId = firebaseManager.getLinkedUserId(caregiverId)
        if (linkedUserId != null) {
            _appState.value = AppState.Linked(linkedUserId)
            startObservingUser(linkedUserId)
        } else {
            _appState.value = AppState.Idle
        }
    }

    fun linkWithAccessKey(accessKey: String) {
        viewModelScope.launch {
            _appState.value = AppState.Loading
            val caregiverId = authManager.getCurrentUserId() ?: return@launch
            val linkedUserId = firebaseManager.linkCaregiver(caregiverId, accessKey)
            if (linkedUserId != null) {
                _appState.value = AppState.Linked(linkedUserId)
                startObservingUser(linkedUserId)
            } else {
                _appState.value = AppState.Error("Invalid access key or user not found.")
            }
        }
    }

    fun unlink() {
        viewModelScope.launch {
            val caregiverId = authManager.getCurrentUserId() ?: return@launch
            firebaseManager.unlinkCaregiver(caregiverId)
            _userData.value = null
            _alerts.value = emptyList()
            _appState.value = AppState.Idle
        }
    }

    private fun startObservingUser(userId: String) {
        viewModelScope.launch {
            firebaseManager.getUserDataFlow(userId)
                .catch { e -> _appState.value = AppState.Error(e.message ?: "Unknown error") }
                .collect { data ->
                    _userData.value = data
                }
        }

        viewModelScope.launch {
            firebaseManager.getAlertsFlow(userId)
                .catch { /* handle error */ }
                .collect { alertsList ->
                    _alerts.value = alertsList
                }
        }
    }

    // Determine current user danger state
    fun isDangerState(): Boolean {
        return userData.value?.sosActive == true
    }
}
