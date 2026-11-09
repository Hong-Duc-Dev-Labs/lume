package labs.hongducdev.lume.data

/**
 * Các mốc thời gian sáng màn hình khoá và màn hình mở khoá.
 *
 * Tách riêng để cả tầng UI lẫn các hook Xposed trong tiến trình hệ thống
 * đều có thể sử dụng mà không phụ thuộc lẫn nhau.
 */
object ScreenTimeoutPresets {
    /** 10 năm - Giá trị vượt quá vòng đời thiết bị, dùng làm giá trị 'Không giới hạn / Luôn sáng'. */
    const val NEVER_MS = 10L * 365L * 24L * 60L * 60L * 1_000L

    val PRESET_MILLIS =
        longArrayOf(
            0L, // 0: Mặc định hệ thống
            15_000L, // 1: 15 giây
            30_000L, // 2: 30 giây
            60_000L, // 3: 1 phút
            120_000L, // 4: 2 phút
            300_000L, // 5: 5 phút
            600_000L, // 6: 10 phút
            1_800_000L, // 7: 30 phút
            3_600_000L, // 8: 1 giờ
            NEVER_MS, // 9: Không giới hạn
        )

    val PRESET_LABELS =
        listOf(
            "Mặc định hệ thống",
            "15 giây",
            "30 giây",
            "1 phút",
            "2 phút",
            "5 phút",
            "10 phút",
            "30 phút",
            "1 giờ",
            "Không giới hạn (Luôn sáng)",
        )

    val count: Int get() = PRESET_MILLIS.size

    /** Trả về null nếu chọn mốc 0 (Mặc định) hoặc index nằm ngoài phạm vi. */
    fun millisForIndex(index: Int): Long? = PRESET_MILLIS.getOrNull(index)?.takeIf { it > 0L }

    fun labelForIndex(index: Int): String = PRESET_LABELS.getOrElse(index) { PRESET_LABELS[0] }
}
