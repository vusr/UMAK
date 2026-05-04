package com.musicplayer.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.os.bundleOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicplayer.MainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that hosts ExoPlayer and the Media3 MediaSession.
 *
 * Audio effects (Equalizer, BassBoost, Virtualizer, LoudnessEnhancer) are owned by
 * [PlayerController], which creates them once this service publishes a valid
 * audioSessionId through the session extras.
 *
 * The persistent playback notification (with album art and transport controls) is
 * provided automatically by MediaSessionService via DefaultMediaNotificationProvider.
 */
@AndroidEntryPoint
class MusicPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        initPlayer()
    }

    private fun initPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer

        val sessionActivityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityIntent)
            .build()

        // Publish the audio session ID once ExoPlayer allocates it on first render.
        // PlayerController observes these extras to create the hardware audio effects.
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sid = exoPlayer.audioSessionId
                    if (sid != 0) {
                        mediaSession?.setSessionExtras(bundleOf("audioSessionId" to sid))
                    }
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
