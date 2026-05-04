package com.musicplayer.data.local.filemanager

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val isAudioFile: Boolean,
)

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioExtensions = setOf(
        "mp3", "flac", "wav", "m4a", "ogg", "opus", "aac", "wma", "alac",
        "ape", "dsf", "dff", "aif", "aiff", "mka",
    )

    fun getRootPath(): String = Environment.getExternalStorageDirectory().absolutePath

    suspend fun listDirectory(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?.map { file ->
                val ext = file.extension.lowercase()
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    mimeType = if (file.isFile) getMimeType(ext) else null,
                    isAudioFile = ext in audioExtensions,
                )
            } ?: emptyList()
    }

    suspend fun renameFile(path: String, newName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            val newFile = File(file.parent, newName)
            if (file.renameTo(newFile)) newFile.absolutePath
            else throw Exception("Rename failed")
        }
    }

    suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (!file.deleteRecursively()) throw Exception("Delete failed")
        }
    }

    suspend fun createDirectory(parentPath: String, name: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(parentPath, name)
            if (!dir.mkdirs()) throw Exception("Failed to create directory")
            dir.absolutePath
        }
    }

    suspend fun copyFile(sourcePath: String, destDirPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(sourcePath)
            val destDir = File(destDirPath)
            val dest = File(destDir, source.name)
            source.copyTo(dest, overwrite = false)
            dest.absolutePath
        }
    }

    suspend fun moveFile(sourcePath: String, destDirPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(sourcePath)
            val destDir = File(destDirPath)
            val dest = File(destDir, source.name)
            source.copyTo(dest, overwrite = false)
            source.delete()
            dest.absolutePath
        }
    }

    private fun getMimeType(extension: String): String = when (extension) {
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        "aac" -> "audio/aac"
        "wma" -> "audio/x-ms-wma"
        "ape" -> "audio/x-ape"
        "dsf" -> "audio/dsf"
        "dff" -> "audio/dff"
        "aif", "aiff" -> "audio/aiff"
        "mka" -> "audio/x-matroska"
        else -> "application/octet-stream"
    }
}
