package com.arkus.shoppyjuan.presentation.components

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arkus.shoppyjuan.data.barcode.BarcodeScanResult
import com.arkus.shoppyjuan.data.barcode.BarcodeScannerManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerScreen(
    scannerManager: BarcodeScannerManager,
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var scanResult by remember { mutableStateOf<BarcodeScanResult>(BarcodeScanResult.Scanning) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            scannerManager.startScanning(lifecycleOwner, previewView).collect { result ->
                scanResult = result
                if (result is BarcodeScanResult.Success) {
                    onBarcodeScanned(result.barcode)
                    onClose()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Camera preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay with scanning frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            // Scanning frame
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .background(Color.Transparent)
            ) {
                // Corner indicators
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                )
            }

            // Instructions
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Coloca el código de barras dentro del marco",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                if (scanResult is BarcodeScanResult.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (scanResult as BarcodeScanResult.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            // Permission request
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Se necesita permiso de cámara",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Conceder permiso")
                }
            }
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.White
            )
        }
    }
}
