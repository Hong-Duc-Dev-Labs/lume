package com.hongducdev.toolkit.services.automation.modules

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.hongducdev.toolkit.domain.diy.Automation
import com.hongducdev.toolkit.domain.diy.Trigger
import com.hongducdev.toolkit.services.automation.AutomationManager

class BluetoothModule : AutomationModule {
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

                    @Suppress("DEPRECATION")
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val address = device?.address ?: ""
                    val name =
                        try {
                            device?.name ?: ""
                        } catch (_: SecurityException) {
                            ""
                        }

                    when (action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            automations.forEach { rule ->
                                val trig = rule.trigger as? Trigger.BluetoothConnected ?: return@forEach
                                if (trig.deviceAddress.isBlank() || trig.deviceAddress.equals(address, ignoreCase = true)) {
                                    AutomationManager.trigger(c, trig)
                                }
                            }
                        }

                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            automations.forEach { rule ->
                                val trig = rule.trigger as? Trigger.BluetoothDisconnected ?: return@forEach
                                if (trig.deviceAddress.isBlank() || trig.deviceAddress.equals(address, ignoreCase = true)) {
                                    AutomationManager.trigger(c, trig)
                                }
                            }
                        }
                    }
                }
            }

        val filter =
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
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
