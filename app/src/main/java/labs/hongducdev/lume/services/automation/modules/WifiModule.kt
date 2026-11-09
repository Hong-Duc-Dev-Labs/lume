package labs.hongducdev.lume.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import labs.hongducdev.lume.domain.diy.Automation
import labs.hongducdev.lume.domain.diy.Trigger
import labs.hongducdev.lume.services.automation.AutomationManager

class WifiModule : AutomationModule {
    private var receiver: BroadcastReceiver? = null
    private var lastConnectedSsid: String? = null

    override fun start(
        context: Context,
        automations: List<Automation>,
    ) {
        if (receiver != null) return

        receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context?,
                    intent: Intent?,
                ) {
                    val c = ctx ?: return
                    val action = intent?.action ?: return

                    if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
                        @Suppress("DEPRECATION")
                        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)

                        @Suppress("DEPRECATION")
                        val wifiInfo = intent.getParcelableExtra<WifiInfo>(WifiManager.EXTRA_WIFI_INFO)

                        val isConnected = networkInfo?.isConnected == true
                        val currentSsid = wifiInfo?.ssid?.replace("\"", "") ?: ""

                        if (isConnected && currentSsid.isNotBlank() && currentSsid != "<unknown ssid>") {
                            lastConnectedSsid = currentSsid
                            automations.forEach { rule ->
                                val trig = rule.trigger as? Trigger.WifiConnected ?: return@forEach
                                if (trig.ssid.isBlank() || trig.ssid.equals(currentSsid, ignoreCase = true)) {
                                    AutomationManager.trigger(c, trig)
                                }
                            }
                        } else if (!isConnected && lastConnectedSsid != null) {
                            val disconnectedSsid = lastConnectedSsid ?: ""
                            lastConnectedSsid = null
                            automations.forEach { rule ->
                                val trig = rule.trigger as? Trigger.WifiDisconnected ?: return@forEach
                                if (trig.ssid.isBlank() || trig.ssid.equals(disconnectedSsid, ignoreCase = true)) {
                                    AutomationManager.trigger(c, trig)
                                }
                            }
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            }
        context.registerReceiver(receiver, filter)
    }

    override fun stop(context: Context) {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {
            }
            receiver = null
        }
    }
}
