package com.yvartpro.dunda.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yvartpro.dunda.logic.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(viewModel: MusicViewModel, navController: NavController) {
    val isEnabled by viewModel.isEqualizerEnabled.collectAsState()
    val bands by viewModel.equalizerBands.collectAsState()
    val range by viewModel.equalizerFreqRange.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleEqualizer() },
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEnabled) "Active" else "Disabled",
                color = if (isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(550.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bands.forEach { band ->
                    EqualizerBandSlider(
                        band = band,
                        range = range,
                        enabled = isEnabled,
                        onValueChange = { newValue ->
                            viewModel.setBandLevel(band.index, newValue.toInt().toShort())
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Frequency Bands",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun EqualizerBandSlider(
    band: MusicViewModel.EqualizerBand,
    range: Pair<Int, Int>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val progressFraction = (band.level.toFloat() - range.first) / (range.second - range.first)
    val barHeight = 250.dp
    val barWidth = 8.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        val freqText = if (band.centerFreq < 1000000) "${band.centerFreq / 1000}Hz" else "${band.centerFreq / 1000000}kHz"
        Text(
            text = freqText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .height(barHeight)
                .width(44.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val fraction = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        val newValue = range.first + (fraction * (range.second - range.first))
                        onValueChange(newValue)
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        val fraction = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        val newValue = range.first + (fraction * (range.second - range.first))
                        onValueChange(newValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            val progressColor = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray

            Canvas(modifier = Modifier.fillMaxHeight().width(barWidth)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val cornerRadius = CornerRadius(canvasWidth / 2, canvasWidth / 2)
                
                // Draw Track
                drawRoundRect(
                    color = trackColor,
                    size = size,
                    cornerRadius = cornerRadius
                )
                
                // Draw Progress (from bottom)
                val activeHeight = canvasHeight * progressFraction.coerceIn(0f, 1f)
                drawRoundRect(
                    color = progressColor,
                    topLeft = Offset(0f, canvasHeight - activeHeight),
                    size = Size(canvasWidth, activeHeight),
                    cornerRadius = cornerRadius
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${band.level / 100}dB",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}
