package com.hongducdev.toolkit

import com.hongducdev.toolkit.services.automation.executors.CombinedActionExecutor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSettingsSafetyTest {
    @Test
    fun acceptsPlainSetting() {
        assertTrue(CombinedActionExecutor.isSafeSetting("system", "screen_off_timeout", "30000"))
        assertTrue(CombinedActionExecutor.isSafeSetting("global", "zen_mode", "1"))
    }

    @Test
    fun rejectsShellInjection() {
        assertFalse(CombinedActionExecutor.isSafeSetting("system", "k", "1; reboot"))
        assertFalse(CombinedActionExecutor.isSafeSetting("system", "k && id", "1"))
        assertFalse(CombinedActionExecutor.isSafeSetting("system", "k", "\$(id)"))
    }

    @Test
    fun rejectsUnknownTableAndBlankKey() {
        assertFalse(CombinedActionExecutor.isSafeSetting("secure; id", "k", "1"))
        assertFalse(CombinedActionExecutor.isSafeSetting("system", "", "1"))
    }
}
