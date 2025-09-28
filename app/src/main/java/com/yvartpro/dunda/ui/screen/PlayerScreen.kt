package com.yvartpro.dunda.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yvartpro.dunda.logic.MusicViewModel
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.Folder
import com.yvartpro.dunda.ui.component.DraggableSheet
import com.yvartpro.dunda.ui.component.FolderSheet
import com.yvartpro.dunda.ui.component.PlayButtons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
  viewModel: MusicViewModel,
  navController: NavController,
  onBack: () -> Unit
) {
  val currentTrack by viewModel.currentTrack.collectAsState()
  val showFolderSheet by viewModel.showFolderSheet.collectAsState()
  val scope = rememberCoroutineScope()

  LaunchedEffect(Unit) {
    scope.launch {
      viewModel.loadFolders()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      Surface(
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.primary),
      ){
        TopAppBar(
          title = {
            Text(
              text = "Dunda - Player",
              color = MaterialTheme.colorScheme.tertiary,
            )
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.tertiary
              )
            }
          },
          actions = {
            IconButton(onClick = {
              viewModel.toggleShowFolderSheet()
              viewModel.loadFolders()
            }) {
              Icon(
                painter = painterResource(R.drawable.folder),
                contentDescription = "Folder",
                tint = MaterialTheme.colorScheme.tertiary
              )
            }
          }
        )
      }
    },
    bottomBar = {
      Surface(
        color = Color.Transparent,
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .background(Color.Transparent)
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
          textAlign = TextAlign.Start
        )
      }
      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}
