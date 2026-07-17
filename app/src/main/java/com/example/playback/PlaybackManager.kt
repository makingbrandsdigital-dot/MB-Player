package com.example.playback

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

object PlaybackManager {
    private const val TAG = "PlaybackManager"

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var currentPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun play(track: Track, newQueue: List<Track> = emptyList()) {
        scope.launch {
            try {
                // If a new queue is provided, update it
                val finalQueue = if (newQueue.isNotEmpty()) newQueue else _state.value.queue
                val index = finalQueue.indexOfFirst { it.id == track.id }
                val queueIdx = if (index != -1) index else {
                    val updatedQueue = finalQueue + track
                    _state.update { it.copy(queue = updatedQueue) }
                    updatedQueue.size - 1
                }

                _state.update {
                    it.copy(
                        currentTrack = track,
                        queue = finalQueue,
                        queueIndex = queueIdx,
                        isPlaying = true,
                        progressMs = 0,
                        durationMs = track.durationSeconds * 1000L
                    )
                }

                // Setup Current Player
                resetPlayers()
                currentPlayer = createPlayerForTrack(track)
                currentPlayer?.let { player ->
                    player.setOnPreparedListener {
                        val vol = if (_state.value.isMuted) 0f else 1f
                        it.setVolume(vol, vol)
                        it.start()
                        _state.update { s -> s.copy(isPlaying = true, durationMs = it.duration.toLong()) }
                        startProgressTracker()
                        prepareNextTrack()
                    }
                    player.setOnCompletionListener {
                        handleTrackCompletion()
                    }
                    player.setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "Player Error: what=$what, extra=$extra")
                        next()
                        true
                    }
                    player.prepareAsync()
                }

                // Notify service to run in foreground
                AudioPlaybackService.startService(appContext!!)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing track", e)
            }
        }
    }

    private fun handleTrackCompletion() {
        if (nextPlayer != null) {
            // Native gapless transition occurred or we are switching players manually
            currentPlayer?.release()
            currentPlayer = nextPlayer
            nextPlayer = null

            val nextIdx = _state.value.queueIndex + 1
            if (nextIdx < _state.value.queue.size) {
                val nextTrack = _state.value.queue[nextIdx]
                _state.update {
                    it.copy(
                        currentTrack = nextTrack,
                        queueIndex = nextIdx,
                        isPlaying = true,
                        progressMs = 0,
                        durationMs = nextTrack.durationSeconds * 1000L
                    )
                }
                currentPlayer?.let { player ->
                    // Set listeners on the newly promoted player
                    player.setOnCompletionListener { handleTrackCompletion() }
                    player.setOnErrorListener { _, _, _ -> next(); true }
                    _state.update { s -> s.copy(durationMs = player.duration.toLong()) }
                }
                prepareNextTrack()
            } else {
                stop()
            }
        } else {
            // No preloaded player, go to next track standard way
            next()
        }
    }

    private fun createPlayerForTrack(track: Track): MediaPlayer {
        val player = MediaPlayer()
        val context = appContext ?: throw IllegalStateException("PlaybackManager not initialized")
        
        // Determine whether to play from local cached file or remote URL
        val uri = if (track.isDownloaded && track.localFilePath != null) {
            val file = File(track.localFilePath)
            if (file.exists()) {
                Uri.fromFile(file)
            } else {
                Uri.parse(track.audioUrl)
            }
        } else {
            Uri.parse(track.audioUrl)
        }
        
        player.setDataSource(context, uri)
        player.setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
        return player
    }

    private fun prepareNextTrack() {
        val nextIdx = _state.value.queueIndex + 1
        if (nextIdx < _state.value.queue.size) {
            val nextTrack = _state.value.queue[nextIdx]
            scope.launch(Dispatchers.IO) {
                try {
                    val player = createPlayerForTrack(nextTrack)
                    player.setOnPreparedListener {
                        nextPlayer = player
                        // Apply native seamless transition link
                        currentPlayer?.setNextMediaPlayer(player)
                        Log.d(TAG, "Gapless next player pre-buffered for track: ${nextTrack.title}")
                    }
                    player.setOnErrorListener { _, _, _ ->
                        nextPlayer = null
                        true
                    }
                    player.prepareAsync()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pre-buffer next track", e)
                }
            }
        }
    }

    fun togglePlayPause() {
        val player = currentPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _state.update { it.copy(isPlaying = false) }
            stopProgressTracker()
            appContext?.let { AudioPlaybackService.stopForegroundState(it) }
        } else {
            player.start()
            _state.update { it.copy(isPlaying = true) }
            startProgressTracker()
            appContext?.let { AudioPlaybackService.startService(it) }
        }
    }

    fun seekTo(positionMs: Long) {
        currentPlayer?.let { player ->
            player.seekTo(positionMs.toInt())
            _state.update { it.copy(progressMs = positionMs) }
        }
    }

    fun next() {
        val nextIdx = _state.value.queueIndex + 1
        if (nextIdx < _state.value.queue.size) {
            play(_state.value.queue[nextIdx])
        } else {
            // Loop to beginning if queue is not empty
            if (_state.value.queue.isNotEmpty()) {
                play(_state.value.queue[0])
            } else {
                stop()
            }
        }
    }

    fun previous() {
        val prevIdx = _state.value.queueIndex - 1
        if (prevIdx >= 0) {
            play(_state.value.queue[prevIdx])
        } else {
            // Seek to beginning if it is the first track
            seekTo(0)
        }
    }

    fun stop() {
        resetPlayers()
        stopProgressTracker()
        _state.update {
            it.copy(
                currentTrack = null,
                isPlaying = false,
                progressMs = 0,
                durationMs = 0,
                queueIndex = -1
            )
        }
        appContext?.let { AudioPlaybackService.stopService(it) }
    }

    fun setLowBatteryMode(enabled: Boolean) {
        _state.update { it.copy(lowBatteryMode = enabled) }
        if (enabled) {
            // In low-battery mode, slow down screen progress updates to save CPU cycles
            startProgressTracker()
        } else {
            startProgressTracker()
        }
    }

    fun setMute(mute: Boolean) {
        _state.update { it.copy(isMuted = mute) }
        val vol = if (mute) 0f else 1f
        try {
            currentPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting current player volume", e)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        val updateInterval = if (_state.value.lowBatteryMode) 1000L else 250L // Slower updates in low battery mode
        progressJob = scope.launch {
            while (isActive) {
                currentPlayer?.let { player ->
                    if (player.isPlaying) {
                        _state.update { it.copy(progressMs = player.currentPosition.toLong()) }
                    }
                }
                delay(updateInterval)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun resetPlayers() {
        try {
            currentPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing currentPlayer", e)
        } finally {
            currentPlayer = null
        }

        try {
            nextPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing nextPlayer", e)
        } finally {
            nextPlayer = null
        }
    }
}
