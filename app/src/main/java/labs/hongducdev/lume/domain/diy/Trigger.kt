package labs.hongducdev.lume.domain.diy

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.DoNotDisturbOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.annotations.SerializedName

@Keep
sealed interface Trigger {
    val title: String
    val icon: ImageVector
    val permissions: List<String> get() = emptyList()
    val isConfigurable: Boolean get() = false

    @Keep
    data object ScreenOff : Trigger {
        override val title: String = "Tắt màn hình"
        override val icon: ImageVector = Icons.Rounded.Lock
    }

    @Keep
    data object ScreenOn : Trigger {
        override val title: String = "Bật màn hình"
        override val icon: ImageVector = Icons.Rounded.Smartphone
    }

    @Keep
    data object DeviceUnlock : Trigger {
        override val title: String = "Mở khóa thiết bị"
        override val icon: ImageVector = Icons.Rounded.LockOpen
    }

    @Keep
    data object ChargerConnected : Trigger {
        override val title: String = "Cắm sạc"
        override val icon: ImageVector = Icons.Rounded.BatteryChargingFull
    }

    @Keep
    data object ChargerDisconnected : Trigger {
        override val title: String = "Rút sạc"
        override val icon: ImageVector = Icons.Rounded.BatteryAlert
    }

    @Keep
    data class Schedule(
        @SerializedName("hour") val hour: Int = 0,
        @SerializedName("minute") val minute: Int = 0,
        @SerializedName("days") val days: Set<Int> = emptySet(),
    ) : Trigger {
        override val title: String get() = "Lập lịch thời gian"
        override val icon: ImageVector get() = Icons.Rounded.Alarm
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data class BluetoothConnected(
        @SerializedName("deviceAddress") val deviceAddress: String = "",
        @SerializedName("deviceName") val deviceName: String = "",
    ) : Trigger {
        override val title: String get() = "Kết nối Bluetooth"
        override val icon: ImageVector get() = Icons.Rounded.BluetoothConnected
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    listOf(android.Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    emptyList()
                }
    }

    @Keep
    data class BluetoothDisconnected(
        @SerializedName("deviceAddress") val deviceAddress: String = "",
        @SerializedName("deviceName") val deviceName: String = "",
    ) : Trigger {
        override val title: String get() = "Ngắt kết nối Bluetooth"
        override val icon: ImageVector get() = Icons.Rounded.BluetoothDisabled
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    listOf(android.Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    emptyList()
                }
    }

    @Keep
    data class WifiConnected(
        @SerializedName("ssid") val ssid: String = "",
    ) : Trigger {
        override val title: String get() = "Kết nối Wi-Fi"
        override val icon: ImageVector get() = Icons.Rounded.Wifi
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Keep
    data class WifiDisconnected(
        @SerializedName("ssid") val ssid: String = "",
    ) : Trigger {
        override val title: String get() = "Ngắt kết nối Wi-Fi"
        override val icon: ImageVector get() = Icons.Rounded.WifiOff
        override val isConfigurable: Boolean get() = true
        override val permissions: List<String>
            get() = listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Keep
    data object PowerSavingOn : Trigger {
        override val title: String = "Bật tiết kiệm pin"
        override val icon: ImageVector = Icons.Rounded.BatterySaver
    }

    @Keep
    data object PowerSavingOff : Trigger {
        override val title: String = "Tắt tiết kiệm pin"
        override val icon: ImageVector = Icons.Rounded.BatteryAlert
    }

    @Keep
    data class NotificationReceived(
        @SerializedName("packageName") val packageName: String = "",
        @SerializedName("titleFilter") val titleFilter: String = "",
    ) : Trigger {
        override val title: String get() = "Nhận thông báo"
        override val icon: ImageVector get() = Icons.Rounded.Notifications
        override val isConfigurable: Boolean get() = true
    }

    @Keep
    data object DndOn : Trigger {
        override val title: String = "Bật Không làm phiền (DND)"
        override val icon: ImageVector = Icons.Rounded.DoNotDisturb
    }

    @Keep
    data object DndOff : Trigger {
        override val title: String = "Tắt Không làm phiền (DND)"
        override val icon: ImageVector = Icons.Rounded.DoNotDisturbOff
    }

    @Keep
    data object AndroidAutoConnected : Trigger {
        override val title: String = "Kết nối Android Auto"
        override val icon: ImageVector = Icons.Rounded.DirectionsCar
    }

    @Keep
    data object AndroidAutoDisconnected : Trigger {
        override val title: String = "Ngắt kết nối Android Auto"
        override val icon: ImageVector = Icons.Rounded.DirectionsCar
    }
}
