package com.hongducdev.toolkit.data

import java.util.concurrent.TimeUnit

/** Cách hiển thị lịch âm trên màn hình khóa. */
enum class DisplayMode {
    /** Dùng dòng ngày của SystemUI khi hook hoạt động, ngược lại vẽ lên hình nền. */
    AUTO,
    WALLPAPER_ONLY,
    DATE_LINE_ONLY,
    ;

    companion object {
        fun fromName(name: String?): DisplayMode = entries.firstOrNull { it.name == name } ?: AUTO
    }
}

object LunarDisplayPolicy {
    private val HOOK_FRESHNESS_MS = TimeUnit.HOURS.toMillis(36)

    /** Hook được xem là hoạt động nếu đã báo hiệu sau lần khởi động này và trong vòng 36 giờ. */
    fun isHookActive(lastSeenMillis: Long, bootMillis: Long, nowMillis: Long): Boolean =
        lastSeenMillis > 0 && lastSeenMillis >= bootMillis && nowMillis - lastSeenMillis < HOOK_FRESHNESS_MS

    fun hookShouldShow(mode: DisplayMode): Boolean = mode != DisplayMode.WALLPAPER_ONLY

    fun wallpaperShouldShow(mode: DisplayMode, hookActive: Boolean): Boolean = when (mode) {
        DisplayMode.WALLPAPER_ONLY -> true
        DisplayMode.DATE_LINE_ONLY -> false
        DisplayMode.AUTO -> !hookActive
    }
}
