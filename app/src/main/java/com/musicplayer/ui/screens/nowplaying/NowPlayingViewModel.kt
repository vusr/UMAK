package com.musicplayer.ui.screens.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NowPlayingUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val volumeLevel: Int = 35,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffled: Boolean = false,
    val repeatMode: Int = 0,
    val isFavorite: Boolean = false,
    val sleepTimerEndMs: Long = 0L,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    // isFavorite is observed live from Room so it is always consistent,
    // regardless of which screen set it or how many times we navigate here.
    val playbackError: SharedFlow<String> = playerController.playbackError

    val uiState: StateFlow<NowPlayingUiState> = playerController.state
        .flatMapLatest { ps ->
            val trackId = ps.currentTrack?.id
            if (trackId != null) {
                repo.getTrackByIdFlow(trackId).map { liveTrack ->
                    NowPlayingUiState(
                        currentTrack = ps.currentTrack,
                        isPlaying = ps.isPlaying,
                        volumeLevel = ps.volumeLevel,
                        progressMs = ps.progressMs,
                        durationMs = ps.durationMs,
                        isShuffled = ps.isShuffled,
                        repeatMode = ps.repeatMode,
                        isFavorite = liveTrack?.isFavorite ?: false,
                        sleepTimerEndMs = ps.sleepTimerEndMs,
                    )
                }
            } else {
                flowOf(
                    NowPlayingUiState(
                        isPlaying = ps.isPlaying,
                        volumeLevel = ps.volumeLevel,
                        isShuffled = ps.isShuffled,
                        repeatMode = ps.repeatMode,
                        sleepTimerEndMs = ps.sleepTimerEndMs,
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NowPlayingUiState(),
        )

    fun toggleFavorite() = viewModelScope.launch {
        val trackId = playerController.state.value.currentTrack?.id ?: return@launch
        repo.setFavorite(trackId, !uiState.value.isFavorite)
        // No local override needed — Room emits the new value through getTrackByIdFlow.
    }

    fun setVolumeLevel(level: Int) {
        playerController.setVolume(level)
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun next() {
        playerController.next()
    }

    fun previous() {
        playerController.previous()
    }

    fun toggleShuffle() {
        playerController.setShuffleEnabled(!playerController.state.value.isShuffled)
    }

    fun cycleRepeat() {
        val next = (playerController.state.value.repeatMode + 1) % 3
        playerController.setRepeatMode(next)
    }

    fun setSleepTimer(minutes: Int) = playerController.setSleepTimer(minutes)
    fun setSleepTimerEndOfTrack() = playerController.setSleepTimerEndOfTrack()
    fun cancelSleepTimer() = playerController.cancelSleepTimer()
}
