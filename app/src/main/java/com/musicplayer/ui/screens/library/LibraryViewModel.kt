package com.musicplayer.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.domain.model.*
import com.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

data class SearchResults(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
)

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: LibraryTab = LibraryTab.Tracks,
    val searchResults: SearchResults? = null,
)

enum class LibraryTab { Tracks, Albums, Artists, Playlists }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _tab = MutableStateFlow(LibraryTab.Tracks)
    private val _rawQuery = MutableStateFlow("")

    // Debounced cross-library search — null when query is blank
    private val _searchResults: StateFlow<SearchResults?> = _rawQuery
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            if (q.isBlank()) {
                flowOf(null)
            } else {
                combine(
                    repo.searchTracks(q),
                    repo.searchAlbums(q),
                    repo.searchArtists(q),
                ) { tracks, albums, artists ->
                    SearchResults(tracks, albums, artists) as SearchResults?
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<LibraryUiState> = combine(
        _tab,
        _rawQuery,
        _searchResults,
        repo.getAllTracks(),
        repo.getAllAlbums(),
        repo.getAllArtists(),
        repo.getAllPlaylists(),
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        LibraryUiState(
            selectedTab = values[0] as LibraryTab,
            searchQuery = values[1] as String,
            searchResults = values[2] as SearchResults?,
            tracks = values[3] as List<Track>,
            albums = values[4] as List<Album>,
            artists = values[5] as List<Artist>,
            playlists = values[6] as List<Playlist>,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun selectTab(tab: LibraryTab) { _tab.value = tab }
    fun setSearchQuery(query: String) { _rawQuery.value = query }

    fun toggleFavorite(trackId: Long, current: Boolean) = viewModelScope.launch {
        repo.setFavorite(trackId, !current)
    }

    fun playTrack(tracks: List<Track>, index: Int) {
        playerController.playQueue(tracks, index)
    }

    // ---- Playlist management ----

    fun createPlaylist(name: String) = viewModelScope.launch {
        repo.createPlaylist(Playlist(name = name, trackIds = "[]"))
    }

    fun renamePlaylist(id: Long, newName: String) = viewModelScope.launch {
        val playlist = repo.getPlaylistById(id) ?: return@launch
        repo.updatePlaylist(playlist.copy(name = newName, modifiedAt = System.currentTimeMillis()))
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch {
        repo.deletePlaylist(id)
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        val playlist = repo.getPlaylistById(playlistId) ?: return@launch
        val ids = parseTrackIds(playlist.trackIds).toMutableList()
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

    private fun parseTrackIds(raw: String): List<Long> {
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getLong(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
