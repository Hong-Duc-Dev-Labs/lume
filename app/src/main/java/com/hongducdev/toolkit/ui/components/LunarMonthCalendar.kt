package com.hongducdev.toolkit.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hongducdev.toolkit.lunar.CalendarDay
import com.hongducdev.toolkit.lunar.LunarCalendar
import com.hongducdev.toolkit.lunar.NotableDay
import com.hongducdev.toolkit.ui.core.containers.RoundedCardContainer
import com.hongducdev.toolkit.ui.theme.EssentialsColorTokens
import com.hongducdev.toolkit.ui.theme.ExpressiveRadius
import com.hongducdev.toolkit.utils.HapticUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Trình xem lịch âm cả tháng (Vietnamese Lunar Monthly Calendar).
 * Thiết kế chuẩn Material 3 Expressive & sameerasw/essentials.
 */
@Composable
fun LunarMonthCalendar(
    modifier: Modifier = Modifier,
    onNavigateToLockscreen: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val today = remember { LocalDate.now(LunarCalendar.VIETNAM_OFFSET) }

    var currentYearMonth by rememberSaveable {
        mutableStateOf(YearMonth.of(today.year, today.monthValue))
    }
    var selectedDate by rememberSaveable {
        mutableStateOf(today)
    }
    var showMonthPicker by remember { mutableStateOf(false) }

    val calendarDays =
        remember(currentYearMonth) {
            LunarCalendar.getMonthCalendarDays(currentYearMonth, today)
        }
    val weeks =
        remember(calendarDays) {
            calendarDays.chunked(7)
        }
    val notableDays =
        remember(currentYearMonth) {
            LunarCalendar.getNotableDaysOfMonth(currentYearMonth)
        }
    val selectedDay =
        remember(selectedDate, currentYearMonth) {
            calendarDays.find { it.date == selectedDate }
                ?: run {
                    val lunar = LunarCalendar.fromSolar(selectedDate)
                    CalendarDay(
                        date = selectedDate,
                        lunarDate = lunar,
                        isCurrentMonth =
                            selectedDate.year == currentYearMonth.year && selectedDate.monthValue == currentYearMonth.monthValue,
                        isToday = selectedDate == today,
                        isWeekend = selectedDate.dayOfWeek == DayOfWeek.SATURDAY || selectedDate.dayOfWeek == DayOfWeek.SUNDAY,
                        holiday = LunarCalendar.getHoliday(selectedDate, lunar),
                        solarTerm = LunarCalendar.getSolarTerm(selectedDate),
                        auspiciousInfo = LunarCalendar.getAuspiciousDayInfo(lunar.month, lunar.dayCanChi),
                    )
                }
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Container chính: Bảng lịch cả tháng
        RoundedCardContainer(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header điều hướng tháng
                MonthNavigatorHeader(
                    currentYearMonth = currentYearMonth,
                    today = today,
                    selectedDate = selectedDate,
                    onPrevMonth = {
                        HapticUtil.performUIHaptic(view)
                        currentYearMonth = currentYearMonth.minusMonths(1)
                    },
                    onNextMonth = {
                        HapticUtil.performUIHaptic(view)
                        currentYearMonth = currentYearMonth.plusMonths(1)
                    },
                    onResetToday = {
                        HapticUtil.performUIHaptic(view)
                        currentYearMonth = YearMonth.of(today.year, today.monthValue)
                        selectedDate = today
                    },
                    onOpenPicker = {
                        HapticUtil.performUIHaptic(view)
                        showMonthPicker = true
                    },
                )

                // Hàng tên các thứ trong tuần (T2 .. CN)
                WeekdayHeaderRow()

                // Lưới ngày trong tháng (7 cột x 5-6 hàng) với animation chuyển tháng
                AnimatedContent(
                    targetState = currentYearMonth,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width / 2 } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width / 2 } + fadeOut(),
                            )
                        } else {
                            (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width / 2 } + fadeOut(),
                            )
                        }
                    },
                    label = "month_transition",
                ) { ym ->
                    val ymDays = remember(ym) { LunarCalendar.getMonthCalendarDays(ym, today) }
                    val ymWeeks = remember(ymDays) { ymDays.chunked(7) }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ymWeeks.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                week.forEach { day ->
                                    DayCell(
                                        day = day,
                                        isSelected = day.date == selectedDate,
                                        onClick = {
                                            HapticUtil.performUIHaptic(view)
                                            selectedDate = day.date
                                            if (!day.isCurrentMonth) {
                                                currentYearMonth = YearMonth.of(day.date.year, day.date.monthValue)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chi tiết ngày được chọn
        SelectedDayDetailCard(day = selectedDay)

        // Các sự kiện & ngày đặc biệt trong tháng
        if (notableDays.isNotEmpty()) {
            NotableDaysSection(
                notableDays = notableDays,
                selectedDate = selectedDate,
                onSelectDay = { date ->
                    HapticUtil.performUIHaptic(view)
                    selectedDate = date
                },
            )
        }

        // Shortcut chuyển sang Cài đặt Màn hình khóa
        if (onNavigateToLockscreen != null) {
            Surface(
                onClick = onNavigateToLockscreen,
                shape = RoundedCornerShape(ExpressiveRadius.Card),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .background(EssentialsColorTokens.PastelBlue, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = EssentialsColorTokens.VibrantBlue,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Cài đặt màn hình khóa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Tùy biến hiển thị âm lịch trên hình nền & dòng ngày",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }

    // Dialog chọn năm và tháng nhanh
    if (showMonthPicker) {
        MonthYearPickerDialog(
            initialYear = currentYearMonth.year,
            initialMonth = currentYearMonth.monthValue,
            onDismiss = { showMonthPicker = false },
            onConfirm = { year, month ->
                currentYearMonth = YearMonth.of(year, month)
                showMonthPicker = false
            },
        )
    }
}

/** Header điều hướng tháng */
@Composable
private fun MonthNavigatorHeader(
    currentYearMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetToday: () -> Unit,
    onOpenPicker: () -> Unit,
) {
    val isCurrent = currentYearMonth.year == today.year && currentYearMonth.monthValue == today.monthValue

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = onPrevMonth,
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Tháng trước",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(ExpressiveRadius.Squircle))
                        .clickable(onClick = onOpenPicker)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Tháng ${currentYearMonth.monthValue}, ${currentYearMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = "Chọn nhanh tháng năm",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = LunarCalendar.getMonthSummary(currentYearMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "Tháng sau",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Nút trở về Hôm nay nếu đang xem tháng khác hoặc ngày chọn khác hôm nay
        AnimatedVisibility(
            visible = !isCurrent || selectedDate != today,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = onResetToday,
                    shape = RoundedCornerShape(ExpressiveRadius.Pill),
                    colors =
                        ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        Icons.Rounded.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Về hôm nay (${today.dayOfMonth}/${today.monthValue})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Hàng tiêu đề thứ trong tuần (T2 - CN) */
@Composable
private fun WeekdayHeaderRow() {
    val days =
        listOf(
            "T2" to MaterialTheme.colorScheme.onSurfaceVariant,
            "T3" to MaterialTheme.colorScheme.onSurfaceVariant,
            "T4" to MaterialTheme.colorScheme.onSurfaceVariant,
            "T5" to MaterialTheme.colorScheme.onSurfaceVariant,
            "T6" to MaterialTheme.colorScheme.onSurfaceVariant,
            "T7" to EssentialsColorTokens.VibrantBlue,
            "CN" to EssentialsColorTokens.VibrantRose,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        days.forEach { (name, color) ->
            Text(
                text = name,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

/** Ô một ngày trong lưới lịch */
@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            day.isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else -> Color.Transparent
        }

    val borderModifier =
        when {
            isSelected -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            day.isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            else -> Modifier
        }

    val solarColor =
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
            day.date.dayOfWeek == DayOfWeek.SUNDAY -> EssentialsColorTokens.VibrantRose
            day.date.dayOfWeek == DayOfWeek.SATURDAY -> EssentialsColorTokens.VibrantBlue
            else -> MaterialTheme.colorScheme.onSurface
        }

    val lunarColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primary
            !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
            day.lunarDate.day == 1 -> EssentialsColorTokens.VibrantRose
            day.lunarDate.day == 15 -> EssentialsColorTokens.VibrantOrange
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Box(
        modifier =
            modifier
                .padding(horizontal = 2.dp, vertical = 3.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .then(borderModifier)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Ngày dương lịch
            Text(
                text = "${day.date.dayOfMonth}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.SemiBold,
                color = solarColor,
                lineHeight = 18.sp,
            )

            // Ngày âm lịch
            val lunarText =
                if (day.lunarDate.day == 1) {
                    "${day.lunarDate.day}/${day.lunarDate.month}${if (day.lunarDate.isLeapMonth) "N" else ""}"
                } else {
                    "${day.lunarDate.day}"
                }
            Text(
                text = lunarText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = if (day.lunarDate.day == 1 || day.lunarDate.day == 15) FontWeight.Bold else FontWeight.Normal,
                color = lunarColor,
                lineHeight = 12.sp,
            )

            // Dấu chấm chỉ báo (Ngày lễ / Mùng 1 / Rằm)
            Spacer(Modifier.height(2.dp))
            val dotColor =
                when {
                    day.holiday != null -> EssentialsColorTokens.VibrantOrange
                    day.lunarDate.day == 1 -> EssentialsColorTokens.VibrantRose
                    day.lunarDate.day == 15 -> EssentialsColorTokens.VibrantBlue
                    else -> null
                }
            if (dotColor != null) {
                Box(
                    modifier =
                        Modifier
                            .size(4.dp)
                            .background(dotColor, CircleShape),
                )
            } else {
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}

/** Thẻ hiển thị chi tiết ngày được chọn */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedDayDetailCard(day: CalendarDay) {
    val lunar = day.lunarDate
    val auspicious = day.auspiciousInfo

    RoundedCardContainer(title = "Chi tiết ngày đã chọn") {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ExpressiveRadius.Card),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Hàng 1: Dương lịch & Badge hôm nay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "${formatDayOfWeek(
                                day.date.dayOfWeek,
                            )}, ngày ${day.date.dayOfMonth} tháng ${day.date.monthValue} năm ${day.date.year}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Dương lịch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (day.isToday) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(ExpressiveRadius.Pill),
                        ) {
                            Text(
                                text = "Hôm nay",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Hàng 2: Âm lịch nổi bật
                Surface(
                    shape = RoundedCornerShape(ExpressiveRadius.Squircle),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Ngày ${lunar.day} tháng ${lunar.month}${if (lunar.isLeapMonth) " (nhuận)" else ""} năm ${lunar.yearCanChi}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Âm lịch Việt Nam (UTC+7)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Biểu tượng trăng non / trăng tròn
                        when (lunar.day) {
                            1 -> {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(36.dp)
                                            .background(EssentialsColorTokens.PastelRose, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.DarkMode,
                                        contentDescription = "Mùng 1 - Trăng mới",
                                        tint = EssentialsColorTokens.VibrantRose,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            15 -> {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(36.dp)
                                            .background(EssentialsColorTokens.PastelOrange, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.WbSunny,
                                        contentDescription = "Ngày Rằm - Trăng tròn",
                                        tint = EssentialsColorTokens.VibrantOrange,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            else -> {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(36.dp)
                                            .background(EssentialsColorTokens.PastelBlue, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.Event,
                                        contentDescription = null,
                                        tint = EssentialsColorTokens.VibrantBlue,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Hàng 3: Các tag thông tin đặc biệt (Hoàng đạo, Tiết khí, Ngày lễ)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Tag Hoàng đạo / Hắc đạo
                    InfoChip(
                        title = "${auspicious.typeName} (${auspicious.starName})",
                        icon = Icons.Rounded.Star,
                        bgColor = if (auspicious.isAuspicious) EssentialsColorTokens.PastelGreen else MaterialTheme.colorScheme.surfaceContainerHighest,
                        textColor = if (auspicious.isAuspicious) EssentialsColorTokens.VibrantGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Tag Tiết khí
                    InfoChip(
                        title = "Tiết ${day.solarTerm}",
                        icon = Icons.Rounded.WbSunny,
                        bgColor = EssentialsColorTokens.PastelBlue,
                        textColor = EssentialsColorTokens.VibrantBlue,
                    )

                    // Tag Ngày lễ nếu có
                    if (day.holiday != null) {
                        InfoChip(
                            title = day.holiday,
                            icon = Icons.Rounded.Celebration,
                            bgColor = EssentialsColorTokens.PastelOrange,
                            textColor = EssentialsColorTokens.VibrantOrange,
                        )
                    }

                    if (lunar.day == 1) {
                        InfoChip(
                            title = "Mùng 1 (Sóc)",
                            icon = Icons.Rounded.DarkMode,
                            bgColor = EssentialsColorTokens.PastelRose,
                            textColor = EssentialsColorTokens.VibrantRose,
                        )
                    } else if (lunar.day == 15) {
                        InfoChip(
                            title = "Ngày Rằm (Vọng)",
                            icon = Icons.Rounded.WbSunny,
                            bgColor = EssentialsColorTokens.PastelOrange,
                            textColor = EssentialsColorTokens.VibrantOrange,
                        )
                    }
                }

                // Hàng 4: Bảng 3 thẻ Can Chi (Ngày, Tháng, Năm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CanChiCard(
                        label = "NGÀY",
                        value = lunar.dayCanChi,
                        modifier = Modifier.weight(1f),
                    )
                    CanChiCard(
                        label = "THÁNG",
                        value = lunar.monthCanChi,
                        modifier = Modifier.weight(1f),
                    )
                    CanChiCard(
                        label = "NĂM",
                        value = lunar.yearCanChi,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Hàng 5: Giờ Hoàng Đạo
                val auspiciousHours =
                    remember(lunar.dayCanChi) {
                        LunarCalendar.getAuspiciousHours(lunar.dayCanChi)
                    }
                if (auspiciousHours.isNotEmpty()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ExpressiveRadius.Squircle))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Giờ Hoàng Đạo trong ngày:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = auspiciousHours.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Thẻ hiển thị Can Chi nhỏ */
@Composable
private fun CanChiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(ExpressiveRadius.Squircle))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Chip thông tin nhỏ có icon */
@Composable
private fun InfoChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    textColor: Color,
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(ExpressiveRadius.Pill),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
        }
    }
}

/** Mục danh sách ngày đặc biệt trong tháng */
@Composable
private fun NotableDaysSection(
    notableDays: List<NotableDay>,
    selectedDate: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
) {
    RoundedCardContainer(title = "Sự kiện & Ngày đặc biệt trong tháng") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            notableDays.forEach { item ->
                val isSelected = item.date == selectedDate
                Surface(
                    onClick = { onSelectDay(item.date) },
                    shape = RoundedCornerShape(ExpressiveRadius.Squircle),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(32.dp)
                                        .background(
                                            if (item.isLunar) EssentialsColorTokens.PastelOrange else EssentialsColorTokens.PastelBlue,
                                            CircleShape,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (item.isLunar) Icons.Rounded.DarkMode else Icons.Rounded.Event,
                                    contentDescription = null,
                                    tint = if (item.isLunar) EssentialsColorTokens.VibrantOrange else EssentialsColorTokens.VibrantBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.8f,
                                            )
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            }
                        }

                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Dialog chọn năm & tháng nhanh */
@Composable
private fun MonthYearPickerDialog(
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    val view = LocalView.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chọn tháng & năm",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Bộ chọn năm (< 2026 >)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            selectedYear--
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = "Năm trước",
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Text(
                        text = "Năm $selectedYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    IconButton(
                        onClick = {
                            HapticUtil.performUIHaptic(view)
                            selectedYear++
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = "Năm sau",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Lưới 12 tháng (4 hàng x 3 cột)
                val months = (1..12).toList().chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    months.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { m ->
                                val isSelected = m == selectedMonth
                                Surface(
                                    onClick = {
                                        HapticUtil.performUIHaptic(view)
                                        selectedMonth = m
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(ExpressiveRadius.Button),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Text(
                                        text = "Tháng $m",
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedYear, selectedMonth) },
                shape = RoundedCornerShape(ExpressiveRadius.Pill),
            ) {
                Text("Chọn", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(ExpressiveRadius.Pill),
            ) {
                Text("Hủy")
            }
        },
        shape = RoundedCornerShape(ExpressiveRadius.Card),
    )
}

private fun formatDayOfWeek(dayOfWeek: DayOfWeek): String =
    when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Thứ Hai"
        DayOfWeek.TUESDAY -> "Thứ Ba"
        DayOfWeek.WEDNESDAY -> "Thứ Tư"
        DayOfWeek.THURSDAY -> "Thứ Năm"
        DayOfWeek.FRIDAY -> "Thứ Sáu"
        DayOfWeek.SATURDAY -> "Thứ Bảy"
        DayOfWeek.SUNDAY -> "Chủ Nhật"
    }
