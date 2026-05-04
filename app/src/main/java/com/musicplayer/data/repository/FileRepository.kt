package com.musicplayer.data.repository

import com.musicplayer.data.local.filemanager.FileItem
import com.musicplayer.data.local.filemanager.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class TagFields(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val trackNumber: String = "",
)

@Singleton
class FileRepository @Inject constructor(
    private val fileManager: FileManager,
) {
    fun getRootPath(): String = fileManager.getRootPath()

    suspend fun listDirectory(path: String): List<FileItem> = fileManager.listDirectory(path)

    suspend fun renameFile(path: String, newName: String): Result<String> =
        fileManager.renameFile(path, newName)

    suspend fun deleteFile(path: String): Result<Unit> =
        fileManager.deleteFile(path)

    suspend fun createDirectory(parentPath: String, name: String): Result<String> =
        fileManager.createDirectory(parentPath, name)

    suspend fun copyFile(sourcePath: String, destDirPath: String): Result<String> =
        fileManager.copyFile(sourcePath, destDirPath)

    suspend fun moveFile(sourcePath: String, destDirPath: String): Result<String> =
        fileManager.moveFile(sourcePath, destDirPath)

    suspend fun readTags(path: String): TagFields = withContext(Dispatchers.IO) {
        runCatching {
            val tag = AudioFileIO.read(File(path)).tag
            TagFields(
                title = tag?.getFirst(FieldKey.TITLE) ?: "",
                artist = tag?.getFirst(FieldKey.ARTIST) ?: "",
                album = tag?.getFirst(FieldKey.ALBUM) ?: "",
                year = tag?.getFirst(FieldKey.YEAR) ?: "",
                trackNumber = tag?.getFirst(FieldKey.TRACK) ?: "",
            )
        }.getOrDefault(TagFields())
    }

    suspend fun writeTags(path: String, fields: TagFields): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val audioFile = AudioFileIO.read(File(path))
            val tag = audioFile.tagOrCreateAndSetDefault
            tag.setField(FieldKey.TITLE, fields.title)
            tag.setField(FieldKey.ARTIST, fields.artist)
            tag.setField(FieldKey.ALBUM, fields.album)
            tag.setField(FieldKey.YEAR, fields.year)
            tag.setField(FieldKey.TRACK, fields.trackNumber)
            audioFile.commit()
        }
    }
}
