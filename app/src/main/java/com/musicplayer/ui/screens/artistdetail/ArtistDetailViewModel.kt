package com.musicplayer.ui.screens.artistdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.Album
import com.musicplayer.domain.model.Artist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val repo: MusicRepository,
) : ViewModel() {
    private val _artistId = MutableStateFlow(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ArtistDetailUiState> = _artistId
        .filter { it >= 0 }
        .flatMapLatest { id ->
            combine(
                flow { emit(repo.getArtistById(id)) },
                repo.getAlbumsByArtist(id),
            ) { artist, albums -> ArtistDetailUiState(artist, albums) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtistDetailUiState())

    fun loadArtist(id: Long) { _artistId.value = id }
}
