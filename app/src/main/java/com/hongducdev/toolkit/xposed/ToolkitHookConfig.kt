package com.hongducdev.toolkit.xposed

import com.hongducdev.toolkit.data.ScreenTimeoutPresets
import com.hongducdev.toolkit.data.ScreenTimeoutSettingsRepository
import de.robv.android.xposed.XSharedPreferences

/**
 * Đọc cấu hình từ app Toolkit bên trong các tiến trình bị hook (SystemUI, system_server).
 *
 * Sử dụng [XSharedPreferences] đọc trực tiếp file XML để có độ trễ cực thấp
 * và loại bỏ nguy cơ deadlock IPC trong tiến trình hệ thống.
 */
object ToolkitHookConfig {
    private const val PACKAGE_NAME = "com.hongducdev.toolkit"

    private val preferences: XSharedPreferences? by lazy {
        runCatching {
            XSharedPreferences(PACKAGE_NAME, ScreenTimeoutSettingsRepository.HOOK_PREFS_NAME).apply {
                makeWorldReadable()
            }
        }.getOrNull()
    }

    fun getLockscreenTimeoutMs(): Long? {
        val prefs = preferences ?: return null
        runCatching {
            if (prefs.hasFileChanged()) {
                prefs.reload()
            }
        }
        val preset =
            runCatching {
                prefs.getInt(ScreenTimeoutSettingsRepository.KEY_LOCKSCREEN_PRESET_NAME, 0)
            }.getOrDefault(0)
        return ScreenTimeoutPresets.millisForIndex(preset)
    }

    fun getScreenTimeoutMs(): Long? {
        val prefs = preferences ?: return null
        runCatching {
            if (prefs.hasFileChanged()) {
                prefs.reload()
            }
        }
        val preset =
            runCatching {
                prefs.getInt(ScreenTimeoutSettingsRepository.KEY_SCREEN_PRESET_NAME, 0)
            }.getOrDefault(0)
        return ScreenTimeoutPresets.millisForIndex(preset)
    }
}
