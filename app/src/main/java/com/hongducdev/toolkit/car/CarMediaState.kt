package com.hongducdev.toolkit.car

import android.graphics.Bitmap

data class CarMediaState(
    val isAvailable: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArt: Bitmap? = null,
    val appPackage: String = "",
    val appName: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isNotificationListenerEnabled: Boolean = false
)
