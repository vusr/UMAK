package com.musicplayer.ui.screens.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val timerOptions = listOf(15, 30, 45, 60, 90)

/**
 * Bottom sheet that lets the user set a sleep timer.
 *
 * @param sleepTimerEndMs epoch millis when timer fires; 0 = no timer; -1 = end-of-track mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepTimerEndMs: Long,
    onSelectMinutes: (Int) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    var remainingLabel by remember { mutableStateOf("") }

    LaunchedEffect(sleepTimerEndMs) {
        if (sleepTimerEndMs > 0) {
            while (true) {
                val remaining = sleepTimerEndMs - System.currentTimeMillis()
                if (remaining <= 0) {
                    remainingLabel = ""
                    break
                }
                val mins = (remaining / 60_000).toInt()
                val secs = ((remaining % 60_000) / 1000).toInt()
                remainingLabel = "%d:%02d remaining".format(mins, secs)
                delay(1_000)
            }
        } else {
            remainingLabel = ""
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Sleep Timer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (sleepTimerEndMs != 0L) {
                    TextButton(onClick = { onCancel(); onDismiss() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel timer", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
            }

            if (sleepTimerEndMs == -1L) {
                Text(
                    "Timer: end of current track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            } else if (remainingLabel.isNotEmpty()) {
                Text(
                    remainingLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            timerOptions.forEach { minutes ->
                ListItem(
                    headlineContent = { Text("$minutes minutes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMinutes(minutes); onDismiss() },
                )
                HorizontalDivider()
            }

            ListItem(
                headlineContent = { Text("End of current track") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectEndOfTrack(); onDismiss() },
            )
        }
    }
}
