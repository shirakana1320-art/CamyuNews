package com.camyuran.camyunews.presentation.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.camyuran.camyunews.data.remote.gemini.ApiKeyProvider
import com.camyuran.camyunews.data.remote.gemini.GeminiError
import com.camyuran.camyunews.data.remote.gemini.GeminiService
import com.camyuran.camyunews.worker.NewsFetchWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class SettingsUiState(
    val apiKey: String = "",
    val apiKeyMasked: String = "",
    val hasApiKey: Boolean = false,
    val notificationEnabled: Boolean = true,
    val darkMode: Boolean = false,
    val textSizeScale: Int = 1,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyProvider: ApiKeyProvider,
    private val workManager: WorkManager,
    private val geminiService: GeminiService
) : ViewModel() {

    companion object {
        val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        val KEY_TEXT_SIZE = intPreferencesKey("text_size")
    }

    private val _uiState = MutableStateFlow(
        SettingsUiState(hasApiKey = apiKeyProvider.hasApiKey())
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.settingsDataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        notificationEnabled = prefs[KEY_NOTIFICATION_ENABLED] != false,
                        darkMode = prefs[KEY_DARK_MODE] == true,
                        textSizeScale = prefs[KEY_TEXT_SIZE] ?: 1
                    )
                }
            }
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            apiKeyProvider.saveApiKey(apiKey.trim())
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveSuccess = true,
                    hasApiKey = apiKeyProvider.hasApiKey()
                )
            }
            manualFetch()
        }
    }

    fun clearApiKey() {
        apiKeyProvider.clearApiKey()
        _uiState.update { it.copy(hasApiKey = false, saveSuccess = false) }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.settingsDataStore.edit { it[KEY_NOTIFICATION_ENABLED] = enabled }
        }
    }

    fun setTextSizeScale(scale: Int) {
        viewModelScope.launch {
            context.settingsDataStore.edit { it[KEY_TEXT_SIZE] = scale }
        }
    }

    fun manualFetch() {
        val request = OneTimeWorkRequestBuilder<NewsFetchWorker>()
            .addTag(NewsFetchWorker.TAG_FETCH)
            .build()
        workManager.enqueueUniqueWork(
            NewsFetchWorker.WORK_NAME_MANUAL,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun testGeminiConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, connectionTestResult = null) }
            try {
                val error = geminiService.testConnection()
                val result = if (error == null) {
                    "接続OK: APIキーが有効です"
                } else {
                    when (error) {
                        is GeminiError.ApiKeyMissing -> "APIキーが未設定または無効です。正しいキーを入力してください"
                        is GeminiError.RateLimitExceeded -> "レートリミット中: APIキーは有効ですが、しばらくお待ちください"
                        is GeminiError.Unknown -> "エラー: ${error.message}"
                    }
                }
                _uiState.update { it.copy(connectionTestResult = result) }
            } finally {
                _uiState.update { it.copy(isTestingConnection = false) }
            }
        }
    }

    fun dismissSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun dismissConnectionTestResult() {
        _uiState.update { it.copy(connectionTestResult = null) }
    }
}
