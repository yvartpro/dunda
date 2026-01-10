package com.yvartpro.dunda.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yvartpro.dunda.logic.MusicViewModel
import com.yvartpro.dunda.R
import com.yvartpro.dunda.ui.component.DraggableSheet
import com.yvartpro.dunda.ui.component.FolderSheet
import com.yvartpro.dunda.ui.component.PlayButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
  viewModel: MusicViewModel,
  navController: NavController,
) {
  val currentTrack by viewModel.currentTrack.collectAsState()
  val showFolderSheet by viewModel.showFolderSheet.collectAsState()


  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      Surface(
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.background),
      ){
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.app_title),
              color = MaterialTheme.colorScheme.onBackground,
            )
          },
          actions = {
            IconButton(onClick = {
              navController.navigate("equalizer")
            }) {
              Icon(
                imageVector = Icons.Filled.Tune,
                contentDescription = "Equalizer",
                tint = MaterialTheme.colorScheme.onBackground
              )
            }
            IconButton(onClick = {
              viewModel.toggleShowFolderSheet()
              viewModel.loadFolders()
            }) {
              Icon(
                painter = painterResource(R.drawable.folder),
                contentDescription = "Folder",
                tint = MaterialTheme.colorScheme.onBackground
              )
            }
          }
        )
      }
    },
    bottomBar = {
      Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.background)
          .padding(top = 16.dp)
          .navigationBarsPadding()
      ) {
        PlayButtons(viewModel)
      }
    }
  ) { padding ->
    if (showFolderSheet) {
      DraggableSheet(
        title = "Select folder",
        onDismiss = { viewModel.toggleShowFolderSheet() }
      ) {
        FolderSheet(
          viewModel = viewModel,
          navController = navController,
        )
      }
    }
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(MaterialTheme.colorScheme.background)
        .padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        color = Color.Transparent,
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(0.2.dp, Color.LightGray, RoundedCornerShape(16.dp))
          .background(Color.Transparent)
          .padding(16.dp),
      ) {
        Image(
          painter = painterResource(R.drawable.bg),
          contentDescription = "Background img"
        )
      }
      Spacer(Modifier.height(12.dp))
      currentTrack?.title?.let {
        Text(
          text = stringResource(R.string.playing,it),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Start,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}
