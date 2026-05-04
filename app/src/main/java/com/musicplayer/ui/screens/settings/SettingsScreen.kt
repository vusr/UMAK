package com.musicplayer.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.musicplayer.ui.components.VolumeSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sampleRateOptions = listOf(0 to "Auto", 44100 to "44.1 kHz", 48000 to "48 kHz",
        96000 to "96 kHz", 192000 to "192 kHz", 384000 to "384 kHz")

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

            // === AUDIO ===
            item { SectionHeader("Audio") }

            item {
                SettingRow(
                    title = "Gapless Playback",
                    subtitle = "Seamless transitions between tracks",
                ) {
                    Switch(checked = state.gaplessPlayback, onCheckedChange = viewModel::setGapless)
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Output Sample Rate", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Force a specific rate (use Auto unless you have a specific reason)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        sampleRateOptions.forEachIndexed { index, (hz, label) ->
                            SegmentedButton(
                                selected = state.forceSampleRateHz == hz,
                                onClick = { viewModel.setForceSampleRate(hz) },
                                shape = SegmentedButtonDefaults.itemShape(index, sampleRateOptions.size),
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Crossfade", style = MaterialTheme.typography.bodyLarge)
                    Slider(
                        value = state.crossfadeDuration.toFloat(),
                        onValueChange = { viewModel.setCrossfade(it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        if (state.crossfadeDuration == 0) "Disabled" else "${state.crossfadeDuration}s",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // === VOLUME ===
            item { SectionHeader("Volume") }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Default Volume on Startup", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    VolumeSlider(
                        level = state.defaultVolumeLevel,
                        onLevelChange = viewModel::setDefaultVolume,
                    )
                }
            }

            // === APPEARANCE ===
            item { SectionHeader("Appearance") }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow {
                        listOf("AmoledBlack", "Dark", "Light").forEachIndexed { i, t ->
                            SegmentedButton(
                                selected = state.theme == t,
                                onClick = { viewModel.setTheme(t) },
                                shape = SegmentedButtonDefaults.itemShape(i, 3),
                                label = { Text(t) },
                            )
                        }
                    }
                }
            }

            // === FILE EXPLORER ===
            item { SectionHeader("File Explorer") }

            item {
                SettingRow(
                    title = "Show Hidden Files",
                    subtitle = "Display files and folders starting with '.'",
                ) {
                    Switch(checked = state.showHiddenFiles, onCheckedChange = viewModel::setShowHiddenFiles)
                }
            }

            // === ABOUT ===
            item { SectionHeader("About") }

            item { InfoRow("App Version", viewModel.appVersion) }
            item { InfoRow("Native Sample Rate", "${viewModel.nativeSampleRate} Hz") }
            item { InfoRow("Native Buffer Size", "${viewModel.nativeBufferSize} frames") }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = trailing,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}
