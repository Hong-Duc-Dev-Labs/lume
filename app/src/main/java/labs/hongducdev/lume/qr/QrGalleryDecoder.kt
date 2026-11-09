package labs.hongducdev.lume.qr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object QrGalleryDecoder {
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

    suspend fun decodeFromUri(
        context: Context,
        uri: Uri,
    ): QrResult? =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                scanner
                    .process(image)
                    .addOnSuccessListener { barcodes ->
                        val first = barcodes.firstOrNull()
                        if (first != null && !first.rawValue.isNullOrBlank()) {
                            continuation.resume(QrParser.fromBarcode(first))
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }
}
