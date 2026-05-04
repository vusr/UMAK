package com.musicplayer.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.Playlist
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val allTracks: List<Track> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _playlistId = MutableStateFlow(-1L)

    private val _playlistState: StateFlow<Pair<Playlist?, List<Track>>> = _playlistId
        .filter { it >= 0 }
        .flatMapLatest { id ->
            repo.getAllPlaylists()
                .map { lists -> lists.find { it.id == id } }
                .map { playlist ->
                    val tracks = parseIds(playlist?.trackIds ?: "")
                        .mapNotNull { repo.getTrackById(it) }
                    Pair(playlist, tracks)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Pair(null, emptyList()))

    private val _allTracks: StateFlow<List<Track>> = repo.getAllTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<PlaylistUiState> = combine(
        _playlistState,
        _allTracks,
    ) { (playlist, tracks), allTracks ->
        PlaylistUiState(playlist = playlist, tracks = tracks, allTracks = allTracks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistUiState())

    fun loadPlaylist(id: Long) { _playlistId.value = id }

    fun playTracks(startIndex: Int) {
        playerController.playQueue(uiState.value.tracks, startIndex)
    }

    fun removeTrack(trackId: Long) = viewModelScope.launch {
        val playlist = uiState.value.playlist ?: return@launch
        val ids = parseIds(playlist.trackIds).filter { it != trackId }
        repo.updatePlaylist(
            playlist.copy(
                trackIds = JSONArray(ids).toString(),
                modifiedAt = System.currentTimeMillis(),
            )
        )
    }

    fun addTrack(trackId: Long) = viewModelScope.launch {
        val playlist = uiState.value.playlist ?: return@launch
        val ids = parseIds(playlist.trackIds).toMutableList()
        if (trackId !in ids) {
            ids.add(trackId)
            repo.updatePlaylist(
                playlist.copy(
                    trackIds = JSONArray(ids).toString(),
                    modifiedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun reorderTracks(newTrackIds: List<Long>) = viewModelScope.launch {
        val playlist = uiState.value.playlist ?: return@launch
        repo.updatePlaylist(
            playlist.copy(
                trackIds = JSONArray(newTrackIds).toString(),
                modifiedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun parseIds(raw: String): List<Long> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
