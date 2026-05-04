package com.musicplayer.ui.screens.albumdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.Album
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AlbumDetailUiState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {
    private val _albumId = MutableStateFlow(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AlbumDetailUiState> = _albumId
        .filter { it >= 0 }
        .flatMapLatest { id ->
            combine(
                flow { emit(repo.getAlbumById(id)) },
                repo.getTracksByAlbum(id),
            ) { album, tracks -> AlbumDetailUiState(album, tracks) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumDetailUiState())

    fun loadAlbum(id: Long) { _albumId.value = id }

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        playerController.playQueue(tracks, startIndex)
    }

    fun shuffleTracks(tracks: List<Track>) {
        playerController.playQueue(tracks.shuffled(), 0)
    }
}
