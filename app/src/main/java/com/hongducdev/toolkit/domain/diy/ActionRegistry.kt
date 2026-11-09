package com.hongducdev.toolkit.domain.diy

object ActionRegistry {
    data class ActionCategory(
        val title: String,
        val actions: List<Action>,
    )

    fun getCategories(): List<ActionCategory> {
        val connectivityActions =
            listOf(
                Action.TurnOnWifi,
                Action.TurnOffWifi,
                Action.ToggleWifi,
                Action.TurnOnCellularData,
                Action.TurnOffCellularData,
                Action.ToggleCellularData,
                Action.TurnOnHotspot,
                Action.TurnOffHotspot,
                Action.ToggleHotspot,
            )

        val soundMediaActions =
            listOf(
                Action.SoundMode(Action.SoundModeType.SOUND),
                Action.CycleSoundModes,
                Action.ToggleMute,
                Action.ToggleDoNotDisturb,
                Action.ToggleVibrate,
                Action.SetVolume(),
                Action.MediaPlayPause,
                Action.MediaNext,
                Action.MediaPrevious,
            )

        val displayActions =
            listOf(
                Action.TurnOnAutoBrightness,
                Action.TurnOffAutoBrightness,
                Action.ToggleAutoBrightness,
                Action.ScreenOff,
                Action.DimWallpaper(),
            )

        val appActions =
            listOf(
                Action.OpenApp(),
                Action.OpenActivity(),
                Action.TakeScreenshot,
            )

        val systemActions =
            listOf(
                Action.TurnOnFlashlight,
                Action.TurnOffFlashlight,
                Action.ToggleFlashlight,
                Action.HapticVibration,
                Action.Wait(seconds = 2),
                Action.SendNotification(),
                Action.CustomSettings(),
            )

        val toolkitActions =
            listOf(
                Action.ToggleLunarLockscreen,
                Action.OpenCarDashboard,
            )

        return listOf(
            ActionCategory("Kết nối mạng", connectivityActions),
            ActionCategory("Âm thanh & Giải trí", soundMediaActions),
            ActionCategory("Màn hình & Hiển thị", displayActions),
            ActionCategory("Ứng dụng", appActions),
            ActionCategory("Hệ thống & Tiện ích", systemActions),
            ActionCategory("Tính năng Toolkit", toolkitActions),
        )
    }
}
