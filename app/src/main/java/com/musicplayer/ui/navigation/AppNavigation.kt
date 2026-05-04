package com.musicplayer.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.musicplayer.service.PlayerController
import com.musicplayer.ui.components.MiniPlayer
import com.musicplayer.ui.screens.albumdetail.AlbumDetailScreen
import com.musicplayer.ui.screens.artistdetail.ArtistDetailScreen
import com.musicplayer.ui.screens.equalizer.EqualizerScreen
import com.musicplayer.ui.screens.fileexplorer.FileExplorerScreen
import com.musicplayer.ui.screens.home.HomeScreen
import com.musicplayer.ui.screens.library.LibraryScreen
import com.musicplayer.ui.screens.nowplaying.NowPlayingScreen
import com.musicplayer.ui.screens.playlist.PlaylistScreen
import com.musicplayer.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object NowPlaying : Screen("now_playing")
    data object Equalizer : Screen("equalizer")
    data object FileExplorer : Screen("file_explorer/{path}") {
        fun createRoute(path: String) = "file_explorer/$path"
    }
    data object AlbumDetail : Screen("album/{albumId}") {
        fun createRoute(albumId: Long) = "album/$albumId"
    }
    data object ArtistDetail : Screen("artist/{artistId}") {
        fun createRoute(artistId: Long) = "artist/$artistId"
    }
    data object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
    data object Settings : Screen("settings")
}

// Tabs shown in the persistent bottom navigation bar.
private data class TopLevelRoute(
    val screen: Screen,
    val label: String,
    val icon: @Composable () -> Unit,
    // The route to navigate to when the tab is tapped (may differ from screen.route if
    // the screen requires path arguments, e.g. FileExplorer uses createRoute("") for root).
    val navigateRoute: String = screen.route,
)

private val topLevelRoutes = listOf(
    TopLevelRoute(
        screen = Screen.Home,
        label = "Home",
        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
    ),
    TopLevelRoute(
        screen = Screen.Library,
        label = "Library",
        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
    ),
    TopLevelRoute(
        screen = Screen.FileExplorer,
        label = "Files",
        icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Files") },
        navigateRoute = Screen.FileExplorer.createRoute(""),
    ),
    TopLevelRoute(
        screen = Screen.Settings,
        label = "Settings",
        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
    ),
)

// Routes where the bottom nav bar should be hidden (full-screen detail screens).
private val routesWithoutBottomNav = setOf(
    Screen.NowPlaying.route,
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    playerController: PlayerController,
) {
    val playerState by playerController.state.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomNav = currentDestination?.route !in routesWithoutBottomNav

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        bottomBar = {
            if (showBottomNav) {
                Column {
                    // MiniPlayer sits above the nav bar when a track is loaded.
                    val track = playerState.currentTrack
                    if (track != null) {
                        MiniPlayer(
                            track = track,
                            isPlaying = playerState.isPlaying,
                            progress = if (playerState.durationMs > 0)
                                playerState.progressMs.toFloat() / playerState.durationMs else 0f,
                            onPlayPauseClick = { playerController.togglePlayPause() },
                            onSkipNextClick = { playerController.next() },
                            onBarClick = { navController.navigate(Screen.NowPlaying.route) },
                        )
                    }

                    NavigationBar {
                        topLevelRoutes.forEach { item ->
                            val selected = currentDestination
                                ?.hierarchy
                                ?.any { it.route == item.screen.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.navigateRoute) {
                                        // Pop back stack to the start destination to avoid
                                        // building up a large stack of the same screen.
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = item.icon,
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut(tween(200)) + slideOutHorizontally { -it / 4 } },
            popEnterTransition = { fadeIn(tween(300)) + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally { it / 4 } },
        ) {

            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }

            composable(Screen.Library.route) {
                LibraryScreen(navController = navController)
            }

            composable(
                route = Screen.NowPlaying.route,
                enterTransition = { fadeIn(tween(350)) + slideInVertically { it / 2 } },
                exitTransition = { fadeOut(tween(250)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(350)) + slideOutVertically { it / 2 } },
            ) {
                NowPlayingScreen(navController = navController)
            }

            composable(Screen.Equalizer.route) {
                EqualizerScreen(navController = navController)
            }

            composable(
                route = Screen.FileExplorer.route,
                arguments = listOf(navArgument("path") { type = NavType.StringType }),
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                FileExplorerScreen(initialPath = path, navController = navController)
            }

            composable(
                route = Screen.AlbumDetail.route,
                arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
                AlbumDetailScreen(albumId = albumId, navController = navController)
            }

            composable(
                route = Screen.ArtistDetail.route,
                arguments = listOf(navArgument("artistId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val artistId = backStackEntry.arguments?.getLong("artistId") ?: return@composable
                ArtistDetailScreen(artistId = artistId, navController = navController)
            }

            composable(
                route = Screen.Playlist.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                PlaylistScreen(playlistId = playlistId, navController = navController)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(navController = navController)
            }
        }
    }
}
