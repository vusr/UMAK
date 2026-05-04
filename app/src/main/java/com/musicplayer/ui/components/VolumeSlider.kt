package com.musicplayer.ui.components

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.musicplayer.ui.theme.VolumeTrackActive
import com.musicplayer.ui.theme.VolumeTrackInactive
import com.musicplayer.util.VolumeUtil

/**
 * A custom 50-step volume slider with logarithmic perceptual scaling.
 * Displays the current level as a dB value.
 * Fires a haptic tick on every level change while dragging.
 */
@Composable
fun VolumeSlider(
    level: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vibrator = LocalContext.current
        .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    var lastLevel by remember { mutableIntStateOf(level) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = VolumeUtil.levelToDbString(level),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Slider(
            value = level.toFloat(),
            onValueChange = { newValue ->
                val newLevel = newValue.toInt()
                if (newLevel != lastLevel) {
                    vibrator.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    )
                    lastLevel = newLevel
                }
                onLevelChange(newLevel)
            },
            valueRange = VolumeUtil.MIN_LEVEL.toFloat()..VolumeUtil.MAX_LEVEL.toFloat(),
            steps = VolumeUtil.MAX_LEVEL - VolumeUtil.MIN_LEVEL - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = VolumeTrackActive,
                inactiveTrackColor = VolumeTrackInactive,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Min", style = MaterialTheme.typography.labelSmall)
            Text("Vol ${level}/50", style = MaterialTheme.typography.labelSmall)
            Text("Max", style = MaterialTheme.typography.labelSmall)
        }
    }
}
