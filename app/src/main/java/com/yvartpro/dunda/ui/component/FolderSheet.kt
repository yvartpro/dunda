package com.yvartpro.dunda.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.MusicViewModel

@Composable
fun FolderSheet(
  viewModel: MusicViewModel,
  navController: NavController,
) {
  val folders by viewModel.folders.collectAsState()
  Text(
    text = stringResource(R.string.all),
    style = MaterialTheme.typography.bodyLarge,
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        viewModel.loadFolders()
        navController.navigate("list") {
          launchSingleTop = true
          popUpTo("play") { inclusive = false}
        }
        viewModel.toggleShowFolderSheet()
      }
      .padding(vertical = 8.dp, horizontal = 16.dp)
  )
  folders.forEach { folder ->
    Text(
      text = folder.name,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          viewModel.selectFolder(folder)
          navController.navigate("list") {
            launchSingleTop = true
            popUpTo("play") { inclusive = false}
          }
          viewModel.toggleShowFolderSheet()
        }
        .padding(vertical = 8.dp, horizontal = 16.dp)
    )
  }
}