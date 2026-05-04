package com.musicplayer.ui.screens.home

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.worker.LibraryScanWorker
import com.musicplayer.domain.model.Album
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val KEY_HAS_SCANNED = booleanPreferencesKey("has_scanned")

data class HomeUiState(
    val recentlyPlayed: List<Track> = emptyList(),
    val recentlyAdded: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val isScanRunning: Boolean = false,
    val scanScanned: Int = 0,
    val scanTotal: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: MusicRepository,
    private val workManager: WorkManager,
    private val dataStore: DataStore<Preferences>,
    private val playerController: PlayerController,
) : ViewModel() {

    private data class ScanState(
        val running: Boolean = false,
        val scanned: Int = 0,
        val total: Int = 0,
    )

    private val scanState: StateFlow<ScanState> = workManager
        .getWorkInfosByTagFlow(LibraryScanWorker.TAG)
        .map { infos ->
            val active = infos.firstOrNull {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            }
            if (active != null) {
                ScanState(
                    running = true,
                    scanned = active.progress.getInt(LibraryScanWorker.KEY_SCANNED, 0),
                    total = active.progress.getInt(LibraryScanWorker.KEY_TOTAL, 0),
                )
            } else {
                ScanState()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanState())

    val uiState: StateFlow<HomeUiState> = combine(
        repo.getRecentlyPlayed(),
        repo.getAllAlbums().map { it.sortedByDescending { a -> a.id }.take(12) },
        scanState,
    ) { recentTracks, recentAlbums, scan ->
        HomeUiState(
            recentlyPlayed = recentTracks,
            recentlyAdded = recentAlbums,
            isLoading = false,
            isScanRunning = scan.running,
            scanScanned = scan.scanned,
            scanTotal = scan.total,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    /** Called by the UI once audio permission is confirmed. Runs the scan only on first launch. */
    fun triggerScanIfFirstLaunch() = viewModelScope.launch {
        val prefs = dataStore.data.first()
        if (prefs[KEY_HAS_SCANNED] != true) {
            enqueue(ExistingWorkPolicy.KEEP)
            dataStore.edit { it[KEY_HAS_SCANNED] = true }
        }
    }

    /** Called by the Rescan button — always re-runs the scan. */
    fun rescan() {
        enqueue(ExistingWorkPolicy.REPLACE)
    }

    fun playTrack(tracks: List<Track>, index: Int) {
        playerController.playQueue(tracks, index)
    }

    fun toggleFavorite(trackId: Long, isFavorite: Boolean) = viewModelScope.launch {
        repo.setFavorite(trackId, !isFavorite)
    }

    private fun enqueue(policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<LibraryScanWorker>()
            .addTag(LibraryScanWorker.TAG)
            .build()
        workManager.enqueueUniqueWork(LibraryScanWorker.UNIQUE_WORK_NAME, policy, request)
    }
}
