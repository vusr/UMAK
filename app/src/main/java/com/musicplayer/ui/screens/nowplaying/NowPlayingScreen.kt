package com.musicplayer.ui.screens.nowplaying

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.musicplayer.ui.components.VolumeSlider
import com.musicplayer.ui.navigation.Screen
import com.musicplayer.util.AudioFormatUtil
import com.musicplayer.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    navController: NavController,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val track = state.currentTrack

    var playbackErrorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.playbackError.collect { error ->
            playbackErrorMessage = error
        }
    }

    if (playbackErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { playbackErrorMessage = null },
            title = { Text("Playback Error") },
            text = { Text(playbackErrorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { playbackErrorMessage = null }) { Text("OK") }
            },
        )
    }

    var showSleepTimerSheet by remember { mutableStateOf(false) }
    if (showSleepTimerSheet) {
        SleepTimerSheet(
            sleepTimerEndMs = state.sleepTimerEndMs,
            onSelectMinutes = { viewModel.setSleepTimer(it) },
            onSelectEndOfTrack = { viewModel.setSleepTimerEndOfTrack() },
            onCancel = { viewModel.cancelSleepTimer() },
            onDismiss = { showSleepTimerSheet = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Collapse")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Equalizer.route) }) {
                        Icon(Icons.Default.Equalizer, "Equalizer")
                    }
                    IconButton(onClick = { showSleepTimerSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep timer",
                            tint = if (state.sleepTimerEndMs != 0L)
                                MaterialTheme.colorScheme.primary
                            else
                                LocalContentColor.current,
                        )
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (maxWidth > maxHeight) {
                // ── Landscape layout ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlbumArt(
                        albumId = track?.albumId,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(vertical = 8.dp),
                    )
                    Spacer(Modifier.width(24.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TrackInfoRow(state = state, onFavoriteClick = viewModel::toggleFavorite)
                        Spacer(Modifier.height(8.dp))
                        SeekSection(state = state, onSeek = viewModel::seekTo)
                        Spacer(Modifier.height(8.dp))
                        TransportRow(state = state, viewModel = viewModel)
                        Spacer(Modifier.height(12.dp))
                        VolumeSlider(
                            level = state.volumeLevel,
                            onLevelChange = viewModel::setVolumeLevel,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                // ── Portrait layout ───────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(16.dp))
                    AlbumArt(
                        albumId = track?.albumId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                    Spacer(Modifier.height(24.dp))
                    TrackInfoRow(state = state, onFavoriteClick = viewModel::toggleFavorite)
                    Spacer(Modifier.height(16.dp))
                    SeekSection(state = state, onSeek = viewModel::seekTo)
                    Spacer(Modifier.height(16.dp))
                    TransportRow(state = state, viewModel = viewModel)
                    Spacer(Modifier.height(24.dp))
                    VolumeSlider(
                        level = state.volumeLevel,
                        onLevelChange = viewModel::setVolumeLevel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumArt(albumId: Long?, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp,
        modifier = modifier.clip(MaterialTheme.shapes.large),
    ) {
        AsyncImage(
            model = if (albumId != null) "content://media/external/audio/albumart/$albumId" else null,
            contentDescription = "Album art",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun TrackInfoRow(state: NowPlayingUiState, onFavoriteClick: () -> Unit) {
    val track = state.currentTrack
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = track?.title ?: "No track selected",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                text = track?.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(),
            )
            if (track != null) {
                Text(
                    text = AudioFormatUtil.formatBadge(
                        track.mimeType, track.sampleRate, track.bitDepth, track.bitrate,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = onFavoriteClick) {
            Icon(
                imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SeekSection(state: NowPlayingUiState, onSeek: (Long) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val serverProgress = if (state.durationMs > 0) state.progressMs.toFloat() / state.durationMs else 0f
    val displayProgress = if (isDragging) dragProgress else serverProgress

    Slider(
        value = displayProgress,
        onValueChange = { v ->
            isDragging = true
            dragProgress = v
        },
        onValueChangeFinished = {
            onSeek((dragProgress * state.durationMs).toLong())
            isDragging = false
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val displayMs = if (isDragging) (dragProgress * state.durationMs).toLong() else state.progressMs
        Text(TimeUtil.formatDuration(displayMs), style = MaterialTheme.typography.bodySmall)
        Text(TimeUtil.formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TransportRow(state: NowPlayingUiState, viewModel: NowPlayingViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { viewModel.toggleShuffle() }) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.isShuffled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { viewModel.previous() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(
            onClick = { viewModel.togglePlayPause() },
            modifier = Modifier.size(64.dp),
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = { viewModel.next() }, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = { viewModel.cycleRepeat() }) {
            Icon(
                imageVector = when (state.repeatMode) {
                    1 -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                },
                contentDescription = "Repeat",
                tint = if (state.repeatMode > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
