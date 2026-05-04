package com.musicplayer.data.local.db.dao

import androidx.room.*
import com.musicplayer.domain.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getAllTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE albumId = :albumId ORDER BY discNumber ASC, trackNumber ASC")
    fun getTracksByAlbum(albumId: Long): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE artistId = :artistId ORDER BY albumName ASC, trackNumber ASC")
    fun getTracksByArtist(artistId: Long): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): Track?

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getTrackByIdFlow(id: Long): Flow<Track?>

    @Query("SELECT * FROM tracks WHERE filePath = :filePath LIMIT 1")
    suspend fun getTrackByPath(filePath: String): Track?

    @Query("""
        SELECT * FROM tracks WHERE 
        title LIKE '%' || :query || '%' OR 
        artist LIKE '%' || :query || '%' OR 
        albumName LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun searchTracks(query: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteTracks(): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY lastPlayed DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 20): Flow<List<Track>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 20): Flow<List<Track>>

    @Upsert
    suspend fun upsertTracks(tracks: List<Track>)

    @Upsert
    suspend fun upsertTrack(track: Track)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayed = :timestamp WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM tracks WHERE id NOT IN (:activeIds)")
    suspend fun deleteRemovedTracks(activeIds: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAllTracks()
}
