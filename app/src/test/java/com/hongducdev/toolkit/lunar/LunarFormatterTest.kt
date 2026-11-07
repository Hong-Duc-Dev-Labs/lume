package com.hongducdev.toolkit.lunar

import org.junit.Assert.assertEquals
import org.junit.Test

class LunarFormatterTest {

    private val sample = LunarDate(
        day = 5,
        month = 7,
        year = 2025,
        isLeapMonth = false,
        dayCanChi = "Giáp Tý",
        monthCanChi = "Canh Thân",
        yearCanChi = "Ất Tỵ",
    )

    private val leapSample = sample.copy(isLeapMonth = true)

    @Test
    fun replacesEachToken() {
        assertEquals("5", LunarFormatter.format("{{ngay_am}}", sample))
        assertEquals("7", LunarFormatter.format("{{thang_am}}", sample))
        assertEquals("2025", LunarFormatter.format("{{nam_am}}", sample))
        assertEquals("Giáp Tý", LunarFormatter.format("{{can_chi_ngay}}", sample))
        assertEquals("Canh Thân", LunarFormatter.format("{{can_chi_thang}}", sample))
        assertEquals("Ất Tỵ", LunarFormatter.format("{{can_chi_nam}}", sample))
    }

    @Test
    fun leapTokenDependsOnLeapFlag() {
        assertEquals("", LunarFormatter.format("{{nhuan}}", sample))
        assertEquals("nhuận", LunarFormatter.format("{{nhuan}}", leapSample))
    }

    @Test
    fun keepsPlainTextAndUnknownTokens() {
        assertEquals(
            "Hôm nay 5/7 {{khong_co}} ÂL",
            LunarFormatter.format("Hôm nay {{ngay_am}}/{{thang_am}} {{khong_co}} ÂL", sample),
        )
    }

    @Test
    fun toleratesSpacesInsideBraces() {
        assertEquals("5/7", LunarFormatter.format("{{ ngay_am }}/{{thang_am }}", sample))
    }

    @Test
    fun emptyTemplateGivesEmptyText() {
        assertEquals("", LunarFormatter.format("", sample))
    }

    @Test
    fun trimsTrailingSpaceWhenLeapIsEmpty() {
        assertEquals("tháng 7", LunarFormatter.format("tháng {{thang_am}} {{nhuan}}", sample))
        assertEquals("tháng 7 nhuận", LunarFormatter.format("tháng {{thang_am}} {{nhuan}}", leapSample))
    }

    @Test
    fun presetsOnlyUseSupportedTokens() {
        val known = LunarFormatter.supportedTokens.toSet()
        val pattern = Regex("""\{\{\s*([A-Za-z_]+)\s*\}\}""")
        LunarFormatter.presets.forEach { preset ->
            pattern.findAll(preset.template).forEach { match ->
                assertEquals("token trong mẫu '${preset.name}'", true, match.groupValues[1] in known)
            }
        }
    }
}
