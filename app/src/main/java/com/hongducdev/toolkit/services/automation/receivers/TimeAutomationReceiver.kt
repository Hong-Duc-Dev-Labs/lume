package com.hongducdev.toolkit.services.automation.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hongducdev.toolkit.domain.diy.Automation
import com.hongducdev.toolkit.domain.diy.DIYRepository
import com.hongducdev.toolkit.services.automation.AutomationManager
import com.hongducdev.toolkit.services.automation.modules.TimeModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimeAutomationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TRIGGER = "com.hongducdev.toolkit.ACTION_TIME_AUTOMATION_TRIGGER"
        const val EXTRA_AUTOMATION_ID = "automation_id"
        const val EXTRA_IS_ENTRY = "is_entry"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_TRIGGER) return

        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID) ?: return
        val isEntry = intent.getBooleanExtra(EXTRA_IS_ENTRY, true)

        val pendingResult = goAsync()
        scope.launch {
            try {
                DIYRepository.init(context)
                val automation = DIYRepository.getAutomation(automationId) ?: return@launch
                if (!automation.isEnabled) return@launch

                TimeModule.rescheduleNext(context, automation, isEntry)

                val actions =
                    when (automation.type) {
                        Automation.Type.TRIGGER -> automation.actionList
                        Automation.Type.STATE -> if (isEntry) automation.entryActionList else automation.exitActionList
                        else -> emptyList()
                    }
                // Hành động có thể chứa "Chờ" rất lâu; chạy ngoài broadcast để không bị ANR.
                AutomationManager.runActions(context, actions)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
