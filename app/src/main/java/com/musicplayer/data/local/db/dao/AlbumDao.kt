package com.musicplayer.data.local.db.dao

import androidx.room.*
import com.musicplayer.domain.model.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<Album>>

    @Query("""
        SELECT DISTINCT albums.* FROM albums
        INNER JOIN tracks ON albums.id = tracks.albumId
        WHERE tracks.artistId = :artistId
        ORDER BY albums.year DESC
    """)
    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: Long): Album?

    @Query("SELECT * FROM albums WHERE name LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchAlbums(query: String): Flow<List<Album>>

    @Upsert
    suspend fun upsertAlbums(albums: List<Album>)

    @Query("DELETE FROM albums WHERE id NOT IN (:activeIds)")
    suspend fun deleteRemovedAlbums(activeIds: List<Long>)

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums()
}
