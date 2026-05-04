package com.musicplayer.data.repository

import com.musicplayer.data.local.db.dao.*
import com.musicplayer.data.local.mediastore.MediaStoreScanner
import com.musicplayer.domain.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val playlistDao: PlaylistDao,
    private val eqPresetDao: EqPresetDao,
    private val scanner: MediaStoreScanner,
) {
    // ---- Tracks ----
    fun getAllTracks(): Flow<List<Track>> = trackDao.getAllTracks()
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>> = trackDao.getTracksByAlbum(albumId)
    fun getTracksByArtist(artistId: Long): Flow<List<Track>> = trackDao.getTracksByArtist(artistId)
    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)
    fun getFavoriteTracks(): Flow<List<Track>> = trackDao.getFavoriteTracks()
    fun getRecentlyPlayed(): Flow<List<Track>> = trackDao.getRecentlyPlayed()
    fun getRecentlyAdded(): Flow<List<Track>> = trackDao.getRecentlyAdded()
    suspend fun getTrackById(id: Long): Track? = trackDao.getTrackById(id)
    fun getTrackByIdFlow(id: Long): Flow<Track?> = trackDao.getTrackByIdFlow(id)
    suspend fun getTrackByPath(filePath: String): Track? = trackDao.getTrackByPath(filePath)
    suspend fun setFavorite(id: Long, isFavorite: Boolean) = trackDao.setFavorite(id, isFavorite)
    suspend fun incrementPlayCount(id: Long) = trackDao.incrementPlayCount(id)

    // ---- Albums ----
    fun getAllAlbums(): Flow<List<Album>> = albumDao.getAllAlbums()
    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> = albumDao.getAlbumsByArtist(artistId)
    suspend fun getAlbumById(id: Long): Album? = albumDao.getAlbumById(id)
    fun searchAlbums(query: String): Flow<List<Album>> = albumDao.searchAlbums(query)

    // ---- Artists ----
    fun getAllArtists(): Flow<List<Artist>> = artistDao.getAllArtists()
    suspend fun getArtistById(id: Long): Artist? = artistDao.getArtistById(id)
    fun searchArtists(query: String): Flow<List<Artist>> = artistDao.searchArtists(query)

    // ---- Playlists ----
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    suspend fun getPlaylistById(id: Long): Playlist? = playlistDao.getPlaylistById(id)
    suspend fun createPlaylist(playlist: Playlist): Long = playlistDao.insertPlaylist(playlist)
    suspend fun updatePlaylist(playlist: Playlist) = playlistDao.updatePlaylist(playlist)
    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylistById(id)

    // ---- EQ Presets ----
    fun getAllEqPresets(): Flow<List<EqPreset>> = eqPresetDao.getAllPresets()
    suspend fun getEqPresetById(id: Long): EqPreset? = eqPresetDao.getPresetById(id)
    suspend fun saveEqPreset(preset: EqPreset): Long = eqPresetDao.insertPreset(preset)
    suspend fun deleteEqPreset(id: Long) = eqPresetDao.deleteUserPreset(id)

    // ---- Library Scan ----
    suspend fun scanLibrary(
        onProgress: (suspend (scanned: Int, total: Int) -> Unit)? = null,
    ) {
        val result = scanner.scan(onProgress)
        trackDao.upsertTracks(result.tracks)
        albumDao.upsertAlbums(result.albums)
        artistDao.upsertArtists(result.artists)
        // Remove stale entries
        val activeTrackIds = result.tracks.map { it.id }
        val activeAlbumIds = result.albums.map { it.id }
        val activeArtistIds = result.artists.map { it.id }
        if (activeTrackIds.isNotEmpty()) trackDao.deleteRemovedTracks(activeTrackIds)
        if (activeAlbumIds.isNotEmpty()) albumDao.deleteRemovedAlbums(activeAlbumIds)
        if (activeArtistIds.isNotEmpty()) artistDao.deleteRemovedArtists(activeArtistIds)
    }
}
