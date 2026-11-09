package labs.hongducdev.lume.xposed

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.TextView
import labs.hongducdev.lume.lunar.LunarCalendar
import labs.hongducdev.lume.lunar.LunarFormatter
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.time.Instant
import java.util.concurrent.Executors

/**
 * Chèn lịch âm vào dòng ngày dưới đồng hồ trên màn hình khóa.
 *
 * Móc vào `IcuDateTextView.onTimeChanged` của smartspace trong SystemUI. Mọi lỗi đều bị
 * nuốt để SystemUI không bao giờ bị crash vì module này.
 */
class SystemUiHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Áp dụng hook phông chữ UI hệ thống
        if (lpparam.packageName == TARGET_PACKAGE || lpparam.packageName == ANDROID_PACKAGE) {
            SystemFontHook.handleLoadPackage(lpparam)
        }

        if (lpparam.packageName == ANDROID_PACKAGE) {
            ScreenTimeoutHook.apply(lpparam)
            return
        }

        if (lpparam.packageName != TARGET_PACKAGE) return
        Log.i(LOG_TAG, "nạp vào SystemUI")

        // Hook thời gian sáng màn hình khóa
        LockscreenTimeoutHook.apply(lpparam)

        try {
            XposedHelpers.findAndHookMethod(
                DATE_VIEW_CLASS,
                lpparam.classLoader,
                "onTimeChanged",
                Boolean::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        appendLunarDate(param.thisObject as? TextView)
                    }
                },
            )
            Log.i(LOG_TAG, "đã móc $DATE_VIEW_CLASS.onTimeChanged")
        } catch (error: Throwable) {
            Log.e(LOG_TAG, "không móc được $DATE_VIEW_CLASS", error)
            XposedBridge.log("Lume: không móc được $DATE_VIEW_CLASS: $error")
        }
    }

    private fun appendLunarDate(view: TextView?) {
        if (view == null) return
        try {
            val base = XposedHelpers.getObjectField(view, "mText") as? String ?: return
            val template = HookSettingsCache.template(view.context)
            HookSettingsCache.heartbeat(view.context)
            if (template.isBlank()) return
            val lunar = LunarFormatter.format(template, LunarCalendar.fromInstant(Instant.now()))
            if (lunar.isBlank()) return
            val combined = "$base$SEPARATOR$lunar"
            if (view.text.toString() != combined) {
                view.text = combined
                Log.i(LOG_TAG, "đã thêm lịch âm: $combined")
            }
        } catch (error: Throwable) {
            Log.e(LOG_TAG, "lỗi khi thêm lịch âm", error)
            XposedBridge.log("Lume: lỗi khi thêm lịch âm: $error")
        }
    }

    private companion object {
        const val LOG_TAG = "LumeHook"
        const val TARGET_PACKAGE = "com.android.systemui"
        const val ANDROID_PACKAGE = "android"
        const val DATE_VIEW_CLASS = "com.google.android.systemui.smartspace.IcuDateTextView"
        const val SEPARATOR = " · "
    }
}

/** Đọc mẫu hiển thị từ app Toolkit qua ContentProvider, có bộ nhớ đệm để không chặn luồng giao diện. */
internal object HookSettingsCache {
    private const val REFRESH_INTERVAL_MS = 30_000L
    private const val HEARTBEAT_INTERVAL_MS = 10 * 60_000L
    private val uri: Uri = Uri.parse("content://${LunarSettingsProvider.AUTHORITY}/hook")
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile private var cachedTemplate: String = LunarFormatter.DEFAULT_TEMPLATE

    @Volatile private var lastRefresh = 0L

    @Volatile private var refreshing = false

    @Volatile private var lastHeartbeat = 0L

    /** Báo cho app Toolkit biết hook đang chạy, tối đa mỗi 10 phút một lần. */
    fun heartbeat(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastHeartbeat < HEARTBEAT_INTERVAL_MS) return
        lastHeartbeat = now
        val appContext = context.applicationContext ?: context
        executor.execute {
            try {
                appContext.contentResolver.call(uri, LunarSettingsProvider.METHOD_HEARTBEAT, null, null)
            } catch (_: Throwable) {
                // App chưa cài hoặc provider chưa sẵn sàng.
            }
        }
    }

    fun template(context: Context): String {
        val now = System.currentTimeMillis()
        if (!refreshing && now - lastRefresh > REFRESH_INTERVAL_MS) {
            refreshing = true
            val appContext = context.applicationContext ?: context
            executor.execute {
                try {
                    appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            cachedTemplate = cursor.getString(0) ?: ""
                        }
                    }
                } catch (_: Throwable) {
                    // App chưa cài hoặc provider chưa sẵn sàng: giữ giá trị cũ.
                } finally {
                    lastRefresh = System.currentTimeMillis()
                    refreshing = false
                }
            }
        }
        return cachedTemplate
    }
}
