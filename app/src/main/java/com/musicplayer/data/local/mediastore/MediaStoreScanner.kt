package com.musicplayer.data.local.mediastore

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.musicplayer.domain.model.Album
import com.musicplayer.domain.model.Artist
import com.musicplayer.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans device storage via MediaStore API and returns structured music metadata.
 * All queries run on IO dispatcher.
 */
@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class ScanResult(
        val tracks: List<Track>,
        val albums: List<Album>,
        val artists: List<Artist>,
    )

    suspend fun scan(
        onProgress: (suspend (scanned: Int, total: Int) -> Unit)? = null,
    ): ScanResult = withContext(Dispatchers.IO) {
        val tracks = scanTracks(onProgress)
        val albums = buildAlbums(tracks)
        val artists = buildArtists(tracks)
        ScanResult(tracks, albums, artists)
    }

    private suspend fun scanTracks(
        onProgress: (suspend (scanned: Int, total: Int) -> Unit)? = null,
    ): List<Track> {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.BITRATE,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 5000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, sortOrder,
        )?.use { cursor ->
            val total = cursor.count
            var scanned = 0

            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val bitrateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val filePath = cursor.getString(dataCol) ?: continue
                val trackNum = cursor.getInt(trackCol)

                // Read hi-res metadata (sample rate, bit depth) via MediaMetadataRetriever
                val (sampleRate, bitDepth) = readAudioMetadata(filePath)
                val replayGain = readReplayGain(filePath)

                tracks.add(
                    Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Unknown",
                        artist = cursor.getString(artistCol) ?: "Unknown Artist",
                        albumName = cursor.getString(albumCol) ?: "Unknown Album",
                        albumId = cursor.getLong(albumIdCol),
                        artistId = cursor.getLong(artistIdCol),
                        duration = cursor.getLong(durationCol),
                        filePath = filePath,
                        mimeType = cursor.getString(mimeCol) ?: "",
                        sampleRate = sampleRate,
                        bitDepth = bitDepth,
                        bitrate = cursor.getInt(bitrateCol) / 1000,
                        fileSize = cursor.getLong(sizeCol),
                        trackNumber = trackNum % 1000,
                        discNumber = trackNum / 1000,
                        year = cursor.getInt(yearCol),
                        genre = "",
                        dateAdded = cursor.getLong(dateAddedCol),
                        dateModified = cursor.getLong(dateModifiedCol),
                        replayGainTrack = replayGain,
                    )
                )

                scanned++
                onProgress?.invoke(scanned, total)
            }
        }
        return tracks
    }

    private fun readAudioMetadata(filePath: String): Pair<Int, Int> {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(filePath)
            val sampleRate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 44100
            val bitDepth = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull() ?: 16
            mmr.release()
            Pair(sampleRate, bitDepth)
        } catch (e: Exception) {
            Pair(44100, 16)
        }
    }

    private fun readReplayGain(filePath: String): Float {
        return try {
            val af = AudioFileIO.read(File(filePath))
            val tag = af.tag ?: return 0f
            // Use valueOf() so the call compiles against any jaudiotagger version;
            // if this version lacks the constant, IllegalArgumentException is caught below.
            val fieldKey = FieldKey.valueOf("REPLAYGAIN_TRACK_GAIN")
            val raw = tag.getFirst(fieldKey) ?: return 0f
            raw.trim().removeSuffix(" dB").removeSuffix("dB").trim().toFloatOrNull() ?: 0f
        } catch (_: IllegalArgumentException) {
            0f  // jaudiotagger version does not expose REPLAYGAIN_TRACK_GAIN
        } catch (_: Exception) {
            0f
        }
    }

    private fun buildAlbums(tracks: List<Track>): List<Album> {
        return tracks.groupBy { it.albumId }.map { (albumId, albumTracks) ->
            val first = albumTracks.first()
            Album(
                id = albumId,
                name = first.albumName,
                artist = first.artist,
                artistId = first.artistId,
                trackCount = albumTracks.size,
                year = albumTracks.maxOfOrNull { it.year } ?: 0,
                albumArtUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString(),
                genre = first.genre,
            )
        }
    }

    private fun buildArtists(tracks: List<Track>): List<Artist> {
        return tracks.groupBy { it.artistId }.map { (artistId, artistTracks) ->
            Artist(
                id = artistId,
                name = artistTracks.first().artist,
                albumCount = artistTracks.map { it.albumId }.distinct().size,
                trackCount = artistTracks.size,
            )
        }
    }
}
