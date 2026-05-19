package mk.qrmenu.qrmenumanager.main.qr

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mk.qrmenu.qrmenumanager.R

private const val QR_SIZE = 720

data class QrUiState(
    val isLoading: Boolean = true,
    val content: String = "",
    val bitmap: Bitmap? = null,
    val errorMessage: String? = null,
)

class QrCodeViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(QrUiState())
    val state: StateFlow<QrUiState> = _state.asStateFlow()

    init {
        generate()
    }

    private fun generate() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            _state.value = QrUiState(
                isLoading = false,
                errorMessage = string(R.string.error_not_signed_in),
            )
            return
        }

        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) { encode(uid, QR_SIZE) }
                _state.value = QrUiState(isLoading = false, content = uid, bitmap = bitmap)
            } catch (t: Throwable) {
                _state.value = QrUiState(
                    isLoading = false,
                    content = uid,
                    errorMessage = t.localizedMessage ?: string(R.string.error_qr_generate_failed),
                )
            }
        }
    }

    private fun encode(content: String, size: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )

        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                pixels[rowOffset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }

    private fun string(resId: Int): String = getApplication<Application>().getString(resId)
}
