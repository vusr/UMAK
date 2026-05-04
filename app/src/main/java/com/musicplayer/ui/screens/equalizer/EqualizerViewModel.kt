package com.musicplayer.ui.screens.equalizer

import android.media.audiofx.Equalizer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.EqPreset
import com.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val KEY_EQ_ENABLED    = booleanPreferencesKey("eq_enabled")
private val KEY_EQ_BAND_GAINS = stringPreferencesKey("eq_band_gains")  // "[0,0,0,0,0]"
private val KEY_EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
private val KEY_EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
private val KEY_EQ_LOUDNESS   = intPreferencesKey("eq_loudness")
private val KEY_EQ_PRESET_ID  = longPreferencesKey("eq_preset_id")     // -1L = none

data class BandState(
    val index: Int,
    val frequencyHz: Int,
    val gainMillibel: Int,
    val minMillibel: Int,
    val maxMillibel: Int,
)

data class EqualizerUiState(
    val isEnabled: Boolean = true,
    val bands: List<BandState> = emptyList(),
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val presets: List<EqPreset> = emptyList(),
    val selectedPresetId: Long? = null,
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val playerController: PlayerController,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _state = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllEqPresets().collect { presets ->
                _state.update { it.copy(presets = presets) }
            }
        }
        viewModelScope.launch {
            playerController.effectsReady.first { it }
            val eq = playerController.equalizer ?: return@launch
            initFromHardware(eq)
            restoreFromDataStore()
        }
    }

    /**
     * Reads band configuration from the device's hardware Equalizer.
     * Called once effects are attached to the active audio session.
     */
    fun initFromHardware(equalizer: Equalizer) {
        val numBands = equalizer.numberOfBands.toInt()
        val bands = (0 until numBands).map { i ->
            val freqRange = equalizer.getBandFreqRange(i.toShort())
            val centerFreq = freqRange?.let { (it[0] + it[1]) / 2 } ?: 0
            BandState(
                index = i,
                frequencyHz = centerFreq / 1000,
                gainMillibel = equalizer.getBandLevel(i.toShort()).toInt(),
                minMillibel = equalizer.bandLevelRange?.get(0)?.toInt() ?: -1500,
                maxMillibel = equalizer.bandLevelRange?.get(1)?.toInt() ?: 1500,
            )
        }
        _state.update { it.copy(bands = bands) }
    }

    /**
     * Reads persisted EQ settings from DataStore and re-applies them to both
     * the hardware effects and the UI state. Called once after [initFromHardware].
     */
    private suspend fun restoreFromDataStore() {
        val prefs = dataStore.data.first()
        val enabled    = prefs[KEY_EQ_ENABLED] ?: return   // nothing saved yet — keep hardware defaults
        val gainsJson  = prefs[KEY_EQ_BAND_GAINS] ?: return
        val bassBoost  = prefs[KEY_EQ_BASS_BOOST] ?: 0
        val virtualizer = prefs[KEY_EQ_VIRTUALIZER] ?: 0
        val loudness   = prefs[KEY_EQ_LOUDNESS] ?: 0
        val presetId   = prefs[KEY_EQ_PRESET_ID] ?: -1L

        val savedGains = parseBandGains(gainsJson)

        // Apply to hardware
        playerController.setEqEnabled(enabled)
        savedGains.forEachIndexed { i, gain -> playerController.setBandGain(i, gain) }
        playerController.setBassBoost(bassBoost)
        playerController.setVirtualizer(virtualizer)
        playerController.setLoudnessGain(loudness)

        // Merge saved gains into existing BandState list (keeps Hz/min/max from hardware)
        val restoredBands = _state.value.bands.mapIndexed { i, band ->
            band.copy(gainMillibel = savedGains.getOrElse(i) { band.gainMillibel })
        }

        _state.update {
            it.copy(
                isEnabled = enabled,
                bands = restoredBands,
                bassBoostStrength = bassBoost,
                virtualizerStrength = virtualizer,
                loudnessGainMb = loudness,
                selectedPresetId = presetId.takeIf { id -> id >= 0 },
            )
        }
    }

    private fun parseBandGains(json: String): List<Int> =
        json.removePrefix("[").removeSuffix("]")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }

    // ── Mutation methods ──────────────────────────────────────────────────────

    fun setBandGain(bandIndex: Int, gainMillibel: Int) {
        val updated = _state.value.bands.toMutableList()
        if (bandIndex in updated.indices) {
            updated[bandIndex] = updated[bandIndex].copy(gainMillibel = gainMillibel)
            _state.update { it.copy(bands = updated, selectedPresetId = null) }
            playerController.setBandGain(bandIndex, gainMillibel)
            persistEqState()
        }
    }

    fun setBassBoost(strength: Int) {
        _state.update { it.copy(bassBoostStrength = strength) }
        playerController.setBassBoost(strength)
        persistEqState()
    }

    fun setVirtualizer(strength: Int) {
        _state.update { it.copy(virtualizerStrength = strength) }
        playerController.setVirtualizer(strength)
        persistEqState()
    }

    fun setLoudnessGain(gainMb: Int) {
        _state.update { it.copy(loudnessGainMb = gainMb) }
        playerController.setLoudnessGain(gainMb)
        persistEqState()
    }

    fun toggleEnabled() {
        val next = !_state.value.isEnabled
        _state.update { it.copy(isEnabled = next) }
        playerController.setEqEnabled(next)
        persistEqState()
    }

    fun applyPreset(preset: EqPreset) {
        val gains = parseBandGains(preset.bandGains)
        val updatedBands = _state.value.bands.mapIndexed { i, band ->
            val gain = gains.getOrElse(i) { 0 }
            playerController.setBandGain(i, gain)
            band.copy(gainMillibel = gain)
        }
        playerController.setBassBoost(preset.bassBoostStrength)
        playerController.setVirtualizer(preset.virtualizerStrength)
        playerController.setLoudnessGain(preset.loudnessGainMb)
        _state.update {
            it.copy(
                bands = updatedBands,
                bassBoostStrength = preset.bassBoostStrength,
                virtualizerStrength = preset.virtualizerStrength,
                loudnessGainMb = preset.loudnessGainMb,
                selectedPresetId = preset.id,
            )
        }
        persistEqState()
    }

    fun saveCurrentAsPreset(name: String) = viewModelScope.launch {
        val gains = "[" + _state.value.bands.joinToString(",") { it.gainMillibel.toString() } + "]"
        val preset = EqPreset(
            name = name,
            bandGains = gains,
            bassBoostStrength = _state.value.bassBoostStrength,
            virtualizerStrength = _state.value.virtualizerStrength,
            loudnessGainMb = _state.value.loudnessGainMb,
        )
        repo.saveEqPreset(preset)
    }

    fun deletePreset(id: Long) = viewModelScope.launch {
        repo.deleteEqPreset(id)
        // If the deleted preset was selected, clear the selection
        if (_state.value.selectedPresetId == id) {
            _state.update { it.copy(selectedPresetId = null) }
            persistEqState()
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persistEqState() {
        val snapshot = _state.value
        val gainsJson = "[" + snapshot.bands.joinToString(",") { it.gainMillibel.toString() } + "]"
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_EQ_ENABLED]     = snapshot.isEnabled
                prefs[KEY_EQ_BAND_GAINS]  = gainsJson
                prefs[KEY_EQ_BASS_BOOST]  = snapshot.bassBoostStrength
                prefs[KEY_EQ_VIRTUALIZER] = snapshot.virtualizerStrength
                prefs[KEY_EQ_LOUDNESS]    = snapshot.loudnessGainMb
                prefs[KEY_EQ_PRESET_ID]   = snapshot.selectedPresetId ?: -1L
            }
        }
    }
}
