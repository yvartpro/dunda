package com.yvartpro.dunda.ui.component

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yvartpro.dunda.R
import com.yvartpro.dunda.logic.MusicViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SheetValue { Dismissed, Collapsed, Expanded }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggableUI(
    viewModel: MusicViewModel,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    // collapsed: show ~65% of screen
    val collapsedHeightPx = screenHeightPx * 0.65f
    // expanded: show ~35% (not full screen)
    val expandedHeightPx = screenHeightPx * 0.35f

    val currTrack by viewModel.currentTrack.collectAsState()
    val shownTrack by viewModel.showTrack.collectAsState()
    val scope = rememberCoroutineScope()

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = SheetValue.Collapsed,
            anchors = DraggableAnchors {
                SheetValue.Dismissed at screenHeightPx
                SheetValue.Collapsed at collapsedHeightPx
                SheetValue.Expanded at expandedHeightPx
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { 1000f },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay()
        )
    }

    // Watch for dismiss
    LaunchedEffect(draggableState.currentValue) {
        if (draggableState.currentValue == SheetValue.Dismissed) {
            onDismiss()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // make the dark background clickable → dismiss
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                onClick = {
                    scope.launch { draggableState.animateTo(SheetValue.Dismissed) }
                }
            )
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(screenHeightDp)
                .offset { IntOffset(0, draggableState.offset.roundToInt()) }
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .anchoredDraggable(state = draggableState, orientation = Orientation.Vertical)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        navController.navigate("player")
                        viewModel.toggleShowSheet()
                    }
            ) {
                // Grab handle
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                val target =
                                    if (draggableState.currentValue == SheetValue.Collapsed)
                                        SheetValue.Expanded else SheetValue.Collapsed
                                draggableState.animateTo(target)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .background(Color.Gray, RoundedCornerShape(50))
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.song_details),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    TextButton(onClick = { shownTrack?.let { viewModel.playTrack(it) } }) {
                        Text(
                            text = stringResource(R.string.play),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = shownTrack?.title ?: "Not a song",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(32.dp))

                    PlayButtons(viewModel)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .padding(bottom = 16.dp, start = 12.dp)
                    ) {
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
    }
}