package com.chargevoice.ai.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceAnnouncementManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLanguage = "English"

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            applyLanguage(currentLanguage)
        } else {
            Log.e("VoiceAnnouncementManager", "TTS Initialization failed!")
        }
    }
    
    fun updateSettings(language: String, speed: Float, pitch: Float) {
        currentLanguage = language
        if (isInitialized) {
            applyLanguage(language)
            tts?.setSpeechRate(speed)
            tts?.setPitch(pitch)
        }
    }

    private fun applyLanguage(language: String) {
        val locale = when (language) {
            "Hindi" -> Locale("hi", "IN")
            "Bangla" -> Locale("bn", "IN")
            else -> Locale.US
        }
        
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("VoiceAnnouncementManager", "Language not supported or missing data for $language")
            // Fallback to English
            tts?.setLanguage(Locale.US)
        }
    }

    fun announce(text: String, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        if (isInitialized) {
            tts?.speak(text, queueMode, null, null)
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun release() {
        if (isInitialized) {
            tts?.stop()
            tts?.shutdown()
        }
    }
    
    fun getLocalizedMilestoneText(percentage: Int): String {
        return when (currentLanguage) {
            "Hindi" -> "आपकी बैटरी अब $percentage प्रतिशत चार्ज हो गई है।"
            "Bangla" -> "আপনার ব্যাটারি এখন $percentage শতাংশ চার্জ হয়েছে।"
            else -> "Your battery is now $percentage percent charged."
        }
    }

    fun getLocalizedFullChargeText(): String {
        return when (currentLanguage) {
            "Hindi" -> "बधाई हो! आपकी बैटरी पूरी तरह से चार्ज हो गई है। कृपया चार्जर हटा दें।"
            "Bangla" -> "অভিনন্দন! আপনার ব্যাটারি সম্পূর্ণ চার্জ হয়েছে। অনুগ্রহ করে চার্জারটি আনপ্লাগ করুন।"
            else -> "Congratulations! Your battery is fully charged. Please unplug the charger."
        }
    }
}
