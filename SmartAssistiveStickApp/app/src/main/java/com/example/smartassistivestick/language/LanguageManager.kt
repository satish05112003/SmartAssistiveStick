package com.example.smartassistivestick.language

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Manages language selection and localization for the app
 * Supports Indian languages and English
 */
class LanguageManager(context: Context) {

    enum class Language(val code: String, val displayName: String, val locale: Locale) {
        ENGLISH("en", "English", Locale.ENGLISH),
        HINDI("hi", "हिंदी", Locale("hi", "IN")),
        TAMIL("ta", "தமிழ்", Locale("ta", "IN")),
        TELUGU("te", "తెలుగు", Locale("te", "IN")),
        KANNADA("kn", "ಕನ್ನಡ", Locale("kn", "IN")),
        MALAYALAM("ml", "മലയാളം", Locale("ml", "IN")),
        BENGALI("bn", "বাংলা", Locale("bn", "IN"))
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("language_prefs", Context.MODE_PRIVATE)
    
    private val _selectedLanguage = MutableStateFlow(
        Language.values().find { it.code == getSavedLanguageCode() } ?: Language.ENGLISH
    )
    val selectedLanguage: StateFlow<Language> = _selectedLanguage

    init {
        setLocale(selectedLanguage.value.locale)
    }

    fun setLanguage(language: Language) {
        _selectedLanguage.value = language
        saveLanguageCode(language.code)
        setLocale(language.locale)
    }

    fun getAvailableLanguages(): List<Language> = Language.values().toList()

    fun getCurrentLanguageCode(): String = _selectedLanguage.value.code

    fun getLocaleForLanguage(language: Language): Locale = language.locale

    private fun saveLanguageCode(code: String) {
        sharedPreferences.edit().putString("selected_language", code).apply()
    }

    private fun getSavedLanguageCode(): String {
        return sharedPreferences.getString("selected_language", Language.ENGLISH.code) ?: Language.ENGLISH.code
    }

    private fun setLocale(locale: Locale) {
        Locale.setDefault(locale)
    }

    // Translation helper
    fun getString(messageKey: String): String {
        return when (messageKey) {
            "welcome" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Welcome to Smart Assistive Stick"
                Language.HINDI -> "स्मार्ट असिस्टिव स्टिक में स्वागत है"
                Language.TAMIL -> "ஸ்மார்ட் அசிஸ்டிவ் ஸ்டிக்குக்கு வரவேற்கிறோம்"
                Language.TELUGU -> "స్మార్ట్ అసిస్టివ్ స్టిక్‌కు స్వాగతం"
                Language.KANNADA -> "ಸ್ಮಾರ್ಟ್ ಅಸಿಸ್ಟಿವ್ ಸ್ಟಿಕ್‌ಗೆ ಸ್ವಾಗತ"
                Language.MALAYALAM -> "സ്മാർട്ട് അസിസ്റ്റീവ് സ്റ്റിക്കിലേക്ക് സ്വാഗതം"
                Language.BENGALI -> "স্মার্ট অ্যাসিস্টিভ স্টিকে স্বাগতম"
            }
            
            "safe" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Everything is clear"
                Language.HINDI -> "सब कुछ स्पष्ट है"
                Language.TAMIL -> "எல்லாம் தெளிவாக உள்ளது"
                Language.TELUGU -> "సবకुछ స్పష్టంగా ఉంది"
                Language.KANNADA -> "ಎಲ್ಲವೂ ಸ್ಪಷ್ಟವಾಗಿದೆ"
                Language.MALAYALAM -> "എല്ലാം വ്യക്തമാണ്"
                Language.BENGALI -> "সবকিছু পরিষ্কার"
            }
            
            "warning" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Be careful, obstacle ahead"
                Language.HINDI -> "सावधान, आगे बाधा है"
                Language.TAMIL -> "조心하세요, முன்னில் தடை உள்ளது"
                Language.TELUGU -> "జాగ్రత్త, ముందుకు అడ్డంకి ఉంది"
                Language.KANNADA -> "ಎಚ್ಚರ, ಮುಂದೆ ಅಡ್ಡಿಕ್ಕೆ ಇರುತ್ತೆ"
                Language.MALAYALAM -> "ജാഗ്രത, മുന്നിൽ തടയുണ്ട്"
                Language.BENGALI -> "সাবধান, সামনে বাধা"
            }
            
            "danger" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Danger! Stop immediately"
                Language.HINDI -> "खतरा! तुरंत रुको"
                Language.TAMIL -> "அபாயம்! உடனே நிறுத்தவும்"
                Language.TELUGU -> "ఆపద! వెంటనే ఆపండి"
                Language.KANNADA -> "ಅಪಾಯ! ತಕ್ಷಣ ನಿಲ್ಲಿಸು"
                Language.MALAYALAM -> "അപകടം! ഉടനെ നിർത്തുക"
                Language.BENGALI -> "বিপদ! অবিলম্বে থামুন"
            }
            
            "sos" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Emergency SOS activated"
                Language.HINDI -> "आपातकालीन एसओएस सक्रिय"
                Language.TAMIL -> "அவசரம் SOS செயல்படுத்தப்பட்டது"
                Language.TELUGU -> "అత్యవసర SOS సక్రియం"
                Language.KANNADA -> "ತುರ್ತುಸ್ಥಿತಿ SOS ಸಕ್ರಿಯ"
                Language.MALAYALAM -> "എമർജൻസി SOS സജീവമാണ്"
                Language.BENGALI -> "জরুরি SOS সক্রিয়"
            }
            
            "indoor_mode" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Switched to indoor mode"
                Language.HINDI -> "इनडोर मोड में स्विच किया"
                Language.TAMIL -> "உள்ளூர் முறைக்கு மாற்றப்பட்டது"
                Language.TELUGU -> "ఇన్‌డోర్ మోడ్‌కు మారారు"
                Language.KANNADA -> "ಇನ್‌ಡೋರ್ ಮೋಡ್‌ಗೆ ಬದಲಾಯಿಸಲಾಯಿತು"
                Language.MALAYALAM -> "ഇൻഡോർ മോഡിലേക്ക് മാറി"
                Language.BENGALI -> "ইনডোর মোডে স্যুইচ করা হয়েছে"
            }
            
            "outdoor_mode" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Switched to outdoor mode"
                Language.HINDI -> "आउटडोर मोड में स्विच किया"
                Language.TAMIL -> "வெளிப்புற முறைக்கு மாற்றப்பட்டது"
                Language.TELUGU -> "బయట మోడ్‌కు మారారు"
                Language.KANNADA -> "ಔಟ್‌ಡೋರ್ ಮೋಡ್‌ಗೆ ಬದಲಾಯಿಸಲಾಯಿತು"
                Language.MALAYALAM -> "ഔട്ടഡോർ മോഡിലേക്ക് മാറി"
                Language.BENGALI -> "আউটডোর মোডে স্যুইচ করা হয়েছে"
            }
            
            "location" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Your location"
                Language.HINDI -> "आपकी स्थिति"
                Language.TAMIL -> "உங்கள் இடம்"
                Language.TELUGU -> "మీ స్థానం"
                Language.KANNADA -> "ನಿಮ್ಮ ಸ್ಥಳ"
                Language.MALAYALAM -> "നിങ്ങളുടെ സ്ഥാനം"
                Language.BENGALI -> "আপনার অবস্থান"
            }

            "calling_caregiver" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Calling your caregiver"
                Language.HINDI -> "आपके देखभालकर्ता को बुला रहे हैं"
                Language.TAMIL -> "உங்கள் பராமரிப்பாளரை அழைக்கிறது"
                Language.TELUGU -> "మీ సంరక్షకను పిలుస్తుంది"
                Language.KANNADA -> "ನಿಮ್ಮ ಕಾಪಾಡುವವನನ್ನು ಕರೆಯುತ್ತಿದೆ"
                Language.MALAYALAM -> "നിങ്ങളുടെ പരിചരണക്കാരനെ വിളിക്കുന്നു"
                Language.BENGALI -> "আপনার যত্নশীলকে ডাচ্ছি"
            }

            "listening" -> when (_selectedLanguage.value) {
                Language.ENGLISH -> "Listening..."
                Language.HINDI -> "सुन रहे हैं..."
                Language.TAMIL -> "கேட்கிறது..."
                Language.TELUGU -> "వింటోంది..."
                Language.KANNADA -> "ಕೇಳುತ್ತಿದೆ..."
                Language.MALAYALAM -> "കേൾക്കുന്നു..."
                Language.BENGALI -> "শুনছি..."
            }

            else -> messageKey
        }
    }
}
