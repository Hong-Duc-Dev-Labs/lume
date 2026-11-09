package com.hongducdev.toolkit.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.screenTimeoutDataStore by preferencesDataStore(name = "screen_timeout_settings")

/**
 * Cài đặt thời gian sáng màn hình khoá và màn hình mở khoá.
 */
data class ScreenTimeoutSettings(
    val lockscreenPresetIndex: Int = 0,
    val screenPresetIndex: Int = 0,
) {
    val lockscreenTimeoutLabel: String
        get() = ScreenTimeoutPresets.labelForIndex(lockscreenPresetIndex)

    val screenTimeoutLabel: String
        get() = ScreenTimeoutPresets.labelForIndex(screenPresetIndex)
}

class ScreenTimeoutSettingsRepository(
    private val context: Context,
) {
    val settings: Flow<ScreenTimeoutSettings> =
        context.screenTimeoutDataStore.data.map { it.toSettings() }

    suspend fun update(transform: (ScreenTimeoutSettings) -> ScreenTimeoutSettings) {
        context.screenTimeoutDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[KEY_LOCKSCREEN_PRESET] = next.lockscreenPresetIndex
            prefs[KEY_SCREEN_PRESET] = next.screenPresetIndex
            syncToHookBridge(next)
        }
    }

    private fun syncToHookBridge(settings: ScreenTimeoutSettings) {
        try {
            val sp = context.getSharedPreferences(HOOK_PREFS_NAME, Context.MODE_PRIVATE)
            sp
                .edit()
                .putInt(KEY_LOCKSCREEN_PRESET_NAME, settings.lockscreenPresetIndex)
                .putInt(KEY_SCREEN_PRESET_NAME, settings.screenPresetIndex)
                .commit()

            // Cấp quyền đọc cho module Xposed (LSPosed / Vector)
            val prefsFile = File(context.applicationInfo.dataDir, "shared_prefs/$HOOK_PREFS_NAME.xml")
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false)
            }
        } catch (_: Throwable) {
            // Không để lỗi đồng bộ ảnh hưởng đến luồng chính
        }
    }

    private fun Preferences.toSettings(): ScreenTimeoutSettings =
        ScreenTimeoutSettings(
            lockscreenPresetIndex = this[KEY_LOCKSCREEN_PRESET] ?: 0,
            screenPresetIndex = this[KEY_SCREEN_PRESET] ?: 0,
        )

    companion object {
        const val HOOK_PREFS_NAME = "toolkit_hook_settings"
        const val KEY_LOCKSCREEN_PRESET_NAME = "lockscreen_timeout_preset"
        const val KEY_SCREEN_PRESET_NAME = "screen_timeout_preset"

        val KEY_LOCKSCREEN_PRESET = intPreferencesKey(KEY_LOCKSCREEN_PRESET_NAME)
        val KEY_SCREEN_PRESET = intPreferencesKey(KEY_SCREEN_PRESET_NAME)
    }
}
