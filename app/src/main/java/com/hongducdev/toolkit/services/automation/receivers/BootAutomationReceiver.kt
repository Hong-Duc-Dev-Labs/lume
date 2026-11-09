package com.hongducdev.toolkit.services.automation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hongducdev.toolkit.services.automation.AutomationManager

class BootAutomationReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            AutomationManager.init(context)
        }
    }
}
