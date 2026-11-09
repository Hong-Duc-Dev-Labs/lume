package labs.hongducdev.lume.car

import android.content.Context
import android.util.Log
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps
import org.maplibre.android.module.http.HttpRequestUtil
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object CarMapHttpConfig {
    private const val TAG = "CarMapHttpConfig"
    private var initialized = false

    private val FASTLY_IPS = listOf(
        InetAddress.getByAddress("a.basemaps.cartocdn.com", byteArrayOf(151.toByte(), 101.toByte(), 2.toByte(), 132.toByte())),
        InetAddress.getByAddress("a.basemaps.cartocdn.com", byteArrayOf(151.toByte(), 101.toByte(), 66.toByte(), 132.toByte())),
        InetAddress.getByAddress("a.basemaps.cartocdn.com", byteArrayOf(151.toByte(), 101.toByte(), 130.toByte(), 132.toByte())),
        InetAddress.getByAddress("a.basemaps.cartocdn.com", byteArrayOf(151.toByte(), 101.toByte(), 194.toByte(), 132.toByte()))
    )

    private val OSM_IPS = listOf(
        InetAddress.getByAddress("tile.openstreetmap.org", byteArrayOf(151.toByte(), 101.toByte(), 1.toByte(), 91.toByte())),
        InetAddress.getByAddress("tile.openstreetmap.org", byteArrayOf(151.toByte(), 101.toByte(), 65.toByte(), 91.toByte()))
    )

    private val CLOUDFLARE_OFM_IPS = listOf(
        InetAddress.getByAddress("tiles.openfreemap.org", byteArrayOf(172.toByte(), 67.toByte(), 74.toByte(), 56.toByte())),
        InetAddress.getByAddress("tiles.openfreemap.org", byteArrayOf(104.toByte(), 21.toByte(), 85.toByte(), 158.toByte()))
    )

    private val resilientDns = object : Dns {
        private var doh: DnsOverHttps? = null

        fun setDoh(dohInstance: DnsOverHttps?) {
            doh = dohInstance
        }

        override fun lookup(hostname: String): List<InetAddress> {
            // 1. Thử DNS chuẩn hệ thống trước
            try {
                val sys = Dns.SYSTEM.lookup(hostname)
                if (sys.isNotEmpty()) return sys
            } catch (e: Exception) {
                Log.w(TAG, "System DNS lookup failed for $hostname: ${e.message}, falling back")
            }

            // 2. Thử DNS over HTTPS qua Cloudflare 1.1.1.1
            doh?.let {
                try {
                    val result = it.lookup(hostname)
                    if (result.isNotEmpty()) return result
                } catch (e: Exception) {
                    Log.d(TAG, "DoH lookup failed for $hostname")
                }
            }

            // 3. Fallback IP tĩnh nếu cả hai phương thức trên không phân giải được
            if (hostname.endsWith("openfreemap.org")) {
                return CLOUDFLARE_OFM_IPS
            }
            if (hostname.endsWith("openstreetmap.org")) {
                return OSM_IPS
            }
            if (hostname.endsWith("basemaps.cartocdn.com")) {
                return FASTLY_IPS
            }

            return Dns.SYSTEM.lookup(hostname)
        }
    }

    fun getResilientDns(): Dns = resilientDns

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return

        try {
            val bootstrapClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val doh = try {
                DnsOverHttps.Builder()
                    .client(bootstrapClient)
                    .url("https://1.1.1.1/dns-query".toHttpUrl())
                    .bootstrapDnsHosts(
                        InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
                        InetAddress.getByAddress(byteArrayOf(1, 0, 0, 1)),
                        InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
                    )
                    .includeIPv6(false)
                    .build()
            } catch (e: Exception) {
                Log.w(TAG, "DoH init fallback", e)
                null
            }

            resilientDns.setDoh(doh)

            val mapHttpClient = OkHttpClient.Builder()
                .dns(resilientDns)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "LumeCarMap/1.0 (Android; MapLibre)")
                        .build()
                    chain.proceed(request)
                }
                .build()

            HttpRequestUtil.setOkHttpClient(mapHttpClient)
            HttpRequestUtil.setPrintRequestUrlOnFailure(true)
            initialized = true
            Log.d(TAG, "MapLibre OkHttpClient configured with Resilient Dns successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure MapLibre OkHttpClient", e)
        }
    }
}
