package labs.hongducdev.lume.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import labs.hongducdev.lume.domain.diy.Automation
import labs.hongducdev.lume.domain.diy.Trigger
import labs.hongducdev.lume.services.automation.AutomationManager

class AndroidAutoModule : AutomationModule {
    private var receiver: BroadcastReceiver? = null
    private var isConnected = false

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
                    val status = intent?.getIntExtra("status", -1) ?: return

                    // Status 2 is connected/projecting in Android Auto
                    val connected = status == 2
                    if (connected != isConnected) {
                        isConnected = connected
                        val trigger = if (connected) Trigger.AndroidAutoConnected else Trigger.AndroidAutoDisconnected
                        AutomationManager.trigger(c, trigger)
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction("com.google.android.gms.car.PROJECTION_STATUS")
            }
        try {
            context.registerReceiver(receiver, filter)
        } catch (_: Exception) {
        }
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
