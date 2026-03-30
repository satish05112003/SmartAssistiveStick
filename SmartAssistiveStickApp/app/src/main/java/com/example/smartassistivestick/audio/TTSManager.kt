package com.example.smartassistivestick.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.smartassistivestick.language.LanguageManager
import java.util.Locale

/**
 * Enhanced TTS Manager with multi-language support
 * Provides slow, clear speech optimized for accessibility
 */
class TTSManager(
    context: Context,
    private val languageManager: LanguageManager? = null,
    private val onInitCompleted: () -> Unit = {}
) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLocale: Locale = Locale.ENGLISH
    
    companion object {
        private const val TAG = "TTSManager"
        // Slow speech rate for better accessibility
        private const val SPEECH_RATE = 0.7f
        private const val PITCH = 1.0f
    }

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupLanguage()
            isInitialized = true
            
            // Settings for accessibility
            tts?.setSpeechRate(SPEECH_RATE) // Slow, clear speech
            tts?.setPitch(PITCH)
            
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
            
            Log.d(TAG, "TTS initialized successfully with locale: $currentLocale")
            onInitCompleted()
        } else {
            Log.e(TAG, "TTS initialization failed with status: $status")
        }
    }

    private fun setupLanguage() {
        currentLocale = languageManager?.let {
            val currentLang = it.selectedLanguage.value
            currentLang.locale
        } ?: Locale.ENGLISH
        
        val result = tts?.setLanguage(currentLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $currentLocale not fully supported, falling back to English")
            tts?.setLanguage(Locale.ENGLISH)
        }
    }

    /**
     * Change language and apply to TTS
     */
    fun setLanguage(language: LanguageManager.Language) {
        if (!isInitialized) return
        
        currentLocale = language.locale
        setupLanguage()
        Log.d(TAG, "Language changed to: ${language.displayName}")
    }

    /**
     * Speak text with current language
     */
    fun speak(text: String) {
        if (isInitialized && text.isNotEmpty()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "utteranceId")
        }
    }

    /**
     * Speak text with specific language
     */
    fun speakInLanguage(text: String, language: LanguageManager.Language) {
        if (!isInitialized) return
        
        val previousLocale = currentLocale
        setLanguage(language)
        speak(text)
        setLanguage(LanguageManager.Language.values().find { it.locale == previousLocale } ?: LanguageManager.Language.ENGLISH)
    }

    /**
     * Queue text to be spoken (won't interrupt current speech)
     */
    fun speakQueued(text: String) {
        if (isInitialized && text.isNotEmpty()) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utteranceId")
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    /**
     * Shutdown TTS service
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
    }
}
