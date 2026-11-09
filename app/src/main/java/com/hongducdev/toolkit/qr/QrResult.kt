package com.hongducdev.toolkit.qr

import android.net.Uri

enum class QrType(
    val label: String,
) {
    URL("Trang web"),
    WIFI("Mạng Wi-Fi"),
    PHONE("Số điện thoại"),
    SMS("Tin nhắn SMS"),
    EMAIL("Thư điện tử"),
    GEO("Tọa độ vị trí"),
    TEXT("Văn bản"),
}

sealed interface QrParsedData {
    data class Url(
        val url: String,
        val host: String,
    ) : QrParsedData

    data class Wifi(
        val ssid: String,
        val password: String = "",
        val security: String = "WPA",
        val isHidden: Boolean = false,
    ) : QrParsedData

    data class Phone(
        val number: String,
    ) : QrParsedData

    data class Sms(
        val number: String,
        val message: String = "",
    ) : QrParsedData

    data class Email(
        val email: String,
        val subject: String = "",
        val body: String = "",
    ) : QrParsedData

    data class Geo(
        val latitude: Double,
        val longitude: Double,
        val query: String? = null,
    ) : QrParsedData

    data class Text(
        val content: String,
    ) : QrParsedData
}

data class QrResult(
    val rawText: String,
    val type: QrType,
    val data: QrParsedData,
    val timestamp: Long = System.currentTimeMillis(),
)
