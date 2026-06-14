package com.example.kaspotify.playback

import android.content.Context
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Background playback host. Holds the ExoPlayer and exposes it as a MediaSession. */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var equalizerController: EqualizerController
    @Inject lateinit var visualizerController: VisualizerController
    @Inject lateinit var reverbController: ReverbController

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Fix a stable audio session id so the Equalizer can bind to the player's output.
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioSessionId = audioManager.generateAudioSessionId()
        player.setAudioSessionId(audioSessionId)
        equalizerController.attach(audioSessionId)
        visualizerController.attach(audioSessionId)
        reverbController.attach(audioSessionId)

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        equalizerController.release()
        visualizerController.release()
        reverbController.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
