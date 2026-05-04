package com.musicplayer.ui.screens.settings

import android.content.Context
import android.media.AudioManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "AmoledBlack",            // "AmoledBlack", "Dark", "Light"
    val gaplessPlayback: Boolean = true,
    val crossfadeDuration: Int = 0,               // seconds, 0 = disabled
    val defaultVolumeLevel: Int = 35,
    val forceSampleRateHz: Int = 0,               // 0 = auto
    val showHiddenFiles: Boolean = false,
    val libraryPaths: List<String> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_GAPLESS = booleanPreferencesKey("gapless")
        val KEY_CROSSFADE = intPreferencesKey("crossfade")
        val KEY_DEFAULT_VOLUME = intPreferencesKey("default_volume")
        val KEY_FORCE_SAMPLE_RATE = intPreferencesKey("force_sample_rate")
        val KEY_SHOW_HIDDEN = booleanPreferencesKey("show_hidden_files")
    }

    val uiState: StateFlow<SettingsUiState> = dataStore.data.map { prefs ->
        SettingsUiState(
            theme = prefs[KEY_THEME] ?: "AmoledBlack",
            gaplessPlayback = prefs[KEY_GAPLESS] ?: true,
            crossfadeDuration = prefs[KEY_CROSSFADE] ?: 0,
            defaultVolumeLevel = prefs[KEY_DEFAULT_VOLUME] ?: 35,
            forceSampleRateHz = prefs[KEY_FORCE_SAMPLE_RATE] ?: 0,
            showHiddenFiles = prefs[KEY_SHOW_HIDDEN] ?: false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // Device audio capabilities — read once at construction
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val nativeSampleRate: String =
        audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE) ?: "Unknown"
    val nativeBufferSize: String =
        audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER) ?: "Unknown"
    val appVersion: String = BuildConfig.VERSION_NAME

    fun setTheme(theme: String) = save { it[KEY_THEME] = theme }
    fun setGapless(enabled: Boolean) = save { it[KEY_GAPLESS] = enabled }
    fun setCrossfade(seconds: Int) = save { it[KEY_CROSSFADE] = seconds }
    fun setDefaultVolume(level: Int) = save { it[KEY_DEFAULT_VOLUME] = level }
    fun setForceSampleRate(hz: Int) = save { it[KEY_FORCE_SAMPLE_RATE] = hz }
    fun setShowHiddenFiles(show: Boolean) = save { it[KEY_SHOW_HIDDEN] = show }

    private fun save(block: (MutablePreferences) -> Unit) = viewModelScope.launch {
        dataStore.edit { block(it) }
    }
}
