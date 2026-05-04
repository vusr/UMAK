package com.musicplayer.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.musicplayer.domain.model.Playlist
import com.musicplayer.ui.components.AlbumCard
import com.musicplayer.ui.components.EmptyState
import com.musicplayer.ui.components.TrackListItem
import com.musicplayer.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = LibraryTab.entries

    // Dialog / sheet state
    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }

    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }

    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    // Track id for "add to playlist" sheet; null when sheet is hidden
    var addToPlaylistTrackId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(title = { Text("Library") })
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search tracks, albums, artists…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
                if (state.searchQuery.isBlank()) {
                    ScrollableTabRow(selectedTabIndex = tabs.indexOf(state.selectedTab)) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = state.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(tab.name) },
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (state.selectedTab == LibraryTab.Playlists && state.searchQuery.isBlank()) {
                FloatingActionButton(onClick = {
                    createName = ""
                    showCreateDialog = true
                }) {
                    Icon(Icons.Default.Add, "Create playlist")
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.searchQuery.isNotBlank()) {
                SearchResultsView(
                    results = state.searchResults,
                    onTrackClick = { track ->
                        val trackList = state.searchResults?.tracks ?: emptyList()
                        val idx = trackList.indexOf(track)
                        viewModel.playTrack(trackList, maxOf(idx, 0))
                        navController.navigate(Screen.NowPlaying.route)
                    },
                    onAlbumClick = { album ->
                        navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                    },
                    onArtistClick = { artist ->
                        navController.navigate(Screen.ArtistDetail.createRoute(artist.id))
                    },
                    onAddToPlaylist = { trackId -> addToPlaylistTrackId = trackId },
                )
            } else {
                when (state.selectedTab) {
                    LibraryTab.Tracks -> if (state.tracks.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.MusicNote,
                            message = "No tracks found — tap the scan button on Home to import your music library.",
                        )
                    } else {
                        LazyColumn {
                            itemsIndexed(state.tracks) { index, track ->
                                TrackListItem(
                                    track = track,
                                    onClick = {
                                        viewModel.playTrack(state.tracks, index)
                                        navController.navigate(Screen.NowPlaying.route)
                                    },
                                    onFavoriteToggle = {
                                        viewModel.toggleFavorite(track.id, track.isFavorite)
                                    },
                                    onAddToPlaylist = { addToPlaylistTrackId = track.id },
                                )
                            }
                        }
                    }

                    LibraryTab.Albums -> if (state.albums.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Album,
                            message = "No albums found — tap the scan button on Home to import your music library.",
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(160.dp),
                            contentPadding = PaddingValues(8.dp),
                        ) {
                            items(state.albums) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = {
                                        navController.navigate(Screen.AlbumDetail.createRoute(album.id))
                                    },
                                )
                            }
                        }
                    }

                    LibraryTab.Artists -> if (state.artists.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.Person,
                            message = "No artists found — tap the scan button on Home to import your music library.",
                        )
                    } else {
                        LazyColumn {
                            items(state.artists) { artist ->
                                ListItem(
                                    headlineContent = { Text(artist.name) },
                                    supportingContent = {
                                        Text("${artist.albumCount} albums · ${artist.trackCount} tracks")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                        .clickable {
                                            navController.navigate(
                                                Screen.ArtistDetail.createRoute(artist.id)
                                            )
                                        },
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    LibraryTab.Playlists -> if (state.playlists.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.LibraryMusic,
                            message = "No playlists yet — tap + to create your first playlist.",
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 88.dp),
                        ) {
                            items(state.playlists, key = { it.id }) { playlist ->
                                PlaylistRow(
                                    playlist = playlist,
                                    onClick = {
                                        navController.navigate(Screen.Playlist.createRoute(playlist.id))
                                    },
                                    onRename = {
                                        renameTarget = playlist
                                        renameText = playlist.name
                                    },
                                    onDelete = { deleteTarget = playlist },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Create Playlist Dialog ----
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = createName,
                    onValueChange = { createName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = createName.isNotBlank(),
                    onClick = {
                        viewModel.createPlaylist(createName.trim())
                        showCreateDialog = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            },
        )
    }

    // ---- Rename Playlist Dialog ----
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        viewModel.renamePlaylist(target.id, renameText.trim())
                        renameTarget = null
                    },
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    // ---- Delete Playlist Confirmation Dialog ----
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Playlist") },
            text = { Text("Delete \"${target.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(target.id)
                        deleteTarget = null
                    },
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    // ---- Add to Playlist Bottom Sheet ----
    addToPlaylistTrackId?.let { trackId ->
        ModalBottomSheet(onDismissRequest = { addToPlaylistTrackId = null }) {
            Text(
                "Add to playlist",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (state.playlists.isEmpty()) {
                Text(
                    "No playlists yet. Create one first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    items(state.playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            supportingContent = {
                                val count = parsePlaylistTrackCount(playlist.trackIds)
                                Text("$count tracks")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.addTrackToPlaylist(playlist.id, trackId)
                                    addToPlaylistTrackId = null
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
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(playlist.name) },
        supportingContent = { Text(playlist.description.ifBlank { "${parsePlaylistTrackCount(playlist.trackIds)} tracks" }) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Playlist options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                        onClick = { onRename(); showMenu = false },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { onDelete(); showMenu = false },
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SearchResultsView(
    results: SearchResults?,
    onTrackClick: (com.musicplayer.domain.model.Track) -> Unit,
    onAlbumClick: (com.musicplayer.domain.model.Album) -> Unit,
    onArtistClick: (com.musicplayer.domain.model.Artist) -> Unit,
    onAddToPlaylist: (Long) -> Unit,
) {
    if (results == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (results.tracks.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (results.tracks.isNotEmpty()) {
            item {
                SearchSectionHeader("Tracks (${results.tracks.size})")
            }
            itemsIndexed(results.tracks) { _, track ->
                TrackListItem(
                    track = track,
                    onClick = { onTrackClick(track) },
                    onAddToPlaylist = { onAddToPlaylist(track.id) },
                )
            }
        }

        if (results.albums.isNotEmpty()) {
            item {
                SearchSectionHeader("Albums (${results.albums.size})")
            }
            items(results.albums) { album ->
                ListItem(
                    headlineContent = { Text(album.name) },
                    supportingContent = { Text(album.artist) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlbumClick(album) },
                )
                HorizontalDivider()
            }
        }

        if (results.artists.isNotEmpty()) {
            item {
                SearchSectionHeader("Artists (${results.artists.size})")
            }
            items(results.artists) { artist ->
                ListItem(
                    headlineContent = { Text(artist.name) },
                    supportingContent = { Text("${artist.trackCount} tracks") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

private fun parsePlaylistTrackCount(trackIds: String): Int {
    if (trackIds.isBlank() || trackIds == "[]") return 0
    return try {
        org.json.JSONArray(trackIds).length()
    } catch (e: Exception) {
        trackIds.removePrefix("[").removeSuffix("]").split(",").count { it.isNotBlank() }
    }
}
