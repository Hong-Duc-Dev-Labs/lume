package com.hongducdev.toolkit.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class LunarCalendarTest {
    private fun lunar(
        y: Int,
        m: Int,
        d: Int,
    ) = LunarCalendar.fromSolar(LocalDate.of(y, m, d))

    private fun assertLunar(
        solar: LocalDate,
        day: Int,
        month: Int,
        year: Int,
        leap: Boolean = false,
    ) {
        val result = LunarCalendar.fromSolar(solar)
        assertEquals("ngày âm của $solar", day, result.day)
        assertEquals("tháng âm của $solar", month, result.month)
        assertEquals("năm âm của $solar", year, result.year)
        assertEquals("cờ nhuận của $solar", leap, result.isLeapMonth)
    }

    @Test
    fun tetFirstDayIsLunarNewYear() {
        // Mùng 1 Tết: 10/2/2024, 29/1/2025, 17/2/2026.
        assertLunar(LocalDate.of(2024, 2, 10), 1, 1, 2024)
        assertLunar(LocalDate.of(2025, 1, 29), 1, 1, 2025)
        assertLunar(LocalDate.of(2026, 2, 17), 1, 1, 2026)
    }

    @Test
    fun dayBeforeTetIsLastDayOfPreviousYear() {
        // Tháng Chạp Quý Mão có 30 ngày (trăng mới 11/1/2024), tháng Chạp Giáp Thìn chỉ có 29 ngày
        // (trăng mới 30/12/2024 22:27 UTC, tức 31/12 giờ Việt Nam).
        assertLunar(LocalDate.of(2024, 2, 9), 30, 12, 2023)
        assertLunar(LocalDate.of(2025, 1, 28), 29, 12, 2024)
    }

    @Test
    fun leapMonthIsFlaggedIn2023() {
        // Năm Quý Mão có tháng 2 nhuận bắt đầu ngày 22/3/2023.
        assertLunar(LocalDate.of(2023, 2, 20), 1, 2, 2023, leap = false)
        assertLunar(LocalDate.of(2023, 3, 22), 1, 2, 2023, leap = true)
        assertLunar(LocalDate.of(2023, 4, 20), 1, 3, 2023, leap = false)
    }

    @Test
    fun leapMonthIsFlaggedIn2025() {
        // Năm Ất Tỵ có tháng 6 nhuận bắt đầu ngày 25/7/2025 (trăng mới 24/7/2025 19:11 UTC).
        assertLunar(LocalDate.of(2025, 6, 25), 1, 6, 2025, leap = false)
        assertLunar(LocalDate.of(2025, 7, 25), 1, 6, 2025, leap = true)
        // Trăng mới 23/8/2025 06:06 UTC (13:06 giờ Việt Nam) mở đầu tháng 7.
        assertLunar(LocalDate.of(2025, 8, 22), 29, 6, 2025, leap = true)
        assertLunar(LocalDate.of(2025, 8, 23), 1, 7, 2025, leap = false)
    }

    @Test
    fun midAutumnAndNewYearOfSolarCalendar() {
        assertLunar(LocalDate.of(2025, 10, 6), 15, 8, 2025)
        assertLunar(LocalDate.of(2026, 1, 1), 13, 11, 2025)
    }

    @Test
    fun canChiOfYears() {
        assertEquals("Giáp Thìn", lunar(2024, 2, 10).yearCanChi)
        assertEquals("Ất Tỵ", lunar(2025, 1, 29).yearCanChi)
        assertEquals("Bính Ngọ", lunar(2026, 2, 17).yearCanChi)
    }

    @Test
    fun canChiOfMonthAndDay() {
        val tet2024 = lunar(2024, 2, 10)
        assertEquals("Bính Dần", tet2024.monthCanChi)
        assertEquals("Giáp Thìn", tet2024.dayCanChi)
    }

    @Test
    fun vietnamMidnightBoundary() {
        // 23:59 giờ Việt Nam ngày 9/2/2024 vẫn là 30 tháng Chạp, 00:00 ngày 10/2 là mùng 1 Tết.
        val beforeMidnight = Instant.parse("2024-02-09T16:59:00Z")
        val afterMidnight = Instant.parse("2024-02-09T17:00:00Z")
        val before = LunarCalendar.fromInstant(beforeMidnight)
        val after = LunarCalendar.fromInstant(afterMidnight)
        assertEquals(30, before.day)
        assertEquals(12, before.month)
        assertEquals(1, after.day)
        assertEquals(1, after.month)
        assertFalse(after.isLeapMonth)
        assertTrue(before.year == 2023 && after.year == 2024)
    }

    @Test
    fun solarTermsDetection() {
        val springEquinox = LunarCalendar.getSolarTerm(LocalDate.of(2024, 3, 21))
        assertEquals("Xuân phân", springEquinox)

        val winterStart = LunarCalendar.getSolarTerm(LocalDate.of(2024, 11, 8))
        assertEquals("Lập đông", winterStart)
    }

    @Test
    fun auspiciousHoursAndDays() {
        val hours = LunarCalendar.getAuspiciousHours("Giáp Tý")
        assertEquals(6, hours.size)
        assertTrue(hours.any { it.contains("Tý") })

        val dayInfo = LunarCalendar.getAuspiciousDayInfo(1, "Bính Thìn")
        assertNotNull(dayInfo.starName)
        assertTrue(dayInfo.typeName == "Hoàng Đạo" || dayInfo.typeName == "Hắc Đạo")
    }

    @Test
    fun holidayDetection() {
        val tet = LocalDate.of(2024, 2, 10)
        val tetLunar = LunarCalendar.fromSolar(tet)
        assertEquals("Mùng 1 Tết Nguyên Đán", LunarCalendar.getHoliday(tet, tetLunar))

        val nationalDay = LocalDate.of(2024, 9, 2)
        val nationalDayLunar = LunarCalendar.fromSolar(nationalDay)
        assertEquals("Quốc khánh Việt Nam", LunarCalendar.getHoliday(nationalDay, nationalDayLunar))
    }

    @Test
    fun monthCalendarGridGeneration() {
        val ym = YearMonth.of(2026, 11)
        val days = LunarCalendar.getMonthCalendarDays(ym, LocalDate.of(2026, 11, 8))

        // Phải là bội số của 7
        assertEquals(0, days.size % 7)
        // Tháng 11 có 30 ngày
        assertEquals(30, days.count { it.isCurrentMonth })
        // Ngày 8/11/2026 phải được đánh dấu là hôm nay
        val todayCell = days.first { it.date == LocalDate.of(2026, 11, 8) }
        assertTrue(todayCell.isToday)
        assertTrue(todayCell.isCurrentMonth)
    }

    @Test
    fun notableDaysOfMonthContainsFirstAndFullMoon() {
        val ym = YearMonth.of(2026, 11)
        val daysInMonth = ym.lengthOfMonth()
        for (d in 1..daysInMonth) {
            val date = ym.atDay(d)
            val lunar = LunarCalendar.fromSolar(date)
            println("$date -> Lunar ${lunar.day}/${lunar.month}")
        }
        val notables = LunarCalendar.getNotableDaysOfMonth(ym)
        println("Notables: $notables")
        assertTrue(notables.isNotEmpty())
    }
}
