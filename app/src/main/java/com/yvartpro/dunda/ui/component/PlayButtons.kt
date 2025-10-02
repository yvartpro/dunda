package com.yvartpro.dunda.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.MusicViewModel


@Composable
fun PlayButtons(viewModel: MusicViewModel) {
  val isPlaying by viewModel.isPlaying.collectAsState()
  val isLooping by viewModel.isLooping.collectAsState()
  val isShuffling by viewModel.isShuffling.collectAsState()
  val context = LocalContext.current

  MusicProgressBar(viewModel) //progression bar
  Row(
    horizontalArrangement = Arrangement.SpaceEvenly,
    modifier = Modifier
      .fillMaxWidth()
      .height(96.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = {
      if (isLooping && !isShuffling) {
        Toast.makeText(context, "Repeat one is active", Toast.LENGTH_SHORT).show()
      } else {
        viewModel.toggleShuffle()
      }
    }) {
      Icon(
        painter = painterResource(R.drawable.shuffle),
        tint = if (isShuffling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        contentDescription = "Shuffle",
        modifier = Modifier.size(24.dp)
      )
    }
    Row {
      IconButton(onClick = { viewModel.playPrev() }) {
        Icon(
          painter = painterResource(R.drawable.previous),
          tint = MaterialTheme.colorScheme.secondary,
          contentDescription = "Previous",
          modifier = Modifier.size(24.dp)
        )
      }
      IconButton(onClick = { viewModel.togglePlayPause() }) {
        Icon(
          painter = if (isPlaying) painterResource(R.drawable.play_pause) else painterResource(R.drawable.play),
          tint = MaterialTheme.colorScheme.secondary,
          contentDescription = "Play/Pause",
          modifier = Modifier.size(24.dp)
        )
      }
      IconButton(onClick = { viewModel.playNext() }) {
        Icon(
          painter = painterResource(R.drawable.next),
          tint = MaterialTheme.colorScheme.secondary,
          contentDescription = "Next",
          modifier = Modifier.size(24.dp)
        )
      }
    }
    IconButton(onClick = { viewModel.toggleLoop() }) {
      Icon(
        painter = if (isLooping) painterResource(R.drawable.repeat_one) else painterResource(R.drawable.repeat),
        tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        contentDescription = "Loop",
        modifier = Modifier.size(24.dp)
      )
    }
  }
}