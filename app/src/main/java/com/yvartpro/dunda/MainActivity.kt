package com.yvartpro.dunda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yvartpro.dunda.ui.theme.DundaTheme
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.yvartpro.dunda.logic.MusicViewModel
import com.yvartpro.dunda.nav.AppNav
import com.yvartpro.dunda.ui.screen.PermissionScreen

class MainActivity : ComponentActivity() {

    private lateinit var musicViewModel: MusicViewModel
    private var hasPermissions by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val essentialGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                results[Manifest.permission.READ_MEDIA_AUDIO] == true
            } else {
                results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }

            if (essentialGranted) {
                hasPermissions = true
                musicViewModel.loadTracksForce()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        musicViewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        
        enableEdgeToEdge()
        hasPermissions = checkPermissionsSilent()

        musicViewModel.handleIntent(intent)

        setContent {
            DundaTheme {
                if (hasPermissions) {
                    AppNav(musicViewModel)
                } else {
                    PermissionScreen(onRequestPermission = {
                        requestPermissions()
                    })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        musicViewModel.handleIntent(intent)
    }

    private fun checkPermissionsSilent(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
}
