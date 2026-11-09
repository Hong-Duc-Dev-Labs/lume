package labs.hongducdev.lume.xposed

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import labs.hongducdev.lume.data.FontSettingsRepository
import labs.hongducdev.lume.data.LunarDisplayPolicy
import labs.hongducdev.lume.data.LunarSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

/**
 * Cho SystemUI (chạy trong tiến trình khác) đọc mẫu hiển thị của hook. Chỉ đọc, chỉ trả về một chuỗi mẫu.
 */
class LunarSettingsProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val context = requireNotNull(context)
        val settings = runBlocking { LunarSettingsRepository(context).settings.first() }
        val template = if (LunarDisplayPolicy.hookShouldShow(settings.displayMode)) settings.hookTemplate else ""
        return MatrixCursor(arrayOf("template")).apply { addRow(arrayOf<Any>(template)) }
    }

    override fun openFile(
        uri: Uri,
        mode: String,
    ): ParcelFileDescriptor? {
        val path = uri.path ?: return null
        if (path.contains("font")) {
            val ctx = context ?: throw FileNotFoundException("Context is null")
            val fontFile = FontSettingsRepository(ctx).getFontFile()
            if (fontFile.exists() && fontFile.length() > 0) {
                return ParcelFileDescriptor.open(fontFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                throw FileNotFoundException("Font file not found: ${fontFile.absolutePath}")
            }
        }
        return super.openFile(uri, mode)
    }

    /** Hook SystemUI gọi `heartbeat` để báo rằng nó đang chạy, hoặc truy vấn cấu hình font UI. */
    override fun call(
        method: String,
        arg: String?,
        extras: Bundle?,
    ): Bundle? {
        val context = context ?: return null
        when (method) {
            METHOD_HEARTBEAT -> {
                // Chỉ SystemUI được báo hiệu, để app khác không giả vờ hook đang chạy.
                if (callingPackage != SYSTEM_UI_PACKAGE) {
                    Log.w(LOG_TAG, "từ chối heartbeat từ $callingPackage")
                    return null
                }
                val now = System.currentTimeMillis()
                runBlocking { LunarSettingsRepository(context).update { it.copy(hookLastSeen = now) } }
                Log.i(LOG_TAG, "đã nhận heartbeat từ SystemUI")
                return Bundle.EMPTY
            }

            METHOD_GET_FONT_CONFIG -> {
                val repo = FontSettingsRepository(context)
                val settings = runBlocking { repo.settings.first() }
                val fontFile = repo.getFontFile()
                val exists = fontFile.exists() && fontFile.length() > 0
                return Bundle().apply {
                    putBoolean(EXTRA_FONT_ENABLED, settings.isEnabled && exists)
                    putString(EXTRA_FONT_NAME, settings.fontName)
                    putLong(EXTRA_FONT_VERSION, settings.fontVersion)
                    putString(EXTRA_FONT_URI, "content://$AUTHORITY/font")
                }
            }
        }
        return null
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.labs.hongducdev.lume.hook"

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "labs.hongducdev.lume.settings"
        const val METHOD_HEARTBEAT = "heartbeat"
        const val METHOD_GET_FONT_CONFIG = "get_font_config"
        const val EXTRA_FONT_ENABLED = "font_enabled"
        const val EXTRA_FONT_NAME = "font_name"
        const val EXTRA_FONT_VERSION = "font_version"
        const val EXTRA_FONT_URI = "font_uri"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val LOG_TAG = "LumeProvider"
    }
}
