package com.musicplayer

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.musicplayer.data.local.db.dao.TrackDao
import com.musicplayer.service.PlayerController
import com.musicplayer.ui.navigation.AppNavigation
import com.musicplayer.ui.theme.AppTheme
import com.musicplayer.ui.theme.MusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var trackDao: TrackDao
    @Inject lateinit var playerController: PlayerController
    @Inject lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val count = trackDao.getAllTracks().first().size
            Log.d("MusicPlayerDB", "Track count in DB: $count")
        }

        setContent {
            val themeString by dataStore.data
                .map { prefs -> prefs[stringPreferencesKey("theme")] ?: "AmoledBlack" }
                .collectAsStateWithLifecycle(initialValue = "AmoledBlack")

            val appTheme = when (themeString) {
                "Light" -> AppTheme.Light
                "Dark" -> AppTheme.Dark
                else -> AppTheme.AmoledBlack
            }

            MusicPlayerTheme(appTheme = appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        playerController = playerController,
                    )
                }
            }
        }
    }
}
