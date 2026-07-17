package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "query", // "query", "artist", or "track"
    val title: String? = null,
    val artist: String? = null,
    val youtubeId: String? = null
)
