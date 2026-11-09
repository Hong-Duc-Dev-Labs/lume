package labs.hongducdev.lume.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import labs.hongducdev.lume.lunar.LunarFormatter
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.toolkitDataStore by preferencesDataStore(name = "toolkit_settings")

/** Cài đặt hiển thị lịch âm trên hình nền màn hình khóa. */
data class LunarSettings(
    val displayMode: DisplayMode = DisplayMode.AUTO,
    /** Thời điểm (ms) hook SystemUI báo hiệu lần gần nhất, 0 nếu chưa từng. */
    val hookLastSeen: Long = 0L,
    val template: String = LunarFormatter.DEFAULT_TEMPLATE,
    val textSizeSp: Float = 22f,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    /** Vị trí tâm chữ theo tỉ lệ chiều ngang màn hình, từ 0 đến 1. */
    val xFraction: Float = 0.5f,
    /** Vị trí đường cơ sở của dòng đầu theo tỉ lệ chiều dọc màn hình, từ 0 đến 1. */
    val yFraction: Float = 0.67f,
    /** Tăng mỗi lần đổi ảnh nền, 0 nghĩa là chưa chọn ảnh. */
    val backgroundVersion: Long = 0L,
    /** Mẫu cho dòng ngày của SystemUI, nên ngắn gọn. */
    val hookTemplate: String = LunarFormatter.DEFAULT_TEMPLATE,
)

class LunarSettingsRepository(private val context: Context) {

    val settings: Flow<LunarSettings> = context.toolkitDataStore.data.map { it.toSettings() }

    suspend fun update(transform: (LunarSettings) -> LunarSettings) {
        context.toolkitDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[KEY_DISPLAY_MODE] = next.displayMode.name
            prefs[KEY_HOOK_LAST_SEEN] = next.hookLastSeen
            prefs[KEY_TEMPLATE] = next.template
            prefs[KEY_TEXT_SIZE] = next.textSizeSp
            prefs[KEY_COLOR] = next.colorArgb
            prefs[KEY_X] = next.xFraction
            prefs[KEY_Y] = next.yFraction
            prefs[KEY_BACKGROUND_VERSION] = next.backgroundVersion
            prefs[KEY_HOOK_TEMPLATE] = next.hookTemplate
        }
    }

    fun backgroundFile(): File = File(context.filesDir, BACKGROUND_FILE_NAME)

    private fun Preferences.toSettings(): LunarSettings {
        val defaults = LunarSettings()
        return LunarSettings(
            displayMode = DisplayMode.fromName(this[KEY_DISPLAY_MODE]),
            hookLastSeen = this[KEY_HOOK_LAST_SEEN] ?: defaults.hookLastSeen,
            template = this[KEY_TEMPLATE] ?: defaults.template,
            textSizeSp = this[KEY_TEXT_SIZE] ?: defaults.textSizeSp,
            colorArgb = this[KEY_COLOR] ?: defaults.colorArgb,
            xFraction = this[KEY_X] ?: defaults.xFraction,
            yFraction = this[KEY_Y] ?: defaults.yFraction,
            backgroundVersion = this[KEY_BACKGROUND_VERSION] ?: defaults.backgroundVersion,
            hookTemplate = this[KEY_HOOK_TEMPLATE] ?: defaults.hookTemplate,
        )
    }

    private companion object {
        const val BACKGROUND_FILE_NAME = "lock_background.jpg"
        val KEY_DISPLAY_MODE = stringPreferencesKey("lunar_display_mode")
        val KEY_HOOK_LAST_SEEN = longPreferencesKey("lunar_hook_last_seen")
        val KEY_TEMPLATE = stringPreferencesKey("lunar_template")
        val KEY_TEXT_SIZE = floatPreferencesKey("lunar_text_size_sp")
        val KEY_COLOR = intPreferencesKey("lunar_color")
        val KEY_X = floatPreferencesKey("lunar_x_fraction")
        val KEY_Y = floatPreferencesKey("lunar_y_fraction")
        val KEY_BACKGROUND_VERSION = longPreferencesKey("lunar_background_version")
        val KEY_HOOK_TEMPLATE = stringPreferencesKey("lunar_hook_template")
    }
}
