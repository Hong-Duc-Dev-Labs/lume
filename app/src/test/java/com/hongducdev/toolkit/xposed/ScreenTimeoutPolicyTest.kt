package com.hongducdev.toolkit.xposed

import com.hongducdev.toolkit.data.ScreenTimeoutPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeoutPolicyTest {
    @Test
    fun presetCountMatchesLabels() {
        assertEquals(10, ScreenTimeoutPresets.count)
        assertEquals(ScreenTimeoutPresets.count, ScreenTimeoutPresets.PRESET_LABELS.size)
    }

    @Test
    fun defaultPresetReturnsNull() {
        assertNull(ScreenTimeoutPresets.millisForIndex(0))
        assertEquals("Mặc định hệ thống", ScreenTimeoutPresets.labelForIndex(0))
    }

    @Test
    fun positivePresetsReturnExpectedMillis() {
        assertEquals(15_000L, ScreenTimeoutPresets.millisForIndex(1))
        assertEquals(30_000L, ScreenTimeoutPresets.millisForIndex(2))
        assertEquals(60_000L, ScreenTimeoutPresets.millisForIndex(3))
        assertEquals(120_000L, ScreenTimeoutPresets.millisForIndex(4))
        assertEquals(300_000L, ScreenTimeoutPresets.millisForIndex(5))
        assertEquals(600_000L, ScreenTimeoutPresets.millisForIndex(6))
        assertEquals(1_800_000L, ScreenTimeoutPresets.millisForIndex(7))
        assertEquals(3_600_000L, ScreenTimeoutPresets.millisForIndex(8))
        assertEquals(ScreenTimeoutPresets.NEVER_MS, ScreenTimeoutPresets.millisForIndex(9))
    }

    @Test
    fun outOfRangeReturnsNull() {
        assertNull(ScreenTimeoutPresets.millisForIndex(-1))
        assertNull(ScreenTimeoutPresets.millisForIndex(10))
        assertNull(ScreenTimeoutPresets.millisForIndex(100))
    }

    @Test
    fun isLockscreenOverrideMatchesWhenExpected() {
        assertTrue(ScreenTimeoutPolicy.isLockscreenOverride(60_000L, 60_000L))
        assertFalse(ScreenTimeoutPolicy.isLockscreenOverride(30_000L, 60_000L))
        assertFalse(ScreenTimeoutPolicy.isLockscreenOverride(0L, 0L))
        assertFalse(ScreenTimeoutPolicy.isLockscreenOverride(60_000L, null))
    }

    @Test
    fun shouldExtendUnlockedScreenOnlyWhenUnlockedAndConfigured() {
        assertTrue(
            ScreenTimeoutPolicy.shouldExtendUnlockedScreen(
                configuredTimeoutMs = 60_000L,
                keyguardShowingAndNotOccluded = false,
            ),
        )
        assertFalse(
            ScreenTimeoutPolicy.shouldExtendUnlockedScreen(
                configuredTimeoutMs = 60_000L,
                keyguardShowingAndNotOccluded = true,
            ),
        )
        assertFalse(
            ScreenTimeoutPolicy.shouldExtendUnlockedScreen(
                configuredTimeoutMs = null,
                keyguardShowingAndNotOccluded = false,
            ),
        )
        assertFalse(
            ScreenTimeoutPolicy.shouldExtendUnlockedScreen(
                configuredTimeoutMs = 0L,
                keyguardShowingAndNotOccluded = false,
            ),
        )
    }

    @Test
    fun shouldExtendLockscreenOnlyWhenVisibleAndNoBouncer() {
        assertTrue(ScreenTimeoutPolicy.shouldExtendLockscreen(originalTimeoutMs = 10_000L, bouncerShowing = false))
        assertFalse(ScreenTimeoutPolicy.shouldExtendLockscreen(originalTimeoutMs = 10_000L, bouncerShowing = true))
        assertFalse(ScreenTimeoutPolicy.shouldExtendLockscreen(originalTimeoutMs = -1L, bouncerShowing = false))
    }

    @Test
    fun resolveExtendedTimeoutClampsAttentiveAndAdminLimits() {
        val resolved =
            ScreenTimeoutPolicy.resolveExtendedTimeout(
                desiredTimeoutMs = ScreenTimeoutPolicy.UNLIMITED_TIMEOUT_MS,
                attentiveTimeoutMs = 7_200_000L,
                deviceAdminEnforced = true,
                adminMaximumMs = 3_600_000L,
                minimumScreenOffTimeoutMs = 10_000L,
            )
        assertEquals(3_600_000L, resolved)
    }

    @Test
    fun resolveExtendedTimeoutRespectsMinimumScreenOff() {
        val resolved =
            ScreenTimeoutPolicy.resolveExtendedTimeout(
                desiredTimeoutMs = 5_000L,
                attentiveTimeoutMs = -1L,
                deviceAdminEnforced = false,
                adminMaximumMs = -1L,
                minimumScreenOffTimeoutMs = 10_000L,
            )
        assertEquals(10_000L, resolved)
    }
}
