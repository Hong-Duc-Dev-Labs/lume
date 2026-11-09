package labs.hongducdev.lume.car

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SignType {
    SPEED_LIMIT,
    SPEED_CAMERA,
    RESIDENTIAL_AREA,
    END_RESIDENTIAL
}

data class RoadSign(
    val type: SignType,
    val title: String,
    val speedLimit: Int? = null,
    val distanceMeters: Int = 0
)

data class CarSpeedAlertState(
    val speedLimit: Int = 60,
    val isOverSpeed: Boolean = false,
    val approachingSign: RoadSign? = RoadSign(
        type = SignType.SPEED_CAMERA,
        title = "Camera phạt nguội",
        speedLimit = 60,
        distanceMeters = 350
    ),
    val isVoiceAlertEnabled: Boolean = true
)

class CarSpeedAlertManager(private val context: Context) {
    private val TAG = "CarSpeedAlertManager"

    private val _alertState = MutableStateFlow(CarSpeedAlertState())
    val alertState: StateFlow<CarSpeedAlertState> = _alertState.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var lastOverspeedVoiceTime: Long = 0
    private var lastSignVoiceTime: Long = 0

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val viLocale = Locale.forLanguageTag("vi-VN")
                    val result = tts?.setLanguage(viLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(TAG, "Vietnamese TTS not supported, trying default locale")
                        tts?.language = Locale.getDefault()
                    }
                    tts?.setPitch(1.0f)
                    tts?.setSpeechRate(1.05f)
                    isTtsReady = true
                    Log.d(TAG, "TTS initialized successfully")
                } else {
                    Log.e(TAG, "TTS init failed with status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing TTS", e)
        }
    }

    fun speak(text: String, flush: Boolean = false) {
        if (!_alertState.value.isVoiceAlertEnabled || !isTtsReady || tts == null) return
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, null, "car_alert_${System.currentTimeMillis()}")
    }

    fun onSpeedChanged(currentSpeedKmh: Int) {
        val limit = _alertState.value.speedLimit
        val isOver = currentSpeedKmh > limit

        if (isOver != _alertState.value.isOverSpeed) {
            _alertState.value = _alertState.value.copy(isOverSpeed = isOver)
        }

        // Phát âm thanh cảnh báo khi chạy quá tốc độ (giãn cách tối thiểu 10 giây/lần)
        if (isOver && currentSpeedKmh > 0) {
            val now = System.currentTimeMillis()
            if (now - lastOverspeedVoiceTime > 10_000L) {
                lastOverspeedVoiceTime = now
                speak("Cảnh báo, bạn đang chạy quá tốc độ $limit kilomet trên giờ!", flush = true)
            }
        }
    }

    fun cycleSpeedLimit() {
        val limits = listOf(50, 60, 80, 100, 120)
        val currentIndex = limits.indexOf(_alertState.value.speedLimit)
        val nextIndex = if (currentIndex in 0 until limits.size - 1) currentIndex + 1 else 0
        val newLimit = limits[nextIndex]
        setSpeedLimit(newLimit)
    }

    fun setSpeedLimit(limit: Int) {
        if (_alertState.value.speedLimit == limit) return
        _alertState.value = _alertState.value.copy(speedLimit = limit)
        speak("Tốc độ giới hạn $limit", flush = false)
    }

    fun cycleApproachingSign() {
        val sampleSigns = listOf(
            RoadSign(SignType.SPEED_CAMERA, "Camera phạt nguội", 60, 350),
            RoadSign(SignType.RESIDENTIAL_AREA, "Khu đông dân cư", 50, 200),
            RoadSign(SignType.END_RESIDENTIAL, "Hết khu đông dân cư", 80, 150),
            null
        )
        val currentSign = _alertState.value.approachingSign
        val currentIndex = sampleSigns.indexOfFirst {
            if (currentSign == null) it == null
            else it?.type == currentSign.type
        }
        val nextIndex = if (currentIndex in 0 until sampleSigns.size - 1) currentIndex + 1 else 0
        val nextSign = sampleSigns[nextIndex]
        setApproachingSign(nextSign)
    }

    fun toggleVoiceAlert() {
        val newState = !_alertState.value.isVoiceAlertEnabled
        _alertState.value = _alertState.value.copy(isVoiceAlertEnabled = newState)
        if (newState) {
            speak("Đã bật âm thanh cảnh báo", flush = true)
        }
    }

    fun setApproachingSign(sign: RoadSign?) {
        _alertState.value = _alertState.value.copy(approachingSign = sign)
        if (sign != null) {
            val now = System.currentTimeMillis()
            if (now - lastSignVoiceTime > 4_000L) {
                lastSignVoiceTime = now
                when (sign.type) {
                    SignType.SPEED_CAMERA -> speak("Chú ý, phía trước 350 mét có camera phạt nguội", flush = false)
                    SignType.RESIDENTIAL_AREA -> speak("Đã vào khu đông dân cư, tốc độ tối đa 50", flush = false)
                    SignType.END_RESIDENTIAL -> speak("Hết khu đông dân cư, tốc độ tối đa 80", flush = false)
                    SignType.SPEED_LIMIT -> sign.speedLimit?.let { speak("Tốc độ tối đa $it", flush = false) }
                }
            }
        }
    }

    fun cleanup() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up TTS", e)
        }
    }
}
