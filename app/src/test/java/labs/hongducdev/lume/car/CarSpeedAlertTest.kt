package labs.hongducdev.lume.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarSpeedAlertTest {

    @Test
    fun defaultAlertState_hasValidDefaults() {
        val state = CarSpeedAlertState()
        assertEquals(60, state.speedLimit)
        assertFalse(state.isOverSpeed)
        assertTrue(state.isVoiceAlertEnabled)
        assertNotNull(state.approachingSign)
        assertEquals(SignType.SPEED_CAMERA, state.approachingSign?.type)
    }

    @Test
    fun overSpeedDetection_triggersWhenSpeedExceedsLimit() {
        val limit = 60
        val underSpeed = 55
        val overSpeed = 65

        assertFalse(underSpeed > limit)
        assertTrue(overSpeed > limit)
    }

    @Test
    fun roadSignModel_containsCorrectDetails() {
        val cameraSign = RoadSign(
            type = SignType.SPEED_CAMERA,
            title = "Camera phạt nguội",
            speedLimit = 60,
            distanceMeters = 350
        )
        assertEquals(SignType.SPEED_CAMERA, cameraSign.type)
        assertEquals(60, cameraSign.speedLimit)
        assertEquals(350, cameraSign.distanceMeters)

        val residentialSign = RoadSign(
            type = SignType.RESIDENTIAL_AREA,
            title = "Khu đông dân cư",
            speedLimit = 50,
            distanceMeters = 200
        )
        assertEquals(SignType.RESIDENTIAL_AREA, residentialSign.type)
        assertEquals(50, residentialSign.speedLimit)
    }
}
