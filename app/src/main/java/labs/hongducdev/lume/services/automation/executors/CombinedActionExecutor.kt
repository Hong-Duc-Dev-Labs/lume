package labs.hongducdev.lume.services.automation.executors

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import labs.hongducdev.lume.data.DisplayMode
import labs.hongducdev.lume.data.LunarSettingsRepository
import labs.hongducdev.lume.domain.diy.Action
import labs.hongducdev.lume.utils.ShellUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

object CombinedActionExecutor {
    private const val TAG = "CombinedActionExecutor"
    private const val NOTIFICATION_CHANNEL_ID = "toolkit_diy_notifications"
    private var isFlashlightOn = false

    suspend fun executeAll(
        context: Context,
        actions: List<Action>,
    ) {
        for (action in actions) {
            try {
                execute(context, action)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to execute action: ${action.title}", e)
            }
        }
    }

    suspend fun execute(
        context: Context,
        action: Action,
    ) = withContext(Dispatchers.IO) {
        when (action) {
            is Action.TurnOnFlashlight -> {
                setFlashlight(context, true)
            }

            is Action.TurnOffFlashlight -> {
                setFlashlight(context, false)
            }

            is Action.ToggleFlashlight -> {
                setFlashlight(context, !isFlashlightOn)
            }

            is Action.SoundMode -> {
                setSoundMode(context, action.mode)
            }

            is Action.CycleSoundModes -> {
                cycleSoundModes(context)
            }

            is Action.ToggleMute -> {
                toggleMute(context)
            }

            is Action.ToggleDoNotDisturb -> {
                toggleDnd(context)
            }

            is Action.ToggleVibrate -> {
                toggleVibrate(context)
            }

            is Action.SetVolume -> {
                setVolume(context, action.streamType, action.volumePercent)
            }

            is Action.MediaPlayPause -> {
                sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }

            is Action.MediaNext -> {
                sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            }

            is Action.MediaPrevious -> {
                sendMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }

            is Action.TurnOnWifi -> {
                ShellUtil.runRoot("svc wifi enable")
            }

            is Action.TurnOffWifi -> {
                ShellUtil.runRoot("svc wifi disable")
            }

            is Action.ToggleWifi -> {
                val status = ShellUtil.runCommand("cmd wifi status")
                if (status.contains("enabled", ignoreCase = true)) {
                    ShellUtil.runRoot("svc wifi disable")
                } else {
                    ShellUtil.runRoot("svc wifi enable")
                }
            }

            is Action.TurnOnCellularData -> {
                ShellUtil.runRoot("svc data enable")
            }

            is Action.TurnOffCellularData -> {
                ShellUtil.runRoot("svc data disable")
            }

            is Action.ToggleCellularData -> {
                val status = ShellUtil.runCommand("cmd phone data_state")
                if (status.contains("enabled", ignoreCase = true) || status.contains("1")) {
                    ShellUtil.runRoot("svc data disable")
                } else {
                    ShellUtil.runRoot("svc data enable")
                }
            }

            is Action.TurnOnHotspot -> {
                ShellUtil.runRoot("cmd connectivity tether start-tethering")
            }

            is Action.TurnOffHotspot -> {
                ShellUtil.runRoot("cmd connectivity tether stop-tethering")
            }

            is Action.ToggleHotspot -> {
                if (isHotspotEnabled(context)) {
                    ShellUtil.runRoot("cmd connectivity tether stop-tethering")
                } else {
                    ShellUtil.runRoot("cmd connectivity tether start-tethering")
                }
            }

            is Action.TurnOnAutoBrightness -> {
                setAutoBrightness(true)
            }

            is Action.TurnOffAutoBrightness -> {
                setAutoBrightness(false)
            }

            is Action.ToggleAutoBrightness -> {
                toggleAutoBrightness()
            }

            is Action.ScreenOff -> {
                ShellUtil.runRoot("input keyevent 26")
            }

            is Action.DimWallpaper -> {
                // Cài đặt độ mờ hình nền qua system settings
                ShellUtil.runRoot("settings put system toolkit_wallpaper_dim ${action.dimAmount}")
            }

            is Action.OpenApp -> {
                if (action.packageName.isNotBlank()) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                    launchIntent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(it)
                    }
                }
            }

            is Action.OpenActivity -> {
                if (action.packageName.isNotBlank() && action.activityName.isNotBlank()) {
                    val intent = Intent().setComponent(ComponentName(action.packageName, action.activityName))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }

            is Action.TakeScreenshot -> {
                ShellUtil.runRoot("input keyevent 120")
            }

            is Action.Wait -> {
                val delayMs = (action.seconds.coerceAtLeast(1)) * 1000L
                delay(delayMs)
            }

            is Action.SendNotification -> {
                sendNotification(context, action.notifTitle, action.notifMessage)
            }

            is Action.CustomSettings -> {
                if (isSafeSetting(action.table, action.key, action.value)) {
                    ShellUtil.runRoot("settings put ${action.table} ${action.key} ${action.value}")
                } else {
                    Log.w(TAG, "Rejected unsafe custom setting: ${action.table} ${action.key}")
                }
            }

            is Action.HapticVibration -> {
                performVibration(context)
            }

            is Action.ToggleLunarLockscreen -> {
                val repo = LunarSettingsRepository(context.applicationContext)
                repo.update { current ->
                    val newMode =
                        if (current.displayMode ==
                            DisplayMode.DATE_LINE_ONLY
                        ) {
                            DisplayMode.AUTO
                        } else {
                            DisplayMode.DATE_LINE_ONLY
                        }
                    current.copy(displayMode = newMode)
                }
            }

            is Action.OpenCarDashboard -> {
                val intent =
                    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        putExtra("screen", "car_dashboard")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                intent?.let { context.startActivity(it) }
            }
        }
    }

    private val settingTables = setOf("system", "secure", "global")
    private val settingToken = Regex("[A-Za-z0-9_.:-]+")
    private val notificationId = AtomicInteger(1000)

    internal fun isSafeSetting(
        table: String,
        key: String,
        value: String,
    ): Boolean = table in settingTables && settingToken.matches(key) && settingToken.matches(value)

    private fun isHotspotEnabled(context: Context): Boolean =
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifi.javaClass.getMethod("isWifiApEnabled").invoke(wifi) as? Boolean ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Cannot read hotspot state", e)
            false
        }

    private fun setFlashlight(
        context: Context,
        enabled: Boolean,
    ) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, enabled)
            isFlashlightOn = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Cannot set flashlight", e)
        }
    }

    private suspend fun setSoundMode(
        context: Context,
        mode: Action.SoundModeType,
    ) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode =
            when (mode) {
                Action.SoundModeType.SOUND -> AudioManager.RINGER_MODE_NORMAL
                Action.SoundModeType.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                Action.SoundModeType.SILENT -> AudioManager.RINGER_MODE_SILENT
            }
        try {
            audioManager.ringerMode = ringerMode
        } catch (_: Exception) {
            ShellUtil.runRoot("settings put global mode_ringer $ringerMode")
        }
    }

    private suspend fun cycleSoundModes(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nextMode =
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
        try {
            audioManager.ringerMode = nextMode
        } catch (_: Exception) {
            ShellUtil.runRoot("settings put global mode_ringer $nextMode")
        }
    }

    private fun toggleMute(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        if (isMuted) {
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol / 2, AudioManager.FLAG_SHOW_UI)
        } else {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        }
    }

    private suspend fun toggleDnd(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            val currentFilter = nm.currentInterruptionFilter
            val newFilter =
                if (currentFilter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                } else {
                    NotificationManager.INTERRUPTION_FILTER_ALL
                }
            nm.setInterruptionFilter(newFilter)
        } else {
            val currentZen = ShellUtil.runCommand("settings get global zen_mode").trim()
            if (currentZen == "0") {
                ShellUtil.runRoot("settings put global zen_mode 1")
            } else {
                ShellUtil.runRoot("settings put global zen_mode 0")
            }
        }
    }

    private fun toggleVibrate(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val nextMode =
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                AudioManager.RINGER_MODE_NORMAL
            } else {
                AudioManager.RINGER_MODE_VIBRATE
            }
        audioManager.ringerMode = nextMode
    }

    private fun setVolume(
        context: Context,
        streamType: Int,
        percent: Int,
    ) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = (maxVolume * percent / 100).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(streamType, targetVolume, AudioManager.FLAG_SHOW_UI)
    }

    private fun sendMediaKey(
        context: Context,
        keyCode: Int,
    ) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private suspend fun setAutoBrightness(enabled: Boolean) {
        val mode = if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        ShellUtil.runRoot("settings put system screen_brightness_mode $mode")
    }

    private suspend fun toggleAutoBrightness() {
        val current = ShellUtil.runCommand("settings get system screen_brightness_mode").trim()
        val next = if (current == "1") "0" else "1"
        ShellUtil.runRoot("settings put system screen_brightness_mode $next")
    }

    private fun performVibration(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun sendNotification(
        context: Context,
        title: String,
        message: String,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Lume Tự động hóa",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            nm.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat
                .Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .build()

        nm.notify(notificationId.incrementAndGet(), notification)
    }
}
