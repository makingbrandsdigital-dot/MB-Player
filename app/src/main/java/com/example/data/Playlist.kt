package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String, // UUID
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
