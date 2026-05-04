package com.musicplayer.ui.screens.home

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.musicplayer.ui.components.AlbumCard
import com.musicplayer.ui.components.TrackListItem
import com.musicplayer.ui.navigation.Screen
import androidx.compose.foundation.lazy.itemsIndexed

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    )

    val audioGranted = permissionsState.permissions
        .first { it.permission == Manifest.permission.READ_MEDIA_AUDIO }
        .status.isGranted

    var showRationale by remember { mutableStateOf(false) }

    // On first composition, request permissions if not yet granted.
    LaunchedEffect(Unit) {
        if (!audioGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // Once audio permission is granted, trigger first-launch scan.
    LaunchedEffect(audioGranted) {
        if (audioGranted) {
            viewModel.triggerScanIfFirstLaunch()
        }
    }

    // Show rationale dialog if the system wants us to explain why we need the permission.
    LaunchedEffect(permissionsState.shouldShowRationale) {
        showRationale = permissionsState.shouldShowRationale
    }

    if (showRationale) {
        PermissionRationaleDialog(
            onConfirm = {
                showRationale = false
                permissionsState.launchMultiplePermissionRequest()
            },
            onDismiss = { showRationale = false },
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("UMAK") },
                actions = {
                    if (state.isScanRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.rescan() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Rescan library")
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        // Scan progress bar below the top bar.
        if (state.isScanRunning) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = padding.calculateTopPadding()),
            ) {
                if (state.scanTotal > 0) {
                    LinearProgressIndicator(
                        progress = { state.scanScanned.toFloat() / state.scanTotal },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Scanning ${state.scanScanned} / ${state.scanTotal} files…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Text(
                    text = "Recently Added",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (state.recentlyAdded.isEmpty() && !state.isScanRunning) {
                item {
                    Text(
                        text = "No albums found. Tap the scan button to import your music library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            } else {
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                        items(state.recentlyAdded) { album ->
                            AlbumCard(
                                album = album,
                                onClick = { navController.navigate(Screen.AlbumDetail.createRoute(album.id)) },
                            )
                        }
                    }
                }
            }

            if (state.recentlyPlayed.isNotEmpty()) {
                item {
                    Text(
                        text = "Recently Played",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                itemsIndexed(state.recentlyPlayed) { index, track ->
                    TrackListItem(
                        track = track,
                        onClick = {
                            viewModel.playTrack(state.recentlyPlayed, index)
                            navController.navigate(Screen.NowPlaying.route)
                        },
                        onFavoriteToggle = { viewModel.toggleFavorite(track.id, track.isFavorite) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions needed") },
        text = {
            Text(
                "Storage access is required to scan your music library. " +
                "Notification permission allows showing the playback notification."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Grant") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}
