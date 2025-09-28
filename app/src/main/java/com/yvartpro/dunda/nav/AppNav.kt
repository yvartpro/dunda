package com.yvartpro.dunda.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.yvartpro.dunda.logic.MusicTrack
import com.yvartpro.dunda.logic.MusicViewModel
import com.yvartpro.dunda.ui.screen.MusicListScreen
import com.yvartpro.dunda.ui.screen.PlayerScreen

@Composable
fun AppNav(viewModel: MusicViewModel) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            MusicListScreen(
                navController = navController,
                viewModel = viewModel
            ) { track: MusicTrack ->
                viewModel.playTrack(track)
                navController.navigate("player")
            }
        }
        composable("player") {
            PlayerScreen(viewModel, navController) {
                navController.popBackStack()
            }
        }
    }
}