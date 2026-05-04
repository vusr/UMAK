package com.musicplayer.ui.screens.equalizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    navController: NavController,
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // EQ on/off toggle
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled() },
                    )
                    IconButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Default.Add, "Save preset")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Preset chips
            item {
                Text("Presets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.presets) { preset ->
                        FilterChip(
                            selected = state.selectedPresetId == preset.id,
                            onClick = { viewModel.applyPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = if (!preset.isBuiltIn) {
                                {
                                    IconButton(
                                        onClick = { viewModel.deletePreset(preset.id) },
                                        modifier = Modifier.size(18.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete preset",
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            } else null,
                        )
                    }
                }
            }

            // Per-band sliders
            item {
                Text(
                    "Frequency Bands",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            items(state.bands) { band ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = if (band.frequencyHz >= 1000) "${band.frequencyHz / 1000} kHz"
                            else "${band.frequencyHz} Hz",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "${"%.1f".format(band.gainMillibel / 100.0)} dB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = band.gainMillibel.toFloat(),
                        onValueChange = { viewModel.setBandGain(band.index, it.toInt()) },
                        valueRange = band.minMillibel.toFloat()..band.maxMillibel.toFloat(),
                        enabled = state.isEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // BassBoost
            item {
                Text("Bass Boost", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = state.bassBoostStrength.toFloat(),
                    onValueChange = { viewModel.setBassBoost(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = state.isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Strength: ${state.bassBoostStrength}", style = MaterialTheme.typography.bodySmall)
            }

            // Virtualizer
            item {
                Text("Virtualizer (Headphones)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = state.virtualizerStrength.toFloat(),
                    onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = state.isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Loudness Enhancer
            item {
                Text("Loudness Enhancer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Slider(
                    value = state.loudnessGainMb.toFloat(),
                    onValueChange = { viewModel.setLoudnessGain(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = state.isEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (presetName.isNotBlank()) {
                        viewModel.saveCurrentAsPreset(presetName)
                        showSaveDialog = false
                        presetName = ""
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            },
        )
    }
}
