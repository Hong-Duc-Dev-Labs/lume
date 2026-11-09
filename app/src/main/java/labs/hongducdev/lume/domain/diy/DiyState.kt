package labs.hongducdev.lume.domain.diy

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.annotations.SerializedName

@Keep
sealed interface DiyState {
    val title: String
    val icon: ImageVector

    @Keep
    data object Charging : DiyState {
        override val title: String = "Đang sạc pin"
        override val icon: ImageVector = Icons.Rounded.BatteryChargingFull
    }

    @Keep
    data class BatteryLevel(
        @SerializedName("minLevel") val minLevel: Int = 0,
        @SerializedName("maxLevel") val maxLevel: Int = 20,
    ) : DiyState {
        override val title: String get() = "Mức pin ($minLevel% - $maxLevel%)"
        override val icon: ImageVector get() = Icons.Rounded.BatteryStd
    }

    @Keep
    data object ScreenOn : DiyState {
        override val title: String = "Màn hình đang bật"
        override val icon: ImageVector = Icons.Rounded.Smartphone
    }

    @Keep
    data object PowerSaving : DiyState {
        override val title: String = "Chế độ tiết kiệm pin đang bật"
        override val icon: ImageVector = Icons.Rounded.BatterySaver
    }

    @Keep
    data class TimePeriod(
        @SerializedName("startHour") val startHour: Int = 0,
        @SerializedName("startMinute") val startMinute: Int = 0,
        @SerializedName("endHour") val endHour: Int = 0,
        @SerializedName("endMinute") val endMinute: Int = 0,
        @SerializedName("days") val days: Set<Int> = emptySet(),
    ) : DiyState {
        override val title: String get() = "Trong khoảng thời gian"
        override val icon: ImageVector get() = Icons.Rounded.Schedule
    }

    @Keep
    data class CalendarEvent(
        @SerializedName("calendarIds") val calendarIds: Set<Long> = emptySet(),
        @SerializedName("includeAllDay") val includeAllDay: Boolean = false,
    ) : DiyState {
        override val title: String get() = "Đang trong sự kiện lịch"
        override val icon: ImageVector get() = Icons.Rounded.CalendarMonth
    }
}
