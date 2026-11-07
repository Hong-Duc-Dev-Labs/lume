package com.hongducdev.toolkit.xposed

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.hongducdev.toolkit.data.LunarDisplayPolicy
import com.hongducdev.toolkit.data.LunarSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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

    /** Hook SystemUI gọi `heartbeat` để báo rằng nó đang chạy. */
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_HEARTBEAT) return null
        // Chỉ SystemUI được báo hiệu, để app khác không giả vờ hook đang chạy.
        if (callingPackage != SYSTEM_UI_PACKAGE) {
            Log.w(LOG_TAG, "từ chối heartbeat từ $callingPackage")
            return null
        }
        val context = context ?: return null
        val now = System.currentTimeMillis()
        runBlocking { LunarSettingsRepository(context).update { it.copy(hookLastSeen = now) } }
        Log.i(LOG_TAG, "đã nhận heartbeat từ SystemUI")
        return Bundle.EMPTY
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.com.hongducdev.toolkit.hook"

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "com.hongducdev.toolkit.settings"
        const val METHOD_HEARTBEAT = "heartbeat"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val LOG_TAG = "ToolkitProvider"
    }
}
