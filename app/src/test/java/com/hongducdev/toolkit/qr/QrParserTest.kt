package com.hongducdev.toolkit.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrParserTest {
    @Test
    fun parseWifiStandard() {
        val raw = "WIFI:T:WPA;S:HomeNetwork;P:pass1234;H:false;;"
        val result = QrParser.parse(raw)

        assertEquals(QrType.WIFI, result.type)
        val data = result.data as QrParsedData.Wifi
        assertEquals("HomeNetwork", data.ssid)
        assertEquals("pass1234", data.password)
        assertEquals("WPA", data.security)
        assertFalse(data.isHidden)
    }

    @Test
    fun parseWifiOpenAndEscaped() {
        val raw = "WIFI:T:nopass;S:Cafe\\;Special;P:;H:true;;"
        val result = QrParser.parse(raw)

        assertEquals(QrType.WIFI, result.type)
        val data = result.data as QrParsedData.Wifi
        assertEquals("Cafe;Special", data.ssid)
        assertEquals("", data.password)
        assertEquals("OPEN", data.security)
        assertTrue(data.isHidden)
    }

    @Test
    fun parseUrlHttpsAndWww() {
        val result1 = QrParser.parse("https://github.com/hongducdev/toolkit")
        assertEquals(QrType.URL, result1.type)
        val data1 = result1.data as QrParsedData.Url
        assertEquals("https://github.com/hongducdev/toolkit", data1.url)
        assertEquals("github.com", data1.host)

        val result2 = QrParser.parse("www.google.com/search?q=test")
        assertEquals(QrType.URL, result2.type)
        val data2 = result2.data as QrParsedData.Url
        assertEquals("https://www.google.com/search?q=test", data2.url)
        assertEquals("www.google.com", data2.host)
    }

    @Test
    fun parsePhoneFormats() {
        val result1 = QrParser.parse("tel:+84912345678")
        assertEquals(QrType.PHONE, result1.type)
        val data1 = result1.data as QrParsedData.Phone
        assertEquals("+84912345678", data1.number)

        val result2 = QrParser.parse("0912345678")
        assertEquals(QrType.PHONE, result2.type)
        val data2 = result2.data as QrParsedData.Phone
        assertEquals("0912345678", data2.number)
    }

    @Test
    fun parseSmsSmsto() {
        val raw = "smsto:+84912345678:Xin chao cac ban"
        val result = QrParser.parse(raw)

        assertEquals(QrType.SMS, result.type)
        val data = result.data as QrParsedData.Sms
        assertEquals("+84912345678", data.number)
        assertEquals("Xin chao cac ban", data.message)
    }

    @Test
    fun parseEmailFormats() {
        val raw = "mailto:dev@example.com?subject=Support&body=BugReport"
        val result = QrParser.parse(raw)

        assertEquals(QrType.EMAIL, result.type)
        val data = result.data as QrParsedData.Email
        assertEquals("dev@example.com", data.email)
        assertEquals("Support", data.subject)
        assertEquals("BugReport", data.body)

        val raw2 = "developer@hongduc.vn"
        val result2 = QrParser.parse(raw2)
        assertEquals(QrType.EMAIL, result2.type)
        val data2 = result2.data as QrParsedData.Email
        assertEquals("developer@hongduc.vn", data2.email)
    }

    @Test
    fun parseGeoCoordinates() {
        val raw = "geo:10.762622,106.660172"
        val result = QrParser.parse(raw)

        assertEquals(QrType.GEO, result.type)
        val data = result.data as QrParsedData.Geo
        assertEquals(10.762622, data.latitude, 0.000001)
        assertEquals(106.660172, data.longitude, 0.000001)

        val rawWithQuery = "geo:0,0?q=21.028511,105.854444(HaNoi)"
        val resultWithQuery = QrParser.parse(rawWithQuery)
        assertEquals(QrType.GEO, resultWithQuery.type)
        val dataWithQuery = resultWithQuery.data as QrParsedData.Geo
        assertEquals(21.028511, dataWithQuery.latitude, 0.000001)
        assertEquals(105.854444, dataWithQuery.longitude, 0.000001)
    }

    @Test
    fun parsePlainTextFallback() {
        val raw = "Simple plain text string without special syntax"
        val result = QrParser.parse(raw)

        assertEquals(QrType.TEXT, result.type)
        val data = result.data as QrParsedData.Text
        assertEquals(raw, data.content)
    }
}
