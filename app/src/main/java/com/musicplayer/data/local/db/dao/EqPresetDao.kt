package com.musicplayer.data.local.db.dao

import androidx.room.*
import com.musicplayer.domain.model.EqPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface EqPresetDao {

    @Query("SELECT * FROM eq_presets ORDER BY isBuiltIn DESC, name ASC")
    fun getAllPresets(): Flow<List<EqPreset>>

    @Query("SELECT * FROM eq_presets WHERE id = :id")
    suspend fun getPresetById(id: Long): EqPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqPreset): Long

    @Update
    suspend fun updatePreset(preset: EqPreset)

    @Query("DELETE FROM eq_presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteUserPreset(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBuiltInPresets(presets: List<EqPreset>)
}
