package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String, // Can be YouTube Video ID or custom UUID
    val title: String,
    val artist: String,
    val audioUrl: String, // Remote audio stream URL
    val durationSeconds: Int,
    val category: String, // e.g. "Worship", "Hymns", "Gospel", "Contemporary"
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val youtubeId: String? = null,
    val thumbnailUrl: String? = null
)
