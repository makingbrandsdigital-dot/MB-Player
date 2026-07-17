package com.example.playback

import com.example.data.Track

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val progressMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val lowBatteryMode: Boolean = false,
    val isMuted: Boolean = false
)
