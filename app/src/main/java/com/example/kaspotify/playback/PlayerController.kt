package com.example.kaspotify.playback

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.kaspotify.data.model.Song
import com.example.kaspotify.data.settings.SettingsRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode { OFF, ALL, ONE }

/** Hearing-safety prompts surfaced to the UI. */
enum class SafetyEvent { HIGH_VOLUME, TAKE_A_BREAK }

/** Single UI-facing entry point to playback. Connects to [PlaybackService] via a MediaController. */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _safetyEvents = MutableSharedFlow<SafetyEvent>(extraBufferCapacity = 2)
    val safetyEvents: SharedFlow<SafetyEvent> = _safetyEvents.asSharedFlow()

    // Hearing-safety bookkeeping.
    private var continuousPlayMs = 0L
    private var remindedThisSession = false
    private var warnedThisSession = false

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val handler = Handler(Looper.getMainLooper())
    private val registry = HashMap<Long, Song>()

    private var sleepRunnable: Runnable? = null

    private val positionPoller = object : Runnable {
        override fun run() {
            controller?.let {
                _positionMs.value = it.currentPosition.coerceAtLeast(0)
                val duration = it.duration
                _durationMs.value = if (duration > 0) duration else 0L
            }
            if (_isPlaying.value) {
                accrueListeningTime(POLL_INTERVAL_MS)
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                handler.removeCallbacks(positionPoller)
                handler.post(positionPoller)
                maybeWarnHighVolume()
            } else {
                // A pause counts as a break: reset the continuous-listening accumulator.
                continuousPlayMs = 0L
                remindedThisSession = false
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncCurrentSong()
            _durationMs.value = (controller?.duration ?: 0L).coerceAtLeast(0L)
            _positionMs.value = 0L
            _queueIndex.value = controller?.currentMediaItemIndex ?: 0
            maybeWarnHighVolume()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffle.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode.toRepeatMode()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            syncQueue()
        }
    }

    /** Must be called once (e.g. from MainActivity) before playback is used. Idempotent. */
    fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            controller = future.get().also { c ->
                c.addListener(listener)
                _shuffle.value = c.shuffleModeEnabled
                _repeatMode.value = c.repeatMode.toRepeatMode()
                _isPlaying.value = c.isPlaying
                _playbackSpeed.value = c.playbackParameters.speed
                syncCurrentSong()
                syncQueue()
                applyVolumeCap()
            }
        }, MoreExecutors.directExecutor())
    }

    /** Applies (or clears) the hearing-safety output ceiling on the ExoPlayer volume. */
    fun applyVolumeCap() {
        val c = controller ?: return
        val s = settingsRepository.settings.value
        c.volume = if (s.maxVolumeCap) (s.maxVolumePercent / 100f).coerceIn(0.1f, 1f) else 1f
    }

    private fun accrueListeningTime(deltaMs: Long) {
        if (!settingsRepository.settings.value.listeningTimeReminder) return
        continuousPlayMs += deltaMs
        if (!remindedThisSession && continuousPlayMs >= BREAK_REMINDER_MS) {
            remindedThisSession = true
            _safetyEvents.tryEmit(SafetyEvent.TAKE_A_BREAK)
        }
    }

    private fun maybeWarnHighVolume() {
        if (!settingsRepository.settings.value.highVolumeWarning) return
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).takeIf { it > 0 } ?: return
        val fraction = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        if (fraction >= HIGH_VOLUME_FRACTION) {
            if (!warnedThisSession) {
                warnedThisSession = true
                _safetyEvents.tryEmit(SafetyEvent.HIGH_VOLUME)
            }
        } else {
            // Dropped to a safe level — allow warning again if they crank it back up.
            warnedThisSession = false
        }
    }

    fun release() {
        handler.removeCallbacks(positionPoller)
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        songs.forEach { registry[it.id] = it }
        c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), 0L)
        c.prepare()
        c.play()
        syncQueue()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() {
        controller?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        registry[song.id] = song
        val index = (c.currentMediaItemIndex + 1).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(index, song.toMediaItem())
        if (c.mediaItemCount == 1) {
            c.prepare()
            c.play()
        }
        syncQueue()
    }

    fun addToQueueEnd(song: Song) {
        val c = controller ?: return
        registry[song.id] = song
        c.addMediaItem(song.toMediaItem())
        if (c.mediaItemCount == 1) {
            c.prepare()
            c.play()
        }
        syncQueue()
    }

    /** Move a queue item from [from] to [to] (both indices into [queue]). */
    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to) return
        c.moveMediaItem(from, to)
        syncQueue()
    }

    /** Remove the queue item at [index]. */
    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        c.removeMediaItem(index)
        syncQueue()
    }

    /** Sets playback speed (and matching pitch) in the 0.5x-1.25x range. 1f = normal. */
    fun setPlaybackSpeed(speed: Float) {
        val c = controller ?: return
        val clamped = speed.coerceIn(0.5f, 1.25f)
        c.playbackParameters = PlaybackParameters(clamped, clamped)
        _playbackSpeed.value = clamped
    }

    /** [minutes] null cancels any running timer. */
    fun setSleepTimer(minutes: Int?) {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        sleepRunnable = null
        _sleepTimerMinutes.value = minutes
        if (minutes != null && minutes > 0) {
            val runnable = Runnable {
                controller?.pause()
                _sleepTimerMinutes.value = null
                sleepRunnable = null
            }
            sleepRunnable = runnable
            handler.postDelayed(runnable, minutes * 60_000L)
        }
    }

    private fun syncCurrentSong() {
        val c = controller ?: return
        val mediaId = c.currentMediaItem?.mediaId?.toLongOrNull()
        _currentSong.value = mediaId?.let { registry[it] }
    }

    private fun syncQueue() {
        val c = controller ?: return
        _queue.value = (0 until c.mediaItemCount).mapNotNull { i ->
            c.getMediaItemAt(i).mediaId.toLongOrNull()?.let { registry[it] }
        }
        _queueIndex.value = c.currentMediaItemIndex
    }

    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumArtist(artist)
            .setAlbumTitle(album)
            // displayTitle/subtitle are what several lock screens & Bluetooth/Auto head units read.
            .setDisplayTitle(title)
            .setSubtitle(artist)
            .setArtworkUri(artworkUri)
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun Int.toRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
        /** Suggest a break after this much continuous playback. */
        private const val BREAK_REMINDER_MS = 60L * 60 * 1000
        /** System music-volume fraction considered "loud" for the safety warning. */
        private const val HIGH_VOLUME_FRACTION = 0.8f
    }
}
