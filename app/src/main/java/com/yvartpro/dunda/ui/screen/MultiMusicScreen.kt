package com.yvartpro.dunda.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.MusicTrack
import com.yvartpro.dunda.logic.MusicViewModel
import com.yvartpro.dunda.ui.component.DraggableSheet
import com.yvartpro.dunda.ui.component.PlayerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
  navController: NavController,
  viewModel: MusicViewModel,
  onTrackSelected: (MusicTrack) -> Unit,
) {
  val tracks = viewModel.filtered.collectAsState()
  val isSearch by viewModel.isSearch.collectAsState()
  var query by rememberSaveable { mutableStateOf<String?>(null) }
  val currentTrack by viewModel.currentTrack.collectAsState()
  val showSheet by viewModel.showSheet.collectAsState()
  val isPlaying by viewModel.isPlaying.collectAsState()
  val exportingTrackId by viewModel.exportingTrackId.collectAsState()
  val exportProgress by viewModel.exportProgress.collectAsState()


  Scaffold(
    topBar = {
      Surface(
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.primary),
      ){
        TopAppBar(
          navigationIcon = {
            if (isSearch) {
              IconButton(onClick = { viewModel.toggleSearch() }) {
                Icon(
                  Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.onBackground
                )
              }
            }
          },
          title = {
            if(isSearch) {
              BasicTextField(
                value = query?: "",
                onValueChange = { query = it; viewModel.filter(it) },
                singleLine = true,
                textStyle = TextStyle(
                  color = MaterialTheme.colorScheme.onBackground,
                  fontSize = 16.sp
                )
              )
            } else {
              Text(text = stringResource(R.string.app_title), color = MaterialTheme.colorScheme.onBackground)
            }
          },
          actions = {
            IconButton(onClick = {
              viewModel.toggleSearch()
              viewModel.filter("")
              query = ""
            }) {
              Icon(
                painter = if (isSearch) painterResource(R.drawable.clear) else painterResource(R.drawable.search),
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = "Search",
                modifier = Modifier.size(32.dp),
              )
            }
          }
        )
      }
    },
    bottomBar = {
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      items(tracks.value) { track ->
        ListItem(
          headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
              )
              if (track.isVideo) {
                Surface(
                  color = MaterialTheme.colorScheme.tertiaryContainer,
                  shape = MaterialTheme.shapes.extraSmall,
                  modifier = Modifier.padding(start = 4.dp)
                ) {
                  Text(
                    text = "VIDEO",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                  )
                }
              }
            }
          },
          trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  if (currentTrack == track) viewModel.togglePlayPause() else viewModel.playTrack(
                    track
                  )
                }
              ) {
                Icon(
                  painter = if (currentTrack == track && isPlaying) painterResource(
                    R.drawable.play_pause
                  ) else painterResource(R.drawable.play),
                  tint = if (currentTrack == track) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                  contentDescription = "play",
                  modifier = Modifier.size(24.dp),
                )
              }
              Spacer(Modifier.width(2.dp))
              IconButton(
                onClick = {
                  viewModel.toggleShownTrack(track)
                  viewModel.toggleShowSheet()
                }
              ) {
                Icon(
                  painter = painterResource(R.drawable.more_vert),
                  tint = if (currentTrack == track) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                  contentDescription = "more",
                  modifier = Modifier.size(24.dp), // Reduced size to match better
                )
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (currentTrack == track) navController.popBackStack() else onTrackSelected(
                track
              )
            },
          colors = ListItemColors(
            headlineColor = MaterialTheme.colorScheme.onBackground,
            containerColor = MaterialTheme.colorScheme.background,
            leadingIconColor = MaterialTheme.colorScheme.primary,
            overlineColor = MaterialTheme.colorScheme.primary,
            supportingTextColor = MaterialTheme.colorScheme.secondary,
            trailingIconColor = Color.LightGray,
            disabledHeadlineColor = Color.Blue,
            disabledLeadingIconColor = Color.Black,
            disabledTrailingIconColor = Color.Green,
          )

        )
        HorizontalDivider(
          modifier = Modifier
        )
      }
    }
    if (showSheet) {
      DraggableSheet(
        title = stringResource(R.string.song_details),
        onDismiss = { viewModel.toggleShowSheet() }
      ) {
        PlayerSheet(viewModel)
      }
    }
  }
}

