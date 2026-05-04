package com.musicplayer.data.local.db.dao

import androidx.room.*
import com.musicplayer.domain.model.Artist
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtists(): Flow<List<Artist>>

    @Query("SELECT * FROM artists WHERE id = :id")
    suspend fun getArtistById(id: Long): Artist?

    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%'")
    fun searchArtists(query: String): Flow<List<Artist>>

    @Upsert
    suspend fun upsertArtists(artists: List<Artist>)

    @Query("DELETE FROM artists WHERE id NOT IN (:activeIds)")
    suspend fun deleteRemovedArtists(activeIds: List<Long>)

    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists()
}
