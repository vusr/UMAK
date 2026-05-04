package com.musicplayer.ui.screens.playlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.musicplayer.domain.model.Track
import com.musicplayer.ui.navigation.Screen
import com.musicplayer.util.TimeUtil
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: Long,
    navController: NavController,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    viewModel.loadPlaylist(playlistId)

    // Local mutable track list drives the UI; persisted when drag ends
    var tracks by remember(state.tracks) { mutableStateOf(state.tracks) }
    var showAddTracksSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        tracks = tracks.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    // Persist reordered list when drag ends
    var wasDragging by remember { mutableStateOf(false) }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (wasDragging && !reorderState.isAnyItemDragging) {
            viewModel.reorderTracks(tracks.map { it.id })
        }
        wasDragging = reorderState.isAnyItemDragging
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(state.playlist?.name ?: "Playlist") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddTracksSheet = true }) {
                        Icon(Icons.Default.Add, "Add tracks")
                    }
                    IconButton(onClick = {
                        viewModel.playTracks(0)
                        navController.navigate(Screen.NowPlaying.route)
                    }) {
                        Icon(Icons.Default.PlayArrow, "Play all")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            items(tracks, key = { it.id }) { track ->
                ReorderableItem(reorderState, key = track.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "drag_elevation")
                    val swipeState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                tracks = tracks.toMutableList().apply { remove(track) }
                                viewModel.removeTrack(track.id)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = swipeState,
                        backgroundContent = {
                            val color = MaterialTheme.colorScheme.errorContainer
                            Surface(color = color, modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        },
                        modifier = Modifier.animateItem(),
                    ) {
                        Surface(shadowElevation = elevation) {
                            PlaylistTrackRow(
                                track = track,
                                dragHandleModifier = Modifier.draggableHandle(),
                                onClick = {
                                    viewModel.playTracks(tracks.indexOf(track))
                                    navController.navigate(Screen.NowPlaying.route)
                                },
                            )
                        }
                    }
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No tracks yet. Tap + to add some.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    // ---- Add Tracks Bottom Sheet ----
    if (showAddTracksSheet) {
        ModalBottomSheet(onDismissRequest = { showAddTracksSheet = false }) {
            Text(
                "Add tracks",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            val existingIds = remember(state.tracks) { state.tracks.map { it.id }.toSet() }
            val available = state.allTracks.filter { it.id !in existingIds }
            if (available.isEmpty()) {
                Text(
                    "All tracks are already in this playlist.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    items(available) { track ->
                        ListItem(
                            headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(track.artist, maxLines = 1) },
                            trailingContent = {
                                Text(
                                    TimeUtil.formatDuration(track.duration),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addTrack(track.id)
                                    showAddTracksSheet = false
                                },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackRow(
    track: Track,
    dragHandleModifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = dragHandleModifier.padding(horizontal = 12.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = TimeUtil.formatDuration(track.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
    HorizontalDivider()
}
