package com.musicplayer.service

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.util.VolumeUtil
import kotlin.math.pow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val KEY_VOLUME_LEVEL = intPreferencesKey("volume_level")
private val KEY_SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
private val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val volumeLevel: Int = 35,
    /** Epoch millis when the sleep timer fires; 0 = no timer; -1 = end-of-track mode. */
    val sleepTimerEndMs: Long = 0L,
)

/**
 * Application-scoped singleton that owns the MediaController connection to MusicPlayerService.
 * All ViewModels and UI components interact with playback exclusively through this class.
 *
 * Also owns the hardware audio effects (Equalizer, BassBoost, Virtualizer, LoudnessEnhancer),
 * which are created once the ExoPlayer audio session ID is published by the service.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: MusicRepository,
    private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controller: MediaController? = null
    private var queue: List<Track> = emptyList()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _playbackError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val playbackError: SharedFlow<String> = _playbackError.asSharedFlow()

    private val _effectsReady = MutableStateFlow(false)
    val effectsReady: StateFlow<Boolean> = _effectsReady.asStateFlow()

    var equalizer: Equalizer? = null
        private set
    var bassBoost: BassBoost? = null
        private set
    var virtualizer: Virtualizer? = null
        private set
    var loudnessEnhancer: LoudnessEnhancer? = null
        private set

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var sleepAtEndOfTrack = false

    // Receives session-level extras pushed by MusicPlayerService (audioSessionId).
    private val controllerListener = object : MediaController.Listener {
        override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
            val sid = extras.getInt("audioSessionId", 0)
            if (sid != 0 && _audioSessionId.value == 0) {
                _audioSessionId.value = sid
            }
        }
    }

    init {
        // Restore persisted volume/shuffle/repeat before connecting to the service.
        scope.launch {
            val prefs = dataStore.data.first()
            val savedVolume = prefs[KEY_VOLUME_LEVEL] ?: 35
            val savedShuffle = prefs[KEY_SHUFFLE_ENABLED] ?: false
            val savedRepeat = prefs[KEY_REPEAT_MODE] ?: Player.REPEAT_MODE_OFF
            _state.update {
                it.copy(
                    volumeLevel = savedVolume,
                    isShuffled = savedShuffle,
                    repeatMode = savedRepeat,
                )
            }
        }

        scope.launch {
            val token = SessionToken(
                context,
                ComponentName(context, MusicPlayerService::class.java),
            )
            val future = MediaController.Builder(context, token)
                .setListener(controllerListener)
                .buildAsync()
            future.addListener(
                {
                    runCatching { future.get() }.onSuccess { ctrl ->
                        controller = ctrl
                        onControllerReady(ctrl)
                    }
                },
                context.mainExecutor,
            )
        }

        // Attach audio effects once audioSessionId becomes available from the service.
        scope.launch {
            audioSessionId.first { it != 0 }.let { sid -> attachAudioEffects(sid) }
        }
    }

    private fun onControllerReady(ctrl: MediaController) {
        // Read audioSessionId that may already be present in session extras.
        val sid = ctrl.sessionExtras.getInt("audioSessionId", 0)
        if (sid != 0) _audioSessionId.value = sid

        // Apply persisted shuffle and repeat to the live controller.
        val saved = _state.value
        ctrl.shuffleModeEnabled = saved.isShuffled
        ctrl.repeatMode = saved.repeatMode
        // Apply persisted volume gain (ReplayGain applied in applyVolume).
        applyVolume()

        _state.update {
            it.copy(
                isPlaying = ctrl.isPlaying,
                isShuffled = ctrl.shuffleModeEnabled,
                repeatMode = ctrl.repeatMode,
            )
        }

        // If the service is already playing (app killed & relaunched), restore current track.
        ctrl.currentMediaItem?.mediaId?.toLongOrNull()?.let { id ->
            scope.launch {
                val track = repo.getTrackById(id)
                val dur = ctrl.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
                _state.update { it.copy(currentTrack = track, durationMs = dur) }
            }
        }

        ctrl.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionPolling() else stopPositionPolling()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (sleepAtEndOfTrack && mediaItem != null) {
                    sleepAtEndOfTrack = false
                    _state.update { it.copy(sleepTimerEndMs = 0L) }
                    scope.launch { delay(50); controller?.pause() }
                }
                val track = mediaItem?.mediaId?.toLongOrNull()?.let { id ->
                    queue.find { it.id == id }
                }
                val dur = ctrl.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
                _state.update { it.copy(currentTrack = track, durationMs = dur, progressMs = 0L) }
                applyVolume()
                track?.let { t -> scope.launch { repo.incrementPlayCount(t.id) } }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = ctrl.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L
                    _state.update { it.copy(durationMs = dur) }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.update { it.copy(isShuffled = shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _state.update { it.copy(repeatMode = repeatMode) }
            }

            override fun onPlayerError(error: PlaybackException) {
                val msg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "File not found. It may have been moved or deleted."
                    PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                        "Cannot read this file — storage permission required."
                    else -> error.message ?: "Playback error"
                }
                _playbackError.tryEmit(msg)
            }
        })
    }

    private fun startPositionPolling() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val ctrl = controller ?: break
                val pos = ctrl.currentPosition.coerceAtLeast(0L)
                val dur = ctrl.duration.takeIf { it != C.TIME_UNSET && it > 0 }
                    ?: _state.value.durationMs
                _state.update { it.copy(progressMs = pos, durationMs = dur) }
                delay(500)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun attachAudioEffects(audioSessionId: Int) {
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            bassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
            virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply { enabled = true }
            _effectsReady.value = true
        } catch (e: Exception) {
            // Some devices throw on effect creation — handle gracefully.
        }
    }

    // ── Playback controls ──────────────────────────────────────────────────────

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        queue = tracks
        val items = tracks.map { it.toMediaItem() }
        controller?.run {
            setMediaItems(items, startIndex.coerceIn(0, tracks.lastIndex), 0L)
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _state.update { it.copy(progressMs = positionMs) }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun setVolume(level: Int) {
        _state.update { it.copy(volumeLevel = level) }
        applyVolume()
        scope.launch { dataStore.edit { it[KEY_VOLUME_LEVEL] = level } }
    }

    private fun applyVolume() {
        val level = _state.value.volumeLevel
        val rgDb = _state.value.currentTrack?.replayGainTrack ?: 0f
        val rgLinear = 10f.pow(rgDb / 20f).coerceIn(0.01f, 4f)
        val finalGain = (VolumeUtil.levelToGain(level) * rgLinear).coerceIn(0f, 1f)
        controller?.volume = finalGain
    }

    fun setShuffleEnabled(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        scope.launch { dataStore.edit { it[KEY_SHUFFLE_ENABLED] = enabled } }
    }

    fun setRepeatMode(mode: Int) {
        controller?.repeatMode = mode
        scope.launch { dataStore.edit { it[KEY_REPEAT_MODE] = mode } }
    }

    // ── Sleep timer ────────────────────────────────────────────────────────────

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepAtEndOfTrack = false
        val delayMs = minutes * 60_000L
        val endMs = System.currentTimeMillis() + delayMs
        _state.update { it.copy(sleepTimerEndMs = endMs) }
        sleepTimerJob = scope.launch {
            delay(delayMs)
            controller?.pause()
            _state.update { it.copy(sleepTimerEndMs = 0L) }
            sleepTimerJob = null
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtEndOfTrack = true
        _state.update { it.copy(sleepTimerEndMs = -1L) }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        sleepAtEndOfTrack = false
        _state.update { it.copy(sleepTimerEndMs = 0L) }
    }

    // ── Audio-effect controls ─────────────────────────────────────────────────

    fun setEqEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        loudnessEnhancer?.enabled = enabled
    }

    fun setBandGain(bandIndex: Int, gainMillibel: Int) {
        equalizer?.setBandLevel(bandIndex.toShort(), gainMillibel.toShort())
    }

    fun setBassBoost(strength: Int) {
        bassBoost?.setStrength(strength.toShort())
    }

    fun setVirtualizer(strength: Int) {
        virtualizer?.setStrength(strength.toShort())
    }

    fun setLoudnessGain(gainMb: Int) {
        loudnessEnhancer?.setTargetGain(gainMb)
    }
}

private fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(filePath)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(albumName)
                .setArtworkUri(Uri.parse("content://media/external/audio/albumart/$albumId"))
                .build()
        )
        .build()
