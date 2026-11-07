package com.hongducdev.toolkit.lunar

/**
 * Thay các token dạng `{{ten}}` trong mẫu do người dùng nhập bằng giá trị lịch âm.
 * Token không có trong danh sách được giữ nguyên chữ.
 */
object LunarFormatter {
    const val TOKEN_DAY = "ngay_am"
    const val TOKEN_MONTH = "thang_am"
    const val TOKEN_YEAR = "nam_am"
    const val TOKEN_LEAP = "nhuan"
    const val TOKEN_DAY_CAN_CHI = "can_chi_ngay"
    const val TOKEN_MONTH_CAN_CHI = "can_chi_thang"
    const val TOKEN_YEAR_CAN_CHI = "can_chi_nam"

    val supportedTokens: List<String> = listOf(
        TOKEN_DAY,
        TOKEN_MONTH,
        TOKEN_YEAR,
        TOKEN_LEAP,
        TOKEN_DAY_CAN_CHI,
        TOKEN_MONTH_CAN_CHI,
        TOKEN_YEAR_CAN_CHI,
    )

    data class Preset(val name: String, val template: String)

    val presets: List<Preset> = listOf(
        Preset("Ngắn gọn", "{{ngay_am}}/{{thang_am}} ÂL"),
        Preset("Đầy đủ", "{{ngay_am}} tháng {{thang_am}} {{nhuan}} năm {{can_chi_nam}}"),
        Preset("Can chi ngày", "{{ngay_am}}/{{thang_am}} - ngày {{can_chi_ngay}}"),
    )

    const val DEFAULT_TEMPLATE = "{{ngay_am}}/{{thang_am}} ÂL"

    private val tokenPattern = Regex("""\{\{\s*([A-Za-z_]+)\s*\}\}""")

    fun format(template: String, date: LunarDate): String =
        tokenPattern.replace(template) { match ->
            valueOf(match.groupValues[1], date) ?: match.value
        }.trim()

    private fun valueOf(token: String, date: LunarDate): String? = when (token) {
        TOKEN_DAY -> date.day.toString()
        TOKEN_MONTH -> date.month.toString()
        TOKEN_YEAR -> date.year.toString()
        TOKEN_LEAP -> if (date.isLeapMonth) "nhuận" else ""
        TOKEN_DAY_CAN_CHI -> date.dayCanChi
        TOKEN_MONTH_CAN_CHI -> date.monthCanChi
        TOKEN_YEAR_CAN_CHI -> date.yearCanChi
        else -> null
    }
}
