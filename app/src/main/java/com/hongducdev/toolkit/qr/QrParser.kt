package com.hongducdev.toolkit.qr

import android.net.Uri
import com.google.mlkit.vision.barcode.common.Barcode

object QrParser {
    fun fromBarcode(barcode: Barcode): QrResult {
        val raw = barcode.rawValue.orEmpty()
        return when (barcode.valueType) {
            Barcode.TYPE_URL -> {
                val url = barcode.url?.url ?: raw
                val host = Uri.parse(url).host.orEmpty()
                QrResult(raw, QrType.URL, QrParsedData.Url(url = url, host = host))
            }

            Barcode.TYPE_WIFI -> {
                val wifi = barcode.wifi
                if (wifi != null) {
                    val sec =
                        when (wifi.encryptionType) {
                            Barcode.WiFi.TYPE_OPEN -> "OPEN"
                            Barcode.WiFi.TYPE_WEP -> "WEP"
                            Barcode.WiFi.TYPE_WPA -> "WPA"
                            else -> "WPA"
                        }
                    QrResult(
                        raw,
                        QrType.WIFI,
                        QrParsedData.Wifi(
                            ssid = wifi.ssid.orEmpty(),
                            password = wifi.password.orEmpty(),
                            security = sec,
                        ),
                    )
                } else {
                    parse(raw)
                }
            }

            Barcode.TYPE_PHONE -> {
                val phone = barcode.phone?.number ?: raw.removePrefix("tel:").removePrefix("TEL:")
                QrResult(raw, QrType.PHONE, QrParsedData.Phone(number = phone))
            }

            Barcode.TYPE_SMS -> {
                val sms = barcode.sms
                if (sms != null) {
                    QrResult(
                        raw,
                        QrType.SMS,
                        QrParsedData.Sms(
                            number = sms.phoneNumber.orEmpty(),
                            message = sms.message.orEmpty(),
                        ),
                    )
                } else {
                    parse(raw)
                }
            }

            Barcode.TYPE_EMAIL -> {
                val email = barcode.email
                if (email != null) {
                    QrResult(
                        raw,
                        QrType.EMAIL,
                        QrParsedData.Email(
                            email = email.address.orEmpty(),
                            subject = email.subject.orEmpty(),
                            body = email.body.orEmpty(),
                        ),
                    )
                } else {
                    parse(raw)
                }
            }

            Barcode.TYPE_GEO -> {
                val geo = barcode.geoPoint
                if (geo != null) {
                    QrResult(
                        raw,
                        QrType.GEO,
                        QrParsedData.Geo(
                            latitude = geo.lat,
                            longitude = geo.lng,
                        ),
                    )
                } else {
                    parse(raw)
                }
            }

            else -> {
                parse(raw)
            }
        }
    }

    fun parse(raw: String): QrResult {
        val trimmed = raw.trim()

        // 1. Wi-Fi: WIFI:S:MySSID;T:WPA;P:MyPassword;H:false;;
        if (trimmed.startsWith("WIFI:", ignoreCase = true)) {
            val wifiData = parseWifi(trimmed)
            return QrResult(raw, QrType.WIFI, wifiData)
        }

        // 2. URL
        if (isUrl(trimmed)) {
            val validUrl = if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
            val host = extractHost(validUrl)
            return QrResult(raw, QrType.URL, QrParsedData.Url(url = validUrl, host = host))
        }

        // 3. Phone
        if (trimmed.startsWith("tel:", ignoreCase = true)) {
            val number = trimmed.substring(4)
            return QrResult(raw, QrType.PHONE, QrParsedData.Phone(number = number))
        }

        // 4. SMS: smsto:+84912345678:Hello or sms:+84912345678?body=Hello
        if (trimmed.startsWith("smsto:", ignoreCase = true) || trimmed.startsWith("sms:", ignoreCase = true)) {
            val smsData = parseSms(trimmed)
            return QrResult(raw, QrType.SMS, smsData)
        }

        // 5. Email: mailto:test@example.com?subject=Hello&body=World
        if (trimmed.startsWith("mailto:", ignoreCase = true)) {
            val emailData = parseEmail(trimmed)
            return QrResult(raw, QrType.EMAIL, emailData)
        }

        // 6. Geo: geo:10.762622,106.660172 or geo:0,0?q=10.762622,106.660172
        if (trimmed.startsWith("geo:", ignoreCase = true)) {
            val geoData = parseGeo(trimmed)
            if (geoData != null) {
                return QrResult(raw, QrType.GEO, geoData)
            }
        }

        // 7. Pure phone number (e.g. +84912345678 or 0912345678)
        if (isPhoneNumber(trimmed)) {
            return QrResult(raw, QrType.PHONE, QrParsedData.Phone(number = trimmed))
        }

        // 8. Pure email
        if (isEmail(trimmed)) {
            return QrResult(raw, QrType.EMAIL, QrParsedData.Email(email = trimmed))
        }

        // Fallback: Plain text
        return QrResult(raw, QrType.TEXT, QrParsedData.Text(content = raw))
    }

    private fun parseWifi(raw: String): QrParsedData.Wifi {
        // Strip WIFI: prefix and trailing ;;
        val content = raw.substring(5).trimEnd(';')
        var ssid = ""
        var password = ""
        var security = "WPA"
        var isHidden = false

        // Tokenize by semicolon, taking care of escaped characters
        val tokens = splitEscaped(content, ';')
        for (token in tokens) {
            val colonIdx = token.indexOf(':')
            if (colonIdx > 0) {
                val key = token.substring(0, colonIdx).uppercase()
                val value = unescapeWifi(token.substring(colonIdx + 1))
                when (key) {
                    "S" -> ssid = value
                    "P" -> password = value
                    "T" -> security = if (value.equals("nopass", ignoreCase = true)) "OPEN" else value.uppercase()
                    "H" -> isHidden = value.equals("true", ignoreCase = true)
                }
            }
        }
        return QrParsedData.Wifi(
            ssid = ssid,
            password = password,
            security = security,
            isHidden = isHidden,
        )
    }

    private fun parseSms(raw: String): QrParsedData.Sms {
        if (raw.startsWith("smsto:", ignoreCase = true)) {
            val content = raw.substring(6)
            val colonIdx = content.indexOf(':')
            return if (colonIdx >= 0) {
                val phone = content.substring(0, colonIdx)
                val body = content.substring(colonIdx + 1)
                QrParsedData.Sms(number = phone, message = body)
            } else {
                QrParsedData.Sms(number = content, message = "")
            }
        }
        val after = raw.removePrefix("sms:").removePrefix("SMS:")
        val phone = after.substringBefore('?')
        val query = if (after.contains('?')) after.substringAfter('?') else ""
        var body = ""
        if (query.isNotEmpty()) {
            for (param in query.split('&')) {
                val idx = param.indexOf('=')
                if (idx > 0) {
                    val k = param.substring(0, idx)
                    val v =
                        try {
                            java.net.URLDecoder.decode(param.substring(idx + 1), "UTF-8")
                        } catch (_: Exception) {
                            param.substring(idx + 1)
                        }
                    if (k.equals("body", ignoreCase = true)) body = v
                }
            }
        }
        return QrParsedData.Sms(number = phone, message = body)
    }

    private fun parseEmail(raw: String): QrParsedData.Email {
        val after = raw.removePrefix("mailto:").removePrefix("MAILTO:")
        val email = after.substringBefore('?')
        val query = if (after.contains('?')) after.substringAfter('?') else ""
        var subject = ""
        var body = ""
        if (query.isNotEmpty()) {
            for (param in query.split('&')) {
                val idx = param.indexOf('=')
                if (idx > 0) {
                    val k = param.substring(0, idx)
                    val v =
                        try {
                            java.net.URLDecoder.decode(param.substring(idx + 1), "UTF-8")
                        } catch (_: Exception) {
                            param.substring(idx + 1)
                        }
                    if (k.equals("subject", ignoreCase = true)) subject = v
                    if (k.equals("body", ignoreCase = true)) body = v
                }
            }
        }
        return QrParsedData.Email(email = email, subject = subject, body = body)
    }

    private fun parseGeo(raw: String): QrParsedData.Geo? {
        val content = raw.substring(4) // after geo:
        val mainPart = content.substringBefore('?')
        val query = if (content.contains('?')) content.substringAfter('?') else null

        val coords = mainPart.split(',')
        if (coords.size >= 2) {
            val lat = coords[0].toDoubleOrNull()
            val lng = coords[1].toDoubleOrNull()
            if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
                return QrParsedData.Geo(latitude = lat, longitude = lng, query = query)
            }
        }

        // If geo:0,0?q=lat,lng or geo:0,0?q=lat,lng(label)
        if (query != null && query.startsWith("q=")) {
            val qValue = query.substring(2).substringBefore('(')
            val qCoords = qValue.split(',')
            if (qCoords.size >= 2) {
                val qLat = qCoords[0].toDoubleOrNull()
                val qLng = qCoords[1].toDoubleOrNull()
                if (qLat != null && qLng != null) {
                    return QrParsedData.Geo(latitude = qLat, longitude = qLng, query = query)
                }
            }
        }
        return null
    }

    private fun isUrl(text: String): Boolean {
        if (text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true)) {
            return true
        }
        if (text.startsWith("www.", ignoreCase = true) && text.contains('.')) {
            return true
        }
        return false
    }

    private fun extractHost(url: String): String =
        try {
            val withoutScheme = if (url.contains("://")) url.substringAfter("://") else url
            withoutScheme
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
                .substringBefore(':')
        } catch (_: Exception) {
            ""
        }

    private fun isPhoneNumber(text: String): Boolean {
        val clean = text.replace(" ", "").replace("-", "")
        if (clean.length in 7..15 && clean.matches(Regex("""^\+?[0-9]+$"""))) {
            return true
        }
        return false
    }

    private fun isEmail(text: String): Boolean = text.matches(Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"""))

    private fun splitEscaped(
        text: String,
        delimiter: Char,
    ): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var escaped = false
        for (char in text) {
            if (escaped) {
                sb.append(char)
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == delimiter) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(char)
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }
        return result
    }

    private fun unescapeWifi(value: String): String =
        value
            .replace("\\;", ";")
            .replace("\\:", ":")
            .replace("\\\\", "\\")
            .replace("\\,", ",")
}
