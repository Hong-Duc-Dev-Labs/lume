package labs.hongducdev.lume.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class CarLocationStateTest {

    @Test
    fun defaultLocationState_hasValidDefaults() {
        val state = CarLocationState()
        assertEquals(21.0285, state.latitude, 0.001)
        assertEquals(105.8542, state.longitude, 0.001)
        assertEquals(0, state.speedKmh)
        assertEquals(0f, state.bearing, 0.001f)
        assertEquals(0f, state.accuracyMeters, 0.001f)
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
    fun destinationItem_includesDistance() {
        val item = DestinationItem(
            name = "Petrolimex 12",
            subtitle = "1.7 km • Đường Cách Mạng Tháng 8",
            latitude = 21.585,
            longitude = 105.801,
            distanceMeters = 1700
        )
        assertEquals(1700, item.distanceMeters)
    }
}
