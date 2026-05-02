package com.camyuran.camyunews.data.remote.gemini

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyProvider @Inject constructor(
    private val encryptedPrefs: SharedPreferences
) {
    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    fun getApiKey(): String? = encryptedPrefs.getString(KEY_GEMINI_API_KEY, null)
        ?.takeIf { it.isNotBlank() }

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    fun hasApiKey(): Boolean = getApiKey() != null
}
