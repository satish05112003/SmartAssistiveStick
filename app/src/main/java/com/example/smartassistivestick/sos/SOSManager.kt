package com.example.smartassistivestick.sos

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.telephony.SmsManager
import android.os.Handler
import android.os.Looper
import com.example.smartassistivestick.language.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SOS Manager for emergency alerts
 * Sends SMS, calls caregivers, and manages priority alerts
 */
class SOSManager(
    private val context: Context,
    private val languageManager: LanguageManager? = null
) {

    private val _sosActive = MutableStateFlow(false)
    val sosActive: StateFlow<Boolean> = _sosActive
    
    private val _alertCount = MutableStateFlow(0)
    val alertCount: StateFlow<Int> = _alertCount
    
    private var sosHandler = Handler(Looper.getMainLooper())
    private var repeatTask: Runnable? = null
    
    companion object {
        private const val TAG = "SOSManager"
        private const val SOS_REPEAT_INTERVAL_MS = 30000L // Repeat every 30 seconds
    }

    data class SOSContact(
        val name: String,
        val phoneNumber: String,
        val notifyType: String = "sms" // sms, call, both
    )

    /**
     * Trigger SOS emergency alert
     */
    suspend fun triggerSOS(
        contacts: List<SOSContact>,
        userLocation: String,
        locationLink: String,
        customMessage: String = ""
    ) {
        try {
            if (_sosActive.value) {
                Log.w(TAG, "SOS already active")
                return
            }
            
            _sosActive.value = true
            _alertCount.value = 1
            
            Log.d(TAG, "SOS triggered at $userLocation")
            
            // Send alerts to all contacts
            for (contact in contacts) {
                when (contact.notifyType) {
                    "sms" -> sendSMS(contact, userLocation, locationLink, customMessage)
                    "call" -> initiateCall(contact)
                    "both" -> {
                        sendSMS(contact, userLocation, locationLink, customMessage)
                        initiateCall(contact)
                    }
                }
            }
            
            // Start repeat alert task
            startRepeatAlerts(contacts, userLocation, locationLink, customMessage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering SOS", e)
        }
    }

    /**
     * Send SMS to contact with location
     */
    private fun sendSMS(
        contact: SOSContact,
        location: String,
        locationLink: String,
        customMessage: String
    ) {
        try {
            val languageCode = languageManager?.getCurrentLanguageCode() ?: "en"
            
            var message = when (languageCode) {
                "hi" -> "आपातकाल! मेरी सहायता चाहिए। स्थिति: $location लिंक: $locationLink"
                "ta" -> "அவசரம்! எனக்கு உதவி தேவை. இருப்பிடம்: $location இணைப்பு: $locationLink"
                "te" -> "అత్యవసర! నాకు సహాయం కావాలి. స్థానం: $location లింక్: $locationLink"
                "kn" -> "ತುರ್ತುಸ್ಥಿತಿ! ನನಗೆ ಸಹಾಯ ಬೇಕು. ಸ್ಥಳ: $location ಲಿಂಕ್: $locationLink"
                "ml" -> "জরുরি! എനിക്ക് സഹായം വേണം. സ്ഥാനം: $location ലിങ്ക്: $locationLink"
                "bn" -> "জরুরি! আমার সাহায্য লাগবে। অবস্থান: $location লিংক: $locationLink"
                else -> "Emergency! I need help. Location: $location Link: $locationLink"
            }
            
            if (customMessage.isNotEmpty()) {
                message += "\nBefore location - $customMessage"
            }
            
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            val parts = smsManager?.divideMessage(message)
            if (parts != null) {
                val sentIntentsList = parts.map {
                    PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent("SMS_SENT"),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                }
                val sentIntents = ArrayList(sentIntentsList)
                
                smsManager.sendMultipartTextMessage(
                    contact.phoneNumber, null, parts, sentIntents, null
                )
                
                Log.d(TAG, "SOS SMS sent to ${contact.name}: ${contact.phoneNumber}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
        }
    }

    /**
     * Initiate call to contact
     */
    private fun initiateCall(contact: SOSContact) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = android.net.Uri.parse("tel:${contact.phoneNumber}")
            context.startActivity(callIntent)
            
            Log.d(TAG, "Call initiated to ${contact.name}: ${contact.phoneNumber}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating call", e)
        }
    }

    /**
     * Start repeating SOS alerts every 30 seconds
     */
    private fun startRepeatAlerts(
        contacts: List<SOSContact>,
        location: String,
        locationLink: String,
        customMessage: String
    ) {
        repeatTask = object : Runnable {
            override fun run() {
                if (_sosActive.value) {
                    _alertCount.value = _alertCount.value + 1
                    
                    Log.d(TAG, "Repeating SOS alert #${_alertCount.value}")
                    
                    // Send alerts again
                    for (contact in contacts) {
                        if (contact.notifyType == "sms" || contact.notifyType == "both") {
                            sendSMS(contact, location, locationLink, customMessage)
                        }
                    }
                    
                    // Schedule next repeat
                    sosHandler.postDelayed(this, SOS_REPEAT_INTERVAL_MS)
                }
            }
        }
        
        repeatTask?.let { sosHandler.postDelayed(it, SOS_REPEAT_INTERVAL_MS) }
    }

    /**
     * Cancel active SOS
     */
    fun cancelSOS() {
        _sosActive.value = false
        _alertCount.value = 0
        
        repeatTask?.let { sosHandler.removeCallbacks(it) }
        
        Log.d(TAG, "SOS cancelled")
    }

    /**
     * Check if SOS is active
     */
    fun isActive(): Boolean = _sosActive.value

    /**
     * Get current alert count
     */
    fun getAlertCount(): Int = _alertCount.value
}
