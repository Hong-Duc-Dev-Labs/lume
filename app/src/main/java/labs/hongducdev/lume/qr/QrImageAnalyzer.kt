package labs.hongducdev.lume.qr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

class QrImageAnalyzer(
    private val onQrDetected: (QrResult) -> Unit,
) : ImageAnalysis.Analyzer {
    private val isScanning = AtomicBoolean(true)

    private val scanner =
        BarcodeScanning.getClient(
            BarcodeScannerOptions
                .Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC,
                    Barcode.FORMAT_DATA_MATRIX,
                ).build(),
        )

    fun pauseScanning() {
        isScanning.set(false)
    }

    fun resumeScanning() {
        isScanning.set(true)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        scanner
            .process(image)
            .addOnSuccessListener { barcodes ->
                if (!isScanning.get()) return@addOnSuccessListener
                val firstBarcode = barcodes.firstOrNull()
                if (firstBarcode != null && !firstBarcode.rawValue.isNullOrBlank()) {
                    isScanning.set(false)
                    val result = QrParser.fromBarcode(firstBarcode)
                    onQrDetected(result)
                }
            }.addOnFailureListener {
                // Ignore transient frame errors
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }
}
