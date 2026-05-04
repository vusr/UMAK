package com.musicplayer.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.musicplayer.data.repository.MusicRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LibraryScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repo: MusicRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        repo.scanLibrary { scanned, total ->
            // Throttle updates: report every 25 tracks and always on the last one
            if (scanned % 25 == 0 || scanned == total) {
                setProgress(workDataOf(KEY_SCANNED to scanned, KEY_TOTAL to total))
            }
        }
        Result.success()
    } catch (e: Exception) {
        if (runAttemptCount < 2) Result.retry() else Result.failure()
    }

    companion object {
        const val TAG = "library_scan"
        const val UNIQUE_WORK_NAME = "library_scan"
        const val KEY_SCANNED = "scanned"
        const val KEY_TOTAL = "total"
    }
}
