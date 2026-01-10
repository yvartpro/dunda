package com.yvartpro.dunda.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.MusicViewModel

@Composable
fun PlayerSheet(
  viewModel: MusicViewModel,
) {
  val currTrack by viewModel.currentTrack.collectAsState()
  val shownTrack by viewModel.showTrack.collectAsState()
  val currentTrack by viewModel.currentTrack.collectAsState()
  val isPlaying by viewModel.isPlaying.collectAsState()

  Column(
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = shownTrack?.title ?: "Not a song",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(0.7f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,

      )

      IconButton(onClick = { shownTrack?.let { viewModel.playTrack(it) } }) {
        Icon(
          painter = if (currentTrack == shownTrack && isPlaying) painterResource(
                  R.drawable.play_pause
                  ) else painterResource(R.drawable.play),
          contentDescription = "Play",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
      }
    }
    Spacer(Modifier.height(32.dp))

    PlayButtons(viewModel)
    Column(
      modifier = Modifier
        .fillMaxWidth(0.6f)
        .padding(bottom = 16.dp, start = 12.dp)
    ) {
      if(currentTrack != shownTrack ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
          val empty = ""
          Text(
            text = stringResource(R.string.playing, empty),
            fontSize = 12.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.primary,
          )
          Spacer(Modifier.width(4.dp))
          Text(
            text = currTrack?.title ?: "",
            fontSize = 12.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}