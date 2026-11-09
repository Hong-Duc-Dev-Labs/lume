package labs.hongducdev.lume.data

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarDisplayPolicyTest {

    private val boot = 1_000_000L
    private val hour = TimeUnit.HOURS.toMillis(1)

    @Test
    fun hookNeverSeenIsNotActive() {
        assertFalse(LunarDisplayPolicy.isHookActive(0L, boot, boot + hour))
    }

    @Test
    fun hookSeenBeforeBootIsNotActive() {
        assertFalse(LunarDisplayPolicy.isHookActive(boot - 1, boot, boot + hour))
    }

    @Test
    fun hookSeenAfterBootIsActive() {
        assertTrue(LunarDisplayPolicy.isHookActive(boot + hour, boot, boot + 2 * hour))
    }

    @Test
    fun hookSeenLongAgoIsNotActive() {
        val seen = boot + hour
        assertFalse(LunarDisplayPolicy.isHookActive(seen, boot, seen + TimeUnit.HOURS.toMillis(37)))
    }

    @Test
    fun autoModeSwitchesWallpaperOffWhenHookActive() {
        assertTrue(LunarDisplayPolicy.wallpaperShouldShow(DisplayMode.AUTO, hookActive = false))
        assertFalse(LunarDisplayPolicy.wallpaperShouldShow(DisplayMode.AUTO, hookActive = true))
    }

    @Test
    fun fixedModesIgnoreHookState() {
        assertTrue(LunarDisplayPolicy.wallpaperShouldShow(DisplayMode.WALLPAPER_ONLY, hookActive = true))
        assertFalse(LunarDisplayPolicy.wallpaperShouldShow(DisplayMode.DATE_LINE_ONLY, hookActive = false))
    }

    @Test
    fun hookIsSkippedOnlyInWallpaperOnlyMode() {
        assertTrue(LunarDisplayPolicy.hookShouldShow(DisplayMode.AUTO))
        assertTrue(LunarDisplayPolicy.hookShouldShow(DisplayMode.DATE_LINE_ONLY))
        assertFalse(LunarDisplayPolicy.hookShouldShow(DisplayMode.WALLPAPER_ONLY))
    }

    @Test
    fun unknownModeNameFallsBackToAuto() {
        assertEquals(DisplayMode.AUTO, DisplayMode.fromName("khong_co"))
        assertEquals(DisplayMode.AUTO, DisplayMode.fromName(null))
        assertEquals(DisplayMode.DATE_LINE_ONLY, DisplayMode.fromName("DATE_LINE_ONLY"))
    }
}
