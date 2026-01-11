package com.arkus.shoppyjuan.data.barcode

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

sealed class BarcodeScanResult {
    object Scanning : BarcodeScanResult()
    data class Success(val barcode: String, val format: String) : BarcodeScanResult()
    data class Error(val message: String) : BarcodeScanResult()
}

@Singleton
class BarcodeScannerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val barcodeScanner = BarcodeScanning.getClient()

    fun startScanning(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ): Flow<BarcodeScanResult> = callbackFlow {
        trySend(BarcodeScanResult.Scanning)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image analyzer
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxy(imageProxy) { result ->
                                trySend(result)
                                if (result is BarcodeScanResult.Success) {
                                    close()
                                }
                            }
                        }
                    }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

            } catch (e: Exception) {
                trySend(BarcodeScanResult.Error("Error iniciando cámara: ${e.message}"))
                close()
            }
        }, ContextCompat.getMainExecutor(context))

        awaitClose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(
        imageProxy: ImageProxy,
        onResult: (BarcodeScanResult) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (rawValue != null) {
                            val format = getBarcodeFormat(barcode.format)
                            onResult(BarcodeScanResult.Success(rawValue, format))
                            return@addOnSuccessListener
                        }
                    }
                }
                .addOnFailureListener { e ->
                    onResult(BarcodeScanResult.Error("Error escaneando: ${e.message}"))
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun getBarcodeFormat(format: Int): String {
        return when (format) {
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            else -> "UNKNOWN"
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}
