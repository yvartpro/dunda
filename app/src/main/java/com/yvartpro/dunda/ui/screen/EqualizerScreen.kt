package com.yvartpro.dunda.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
                    .weight(1f)
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Frequency Bands",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        // Frequency label (e.g., 60Hz, 1kHz)
        val freqText = if (band.centerFreq < 1000000) "${band.centerFreq / 1000}Hz" else "${band.centerFreq / 1000000}kHz"
        Text(
            text = freqText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Vertical Slider using a rotated Slider
        Box(
            modifier = Modifier
                .weight(1f)
                .width(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = band.level.toFloat(),
                onValueChange = onValueChange,
                valueRange = range.first.toFloat()..range.second.toFloat(),
                enabled = enabled,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = -90f
                    }
                    .width(280.dp) // This will be the height of the vertical slider
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        // Level label (e.g., +3dB)
        Text(
            text = "${band.level / 100}dB",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray
        )
    }
}
