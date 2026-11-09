package labs.hongducdev.lume.xposed

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.hookAllMethods
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.findClassIfExists
import de.robv.android.xposed.XposedHelpers.getBooleanField
import de.robv.android.xposed.XposedHelpers.getLongField
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hook system_server: Điều chỉnh thời gian chờ tắt màn hình trong PowerManagerService.
 *
 * Xử lý cả 2 trường hợp:
 * 1. Lockscreen: Nhận diện tín hiệu override từ WindowManager và áp dụng thời gian sáng màn hình khoá.
 * 2. Unlocked screen: Kéo dài thời gian sáng màn hình chính khi đang mở khoá theo mốc cấu hình hoặc không giới hạn.
 */
object ScreenTimeoutHook {
    private const val POWER_MANAGER_SERVICE = "com.android.server.power.PowerManagerService"
    private const val LOG_TAG = "LumeScreenTimeoutHook"

    private val errorLogged = AtomicBoolean(false)

    fun apply(param: LoadPackageParam) {
        val service =
            findClassIfExists(POWER_MANAGER_SERVICE, param.classLoader) ?: run {
                Log.w(LOG_TAG, "Không tìm thấy PowerManagerService, bỏ qua hook")
                return
            }

        runCatching {
            hookAllMethods(
                service,
                "getScreenOffTimeoutLocked",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(methodParam: MethodHookParam) {
                        runCatching {
                            extend(methodParam)
                        }.onFailure(::logOnce)
                    }
                },
            )
            Log.i(LOG_TAG, "Đã kích hoạt ScreenTimeoutHook trong system_server")
        }.onFailure {
            Log.e(LOG_TAG, "Lỗi khi hook getScreenOffTimeoutLocked: ${it.message}")
        }
    }

    private fun extend(methodParam: XC_MethodHook.MethodHookParam) {
        val service = methodParam.thisObject

        if (booleanFieldOrFalse(service, "mIsDualViewMode")) return

        val lockscreenMs = ToolkitHookConfig.getLockscreenTimeoutMs()
        val screenTimeoutMs = ToolkitHookConfig.getScreenTimeoutMs()

        if (lockscreenMs == null && screenTimeoutMs == null) return

        val windowManagerOverride =
            runCatching {
                getLongField(service, "mUserActivityTimeoutOverrideFromWindowManager")
            }.getOrDefault(-1L)

        val desired =
            when {
                ScreenTimeoutPolicy.isLockscreenOverride(windowManagerOverride, lockscreenMs) -> {
                    lockscreenMs
                }

                ScreenTimeoutPolicy.shouldExtendUnlockedScreen(
                    configuredTimeoutMs = screenTimeoutMs,
                    keyguardShowingAndNotOccluded = isKeyguardUp(service),
                ) -> {
                    screenTimeoutMs
                }

                else -> {
                    null
                }
            } ?: return

        methodParam.result =
            ScreenTimeoutPolicy.resolveExtendedTimeout(
                desiredTimeoutMs = desired,
                attentiveTimeoutMs = methodParam.args.getOrNull(1) as? Long ?: -1L,
                deviceAdminEnforced =
                    runCatching {
                        callMethod(service, "isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked") == true
                    }.getOrDefault(false),
                adminMaximumMs =
                    runCatching {
                        getLongField(service, "mMaximumScreenOffTimeoutFromDeviceAdmin")
                    }.getOrDefault(-1L),
                minimumScreenOffTimeoutMs =
                    runCatching {
                        getLongField(service, "mMinimumScreenOffTimeoutConfig")
                    }.getOrDefault(0L),
            )
    }

    /** Trả về true (an toàn) nếu không đọc được trạng thái keyguard. */
    private fun isKeyguardUp(service: Any): Boolean {
        val policy = getObjectField(service, "mPolicy") ?: return true
        return callMethod(policy, "isKeyguardShowingAndNotOccluded") == true
    }

    private fun booleanFieldOrFalse(
        instance: Any,
        name: String,
    ): Boolean = runCatching { getBooleanField(instance, name) }.getOrDefault(false)

    private fun logOnce(error: Throwable) {
        if (errorLogged.compareAndSet(false, true)) {
            XposedBridge.log("Lume: lỗi khi điều chỉnh screen timeout — ${error.message}")
        }
    }
}
