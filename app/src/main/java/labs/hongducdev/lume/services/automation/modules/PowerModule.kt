package labs.hongducdev.lume.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import labs.hongducdev.lume.domain.diy.Automation
import labs.hongducdev.lume.domain.diy.DiyState
import labs.hongducdev.lume.domain.diy.Trigger
import labs.hongducdev.lume.services.automation.AutomationManager

class PowerModule : AutomationModule {
    private var receiver: BroadcastReceiver? = null

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

                    when (action) {
                        Intent.ACTION_POWER_CONNECTED -> {
                            AutomationManager.trigger(c, Trigger.ChargerConnected)
                            AutomationManager.evaluateState(c, "power") { rule ->
                                rule.state is DiyState.Charging
                            }
                        }

                        Intent.ACTION_POWER_DISCONNECTED -> {
                            AutomationManager.trigger(c, Trigger.ChargerDisconnected)
                            AutomationManager.evaluateState(c, "power") { rule ->
                                false
                            }
                        }

                        Intent.ACTION_BATTERY_CHANGED -> {
                            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

                            AutomationManager.evaluateState(c, "power") { rule ->
                                when (val s = rule.state) {
                                    is DiyState.BatteryLevel -> batteryPct in s.minLevel..s.maxLevel
                                    else -> false
                                }
                            }
                        }

                        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                            val pm = c.getSystemService(Context.POWER_SERVICE) as? PowerManager
                            val isPowerSave = pm?.isPowerSaveMode ?: false
                            if (isPowerSave) {
                                AutomationManager.trigger(c, Trigger.PowerSavingOn)
                            } else {
                                AutomationManager.trigger(c, Trigger.PowerSavingOff)
                            }
                            AutomationManager.evaluateState(c, "power") { rule ->
                                rule.state is DiyState.PowerSaving && isPowerSave
                            }
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
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
