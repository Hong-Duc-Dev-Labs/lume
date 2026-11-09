package labs.hongducdev.lume.services.automation.modules

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import labs.hongducdev.lume.domain.diy.Automation
import labs.hongducdev.lume.domain.diy.DiyState
import labs.hongducdev.lume.domain.diy.Trigger
import labs.hongducdev.lume.services.automation.AutomationManager

class DisplayModule : AutomationModule {
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
                        Intent.ACTION_SCREEN_ON -> {
                            AutomationManager.trigger(c, Trigger.ScreenOn)
                            AutomationManager.evaluateState(c, "display") { rule ->
                                rule.state is DiyState.ScreenOn
                            }
                        }

                        Intent.ACTION_SCREEN_OFF -> {
                            AutomationManager.trigger(c, Trigger.ScreenOff)
                            AutomationManager.evaluateState(c, "display") { rule ->
                                false
                            }
                        }

                        Intent.ACTION_USER_PRESENT -> {
                            AutomationManager.trigger(c, Trigger.DeviceUnlock)
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
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
