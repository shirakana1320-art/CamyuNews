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
    val saveSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyProvider: ApiKeyProvider,
    private val workManager: WorkManager
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
                _uiState.value = _uiState.value.copy(
                    notificationEnabled = prefs[KEY_NOTIFICATION_ENABLED] != false,
                    darkMode = prefs[KEY_DARK_MODE] == true,
                    textSizeScale = prefs[KEY_TEXT_SIZE] ?: 1
                )
            }
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            apiKeyProvider.saveApiKey(apiKey.trim())
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                saveSuccess = true,
                hasApiKey = apiKeyProvider.hasApiKey()
            )
        }
    }

    fun clearApiKey() {
        apiKeyProvider.clearApiKey()
        _uiState.value = _uiState.value.copy(hasApiKey = false, saveSuccess = false)
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
        val request = OneTimeWorkRequestBuilder<NewsFetchWorker>().build()
        workManager.enqueueUniqueWork(
            "ManualFetch",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun dismissSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
