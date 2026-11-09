package labs.hongducdev.lume.xposed

import labs.hongducdev.lume.data.ScreenTimeoutPresets
import kotlin.math.max
import kotlin.math.min

/**
 * Logic tính toán thời gian chờ tắt màn hình cho cả Lockscreen và Screen mở khoá.
 * Độc lập với Android framework để có thể test 100% bằng Unit Test.
 */
object ScreenTimeoutPolicy {
    const val UNLIMITED_TIMEOUT_MS = ScreenTimeoutPresets.NEVER_MS

    /**
     * Kiểm tra xem WindowManager có đang yêu cầu đúng giá trị timeout của lockscreen không.
     * Đây là tín hiệu cầu nối giữa SystemUI và system_server.
     */
    fun isLockscreenOverride(
        windowManagerOverrideMs: Long,
        configuredTimeoutMs: Long?,
    ): Boolean =
        configuredTimeoutMs != null &&
            configuredTimeoutMs > 0L &&
            windowManagerOverrideMs == configuredTimeoutMs

    fun shouldExtendUnlockedScreen(
        configuredTimeoutMs: Long?,
        keyguardShowingAndNotOccluded: Boolean,
    ): Boolean =
        configuredTimeoutMs != null &&
            configuredTimeoutMs > 0L &&
            !keyguardShowingAndNotOccluded

    fun shouldExtendLockscreen(
        originalTimeoutMs: Long,
        bouncerShowing: Boolean,
    ): Boolean = originalTimeoutMs >= 0L && !bouncerShowing

    /**
     * Giới hạn timeout yêu cầu với các ràng buộc bảo mật / hệ thống hợp lệ
     * (chính sách quản trị thiết bị Admin, attentive timeout, minimumScreenOffTimeoutConfig).
     */
    fun resolveExtendedTimeout(
        desiredTimeoutMs: Long,
        attentiveTimeoutMs: Long,
        deviceAdminEnforced: Boolean,
        adminMaximumMs: Long,
        minimumScreenOffTimeoutMs: Long,
    ): Long {
        val attentiveClamped =
            if (attentiveTimeoutMs >= 0L) min(desiredTimeoutMs, attentiveTimeoutMs) else desiredTimeoutMs
        val adminClamped =
            if (deviceAdminEnforced && adminMaximumMs in 0 until Long.MAX_VALUE) {
                min(attentiveClamped, adminMaximumMs)
            } else {
                attentiveClamped
            }
        return max(adminClamped, minimumScreenOffTimeoutMs)
    }
}
