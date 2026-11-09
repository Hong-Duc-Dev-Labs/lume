package labs.hongducdev.lume.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.fontDataStore by preferencesDataStore(name = "font_settings")

/**
 * Cài đặt phông chữ giao diện hệ thống (System UI Font).
 */
data class FontSettings(
    val isEnabled: Boolean = false,
    val fontName: String = "",
    val fontVersion: Long = 0L,
    val fileSize: Long = 0L,
)

class FontSettingsRepository(
    private val context: Context,
) {
    val settings: Flow<FontSettings> = context.fontDataStore.data.map { it.toFontSettings() }

    fun getFontFile(): File {
        val dir = File(context.filesDir, "fonts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, FONT_FILE_NAME)
    }

    suspend fun saveFontFromUri(uri: Uri): Result<FontSettings> =
        withContext(Dispatchers.IO) {
            try {
                var displayName = "custom_font.ttf"
                var size = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) {
                            cursor.getString(nameIndex)?.let { displayName = it }
                        }
                        if (sizeIndex != -1) {
                            size = cursor.getLong(sizeIndex)
                        }
                    }
                }

                val targetFile = getFontFile()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(Exception("Không thể mở luồng đọc file font từ hệ thống."))

                if (size <= 0L) {
                    size = targetFile.length()
                }
                val newVersion = System.currentTimeMillis()

                val newSettings =
                    FontSettings(
                        isEnabled = true,
                        fontName = displayName,
                        fontVersion = newVersion,
                        fileSize = size,
                    )

                context.fontDataStore.edit { prefs ->
                    prefs[KEY_IS_ENABLED] = newSettings.isEnabled
                    prefs[KEY_FONT_NAME] = newSettings.fontName
                    prefs[KEY_FONT_VERSION] = newSettings.fontVersion
                    prefs[KEY_FILE_SIZE] = newSettings.fileSize
                }

                Result.success(newSettings)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun setEnabled(enabled: Boolean) {
        context.fontDataStore.edit { prefs ->
            prefs[KEY_IS_ENABLED] = enabled
        }
    }

    suspend fun resetFont() =
        withContext(Dispatchers.IO) {
            val file = getFontFile()
            if (file.exists()) {
                file.delete()
            }
            context.fontDataStore.edit { prefs ->
                prefs[KEY_IS_ENABLED] = false
                prefs[KEY_FONT_NAME] = ""
                prefs[KEY_FONT_VERSION] = 0L
                prefs[KEY_FILE_SIZE] = 0L
            }
        }

    private fun Preferences.toFontSettings(): FontSettings {
        val defaults = FontSettings()
        return FontSettings(
            isEnabled = this[KEY_IS_ENABLED] ?: defaults.isEnabled,
            fontName = this[KEY_FONT_NAME] ?: defaults.fontName,
            fontVersion = this[KEY_FONT_VERSION] ?: defaults.fontVersion,
            fileSize = this[KEY_FILE_SIZE] ?: defaults.fileSize,
        )
    }

    companion object {
        const val FONT_FILE_NAME = "custom_system_font.ttf"
        private val KEY_IS_ENABLED = booleanPreferencesKey("font_enabled")
        private val KEY_FONT_NAME = stringPreferencesKey("font_name")
        private val KEY_FONT_VERSION = longPreferencesKey("font_version")
        private val KEY_FILE_SIZE = longPreferencesKey("font_file_size")
    }
}
