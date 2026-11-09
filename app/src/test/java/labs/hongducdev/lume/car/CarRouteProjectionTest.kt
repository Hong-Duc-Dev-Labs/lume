package labs.hongducdev.lume.car

import labs.hongducdev.lume.car.auto.CarRouteProjection
import labs.hongducdev.lume.car.auto.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarRouteProjectionTest {

    private val points = listOf(
        GeoPoint(21.00, 105.80),
        GeoPoint(21.10, 105.90),
        GeoPoint(21.05, 105.85)
    )

    @Test
    fun project_keepsAllPointsInsideRect() {
        val projection = CarRouteProjection(points, 100f, 50f, 400f, 300f)
        points.forEach { p ->
            assertTrue(projection.x(p) in 100f..500f)
            assertTrue(projection.y(p) in 50f..350f)
        }
    }

    @Test
    fun project_northIsUp() {
        val projection = CarRouteProjection(points, 0f, 0f, 400f, 300f)
        assertTrue(projection.y(GeoPoint(21.10, 105.85)) < projection.y(GeoPoint(21.00, 105.85)))
    }

    @Test
    fun project_singlePointDoesNotCrash() {
        val projection = CarRouteProjection(listOf(GeoPoint(21.0, 105.8)), 0f, 0f, 200f, 200f)
        assertEquals(projection.x(GeoPoint(21.0, 105.8)), projection.x(GeoPoint(21.0, 105.8)), 0f)
    }

    @Test
    fun project_emptyPointsDoesNotCrash() {
        val projection = CarRouteProjection(emptyList(), 0f, 0f, 200f, 200f)
        val dummy = GeoPoint(0.0, 0.0)
        assertTrue(projection.x(dummy) >= 0f)
        assertTrue(projection.y(dummy) >= 0f)
    }
}
