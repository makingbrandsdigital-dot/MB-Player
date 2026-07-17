package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.AudioRepository
import com.example.playback.PlaybackManager
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the playback engine with application context
        PlaybackManager.init(this)
        
        // Initialize local Room persistence and Repository
        val database = AppDatabase.getDatabase(this)
        val repository = AudioRepository(database.trackDao(), database.playlistDao(), database.searchHistoryDao())
        
        // Create ViewModel through our custom factory
        val viewModel = ViewModelProvider(
            this, 
            MainViewModelFactory(repository)
        )[MainViewModel::class.java]
        
        // Enable Edge-to-Edge display
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
