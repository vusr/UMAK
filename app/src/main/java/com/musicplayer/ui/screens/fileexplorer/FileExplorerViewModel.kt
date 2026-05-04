package com.musicplayer.ui.screens.fileexplorer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.musicplayer.data.local.filemanager.FileItem
import com.musicplayer.data.repository.FileRepository
import com.musicplayer.data.repository.MusicRepository
import com.musicplayer.data.repository.TagFields
import com.musicplayer.data.worker.LibraryScanWorker
import com.musicplayer.domain.model.Track
import com.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FileExplorerUiState(
    val currentPath: String = "",
    val items: List<FileItem> = emptyList(),
    val breadcrumbs: List<Pair<String, String>> = emptyList(),
    val isLoading: Boolean = false,
    val clipboardPath: String? = null,
    val clipboardIsCut: Boolean = false,
    val selectedPaths: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val tagSaveError: String? = null,
    val tagEditorPath: String? = null,
    val tagFields: TagFields? = null,
    val isLoadingTags: Boolean = false,
    val isSavingTags: Boolean = false,
)

@HiltViewModel
class FileExplorerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileRepo: FileRepository,
    private val musicRepo: MusicRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _state = MutableStateFlow(FileExplorerUiState())
    val uiState: StateFlow<FileExplorerUiState> = _state.asStateFlow()

    fun init(initialPath: String) {
        val startPath = initialPath.ifBlank { fileRepo.getRootPath() }
        navigateTo(startPath)
    }

    fun navigateTo(path: String) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val items = fileRepo.listDirectory(path)
        val breadcrumbs = buildBreadcrumbs(path)
        _state.update {
            it.copy(
                currentPath = path,
                items = items,
                breadcrumbs = breadcrumbs,
                isLoading = false,
                selectedPaths = emptySet(),
            )
        }
    }

    fun navigateUp() {
        val parent = File(_state.value.currentPath).parent ?: return
        navigateTo(parent)
    }

    fun toggleSelection(path: String) {
        val current = _state.value.selectedPaths.toMutableSet()
        if (path in current) current.remove(path) else current.add(path)
        _state.update { it.copy(selectedPaths = current) }
    }

    fun selectAll() {
        val all = _state.value.items.map { it.path }.toSet()
        _state.update { it.copy(selectedPaths = all) }
    }

    fun clearSelection() = _state.update { it.copy(selectedPaths = emptySet()) }

    fun cutSelected() {
        val selected = _state.value.selectedPaths.firstOrNull() ?: return
        _state.update { it.copy(clipboardPath = selected, clipboardIsCut = true) }
    }

    fun copySelected() {
        val selected = _state.value.selectedPaths.firstOrNull() ?: return
        _state.update { it.copy(clipboardPath = selected, clipboardIsCut = false) }
    }

    fun paste() = viewModelScope.launch {
        val sourcePath = _state.value.clipboardPath ?: return@launch
        val destDir = _state.value.currentPath
        val result = if (_state.value.clipboardIsCut)
            fileRepo.moveFile(sourcePath, destDir)
        else
            fileRepo.copyFile(sourcePath, destDir)
        result.onSuccess { _state.update { it.copy(clipboardPath = null) }; refresh() }
        result.onFailure { err -> _state.update { it.copy(errorMessage = err.message ?: "Paste failed") } }
    }

    fun rename(path: String, newName: String) = viewModelScope.launch {
        fileRepo.renameFile(path, newName)
            .onSuccess { refresh() }
            .onFailure { err -> _state.update { it.copy(errorMessage = err.message ?: "Rename failed") } }
    }

    fun delete(path: String) = viewModelScope.launch {
        fileRepo.deleteFile(path)
            .onSuccess { refresh() }
            .onFailure { err -> _state.update { it.copy(errorMessage = err.message ?: "Delete failed") } }
    }

    fun deleteSelected() = viewModelScope.launch {
        val paths = _state.value.selectedPaths.toList()
        var firstError: String? = null
        for (path in paths) {
            fileRepo.deleteFile(path).onFailure { err ->
                if (firstError == null) firstError = err.message ?: "Delete failed"
            }
        }
        _state.update { it.copy(selectedPaths = emptySet(), errorMessage = firstError) }
        refresh()
    }

    fun createFolder(name: String) = viewModelScope.launch {
        fileRepo.createDirectory(_state.value.currentPath, name)
            .onSuccess { refresh() }
            .onFailure { err -> _state.update { it.copy(errorMessage = err.message ?: "Create folder failed") } }
    }

    fun playFile(path: String) = viewModelScope.launch {
        val track = musicRepo.getTrackByPath(path) ?: run {
            // File not yet in the library — build a minimal stub so playback still works.
            val file = File(path)
            Track(
                id = 0L,
                title = file.nameWithoutExtension,
                artist = "",
                albumName = "",
                albumId = 0L,
                artistId = 0L,
                duration = 0L,
                filePath = path,
                mimeType = "",
                sampleRate = 0,
                bitDepth = 0,
                bitrate = 0,
                fileSize = file.length(),
                trackNumber = 0,
                discNumber = 0,
                year = 0,
                genre = "",
                dateAdded = 0L,
                dateModified = file.lastModified(),
            )
        }
        playerController.playQueue(listOf(track), startIndex = 0)
    }

    fun openTagEditor(path: String) {
        _state.update { it.copy(tagEditorPath = path, tagFields = null, isLoadingTags = true) }
        viewModelScope.launch {
            val fields = fileRepo.readTags(path)
            _state.update { it.copy(tagFields = fields, isLoadingTags = false) }
        }
    }

    fun closeTagEditor() = _state.update {
        it.copy(tagEditorPath = null, tagFields = null, isLoadingTags = false, isSavingTags = false)
    }

    fun saveTags(fields: TagFields) {
        val path = _state.value.tagEditorPath ?: return
        _state.update { it.copy(isSavingTags = true) }
        viewModelScope.launch {
            fileRepo.writeTags(path, fields)
                .onSuccess {
                    workManager.enqueueUniqueWork(
                        LibraryScanWorker.UNIQUE_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<LibraryScanWorker>()
                            .addTag(LibraryScanWorker.TAG)
                            .build(),
                    )
                    closeTagEditor()
                }
                .onFailure { err ->
                    _state.update { it.copy(isSavingTags = false, tagSaveError = err.message ?: "Failed to save tags") }
                }
        }
    }

    fun clearError() = _state.update { it.copy(errorMessage = null) }
    fun clearTagSaveError() = _state.update { it.copy(tagSaveError = null) }

    fun refresh() { navigateTo(_state.value.currentPath) }

    private fun buildBreadcrumbs(path: String): List<Pair<String, String>> {
        val root = fileRepo.getRootPath()
        if (!path.startsWith(root)) return listOf("Storage" to path)
        val relative = path.removePrefix(root).trimStart('/', '\\')
        val parts = mutableListOf("Storage" to root)
        var current = root
        if (relative.isNotEmpty()) {
            relative.split("/", "\\").forEach { segment ->
                current = "$current/$segment"
                parts.add(segment to current)
            }
        }
        return parts
    }
}
