package tech.clover0916.elevenmic.ui

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import tech.clover0916.elevenmic.ui.viewmodel.HomeViewModel
import tech.clover0916.elevenmic.ui.viewmodel.HomeViewModelFactory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomePage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(context))

    val ipAddress = viewModel.ipAddress.observeAsState()

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            style = MaterialTheme.typography.headlineLarge,
            text = "Share Mic to PC"
        )
        Text(text = "Press the button to share your mic to PC.")

        Spacer(modifier = Modifier.height(16.dp)) // Add spacing

        if (ipAddress.value?.isNotEmpty() == true) {
            Text(text = "Your IP address: ${ipAddress.value}")
        } else {
            Text(text = "No network connection")
        }

        Spacer(modifier = Modifier.height(16.dp)) // Add spacing

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

            // Request permission if not granted
            if (!permissionState.status.isGranted) {
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text(text = "Grant Audio Permission")
                }
            } else {
                AudioControl(viewModel)
            }
        }
    }
}

@Composable
fun AudioControl(viewModel: HomeViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { viewModel.toggleAudioStream() }) {
            val isServerRunning by viewModel.isServerRunning.collectAsState()
            Text(text = if (isServerRunning) "Close Connection" else "Share Mic")
        }
        val connectionStatus by viewModel.connectionStatus.collectAsState()
        Text(text = connectionStatus)
    }
}