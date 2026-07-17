package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Playlist
import com.example.data.Track
import com.example.playback.PlaybackState
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.data.SearchHistoryEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // View Model states
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isSearchingOnline by viewModel.isSearchingOnline.collectAsState()
    val onlineResults by viewModel.onlineSearchResults.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    
    val allTracks by viewModel.allTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val activePlaylistId by viewModel.activePlaylistId.collectAsState()
    val playlistTracks by viewModel.playlistTracks.collectAsState()

    // Local UI states
    var showPlayerFullScreen by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddTrackDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf<Track?>(null) }
    var isOledScreenSaverActive by remember { mutableStateOf(false) }

    val activePlaylistName = allPlaylists.find { it.id == activePlaylistId }?.name ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (playbackState.lowBatteryMode) DeepBlack else DarkBackground)
    ) {
        // Main Screen Content
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (playbackState.lowBatteryMode) DeepBlack else SurfaceDark,
                        titleContentColor = AmberPrimary
                    ),
                    title = {
                        Column {
                            Text(
                                text = "GraceAudio",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("app_title")
                            )
                            Text(
                                text = "Background Devotional Player",
                                fontSize = 11.sp,
                                color = OnDarkSecondary
                            )
                        }
                    },
                    actions = {
                        // Low Battery Toggle
                        IconButton(
                            onClick = {
                                viewModel.toggleLowBatteryMode()
                                Toast.makeText(
                                    context,
                                    if (playbackState.lowBatteryMode) "Low-Battery Mode Enabled (Updates Throttled)" 
                                    else "Low-Battery Mode Disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("low_battery_toggle")
                        ) {
                            Icon(
                                imageVector = if (playbackState.lowBatteryMode) Icons.Default.BatteryAlert else Icons.Default.BatteryChargingFull,
                                contentDescription = "Toggle Low Battery Mode",
                                tint = if (playbackState.lowBatteryMode) AmberPrimary else OnDarkSecondary
                            )
                        }

                        // Add Song Button
                        IconButton(
                            onClick = { showAddTrackDialog = true },
                            modifier = Modifier.testTag("add_custom_song_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom YouTube Song",
                                tint = AmberPrimary
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // YouTube Browser View is the primary view
                Box(modifier = Modifier.weight(1f)) {
                    val searchHistory by viewModel.recentSearchHistory.collectAsState()
                    YouTubeBrowserView(
                        initialSearchQuery = searchQuery,
                        searchHistory = searchHistory,
                        onPlayTrackDirectly = { title, artist, ytId ->
                            viewModel.playCustomTrack(title, artist, ytId, "YouTube")
                        },
                        onAddToPlaylist = { track ->
                            viewModel.addCustomYoutubeTrack(track.title, track.artist, track.youtubeId ?: "", "YouTube")
                            showAddToPlaylistSheet = track
                        },
                        onSaveSearch = { query -> viewModel.saveSearchQuery(query, "query") },
                        onDeleteHistoryItem = { id -> viewModel.deleteSearchHistoryItem(id) },
                        onClearHistory = { viewModel.clearSearchHistory() }
                    )
                }

                // Small Player bar spacer
                if (playbackState.currentTrack != null) {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // Mini player anchored to bottom
        if (playbackState.currentTrack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp) // Anchored to bottom since bottom buttons are removed
                    .padding(horizontal = 8.dp)
            ) {
                MiniPlayer(
                    track = playbackState.currentTrack!!,
                    isPlaying = playbackState.isPlaying,
                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipNext() },
                    onClick = { showPlayerFullScreen = true },
                    lowBatteryMode = playbackState.lowBatteryMode
                )
            }
        }

        // Expanded player full-screen
        AnimatedVisibility(
            visible = showPlayerFullScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            FullscreenPlayer(
                playbackState = playbackState,
                onClose = { showPlayerFullScreen = false },
                onPlayPauseToggle = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrev = { viewModel.skipPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onBatterySaverClick = { isOledScreenSaverActive = true },
                onAddToPlaylistClick = { showAddToPlaylistSheet = playbackState.currentTrack },
                onMuteToggle = { viewModel.setMuted(it) }
            )
        }

        // Battery saver dim overlay
        if (isOledScreenSaverActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBlack)
                    .clickable { isOledScreenSaverActive = false }
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = "OLED battery saver is active",
                        tint = LowBatteryDimGold,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AMOLED Battery Saver Active",
                        color = Color.DarkGray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Playing: ${playbackState.currentTrack?.title ?: "No track"}",
                        color = Color.DarkGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "TAP SCREEN TO WAKE",
                        color = LowBatteryDimGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Dialogs
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreate = { viewModel.createPlaylist(it); showCreatePlaylistDialog = false }
            )
        }

        if (showAddTrackDialog) {
            AddCustomTrackDialog(
                onDismiss = { showAddTrackDialog = false },
                onAdd = { title, artist, ytId, cat ->
                    val success = viewModel.addCustomYoutubeTrack(title, artist, ytId, cat)
                    if (success) {
                        Toast.makeText(context, "Track added successfully!", Toast.LENGTH_SHORT).show()
                        showAddTrackDialog = false
                    } else {
                        Toast.makeText(context, "Error: Please fill in all fields correctly!", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        if (showAddToPlaylistSheet != null) {
            AddToPlaylistDialog(
                track = showAddToPlaylistSheet!!,
                playlists = allPlaylists,
                onDismiss = { showAddToPlaylistSheet = null },
                onSelectPlaylist = { playlist ->
                    viewModel.addTrackToPlaylist(playlist.id, showAddToPlaylistSheet!!)
                    Toast.makeText(context, "Added to playlist: ${playlist.name}", Toast.LENGTH_SHORT).show()
                    showAddToPlaylistSheet = null
                }
            )
        }
    }
}

// ------------------------------------ SUB-VIEWS ------------------------------------

@Composable
fun ExploreView(
    searchQuery: String,
    isSearchingOnline: Boolean,
    onlineResults: List<Track>,
    localTracks: List<Track>,
    selectedCategory: String,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    downloadProgress: Map<String, Int>,
    onAddToPlaylist: (Track) -> Unit,
    onDirectYouTubeSearch: () -> Unit
) {
    val displayedTracks = remember(localTracks, selectedCategory, searchQuery) {
        localTracks.filter { track ->
            val matchesCategory = selectedCategory == "All" || track.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() || 
                    track.title.contains(searchQuery, ignoreCase = true) || 
                    track.artist.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    if (searchQuery.isNotEmpty() && onlineResults.isEmpty() && !isSearchingOnline) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* Search is triggered by Enter or trailing icon */ },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated, contentColor = AmberPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Search YouTube via Gemini AI")
            }
            Button(
                onClick = onDirectYouTubeSearch,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📺 Search directly on YouTube Browser")
            }
        }
    }

    if (isSearchingOnline) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AmberPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Gemini is fetching YouTube songs...", color = OnDarkSecondary)
            }
        }
    } else {
        val tracksToRender = if (searchQuery.isNotEmpty() && onlineResults.isNotEmpty()) onlineResults else displayedTracks

        if (tracksToRender.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.MusicNote, "No tracks", tint = MutedText, modifier = Modifier.size(64.dp))
                    Text("No tracks found", color = OnDarkSecondary)
                    Text("Try adding a custom YouTube ID above!", color = MutedText, fontSize = 12.sp)
                    if (searchQuery.isNotEmpty()) {
                        Button(
                            onClick = onDirectYouTubeSearch,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("📺 Search '$searchQuery' on YouTube")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isNotEmpty() && onlineResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "Online YouTube Search Results",
                            color = AmberPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                items(tracksToRender, key = { it.id }) { track ->
                    TrackRowItem(
                        track = track,
                        onPlay = { onPlayTrack(track, tracksToRender) },
                        onDownload = { onDownloadTrack(track) },
                        downloadProgress = downloadProgress[track.id],
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistsView(
    playlists: List<Playlist>,
    onCreatePlaylistClick: () -> Unit,
    onPlaylistSelect: (Playlist) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Playlists", color = OnDarkPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(
                onClick = onCreatePlaylistClick,
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = DeepBlack),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_playlist_button")
            ) {
                Icon(Icons.Default.Add, "Create")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New")
            }
        }

        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No custom playlists yet. Tap 'New' to create one!", color = MutedText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaylistSelect(playlist) }
                            .testTag("playlist_item_${playlist.name.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QueueMusic, "Playlist", tint = AmberPrimary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(playlist.name, color = OnDarkPrimary, fontWeight = FontWeight.Bold)
                                    Text("Custom Playlist", color = OnDarkSecondary, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { onDeletePlaylist(playlist) }) {
                                Icon(Icons.Default.Delete, "Delete Playlist", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlistName: String,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onRemoveTrack: (String) -> Unit,
    downloadProgress: Map<String, Int>,
    onDownload: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = AmberPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(playlistName, color = OnDarkPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${tracks.size} tracks", color = OnDarkSecondary, fontSize = 12.sp)
            }
        }

        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Playlist is empty. Add songs from Explore tab!", color = MutedText)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Video / Album thumbnail
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceElevated)
                                    .clickable { onPlayTrack(track) }
                            ) {
                                if (track.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = "Thumbnail",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, "Audio", tint = AmberPrimary, modifier = Modifier.align(Alignment.Center))
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPlayTrack(track) }
                            ) {
                                Text(track.title, color = OnDarkPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = OnDarkSecondary, fontSize = 12.sp, maxLines = 1)
                            }

                            // Download action
                            if (track.isDownloaded) {
                                Icon(Icons.Default.Check, "Downloaded", tint = SuccessGreen, modifier = Modifier.size(24.dp))
                            } else {
                                val progress = downloadProgress[track.id]
                                if (progress != null) {
                                    CircularProgressIndicator(
                                        progress = { progress / 100f },
                                        modifier = Modifier.size(24.dp),
                                        color = AmberPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(onClick = { onDownload(track) }) {
                                        Icon(Icons.Default.Download, "Download Offline", tint = OnDarkSecondary)
                                    }
                                }
                            }

                            IconButton(onClick = { onRemoveTrack(track.id) }) {
                                Icon(Icons.Default.Delete, "Remove from Playlist", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsView(
    downloadedTracks: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Offline Downloads",
            color = OnDarkPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (downloadedTracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudDownload, "No Downloads", tint = MutedText, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No downloaded songs yet.", color = OnDarkSecondary)
                    Text("Tap the download icon in Explore to save offline!", color = MutedText, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(downloadedTracks, key = { it.id }) { track ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayTrack(track) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MusicNote, "Audio", tint = SuccessGreen, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = OnDarkPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(track.artist, color = OnDarkSecondary, fontSize = 12.sp)
                                Text("Offline (Local Saved)", color = SuccessGreen, fontSize = 10.sp)
                            }
                            IconButton(onClick = { onRemoveDownload(track) }) {
                                Icon(Icons.Default.Delete, "Delete Download", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackRowItem(
    track: Track,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    downloadProgress: Int?,
    onAddToPlaylist: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("track_row_${track.id}")
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Cover Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevated)
            ) {
                if (track.thumbnailUrl != null) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.MusicNote, "Audio", tint = AmberPrimary, modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = OnDarkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = OnDarkSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(track.category, fontSize = 9.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SurfaceElevated,
                            labelColor = AmberPrimary
                        ),
                        border = null,
                        modifier = Modifier.height(18.dp)
                    )
                    if (track.isDownloaded) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.CloudDone, "Offline Available", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Playlist Add Action
            IconButton(onClick = onAddToPlaylist, modifier = Modifier.testTag("add_to_playlist_${track.id}")) {
                Icon(Icons.Default.PlaylistAdd, "Add to Playlist", tint = AmberPrimary)
            }

            // Download Action
            if (track.isDownloaded) {
                Icon(Icons.Default.Check, "Downloaded", tint = SuccessGreen, modifier = Modifier.size(24.dp))
            } else {
                if (downloadProgress != null) {
                    CircularProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.size(24.dp),
                        color = AmberPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onDownload, modifier = Modifier.testTag("download_${track.id}")) {
                        Icon(Icons.Default.Download, "Download", tint = OnDarkSecondary)
                    }
                }
            }
        }
    }
}

// ------------------------------------ PLAYBACK PANELS ------------------------------------

@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onClick: () -> Unit,
    lowBatteryMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (lowBatteryMode) DeepBlack else SurfaceDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("mini_player")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceElevated)
            ) {
                if (track.thumbnailUrl != null) {
                    AsyncImage(
                        model = track.thumbnailUrl,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.MusicNote, "Audio", tint = AmberPrimary, modifier = Modifier.align(Alignment.Center))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = OnDarkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = OnDarkSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPlayPauseToggle, modifier = Modifier.testTag("mini_play_pause")) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = AmberPrimary
                )
            }

            IconButton(onClick = onSkipNext, modifier = Modifier.testTag("mini_skip_next")) {
                Icon(Icons.Default.SkipNext, "Next Track", tint = AmberPrimary)
            }
        }
    }
}

@Composable
fun FullscreenPlayer(
    playbackState: PlaybackState,
    onClose: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onBatterySaverClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onMuteToggle: (Boolean) -> Unit
) {
    val track = playbackState.currentTrack ?: return

    val progressSec = playbackState.progressMs / 1000
    val durationSec = playbackState.durationMs / 1000

    fun formatTime(sec: Long): String {
        val minutes = sec / 60
        val seconds = sec % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (playbackState.lowBatteryMode) DeepBlack else DarkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, "Close Player", tint = AmberPrimary, modifier = Modifier.size(32.dp))
                }
                Text("Now Playing", color = OnDarkPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onAddToPlaylistClick) {
                    Icon(Icons.Default.PlaylistAdd, "Add to Playlist", tint = AmberPrimary)
                }
            }

            // Audio / Video Toggle
            if (track.youtubeId != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isVideo = playbackState.isMuted
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!isVideo) AmberPrimary else Color.Transparent)
                            .clickable { onMuteToggle(false) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🎵 Audio Only",
                            color = if (!isVideo) Color.White else OnDarkSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isVideo) AmberPrimary else Color.Transparent)
                            .clickable { onMuteToggle(true) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "📺 Watch Video",
                            color = if (isVideo) Color.White else OnDarkSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Cover Art Frame or Video Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .aspectRatio(16f / 10f) // Slightly wider layout for immersive video experience
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                if (playbackState.isMuted && track.youtubeId != null) {
                    YouTubeVideoPlayer(
                        youtubeId = track.youtubeId,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (track.thumbnailUrl != null) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = "Cover Art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, "Audio", tint = AmberPrimary, modifier = Modifier.size(128.dp))
                    }
                }
            }

            // Track Details
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = track.title,
                    color = OnDarkPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    color = AmberPrimary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (track.isDownloaded) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, "Downloaded", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Available Offline", color = SuccessGreen, fontSize = 11.sp)
                    }
                }
            }

            // Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (playbackState.durationMs > 0) playbackState.progressMs.toFloat() else 0f,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..(if (playbackState.durationMs > 0) playbackState.durationMs.toFloat() else 100f),
                    colors = SliderDefaults.colors(
                        activeTrackColor = AmberPrimary,
                        inactiveTrackColor = Color.DarkGray,
                        thumbColor = AmberPrimary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(progressSec), color = OnDarkSecondary, fontSize = 12.sp)
                    Text(formatTime(durationSec), color = OnDarkSecondary, fontSize = 12.sp)
                }
            }

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrev) {
                    Icon(Icons.Default.SkipPrevious, "Previous Track", tint = AmberPrimary, modifier = Modifier.size(42.dp))
                }
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(AmberPrimary)
                        .clickable { onPlayPauseToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = DeepBlack,
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Default.SkipNext, "Next Track", tint = AmberPrimary, modifier = Modifier.size(42.dp))
                }
            }

            // Low Battery & Transitions Utility
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onBatterySaverClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = AmberPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.BatteryChargingFull, "Battery")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable AMOLED Dim Screen", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Seamless transition active",
                    color = Color.DarkGray,
                    fontSize = 10.sp
                )
            }
        }
    }
}

// ------------------------------------ DIALOGS & OVERLAYS ------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Create Playlist", color = AmberPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = OnDarkPrimary,
                        unfocusedTextColor = OnDarkPrimary,
                        focusedIndicatorColor = AmberPrimary,
                        unfocusedIndicatorColor = SurfaceElevated
                    ),
                    singleLine = true,
                    modifier = Modifier.testTag("create_playlist_input")
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OnDarkSecondary)
                    }
                    Button(
                        onClick = { onCreate(name) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = DeepBlack),
                        modifier = Modifier.testTag("create_playlist_submit")
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomTrackDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var youtubeId by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Worship") }

    val categories = listOf("Pop", "Rock", "Lo-Fi", "Worship", "Classical")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add YouTube Track", color = AmberPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Song Title") },
                    colors = TextFieldDefaults.colors(focusedTextColor = OnDarkPrimary, unfocusedTextColor = OnDarkPrimary, focusedIndicatorColor = AmberPrimary),
                    singleLine = true,
                    modifier = Modifier.testTag("add_song_title")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist / Band") },
                    colors = TextFieldDefaults.colors(focusedTextColor = OnDarkPrimary, unfocusedTextColor = OnDarkPrimary, focusedIndicatorColor = AmberPrimary),
                    singleLine = true,
                    modifier = Modifier.testTag("add_song_artist")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = youtubeId,
                    onValueChange = { youtubeId = it },
                    label = { Text("YouTube Video ID") },
                    colors = TextFieldDefaults.colors(focusedTextColor = OnDarkPrimary, unfocusedTextColor = OnDarkPrimary, focusedIndicatorColor = AmberPrimary),
                    singleLine = true,
                    placeholder = { Text("e.g. iJCV_2S9xDg") },
                    modifier = Modifier.testTag("add_song_youtube_id")
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection
                Text("Category", color = OnDarkSecondary, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCat == cat
                        AssistChip(
                            onClick = { selectedCat = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) AmberPrimary else SurfaceElevated,
                                labelColor = if (isSelected) DeepBlack else OnDarkSecondary
                            ),
                            border = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OnDarkSecondary)
                    }
                    Button(
                        onClick = { onAdd(title, artist, youtubeId, selectedCat) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = DeepBlack),
                        modifier = Modifier.testTag("add_song_submit")
                    ) {
                        Text("Add Song")
                    }
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add Track to Playlist", color = AmberPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(track.title, color = OnDarkPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(track.artist, color = OnDarkSecondary, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(16.dp))

                if (playlists.isEmpty()) {
                    Text("No playlists found. Create one first!", color = MutedText, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(playlists) { playlist ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlaylist(playlist) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlaylistPlay, "Playlist", tint = AmberPrimary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(playlist.name, color = OnDarkPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close", color = OnDarkSecondary)
                }
            }
        }
    }
}

@Composable
fun YouTubeVideoPlayer(youtubeId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webView = remember(youtubeId) {
        WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
                domStorageEnabled = true
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false
                }
            }
            val html = """
                <html>
                <body style="margin:0;padding:0;background-color:black;">
                    <iframe 
                        width="100%" 
                        height="100%" 
                        src="https://www.youtube.com/embed/$youtubeId?autoplay=1&mute=0&controls=1&modestbranding=1&rel=0" 
                        frameborder="0" 
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                        allowfullscreen>
                    </iframe>
                </body>
                </html>
            """.trimIndent()
            loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}

@Composable
fun YouTubeBrowserView(
    initialSearchQuery: String = "",
    searchHistory: List<SearchHistoryEntry> = emptyList(),
    onPlayTrackDirectly: (String, String, String) -> Unit, // title, artist, youtubeId
    onAddToPlaylist: (Track) -> Unit,
    onSaveSearch: (String) -> Unit = {},
    onDeleteHistoryItem: (Long) -> Unit = {},
    onClearHistory: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    
    val initialUrl = remember(initialSearchQuery) {
        if (initialSearchQuery.isNotEmpty()) {
            val encoded = java.net.URLEncoder.encode(initialSearchQuery, "UTF-8")
            "https://m.youtube.com/results?search_query=$encoded"
        } else {
            "https://m.youtube.com"
        }
    }
    
    var currentUrl by remember { mutableStateOf(initialUrl) }
    var currentTitle by remember { mutableStateOf("YouTube") }
    
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    var customView by remember { mutableStateOf<android.view.View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val customWebChromeClient = remember {
        object : WebChromeClient() {
            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
            }

            override fun onHideCustomView() {
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
            }
        }
    }
    
    // Check if the current URL contains a watchable video ID
    val detectedYoutubeId = remember(currentUrl) { extractYoutubeId(currentUrl) }
    
    // Clean up title: e.g. "Song Name - YouTube" -> "Song Name"
    val cleanedTitle = remember(currentTitle) {
        currentTitle.replace(" - YouTube", "", ignoreCase = true).trim()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sleek navigation row (Back, Forward, Home, Refresh)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Back button
                IconButton(
                    onClick = { webViewRef?.goBack() },
                    enabled = webViewRef?.canGoBack() == true
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (webViewRef?.canGoBack() == true) AmberPrimary else OnDarkSecondary.copy(alpha = 0.5f)
                    )
                }
                
                // Forward button
                IconButton(
                    onClick = { webViewRef?.goForward() },
                    enabled = webViewRef?.canGoForward() == true
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (webViewRef?.canGoForward() == true) AmberPrimary else OnDarkSecondary.copy(alpha = 0.5f)
                    )
                }

                // Home button
                IconButton(
                    onClick = { webViewRef?.loadUrl("https://m.youtube.com") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = AmberPrimary
                    )
                }

                // Refresh button
                IconButton(
                    onClick = { webViewRef?.reload() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = AmberPrimary
                    )
                }
            }

            // Current YouTube Page Title
            Text(
                text = cleanedTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnDarkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                textAlign = TextAlign.End
            )
        }

        // WebView Container
        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let { currentUrl = it }
                                view?.title?.let { currentTitle = it }
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                url?.let { currentUrl = it }
                                view?.title?.let { currentTitle = it }
                            }
                        }
                        webChromeClient = customWebChromeClient
                        loadUrl(initialUrl)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating action panel when video is detected
        AnimatedVisibility(
            visible = detectedYoutubeId != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            detectedYoutubeId?.let { ytId ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thumbnail Preview
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = "https://img.youtube.com/vi/$ytId/hqdefault.jpg",
                                contentDescription = "Video Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Info & Actions
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cleanedTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = OnDarkPrimary
                            )
                            Text(
                                text = "ID: $ytId • Ready for GraceAudio",
                                fontSize = 11.sp,
                                color = OnDarkSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onPlayTrackDirectly(cleanedTitle, "YouTube Browser", ytId)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play in App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        // Construct track and prompt to add to playlist
                                        val track = Track(
                                            id = ytId,
                                            title = cleanedTitle,
                                            artist = "YouTube Browser",
                                            audioUrl = "https://archive.org/download/hymns_instrumental_01/amazing_grace.mp3",
                                            durationSeconds = 240,
                                            category = "YouTube",
                                            youtubeId = ytId,
                                            thumbnailUrl = "https://img.youtube.com/vi/$ytId/hqdefault.jpg"
                                        )
                                        onAddToPlaylist(track)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, "Add", modifier = Modifier.size(16.dp), tint = OnDarkPrimary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Playlist", fontSize = 11.sp, color = OnDarkPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (customView != null) {
        Dialog(
            onDismissRequest = {
                customWebChromeClient.onHideCustomView()
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = {
                        (customView?.parent as? android.view.ViewGroup)?.removeView(customView)
                        customView!!
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Close button overlay
                IconButton(
                    onClick = { customWebChromeClient.onHideCustomView() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

fun extractYoutubeId(url: String): String? {
    return try {
        if (url.contains("youtube.com/watch")) {
            val uri = android.net.Uri.parse(url)
            uri.getQueryParameter("v")
        } else if (url.contains("youtu.be/")) {
            url.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
        } else if (url.contains("youtube.com/embed/")) {
            url.substringAfter("embed/").substringBefore("?").substringBefore("/")
        } else if (url.contains("youtube.com/v/")) {
            url.substringAfter("v/").substringBefore("?").substringBefore("/")
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
