package com.hongducdev.toolkit.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class CarDashboardStateTest {

    @Test
    fun defaultLocationState_hasValidDefaults() {
        val state = CarLocationState()
        assertEquals(21.0285, state.latitude, 0.001)
        assertEquals(105.8542, state.longitude, 0.001)
        assertEquals(0, state.speedKmh)
        assertEquals(0f, state.bearing, 0.001f)
        assertFalse(state.hasGpsFix)
        assertFalse(state.isPermissionGranted)
    }

    @Test
    fun speedConversion_mpsToKmh_isCorrect() {
        val speedMps = 16.67f // ~60 km/h
        val speedKmh = (speedMps * 3.6f).roundToInt()
        assertEquals(60, speedKmh)

        val speedMps100 = 27.78f // ~100 km/h
        val speedKmh100 = (speedMps100 * 3.6f).roundToInt()
        assertEquals(100, speedKmh100)
    }

    @Test
    fun defaultMediaState_isNotPlayingAndUnavailable() {
        val media = CarMediaState()
        assertFalse(media.isAvailable)
        assertFalse(media.isPlaying)
        assertEquals("", media.title)
        assertEquals("", media.artist)
    }

    @Test
    fun mediaState_updatesCorrectly() {
        val media = CarMediaState(
            isAvailable = true,
            isPlaying = true,
            title = "See You Again",
            artist = "Wiz Khalifa",
            album = "Furious 7",
            appName = "Spotify",
            isNotificationListenerEnabled = true
        )
        assertTrue(media.isAvailable)
        assertTrue(media.isPlaying)
        assertEquals("See You Again", media.title)
        assertEquals("Wiz Khalifa", media.artist)
        assertEquals("Spotify", media.appName)
        assertTrue(media.isNotificationListenerEnabled)
    }
}
