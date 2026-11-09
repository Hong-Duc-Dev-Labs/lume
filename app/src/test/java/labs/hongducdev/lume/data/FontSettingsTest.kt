package labs.hongducdev.lume.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FontSettingsTest {
    @Test
    fun defaultSettings_hasSensibleDefaults() {
        val settings = FontSettings()
        assertFalse(settings.isEnabled)
        assertEquals("", settings.fontName)
        assertEquals(0L, settings.fontVersion)
        assertEquals(0L, settings.fileSize)
    }

    @Test
    fun customSettings_preservesAllProperties() {
        val timestamp = 1715000000000L
        val size = 2 * 1024 * 1024L // 2MB
        val settings =
            FontSettings(
                isEnabled = true,
                fontName = "BeVietnamPro-Regular.ttf",
                fontVersion = timestamp,
                fileSize = size,
            )

        assertTrue(settings.isEnabled)
        assertEquals("BeVietnamPro-Regular.ttf", settings.fontName)
        assertEquals(timestamp, settings.fontVersion)
        assertEquals(size, settings.fileSize)
    }

    @Test
    fun fontFileName_isStandardized() {
        assertEquals("custom_system_font.ttf", FontSettingsRepository.FONT_FILE_NAME)
    }
}
