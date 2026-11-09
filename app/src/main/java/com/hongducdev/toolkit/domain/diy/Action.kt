package com.hongducdev.toolkit.domain.diy

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.annotations.SerializedName

@Keep
sealed interface Action {
    val title: String
    val icon: ImageVector
    val permissions: List<String> get() = emptyList()
    val isConfigurable: Boolean get() = false

    // --- Flashlight ---
    @Keep
    data object TurnOnFlashlight : Action {
        override val title: String = "Bật đèn pin"
        override val icon: ImageVector = Icons.Rounded.FlashlightOn
    }

    @Keep
    data object TurnOffFlashlight : Action {
        override val title: String = "Tắt đèn pin"
        override val icon: ImageVector = Icons.Rounded.FlashlightOff
    }

    @Keep
    data object ToggleFlashlight : Action {
        override val title: String = "Bật/Tắt đèn pin"
        override val icon: ImageVector = Icons.Rounded.FlashlightOn
    }

    // --- Sound & Audio ---
    @Keep
    enum class SoundModeType {
        @SerializedName("SOUND")
        SOUND,

        @SerializedName("VIBRATE")
        VIBRATE,

        @SerializedName("SILENT")
        SILENT,
    }

    @Keep
    data class SoundMode(
        @SerializedName("mode") val mode: SoundModeType = SoundModeType.SOUND,
    ) : Action {
        override val title: String get() = "Chế độ âm thanh: ${mode.name}"
        override val icon: ImageVector get() = Icons.Rounded.VolumeUp
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data object CycleSoundModes : Action {
        override val title: String = "Chuyển vòng chế độ âm thanh"
        override val icon: ImageVector = Icons.Rounded.VolumeUp
    }

    @Keep
    data object ToggleMute : Action {
        override val title: String = "Tắt/Bật tiếng"
        override val icon: ImageVector = Icons.Rounded.VolumeOff
    }

    @Keep
    data object ToggleDoNotDisturb : Action {
        override val title: String = "Bật/Tắt Không làm phiền (DND)"
        override val icon: ImageVector = Icons.Rounded.DoNotDisturb
    }

    @Keep
    data object ToggleVibrate : Action {
        override val title: String = "Bật/Tắt rung"
        override val icon: ImageVector = Icons.Rounded.Vibration
    }

    @Keep
    data class SetVolume(
        @SerializedName("streamType") val streamType: Int = 3, // AudioManager.STREAM_MUSIC
        @SerializedName("volumePercent") val volumePercent: Int = 50,
    ) : Action {
        override val title: String get() = "Đặt âm lượng ($volumePercent%)"
        override val icon: ImageVector get() = Icons.Rounded.VolumeUp
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data object MediaPlayPause : Action {
        override val title: String = "Phát / Tạm dừng đa phương tiện"
        override val icon: ImageVector = Icons.Rounded.VolumeMute
    }

    @Keep
    data object MediaNext : Action {
        override val title: String = "Bài tiếp theo"
        override val icon: ImageVector = Icons.Rounded.SkipNext
    }

    @Keep
    data object MediaPrevious : Action {
        override val title: String = "Bài trước đó"
        override val icon: ImageVector = Icons.Rounded.SkipPrevious
    }

    // --- Connectivity ---
    @Keep
    data object TurnOnWifi : Action {
        override val title: String = "Bật Wi-Fi"
        override val icon: ImageVector = Icons.Rounded.Wifi
    }

    @Keep
    data object TurnOffWifi : Action {
        override val title: String = "Tắt Wi-Fi"
        override val icon: ImageVector = Icons.Rounded.WifiOff
    }

    @Keep
    data object ToggleWifi : Action {
        override val title: String = "Bật/Tắt Wi-Fi"
        override val icon: ImageVector = Icons.Rounded.Wifi
    }

    @Keep
    data object TurnOnCellularData : Action {
        override val title: String = "Bật Dữ liệu di động"
        override val icon: ImageVector = Icons.Rounded.CellTower
    }

    @Keep
    data object TurnOffCellularData : Action {
        override val title: String = "Tắt Dữ liệu di động"
        override val icon: ImageVector = Icons.Rounded.CellTower
    }

    @Keep
    data object ToggleCellularData : Action {
        override val title: String = "Bật/Tắt Dữ liệu di động"
        override val icon: ImageVector = Icons.Rounded.CellTower
    }

    @Keep
    data object TurnOnHotspot : Action {
        override val title: String = "Bật Điểm phát sóng (Hotspot)"
        override val icon: ImageVector = Icons.Rounded.WifiTethering
    }

    @Keep
    data object TurnOffHotspot : Action {
        override val title: String = "Tắt Điểm phát sóng (Hotspot)"
        override val icon: ImageVector = Icons.Rounded.WifiTethering
    }

    @Keep
    data object ToggleHotspot : Action {
        override val title: String = "Bật/Tắt Điểm phát sóng (Hotspot)"
        override val icon: ImageVector = Icons.Rounded.WifiTethering
    }

    // --- Display ---
    @Keep
    data object TurnOnAutoBrightness : Action {
        override val title: String = "Bật Độ sáng tự động"
        override val icon: ImageVector = Icons.Rounded.Tune
    }

    @Keep
    data object TurnOffAutoBrightness : Action {
        override val title: String = "Tắt Độ sáng tự động"
        override val icon: ImageVector = Icons.Rounded.Tune
    }

    @Keep
    data object ToggleAutoBrightness : Action {
        override val title: String = "Bật/Tắt Độ sáng tự động"
        override val icon: ImageVector = Icons.Rounded.Tune
    }

    @Keep
    data object ScreenOff : Action {
        override val title: String = "Khóa màn hình"
        override val icon: ImageVector = Icons.Rounded.Lock
    }

    @Keep
    data class DimWallpaper(
        @SerializedName("dimAmount") val dimAmount: Float = 0.5f,
    ) : Action {
        override val title: String get() = "Làm mờ hình nền"
        override val icon: ImageVector get() = Icons.Rounded.Wallpaper
        override val isConfigurable: Boolean get() = true
    }

    // --- Apps & System ---
    @Keep
    data class OpenApp(
        @SerializedName("packageName") val packageName: String = "",
        @SerializedName("appName") val appName: String = "",
    ) : Action {
        override val title: String get() = if (appName.isNotBlank()) "Mở ứng dụng $appName" else "Mở ứng dụng"
        override val icon: ImageVector get() = Icons.Rounded.Apps
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data class OpenActivity(
        @SerializedName("packageName") val packageName: String = "",
        @SerializedName("activityName") val activityName: String = "",
    ) : Action {
        override val title: String get() = "Mở Activity: ${activityName.substringAfterLast('.')}"
        override val icon: ImageVector get() = Icons.Rounded.Apps
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data object TakeScreenshot : Action {
        override val title: String = "Chụp ảnh màn hình"
        override val icon: ImageVector = Icons.Rounded.PhotoCamera
    }

    @Keep
    data class Wait(
        @SerializedName("seconds") val seconds: Int = 1,
    ) : Action {
        override val title: String get() = "Chờ $seconds giây"
        override val icon: ImageVector get() = Icons.Rounded.HourglassEmpty
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data class SendNotification(
        @SerializedName("title") val notifTitle: String = "Toolkit DIY",
        @SerializedName("message") val notifMessage: String = "Quy tắc tự động hóa đã thực thi",
    ) : Action {
        override val title: String get() = "Gửi thông báo: $notifTitle"
        override val icon: ImageVector get() = Icons.Rounded.Notifications
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data class CustomSettings(
        @SerializedName("table") val table: String = "system", // system, secure, global
        @SerializedName("key") val key: String = "",
        @SerializedName("value") val value: String = "",
    ) : Action {
        override val title: String get() = "Cài đặt hệ thống: $key = $value"
        override val icon: ImageVector get() = Icons.Rounded.Tune
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data object HapticVibration : Action {
        override val title: String = "Rung phản hồi (Haptic)"
        override val icon: ImageVector = Icons.Rounded.Vibration
    }

    // --- Toolkit Extensions ---
    @Keep
    data object ToggleLunarLockscreen : Action {
        override val title: String = "Bật/Tắt Lịch âm màn hình khóa"
        override val icon: ImageVector = Icons.Rounded.CalendarMonth
    }

    @Keep
    data object OpenCarDashboard : Action {
        override val title: String = "Mở giao diện Lái xe (Car Dashboard)"
        override val icon: ImageVector = Icons.Rounded.DirectionsCar
    }
}
