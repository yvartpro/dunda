package com.yvartpro.dunda.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.yvartpro.dunda.logic.MusicViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MusicProgressBar(viewModel: MusicViewModel) {
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()

    if (duration > 0) {
        val coroutineScope = rememberCoroutineScope()
        val progressFraction = (progress / duration).coerceIn(0f, 1f)

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset: Offset ->
                            val tappedFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            val newPosition = (duration * tappedFraction).toInt()
                            viewModel.seekTo(newPosition)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newPosition = (duration * dragFraction).roundToInt()
                            coroutineScope.launch {
                                viewModel.seekTo(newPosition)
                            }
                        }
                    }
            ) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(progress.toInt()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                Text(formatTime(duration.toInt()), style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
  val h = seconds / 3600
  val min = (seconds % 3600) / 60
  val sec = seconds % 60
  return if (h > 0)
    "%d:%02d:%02d".format(h,min, sec)
  else
    "%d:%02d".format(min, sec)
}