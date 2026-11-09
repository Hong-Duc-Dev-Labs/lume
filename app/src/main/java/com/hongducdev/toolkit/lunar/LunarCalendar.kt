package com.hongducdev.toolkit.lunar

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** Ngày âm lịch Việt Nam. */
data class LunarDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeapMonth: Boolean,
    val dayCanChi: String,
    val monthCanChi: String,
    val yearCanChi: String,
)

/** Thông tin Ngày Hoàng Đạo / Hắc Đạo */
data class AuspiciousDayInfo(
    val isAuspicious: Boolean,
    val starName: String,
    val typeName: String = if (isAuspicious) "Hoàng Đạo" else "Hắc Đạo",
)

/** Ngày trong lưới lịch tháng */
data class CalendarDay(
    val date: LocalDate,
    val lunarDate: LunarDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isWeekend: Boolean,
    val holiday: String?,
    val solarTerm: String,
    val auspiciousInfo: AuspiciousDayInfo,
)

/** Sự kiện / Ngày đặc biệt trong tháng */
data class NotableDay(
    val date: LocalDate,
    val title: String,
    val subtitle: String,
    val isLunar: Boolean,
)

/**
 * Chuyển ngày dương sang âm lịch Việt Nam (múi giờ UTC+7).
 *
 * Dựa trên thuật toán thiên văn của Hồ Ngọc Đức: tính thời điểm trăng mới
 * và kinh độ Mặt Trời để xác định tháng 11 và tháng nhuận.
 */
object LunarCalendar {
    const val TIME_ZONE_HOURS = 7.0

    val VIETNAM_OFFSET: ZoneOffset = ZoneOffset.ofHours(7)

    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI =
        arrayOf(
            "Tý",
            "Sửu",
            "Dần",
            "Mão",
            "Thìn",
            "Tỵ",
            "Ngọ",
            "Mùi",
            "Thân",
            "Dậu",
            "Tuất",
            "Hợi",
        )

    private val SOLAR_TERMS =
        arrayOf(
            "Xuân phân",
            "Thanh minh",
            "Cốc vũ",
            "Lập hạ",
            "Tiểu mãn",
            "Mang chủng",
            "Hạ chí",
            "Tiểu thử",
            "Đại thử",
            "Lập thu",
            "Xử thử",
            "Bạch lộ",
            "Thu phân",
            "Hàn lộ",
            "Sương giáng",
            "Lập đông",
            "Tiểu tuyết",
            "Đại tuyết",
            "Đông chí",
            "Tiểu hàn",
            "Đại hàn",
            "Lập xuân",
            "Vũ thủy",
            "Kinh trập",
        )

    private val STARS =
        arrayOf(
            "Thanh Long",
            "Minh Đường",
            "Thiên Hình",
            "Chu Tước",
            "Kim Quỹ",
            "Kim Đường",
            "Bạch Hổ",
            "Ngọc Đường",
            "Thiên Lao",
            "Huyền Vũ",
            "Tư Mệnh",
            "Câu Trận",
        )

    fun fromInstant(instant: Instant): LunarDate = fromSolar(instant.atOffset(VIETNAM_OFFSET).toLocalDate())

    fun fromSolar(date: LocalDate): LunarDate = fromSolar(date.dayOfMonth, date.monthValue, date.year)

    fun fromSolar(
        dd: Int,
        mm: Int,
        yy: Int,
    ): LunarDate {
        val dayNumber = julianDayFromDate(dd, mm, yy)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = newMoonDay(k + 1)
        if (monthStart > dayNumber) {
            monthStart = newMoonDay(k)
        }
        var a11 = lunarMonth11(yy)
        var b11 = a11
        val lunarYearBase: Int
        if (a11 >= monthStart) {
            lunarYearBase = yy
            a11 = lunarMonth11(yy - 1)
        } else {
            lunarYearBase = yy + 1
            b11 = lunarMonth11(yy + 1)
        }
        val lunarDay = dayNumber - monthStart + 1
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var isLeap = false
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = leapMonthOffset(a11)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    isLeap = true
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth -= 12
        }
        var lunarYear = lunarYearBase
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1
        }
        return LunarDate(
            day = lunarDay,
            month = lunarMonth,
            year = lunarYear,
            isLeapMonth = isLeap,
            dayCanChi = dayCanChi(dayNumber),
            monthCanChi = monthCanChi(lunarMonth, lunarYear),
            yearCanChi = yearCanChi(lunarYear),
        )
    }

    private fun yearCanChi(year: Int): String = "${CAN[Math.floorMod(year + 6, 10)]} ${CHI[Math.floorMod(year + 8, 12)]}"

    private fun monthCanChi(
        month: Int,
        year: Int,
    ): String {
        val can = CAN[Math.floorMod(year * 12 + month + 3, 10)]
        val chi = CHI[Math.floorMod(month + 1, 12)]
        return "$can $chi"
    }

    private fun dayCanChi(julianDay: Int): String = "${CAN[Math.floorMod(julianDay + 9, 10)]} ${CHI[Math.floorMod(julianDay + 1, 12)]}"

    private fun julianDayFromDate(
        dd: Int,
        mm: Int,
        yy: Int,
    ): Int {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        return dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    private fun newMoon(k: Int): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 += -0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 += 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 += -0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 += -0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 += 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        val deltaT =
            if (t < -11) {
                0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
            } else {
                -0.000278 + 0.000265 * t + 0.000262 * t2
            }
        return jd1 + c1 - deltaT
    }

    private fun sunLongitudeSector(jdn: Int): Int {
        val t = (jdn - 2451545.5 - TIME_ZONE_HOURS / 24) / 36525
        val t2 = t * t
        val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= PI * 2 * floor(l / (PI * 2))
        return floor(l / PI * 6).toInt()
    }

    private fun newMoonDay(k: Int): Int = floor(newMoon(k) + 0.5 + TIME_ZONE_HOURS / 24).toInt()

    private fun lunarMonth11(yy: Int): Int {
        val off = julianDayFromDate(31, 12, yy) - 2415021
        val k = floor(off / 29.530588853).toInt()
        var nm = newMoonDay(k)
        if (sunLongitudeSector(nm) >= 9) {
            nm = newMoonDay(k - 1)
        }
        return nm
    }

    private fun leapMonthOffset(a11: Int): Int {
        val k = floor((a11 - 2415021.076998695) / 29.530588853 + 0.5).toInt()
        var i = 1
        var arc = sunLongitudeSector(newMoonDay(k + i))
        var last: Int
        do {
            last = arc
            i++
            arc = sunLongitudeSector(newMoonDay(k + i))
        } while (arc != last && i < 14)
        return i - 1
    }

    /** Xác định tiết khí (24 tiết khí) theo kinh độ Mặt Trời */
    fun getSolarTerm(date: LocalDate): String {
        val jdn = julianDayFromDate(date.dayOfMonth, date.monthValue, date.year)
        val t = (jdn - 2451545.0) / 36525.0
        val t2 = t * t
        val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= PI * 2 * floor(l / (PI * 2))
        val sector = floor(l / (PI / 12)).toInt()
        return SOLAR_TERMS[Math.floorMod(sector, 24)]
    }

    /** Xác định Ngày Hoàng Đạo / Hắc Đạo theo 12 sao Thanh Long */
    fun getAuspiciousDayInfo(
        lunarMonth: Int,
        dayCanChi: String,
    ): AuspiciousDayInfo {
        val dayChi = dayCanChi.substringAfterLast(" ")
        val dayChiIndex = CHI.indexOf(dayChi).takeIf { it >= 0 } ?: 0
        val startChiIndex = (((lunarMonth - 1) % 6) * 2 + 2) % 12
        val starIndex = Math.floorMod(dayChiIndex - startChiIndex, 12)
        val isAuspicious = starIndex in setOf(0, 1, 4, 5, 7, 10)
        return AuspiciousDayInfo(
            isAuspicious = isAuspicious,
            starName = STARS[starIndex],
        )
    }

    /** Danh sách các Giờ Hoàng Đạo trong ngày */
    fun getAuspiciousHours(dayCanChi: String): List<String> {
        val chi = dayCanChi.substringAfterLast(" ")
        return when (chi) {
            "Tý", "Ngọ" -> listOf("Tý (23-1h)", "Sửu (1-3h)", "Mão (5-7h)", "Ngọ (11-13h)", "Thân (15-17h)", "Dậu (17-19h)")
            "Sửu", "Mùi" -> listOf("Dần (3-5h)", "Mão (5-7h)", "Tỵ (9-11h)", "Thân (15-17h)", "Tuất (19-21h)", "Hợi (21-23h)")
            "Dần", "Thân" -> listOf("Tý (23-1h)", "Sửu (1-3h)", "Thìn (7-9h)", "Tỵ (9-11h)", "Mùi (13-15h)", "Tuất (19-21h)")
            "Mão", "Dậu" -> listOf("Tý (23-1h)", "Dần (3-5h)", "Mão (5-7h)", "Ngọ (11-13h)", "Mùi (13-15h)", "Dậu (17-19h)")
            "Thìn", "Tuất" -> listOf("Dần (3-5h)", "Thìn (7-9h)", "Tỵ (9-11h)", "Thân (15-17h)", "Dậu (17-19h)", "Hợi (21-23h)")
            "Tỵ", "Hợi" -> listOf("Sửu (1-3h)", "Thìn (7-9h)", "Ngọ (11-13h)", "Mùi (13-15h)", "Tuất (19-21h)", "Hợi (21-23h)")
            else -> emptyList()
        }
    }

    /** Ngày lễ âm lịch hoặc dương lịch nếu có */
    fun getHoliday(
        solarDate: LocalDate,
        lunarDate: LunarDate,
    ): String? {
        if (!lunarDate.isLeapMonth) {
            when {
                lunarDate.month == 1 && lunarDate.day == 1 -> {
                    return "Mùng 1 Tết Nguyên Đán"
                }

                lunarDate.month == 1 && lunarDate.day == 2 -> {
                    return "Mùng 2 Tết"
                }

                lunarDate.month == 1 && lunarDate.day == 3 -> {
                    return "Mùng 3 Tết"
                }

                lunarDate.month == 1 && lunarDate.day == 15 -> {
                    return "Tết Nguyên Tiêu (Rằm tháng Giêng)"
                }

                lunarDate.month == 3 && lunarDate.day == 10 -> {
                    return "Giỗ Tổ Hùng Vương"
                }

                lunarDate.month == 4 && lunarDate.day == 15 -> {
                    return "Lễ Phật Đản"
                }

                lunarDate.month == 5 && lunarDate.day == 5 -> {
                    return "Tết Đoan Ngọ"
                }

                lunarDate.month == 7 && lunarDate.day == 15 -> {
                    return "Lễ Vu Lan (Báo hiếu)"
                }

                lunarDate.month == 8 && lunarDate.day == 15 -> {
                    return "Tết Trung Thu"
                }

                lunarDate.month == 9 && lunarDate.day == 9 -> {
                    return "Tết Trùng Cửu"
                }

                lunarDate.month == 10 && lunarDate.day == 10 -> {
                    return "Tết Trùng Thập"
                }

                lunarDate.month == 10 && lunarDate.day == 15 -> {
                    return "Tết Hạ Nguyên (Rằm tháng 10)"
                }

                lunarDate.month == 12 && lunarDate.day == 23 -> {
                    return "Ông Táo chầu trời"
                }

                lunarDate.month == 12 && lunarDate.day == 30 -> {
                    return "Đêm Giao Thừa (Tất Niên)"
                }

                lunarDate.month == 12 && lunarDate.day == 29 -> {
                    // Nếu tháng Chạp chỉ có 29 ngày thì ngày 29 là Tất Niên
                    val nextDayLunar = fromSolar(solarDate.plusDays(1))
                    if (nextDayLunar.day == 1 && nextDayLunar.month == 1) {
                        return "Đêm Giao Thừa (Tất Niên)"
                    }
                }
            }
        }
        return when {
            solarDate.monthValue == 1 && solarDate.dayOfMonth == 1 -> "Tết Dương Lịch"
            solarDate.monthValue == 2 && solarDate.dayOfMonth == 14 -> "Lễ Tình Nhân (Valentine)"
            solarDate.monthValue == 3 && solarDate.dayOfMonth == 8 -> "Quốc tế Phụ nữ"
            solarDate.monthValue == 4 && solarDate.dayOfMonth == 30 -> "Giải phóng Miền Nam"
            solarDate.monthValue == 5 && solarDate.dayOfMonth == 1 -> "Quốc tế Lao động"
            solarDate.monthValue == 6 && solarDate.dayOfMonth == 1 -> "Quốc tế Thiếu nhi"
            solarDate.monthValue == 9 && solarDate.dayOfMonth == 2 -> "Quốc khánh Việt Nam"
            solarDate.monthValue == 10 && solarDate.dayOfMonth == 20 -> "Phụ nữ Việt Nam"
            solarDate.monthValue == 11 && solarDate.dayOfMonth == 20 -> "Nhà giáo Việt Nam"
            solarDate.monthValue == 12 && solarDate.dayOfMonth == 24 -> "Lễ Giáng Sinh (Noel)"
            else -> null
        }
    }

    /** Tạo danh sách tất cả các ngày trong lưới lịch tháng (bao gồm cả các ngày đệm đầu/cuối tuần) */
    fun getMonthCalendarDays(
        yearMonth: YearMonth,
        today: LocalDate = LocalDate.now(VIETNAM_OFFSET),
    ): List<CalendarDay> {
        val firstDay = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()

        // Trong tuần Việt Nam, Thứ Hai là ngày đầu tuần (DayOfWeek.MONDAY = 1).
        val leadingDaysCount = firstDay.dayOfWeek.value - 1
        val result = mutableListOf<CalendarDay>()

        // Các ngày cuối của tháng trước
        for (i in leadingDaysCount downTo 1) {
            val d = firstDay.minusDays(i.toLong())
            result.add(createCalendarDay(d, isCurrentMonth = false, today = today))
        }

        // Các ngày của tháng hiện tại
        for (day in 1..daysInMonth) {
            val d = yearMonth.atDay(day)
            result.add(createCalendarDay(d, isCurrentMonth = true, today = today))
        }

        // Các ngày đầu của tháng sau để làm tròn hàng 7 ngày
        val trailingDaysCount = if (result.size % 7 != 0) 7 - (result.size % 7) else 0
        val lastDay = yearMonth.atDay(daysInMonth)
        for (i in 1..trailingDaysCount) {
            val d = lastDay.plusDays(i.toLong())
            result.add(createCalendarDay(d, isCurrentMonth = false, today = today))
        }

        return result
    }

    private fun createCalendarDay(
        date: LocalDate,
        isCurrentMonth: Boolean,
        today: LocalDate,
    ): CalendarDay {
        val lunar = fromSolar(date)
        val term = getSolarTerm(date)
        val auspicious = getAuspiciousDayInfo(lunar.month, lunar.dayCanChi)
        val holiday = getHoliday(date, lunar)
        val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        return CalendarDay(
            date = date,
            lunarDate = lunar,
            isCurrentMonth = isCurrentMonth,
            isToday = date == today,
            isWeekend = isWeekend,
            holiday = holiday,
            solarTerm = term,
            auspiciousInfo = auspicious,
        )
    }

    /** Tạo tóm tắt âm lịch cho tháng dương lịch (ví dụ: Tháng 10 ÂL (Ất Hợi) • Năm Bính Ngọ) */
    fun getMonthSummary(yearMonth: YearMonth): String {
        val midDay = yearMonth.atDay(15)
        val midLunar = fromSolar(midDay)
        val firstLunar = fromSolar(yearMonth.atDay(1))
        val lastLunar = fromSolar(yearMonth.atDay(yearMonth.lengthOfMonth()))

        return if (firstLunar.month == lastLunar.month) {
            "Tháng ${midLunar.month} ÂL (${midLunar.monthCanChi}) • Năm ${midLunar.yearCanChi}"
        } else {
            "Tháng ${firstLunar.month} - ${lastLunar.month} ÂL • Năm ${midLunar.yearCanChi}"
        }
    }

    /** Danh sách sự kiện & ngày đặc biệt trong tháng (Mùng 1, Rằm, ngày lễ) */
    fun getNotableDaysOfMonth(yearMonth: YearMonth): List<NotableDay> {
        val daysInMonth = yearMonth.lengthOfMonth()
        val list = mutableListOf<NotableDay>()

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val lunar = fromSolar(date)
            val holiday = getHoliday(date, lunar)

            if (holiday != null) {
                list.add(
                    NotableDay(
                        date = date,
                        title = holiday,
                        subtitle = "Ngày ${date.dayOfMonth}/${date.monthValue} DL • ${lunar.day}/${lunar.month} ÂL",
                        isLunar =
                            holiday.contains("Tết") || holiday.contains("Rằm") || holiday.contains("Vua") || holiday.contains("Vu Lan") ||
                                holiday.contains("Táo"),
                    ),
                )
            } else if (lunar.day == 1) {
                list.add(
                    NotableDay(
                        date = date,
                        title = "Mùng 1 tháng ${lunar.month} ÂL${if (lunar.isLeapMonth) " (nhuận)" else ""}",
                        subtitle = "Ngày ${date.dayOfMonth}/${date.monthValue} DL • Ngày Sóc (Trăng mới)",
                        isLunar = true,
                    ),
                )
            } else if (lunar.day == 15) {
                list.add(
                    NotableDay(
                        date = date,
                        title = "Rằm tháng ${lunar.month} ÂL${if (lunar.isLeapMonth) " (nhuận)" else ""}",
                        subtitle = "Ngày ${date.dayOfMonth}/${date.monthValue} DL • Ngày Vọng (Trăng tròn)",
                        isLunar = true,
                    ),
                )
            }
        }

        return list.sortedBy { it.date }
    }
}
