package com.example.smartassistivestick.voice

import android.app.Service
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground Service for always-on voice listening
 * Detects wake word "Hey Assist" and activates voice commands
 */
class VoiceService : Service(), RecognitionListener {

    inner class VoiceBinder : Binder() {
        fun getService(): VoiceService = this@VoiceService
    }

    private val binder = VoiceBinder()
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState
    
    private val _recognizedCommand = MutableStateFlow("")
    val recognizedCommand: StateFlow<String> = _recognizedCommand
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private var onCommandListener: ((String) -> Unit)? = null
    
    companion object {
        private const val TAG = "VoiceService"
        private const val WAKE_WORD = "hey assist"
        private const val NOTIFICATION_ID = 1001
    }

    enum class VoiceState {
        IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VoiceService created")
        
        // Create speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            Log.d(TAG, "VoiceService started")
            startForegroundService()

            val hasAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasAudioPermission) {
                startListeningForWakeWord()
            } else {
                Log.e(TAG, "RECORD_AUDIO permission missing. VoiceService will stay idle.")
            }

            START_STICKY
        } catch (e: Exception) {
            Log.e(TAG, "VoiceService start failed", e)
            START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "VoiceService destroyed")
        
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    /**
     * Start listening for wake word
     */
    fun startListeningForWakeWord() {
        if (speechRecognizer == null) return

        val hasAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasAudioPermission) {
            Log.e(TAG, "Cannot listen: RECORD_AUDIO permission missing")
            _voiceState.value = VoiceState.ERROR
            return
        }
        
        _isListening.value = true
        _voiceState.value = VoiceState.LISTENING
        
        val intent = android.speech.RecognizerIntent.getVoiceDetailsIntent(this)
        runCatching { speechRecognizer?.startListening(intent) }
            .onFailure {
                Log.e(TAG, "Failed to start speech recognizer", it)
                _voiceState.value = VoiceState.ERROR
            }
        
        Log.d(TAG, "Started listening for wake word")
    }

    /**
     * Stop listening
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        _voiceState.value = VoiceState.IDLE
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Set listener for voice commands
     */
    fun setOnCommandListener(listener: (String) -> Unit) {
        onCommandListener = listener
    }

    override fun onReadyForSpeech(params: android.os.Bundle?) {
        Log.d(TAG, "Ready for speech")
        _voiceState.value = VoiceState.LISTENING
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Speech detected")
        _voiceState.value = VoiceState.PROCESSING
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Audio levels change
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Buffer received
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech detected")
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error: $error"
        }
        
        Log.e(TAG, "Speech recognition error: $errorMessage")
        _voiceState.value = VoiceState.ERROR
        
        // Restart listening after a delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (_isListening.value) {
                startListeningForWakeWord()
            }
        }, 1000)
    }

    override fun onResults(bundle: android.os.Bundle?) {
        val results = bundle?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
        
        if (!results.isNullOrEmpty()) {
            val heard = results[0].lowercase()
            val confidence = bundle?.getFloatArray(android.speech.SpeechRecognizer.CONFIDENCE_SCORES)
                ?.getOrNull(0) ?: 0f
            
            Log.d(TAG, "Recognized: '$heard' with confidence: $confidence")
            
            // Check for wake word
            if (heard.contains(WAKE_WORD) && confidence > 0.5f) {
                Log.d(TAG, "Wake word detected!")
                _voiceState.value = VoiceState.SPEAKING
                
                // Notify that wake word was detected
                onCommandListener?.invoke("wake_word_detected")
                
                // Stop and wait for command
                stopListening()
            } else {
                // Continue listening for wake word
                startListeningForWakeWord()
            }
        } else {
            // No results, continue listening
            startListeningForWakeWord()
        }
    }

    override fun onPartialResults(bundle: android.os.Bundle?) {
        val partial = bundle?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
        if (!partial.isNullOrEmpty()) {
            Log.d(TAG, "Partial: ${partial[0]}")
        }
    }

    override fun onEvent(eventType: Int, params: android.os.Bundle?) {
        // Event occurred
    }

    /**
     * Start foreground service with notification
     */
    private fun startForegroundService() {
        val channelId = "voice_service_channel"
        
        // Create notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Voice Assistant",
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        
        // Build notification
        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("Smart Assistive Stick")
            .setContentText("Voice assistant is active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
}
