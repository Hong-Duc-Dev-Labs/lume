package labs.hongducdev.lume.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarRoutingTest {

    @Test
    fun destinationItem_formatsCorrectly() {
        val dest = DestinationItem(
            name = "Sân bay Quốc tế Nội Bài",
            subtitle = "Xã Phú Minh, Sóc Sơn, Hà Nội",
            latitude = 21.2189,
            longitude = 105.8045
        )
        assertEquals("Sân bay Quốc tế Nội Bài", dest.name)
        assertEquals("Xã Phú Minh, Sóc Sơn, Hà Nội", dest.subtitle)
        assertEquals(21.2189, dest.latitude, 0.0001)
        assertEquals(105.8045, dest.longitude, 0.0001)
    }

    @Test
    fun routeInfo_distanceAndDurationFormatting_isAccurate() {
        // Tuyến dài 25.8 km, 28 phút (1669.2s = 27.82m -> làm tròn 28 phút)
        val route = RouteInfo(
            destinationName = "Sân bay Nội Bài",
            destinationLat = 21.2189,
            destinationLng = 105.8045,
            distanceMeters = 25755.8,
            durationSeconds = 1669.2,
            geoJson = "{\"type\":\"LineString\",\"coordinates\":[]}"
        )

        assertEquals("25.8 km", route.distanceDisplay)
        assertEquals("28 phút", route.durationDisplay)

        // Tuyến ngắn < 1km, 1 phút (60s)
        val shortRoute = RouteInfo(
            destinationName = "Cây xăng Petrolimex",
            destinationLat = 21.0300,
            destinationLng = 105.8550,
            distanceMeters = 450.0,
            durationSeconds = 60.0,
            geoJson = "{\"type\":\"LineString\",\"coordinates\":[]}"
        )
        assertEquals("450 m", shortRoute.distanceDisplay)
        assertEquals("1 phút", shortRoute.durationDisplay)

        // Tuyến dài liên tỉnh > 1 giờ
        val longRoute = RouteInfo(
            destinationName = "Hạ Long, Quảng Ninh",
            destinationLat = 20.9500,
            destinationLng = 107.0800,
            distanceMeters = 155000.0,
            durationSeconds = 9300.0, // 155 phút = 2 giờ 35 phút
            geoJson = "{\"type\":\"LineString\",\"coordinates\":[]}"
        )
        assertEquals("155.0 km", longRoute.distanceDisplay)
        assertEquals("2 giờ 35 phút", longRoute.durationDisplay)

        // Tuyến tròn 1 giờ (3600s) -> hiển thị "1 giờ" thay vì "1 giờ 0 phút"
        val exactHourRoute = RouteInfo(
            destinationName = "Bắc Ninh",
            destinationLat = 21.18,
            destinationLng = 106.07,
            distanceMeters = 35000.0,
            durationSeconds = 3600.0,
            geoJson = "{}"
        )
        assertEquals("1 giờ", exactHourRoute.durationDisplay)

        // Tuyến 0s hoặc điểm tại chỗ
        val zeroRoute = RouteInfo(
            destinationName = "Điểm hiện tại",
            destinationLat = 21.0,
            destinationLng = 105.8,
            distanceMeters = 0.0,
            durationSeconds = 0.0,
            geoJson = "{}"
        )
        assertEquals("0 m", zeroRoute.distanceDisplay)
        assertEquals("0 phút", zeroRoute.durationDisplay)
    }

    @Test
    fun quickSearchChips_areAvailableAndMeaningful() {
        assertTrue(CarRoutingManager.QUICK_SEARCH_CHIPS.isNotEmpty())
        assertTrue(CarRoutingManager.QUICK_SEARCH_CHIPS.contains("Cây xăng"))
        assertTrue(CarRoutingManager.QUICK_SEARCH_CHIPS.contains("Bãi đỗ xe"))
    }
}
