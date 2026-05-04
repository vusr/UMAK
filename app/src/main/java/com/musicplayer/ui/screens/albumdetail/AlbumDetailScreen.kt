package com.musicplayer.ui.screens.albumdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.musicplayer.ui.components.EmptyState
import com.musicplayer.ui.components.TrackListItem
import com.musicplayer.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    navController: NavController,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.loadAlbum(albumId)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(state.album?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(shape = MaterialTheme.shapes.medium, modifier = Modifier.size(120.dp)) {
                        AsyncImage(
                            model = state.album?.albumArtUri,
                            contentDescription = "Album art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.album?.name ?: "", style = MaterialTheme.typography.headlineMedium)
                        Text(state.album?.artist ?: "", style = MaterialTheme.typography.bodyLarge)
                        if ((state.album?.year ?: 0) > 0) {
                            Text(state.album!!.year.toString(), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("${state.tracks.size} tracks", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Button(onClick = {
                                viewModel.playTracks(state.tracks, 0)
                                navController.navigate(Screen.NowPlaying.route)
                            }) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Play All")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                viewModel.shuffleTracks(state.tracks)
                                navController.navigate(Screen.NowPlaying.route)
                            }) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            if (state.tracks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.MusicNote,
                        message = "No tracks in this album",
                        modifier = Modifier.height(200.dp),
                    )
                }
            } else {
                itemsIndexed(state.tracks) { index, track ->
                    TrackListItem(
                        track = track,
                        onClick = {
                            viewModel.playTracks(state.tracks, index)
                            navController.navigate(Screen.NowPlaying.route)
                        },
                        onFavoriteToggle = {},
                    )
                }
            }
        }
    }
}
