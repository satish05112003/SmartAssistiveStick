package com.example.smartassistivestick.viewmodel

import android.app.Application
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartassistivestick.SmartAssistiveApp
import com.example.smartassistivestick.audio.AudioManagerHelper
import com.example.smartassistivestick.audio.SpeechManager
import com.example.smartassistivestick.audio.TTSManager
import com.example.smartassistivestick.bluetooth.BluetoothManager
import com.example.smartassistivestick.caregiver.CaregiverManager
import com.example.smartassistivestick.firebase.AccessKeyGenerator
import com.example.smartassistivestick.firebase.AuthManager
import com.example.smartassistivestick.firebase.FirebaseManager
import com.example.smartassistivestick.firebase.LocationSyncManager
import com.example.smartassistivestick.language.LanguageManager
import com.example.smartassistivestick.location.LocationManager
import com.example.smartassistivestick.location.LocationTrackingService
import com.example.smartassistivestick.navigation.NavigationManager
import com.example.smartassistivestick.sos.SOSManager
import com.example.smartassistivestick.sms.SMSManager
import com.example.smartassistivestick.voice.VoiceService
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Main ViewModel for Smart Assistive Stick
 * Manages all core managers and orchestrates the assistive system
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "APP_DEBUG"
    }

    // Core Managers
    private val bluetoothManager = BluetoothManager(application)
    private val locationManager = LocationManager(application)
    private val smsManager = SMSManager(application)
    private val audioManagerHelper = AudioManagerHelper(application)
    
    // New Premium Managers
    private val languageManager = LanguageManager(application)
    private val caregiverManager = CaregiverManager()
    private val navigationManager = NavigationManager(application)
    private val sosManager = SOSManager(application, languageManager)
    private val authManager = AuthManager(application)
    private val firebaseManager = FirebaseManager()
    private val accessKeyGenerator = AccessKeyGenerator()
    private val locationSyncManager = LocationSyncManager(
        context = application,
        firebaseManager = firebaseManager,
        locationManager = locationManager,
        updateIntervalMs = 5000L
    )
    
    private var ttsManager: TTSManager? = null
    private var speechManager: SpeechManager? = null

    // Public State Flows
    val isBluetoothConnected = bluetoothManager.connectionState
    val bluetoothConnectionStatus = bluetoothManager.connectionStatus
    
    private val _currentAlert = MutableStateFlow("EVERYTHING IS CLEAR")
    val currentAlert: StateFlow<String> = _currentAlert

    private val _currentAlertSubtext = MutableStateFlow("Safe to move")
    val currentAlertSubtext: StateFlow<String> = _currentAlertSubtext

    private val _locationText = MutableStateFlow("Fetching...")
    val locationText: StateFlow<String> = _locationText

    private val _currentLatLng = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLatLng: StateFlow<Pair<Double, Double>?> = _currentLatLng
    
    private val _selectedLanguage = MutableStateFlow(languageManager.selectedLanguage.value)
    val selectedLanguage: StateFlow<LanguageManager.Language> = _selectedLanguage
    
    private val _linkedCaregivers = MutableStateFlow<List<CaregiverManager.CaregiverAccess>>(emptyList())
    val linkedCaregivers: StateFlow<List<CaregiverManager.CaregiverAccess>> = _linkedCaregivers
    
    private val _accessCode = MutableStateFlow("")
    val accessCode: StateFlow<String> = _accessCode
    
    private val _systemStatus = MutableStateFlow(SystemStatus.INITIALIZING)
    val systemStatus: StateFlow<SystemStatus> = _systemStatus
    
    private val _voiceState = MutableStateFlow(VoiceAssistantState.IDLE)
    val voiceState: StateFlow<VoiceAssistantState> = _voiceState
    
    private val _sosActive = MutableStateFlow(false)
    val sosActive: StateFlow<Boolean> = _sosActive

    private val _firebaseUid = MutableStateFlow<String?>(null)
    val firebaseUid: StateFlow<String?> = _firebaseUid

    private val _firebaseError = MutableStateFlow<String?>(null)
    val firebaseError: StateFlow<String?> = _firebaseError

    // Message priority system
    private val sensorMessages = setOf("F1", "L1", "R1", "B1", "FL", "FR", "FB", "SAFE")
    private val modeMessages = setOf("MODE_INDOOR", "MODE_OUTDOOR")
    private val supportedMessages = sensorMessages + modeMessages + setOf("SYSTEM_STARTED", "SOS")

    private val repeatIntervalMs = 3000L
    private val modeHoldDurationMs = 2000L

    private var lastReceivedRawMessage: String = ""
    private var lastDisplayedMessage: String = ""
    private var lastSpokenMessage: String = ""
    private var lastSpokenTime: Long = 0L
    private var priorityHoldActive: Boolean = false
    private var priorityHoldUntil: Long = 0L
    private var currentPriorityType: String = "NONE"
    private var speechBusyUntil: Long = 0L
    private var locationUpdateJob: Job? = null
    private var runtimePermissionsGranted: Boolean = false
    private var locationPermissionGranted: Boolean = false
    private var audioPermissionGranted: Boolean = false
    private var bluetoothPermissionGranted: Boolean = false
    private var servicesStarted: Boolean = false

    enum class SystemStatus {
        INITIALIZING, READY, ERROR, INACTIVE
    }

    enum class VoiceAssistantState {
        IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
    }

    init {
        _systemStatus.value = SystemStatus.INITIALIZING
        Log.d(TAG, "Step reached: MainViewModel init")
        
        // Initialize TTS with language support
        ttsManager = TTSManager(application, languageManager) {
            updateSystemStatus(SystemStatus.READY)
        }

        // Initialize speech manager
        speechManager = SpeechManager(application) { command ->
            handleVoiceCommand(command)
        }

        // Start audio monitoring
        audioManagerHelper.startMonitoring()

        // Monitor language changes
        viewModelScope.launch {
            languageManager.selectedLanguage.collect { language ->
                _selectedLanguage.value = language
                ttsManager?.setLanguage(language)
            }
        }

        // Monitor Bluetooth messages
        monitorBluetoothMessages()

        // Initialize Firebase anonymous auth + user profile + access key + location sync
        initializeFirebaseRealtime()
        
        // Update location continuously
        updateLocationContinuously()
        
        // Load caregivers
        loadLinkedCaregivers()
        
        // Start background services
        // Services are started only after runtime permissions are granted.
    }

    private fun initializeFirebaseRealtime() {
        viewModelScope.launch {
            val signIn = runCatching { authManager.signInAnonymouslyIfNeeded() }
                .onFailure { Log.e(TAG, "Firebase auth flow crashed", it) }
                .getOrNull() ?: return@launch
            if (signIn.isFailure) {
                _firebaseError.value = signIn.exceptionOrNull()?.message ?: "Anonymous sign-in failed"
                Log.e(TAG, "Firebase anonymous sign in failed: ${_firebaseError.value}")
                return@launch
            }

            val uid = signIn.getOrNull() ?: return@launch
            _firebaseUid.value = uid

            try {
                firebaseManager.createOrUpdateUser(uid)
                val existingKey = firebaseManager.getAccessKey(uid)
                if (existingKey.isNullOrBlank()) {
                    val newKey = accessKeyGenerator.generateUniqueAccessKey()
                    firebaseManager.saveAccessKey(uid, newKey)
                    _accessCode.value = newKey
                } else {
                    _accessCode.value = existingKey
                }
                if (locationPermissionGranted) {
                    locationSyncManager.start(uid)
                }
                Log.d(TAG, "Step reached: Firebase initialized")
            } catch (e: Exception) {
                _firebaseError.value = e.message ?: "Firebase initialization failed"
                Log.e(TAG, "Firebase init failed", e)
            }
        }
    }

    fun onRuntimePermissionsUpdated(
        allGranted: Boolean,
        locationGranted: Boolean,
        audioGranted: Boolean,
        bluetoothGranted: Boolean
    ) {
        runtimePermissionsGranted = allGranted
        locationPermissionGranted = locationGranted
        audioPermissionGranted = audioGranted
        bluetoothPermissionGranted = bluetoothGranted

        Log.d(
            TAG,
            "Step reached: permissions updated all=$allGranted loc=$locationGranted audio=$audioGranted bt=$bluetoothGranted"
        )

        if (runtimePermissionsGranted) {
            startBackgroundServicesSafely()
        }

        if (locationPermissionGranted) {
            _firebaseUid.value?.let { uid ->
                runCatching { locationSyncManager.start(uid) }
                    .onFailure { Log.e(TAG, "Failed to start location sync after permissions", it) }
            }
        }
    }

    /**
     * Start background services for always-on functionality
     */
    private fun startBackgroundServicesSafely() {
        if (servicesStarted) {
            Log.d(TAG, "Step reached: services already started")
            return
        }

        val context = getApplication<SmartAssistiveApp>()

        try {
            if (audioPermissionGranted) {
                val voiceIntent = Intent(context, VoiceService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(voiceIntent)
                } else {
                    @Suppress("DEPRECATION")
                    context.startService(voiceIntent)
                }
                Log.d(TAG, "Step reached: VoiceService started")
            }

            if (locationPermissionGranted) {
                val locationIntent = Intent(context, LocationTrackingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(locationIntent)
                } else {
                    @Suppress("DEPRECATION")
                    context.startService(locationIntent)
                }
                Log.d(TAG, "Step reached: LocationTrackingService started")
            }

            if (bluetoothPermissionGranted) {
                runCatching {
                    val clazz = Class.forName("com.example.smartassistivestick.bluetooth.BluetoothBackgroundService")
                    val btIntent = Intent(context, clazz)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(btIntent)
                    } else {
                        @Suppress("DEPRECATION")
                        context.startService(btIntent)
                    }
                    Log.d(TAG, "Step reached: BluetoothBackgroundService started")
                }.onFailure {
                    Log.e(TAG, "BluetoothBackgroundService unavailable or failed to start", it)
                }
            }

            servicesStarted = true
        } catch (e: Exception) {
            Log.e(TAG, "Critical service startup error", e)
        }
    }

    private fun updateSystemStatus(status: SystemStatus) {
        _systemStatus.value = status
    }


    private fun monitorBluetoothMessages() {
        viewModelScope.launch {
            bluetoothManager.incomingMessages.collect { rawMsg ->
                processIncomingChunk(rawMsg)
            }
        }
    }

    private fun getSensorPriority(msg: String): Int {
        return when (msg) {
            "FB" -> 6
            "FL", "FR" -> 5
            "F1" -> 4
            "B1" -> 3
            "L1", "R1" -> 2
            "SAFE" -> 1
            else -> 0
        }
    }

    private fun processIncomingChunk(rawChunk: String) {
        if (rawChunk.isBlank()) {
            return
        }

        val messages = rawChunk
            .split('\n', '\r', ' ', '\t', ',')
            .map { normalizeMessage(it) }
            .filter { it.isNotEmpty() && it in supportedMessages }

        if (messages.isEmpty()) {
            return
        }

        // 1. Process SOS
        if (messages.contains("SOS")) {
            handleSosMessage()
            return
        }

        // 2. Process System Started
        if (messages.contains("SYSTEM_STARTED")) {
            handleSystemMessage(System.currentTimeMillis())
        }

        // 3. Check Mode messages
        val modeMsg = messages.lastOrNull { it in modeMessages }
        if (modeMsg != null) {
            handleModeMessage(modeMsg, System.currentTimeMillis())
            return
        }

        // 4. Handle Normal Sensor Messages
        val sensorMsgsInChunk = messages.filter { it in sensorMessages }
        if (sensorMsgsInChunk.isNotEmpty()) {
            val bestSensorMsg = sensorMsgsInChunk.maxByOrNull { getSensorPriority(it) }
            if (bestSensorMsg != null) {
                processMessage(bestSensorMsg)
            }
        }
    }

    fun processMessage(msg: String) {
        val normalized = normalizeMessage(msg)
        if (normalized.isEmpty() || normalized !in supportedMessages) {
            return
        }

        val currentTime = System.currentTimeMillis()
        clearExpiredModeHold(currentTime)

        when {
            normalized == "SOS" -> handleSosMessage()
            normalized in modeMessages -> handleModeMessage(normalized, currentTime)
            normalized in sensorMessages -> handleSensorMessage(normalized, currentTime)
            normalized == "SYSTEM_STARTED" -> handleSystemMessage(currentTime)
        }
    }

    private fun normalizeMessage(message: String): String {
        return message.trim().uppercase(Locale.US)
    }

    private fun clearExpiredModeHold(currentTime: Long) {
        if (priorityHoldActive && currentTime >= priorityHoldUntil) {
            priorityHoldActive = false
            currentPriorityType = "NONE"
        }
    }

    private fun handleModeMessage(rawMessage: String, currentTime: Long) {
        val message = mapMessage(rawMessage)

        priorityHoldActive = true
        priorityHoldUntil = currentTime + modeHoldDurationMs
        currentPriorityType = "MODE"

        speakModeImmediately(message, currentTime)
        updateUIState(rawMessage, message)
        lastReceivedRawMessage = rawMessage

        val mode = if (rawMessage == "MODE_INDOOR") "INDOOR" else "OUTDOOR"
        syncModeToFirebase(mode)
    }

    private fun syncModeToFirebase(mode: String) {
        viewModelScope.launch {
            val uid = _firebaseUid.value ?: return@launch
            runCatching { firebaseManager.updateMode(uid, mode) }
                .onFailure { _firebaseError.value = it.message ?: "Mode sync failed" }
        }
    }

    private fun handleSensorMessage(rawMessage: String, currentTime: Long) {
        if (priorityHoldActive && currentTime < priorityHoldUntil) {
            return
        }

        if (rawMessage == lastReceivedRawMessage && (currentTime - lastSpokenTime) < repeatIntervalMs) {
            return
        }

        if (currentTime < speechBusyUntil) {
            return
        }

        lastReceivedRawMessage = rawMessage
        val message = mapMessage(rawMessage)
        speakSensorMessage(message, currentTime, rawMessage)
        updateUIState(rawMessage, message)
    }

    private fun handleSystemMessage(currentTime: Long) {
        val message = mapMessage("SYSTEM_STARTED")
        speakModeImmediately(message, currentTime)
        updateUIState("SYSTEM_STARTED", message)
    }

    private fun handleSosMessage() {
        val spokenMessage = mapMessage("SOS")

        priorityHoldActive = false
        priorityHoldUntil = 0L
        currentPriorityType = "SOS"
        speechBusyUntil = 0L

        speakImmediately(spokenMessage)
        updateUIState("SOS", spokenMessage)
        lastReceivedRawMessage = "SOS"
        triggerSOS()
    }

    private fun speakModeImmediately(message: String, currentTime: Long) {
        ttsManager?.stop()
        ttsManager?.speak(message)
        lastSpokenMessage = message
        lastSpokenTime = currentTime
        speechBusyUntil = currentTime // Modes override but don't block
    }

    private fun speakSensorMessage(message: String, currentTime: Long, rawCode: String) {
        ttsManager?.stop()
        ttsManager?.speak(message)
        lastSpokenMessage = message
        lastSpokenTime = currentTime
        
        val gap = if (rawCode == "SAFE") 2500L else 1800L
        speechBusyUntil = currentTime + gap
    }

    private fun speakImmediately(message: String) {
        ttsManager?.stop()
        ttsManager?.speak(message)
        lastSpokenMessage = message
        lastSpokenTime = System.currentTimeMillis()
    }

    private fun mapMessage(rawMessage: String): String {
        return when (rawMessage) {
            "F1" -> languageManager.getString("warning")
            "L1" -> "Obstacle on left"
            "R1" -> "Obstacle on right"
            "B1" -> "Obstacles on both sides"
            "FL" -> "Obstacle ahead on left"
            "FR" -> "Obstacle ahead on right"
            "FB" -> languageManager.getString("danger")
            "SAFE" -> languageManager.getString("safe")
            "MODE_INDOOR" -> languageManager.getString("indoor_mode")
            "MODE_OUTDOOR" -> languageManager.getString("outdoor_mode")
            "SOS" -> languageManager.getString("sos")
            "SYSTEM_STARTED" -> "System started"
            else -> rawMessage
        }
    }

    private fun updateUIState(code: String, message: String) {
        lastDisplayedMessage = message
        _currentAlert.value = when (code) {
            "F1", "L1", "R1", "B1", "FL", "FR", "FB" -> "⚠ WARNING"
            "SAFE" -> "✓ SAFE"
            "MODE_INDOOR" -> "🏠 INDOOR"
            "MODE_OUTDOOR" -> "🌍 OUTDOOR"
            "SOS" -> "🆘 EMERGENCY"
            "SYSTEM_STARTED" -> "✓ READY"
            else -> message.uppercase()
        }
        _currentAlertSubtext.value = message
    }

    private fun triggerSOS() {
        viewModelScope.launch {
            try {
                val location = locationManager.getCurrentLocation()
                val mapLink = locationManager.getCurrentLocationMapLink()
                
                val sosContacts = _linkedCaregivers.value.map { caregiver ->
                    SOSManager.SOSContact(
                        name = caregiver.caregiverName,
                        phoneNumber = caregiver.caregiverPhone,
                        notifyType = "both"
                    )
                }
                
                sosManager.triggerSOS(
                    contacts = sosContacts,
                    userLocation = location ?: "Unknown location",
                    locationLink = mapLink ?: "",
                    customMessage = "I am in emergency. Please help!"
                )
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    /**
     * Generate access code for caregiver linking
     */
    fun generateAccessCode() {
        viewModelScope.launch {
            try {
                val uid = _firebaseUid.value ?: return@launch
                val code = accessKeyGenerator.generateUniqueAccessKey()
                firebaseManager.saveAccessKey(uid, code)
                _accessCode.value = code
            } catch (e: Exception) {
                _firebaseError.value = e.message ?: "Access key generation failed"
            }
        }
    }

    fun linkCaregiverWithAccessKey(accessKey: String) {
        viewModelScope.launch {
            val caregiverId = _firebaseUid.value ?: return@launch
            val result = firebaseManager.linkCaregiverByAccessKey(caregiverId, accessKey)
            if (result.isFailure) {
                _firebaseError.value = result.exceptionOrNull()?.message ?: "Caregiver link failed"
            }
        }
    }

    /**
     * Load linked caregivers
     */
    private fun loadLinkedCaregivers() {
        viewModelScope.launch {
            try {
                caregiverManager.loadLinkedCaregivers()
                caregiverManager.linkedCaregivers.collect { caregivers ->
                    _linkedCaregivers.value = caregivers
                }
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    /**
     * Revoke caregiver access
     */
    fun revokeCaregiverAccess(caregiverId: String) {
        viewModelScope.launch {
            caregiverManager.revokeCaregiverAccess(caregiverId)
            runCatching { firebaseManager.revokeCaregiver(caregiverId) }
                .onFailure { _firebaseError.value = it.message ?: "Revoke failed" }
        }
    }

    /**
     * Cancel active SOS
     */
    fun cancelSOS() {
        sosManager.cancelSOS()
        _sosActive.value = false
    }

    /**
     * Switch language
     */
    fun setLanguage(language: LanguageManager.Language) {
        languageManager.setLanguage(language)
    }

    fun connectToBluetooth(address: String) {
        runCatching {
            bluetoothManager.connectToDevice(address)
        }.onFailure {
            Log.e(TAG, "Bluetooth connect failed", it)
        }
    }

    fun startVoiceCommand() {
        _voiceState.value = VoiceAssistantState.LISTENING
        runCatching {
            speechManager?.startListening()
        }.onFailure {
            Log.e(TAG, "startVoiceCommand failed", it)
            _voiceState.value = VoiceAssistantState.ERROR
        }
    }

    private fun handleVoiceCommand(command: String) {
        val lowercaseCmd = command.lowercase()
        _voiceState.value = VoiceAssistantState.PROCESSING
        
        when {
            lowercaseCmd.contains("where am i") -> {
                handleWhereAmI()
            }
            lowercaseCmd.contains("send sos") || lowercaseCmd.contains("emergency") -> {
                handleSosMessage()
            }
            lowercaseCmd.contains("navigate to") -> {
                handleNavigateTo(lowercaseCmd)
            }
            lowercaseCmd.contains("call caregiver") -> {
                handleCallCaregiver()
            }
        }
        
        _voiceState.value = VoiceAssistantState.IDLE
    }

    private fun handleWhereAmI() {
        viewModelScope.launch {
            val loc = locationManager.getCurrentLocation()
            val speechText = loc ?: "Unable to fetch location"
            ttsManager?.stop()
            ttsManager?.speak("You are at $speechText")
        }
    }

    private fun handleNavigateTo(command: String) {
        // Extract destination from command
        val destination = command.replace("navigate to", "").trim()
        _voiceState.value = VoiceAssistantState.SPEAKING
        ttsManager?.speak("Navigating to $destination")
    }

    private fun handleCallCaregiver() {
        if (_linkedCaregivers.value.isNotEmpty()) {
            val caregiver = _linkedCaregivers.value.first()
            _voiceState.value = VoiceAssistantState.SPEAKING
            ttsManager?.speak("Calling ${caregiver.caregiverName}")
            // Trigger call
        }
    }

    fun fetchLocation() {
        updateLocationContinuously()
    }

    private fun updateLocationContinuously() {
        if (locationUpdateJob?.isActive == true) return

        locationUpdateJob = viewModelScope.launch {
            while (isActive) {
                try {
                    if (locationPermissionGranted) {
                        val loc = locationManager.getCurrentLocation()
                        val latLng = locationManager.getCurrentLatLng()
                        _locationText.value = loc ?: "Location unavailable"
                        _currentLatLng.value = latLng
                    } else {
                        _locationText.value = "Location permission required"
                        _currentLatLng.value = null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Location update loop failed", e)
                }
                delay(3000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager?.shutdown()
        speechManager?.destroy()
        bluetoothManager.disconnect()
        sosManager.cancelSOS()
        locationSyncManager.stop()
        locationUpdateJob?.cancel()
    }
}
