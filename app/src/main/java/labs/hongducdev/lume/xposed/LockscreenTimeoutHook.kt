package labs.hongducdev.lume.xposed

import android.util.Log
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookAllMethods
import de.robv.android.xposed.XposedHelpers.findClassIfExists
import de.robv.android.xposed.XposedHelpers.getBooleanField
import de.robv.android.xposed.XposedHelpers.getLongField
import de.robv.android.xposed.XposedHelpers.setLongField
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hook SystemUI: Yêu cầu WindowManager duy trì cửa sổ lockscreen sáng theo mốc đã cấu hình.
 *
 * Móc vào NotificationShadeWindowControllerImpl để bám sát trạng thái Bouncer (nhập mã PIN),
 * và WindowManagerGlobal để gán userActivityTimeout cho cửa sổ Shade/Keyguard.
 */
object LockscreenTimeoutHook {
    private val WINDOW_ROOT_VIEWS =
        setOf(
            "com.android.systemui.scene.ui.view.WindowRootView",
            "com.android.systemui.shade.NotificationShadeWindowView",
        )
    private const val LAYOUT_PARAMS = "android.view.WindowManager\$LayoutParams"
    private const val SHADE_WINDOW_CONTROLLER =
        "com.android.systemui.shade.NotificationShadeWindowControllerImpl"
    private const val LOG_TAG = "LumeLockscreenHook"

    private val errorLogged = AtomicBoolean(false)
    private val appliedLogged = AtomicBoolean(false)

    /**
     * Bouncer state được xếp vào stack để xử lý các lệnh apply lồng nhau.
     */
    private val bouncerDuringApply = ThreadLocal<ArrayDeque<Boolean>>()

    fun apply(param: LoadPackageParam) {
        val controller = findClassIfExists(SHADE_WINDOW_CONTROLLER, param.classLoader)
        val windowManagerGlobal =
            findClassIfExists("android.view.WindowManagerGlobal", param.classLoader)
        if (controller == null || windowManagerGlobal == null) {
            Log.w(LOG_TAG, "Không tìm thấy controller hoặc WindowManagerGlobal, bỏ qua hook")
            return
        }

        runCatching {
            hookAllMethods(
                controller,
                "apply",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(methodParam: MethodHookParam) {
                        val showing =
                            runCatching {
                                getBooleanField(methodParam.args.first(), "bouncerShowing")
                            }.getOrElse {
                                logOnce(it)
                                true
                            }
                        val stack =
                            bouncerDuringApply.get()
                                ?: ArrayDeque<Boolean>().also(bouncerDuringApply::set)
                        stack.addLast(showing)
                    }

                    override fun afterHookedMethod(methodParam: MethodHookParam) {
                        val stack = bouncerDuringApply.get() ?: return
                        if (stack.isNotEmpty()) stack.removeLast()
                        if (stack.isEmpty()) bouncerDuringApply.remove()
                    }
                },
            )
        }

        val windowHook =
            object : XC_MethodHook() {
                override fun beforeHookedMethod(methodParam: MethodHookParam) {
                    runCatching { applyTimeout(methodParam.args) }.onFailure(::logOnce)
                }
            }

        runCatching {
            hookAllMethods(windowManagerGlobal, "addView", windowHook)
            hookAllMethods(windowManagerGlobal, "updateViewLayout", windowHook)
            Log.i(LOG_TAG, "Đã kích hoạt LockscreenTimeoutHook trong SystemUI")
        }.onFailure {
            Log.e(LOG_TAG, "Lỗi khi hook WindowManagerGlobal: ${it.message}")
        }
    }

    private fun applyTimeout(args: Array<Any?>) {
        val timeoutMs = ToolkitHookConfig.getLockscreenTimeoutMs() ?: return
        val view = args.firstOrNull() as? View ?: return
        val layoutParams = args.firstOrNull { it?.javaClass?.name == LAYOUT_PARAMS } ?: return

        if (view.javaClass.name !in WINDOW_ROOT_VIEWS) {
            return
        }

        val original =
            runCatching { getLongField(layoutParams, "userActivityTimeout") }.getOrDefault(-1L)
        val bouncer = isBouncerShowing(view)
        if (!ScreenTimeoutPolicy.shouldExtendLockscreen(original, bouncer)) {
            return
        }

        setLongField(layoutParams, "userActivityTimeout", timeoutMs)
        setLongField(layoutParams, "screenDimDuration", 0L)
        if (appliedLogged.compareAndSet(false, true)) {
            XposedBridge.log("Lume: đã áp dụng timeout màn hình khóa ${timeoutMs}ms")
        }
    }

    private fun isBouncerShowing(view: View): Boolean {
        bouncerDuringApply.get()?.peekLast()?.let { return it }
        return runCatching { getBooleanField(view, "mBouncerShowing") }.getOrElse {
            logOnce(it)
            true
        }
    }

    private fun logOnce(error: Throwable) {
        if (errorLogged.compareAndSet(false, true)) {
            XposedBridge.log("Lume: lỗi khi áp dụng lockscreen timeout — ${error.message}")
        }
    }
}
