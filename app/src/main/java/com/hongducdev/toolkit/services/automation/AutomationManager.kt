package com.hongducdev.toolkit.services.automation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hongducdev.toolkit.domain.diy.Action
import com.hongducdev.toolkit.domain.diy.Automation
import com.hongducdev.toolkit.domain.diy.DIYRepository
import com.hongducdev.toolkit.domain.diy.DIYRuntimeStore
import com.hongducdev.toolkit.domain.diy.DiyState
import com.hongducdev.toolkit.domain.diy.Trigger
import com.hongducdev.toolkit.services.automation.executors.CombinedActionExecutor
import com.hongducdev.toolkit.services.automation.modules.AndroidAutoModule
import com.hongducdev.toolkit.services.automation.modules.AutomationModule
import com.hongducdev.toolkit.services.automation.modules.BluetoothModule
import com.hongducdev.toolkit.services.automation.modules.DisplayModule
import com.hongducdev.toolkit.services.automation.modules.PowerModule
import com.hongducdev.toolkit.services.automation.modules.TimeModule
import com.hongducdev.toolkit.services.automation.modules.WifiModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object AutomationManager {
    private const val TAG = "AutomationManager"
    private val scope = CoroutineScope(Dispatchers.Main)
    private var service: AutomationService? = null
    private var applicationContext: Context? = null

    private val activeModules = ConcurrentHashMap<String, AutomationModule>()

    fun init(context: Context) {
        applicationContext = context.applicationContext
        DIYRepository.init(context)

        scope.launch {
            DIYRepository.automations.collect { automations ->
                refreshModules(automations)
            }
        }
    }

    fun onServiceConnected(serviceInstance: AutomationService) {
        service = serviceInstance
        val automations = DIYRepository.automations.value
        refreshModules(automations)
    }

    fun onServiceDisconnected(serviceInstance: AutomationService) {
        if (service == serviceInstance) {
            service = null
            stopAllModules()
        }
    }

    /** Chạy danh sách hành động ngoài vòng đời broadcast, được giữ sống bởi foreground service. */
    fun runActions(
        context: Context,
        actions: List<Action>,
    ) {
        if (actions.isEmpty()) return
        ensureServiceStarted(context)
        scope.launch(Dispatchers.IO) {
            CombinedActionExecutor.executeAll(context, actions)
        }
    }

    fun trigger(
        context: Context,
        trigger: Trigger,
    ) {
        scope.launch(Dispatchers.IO) {
            val automations = DIYRepository.automations.value
            automations
                .filter { it.isEnabled && it.type == Automation.Type.TRIGGER && it.trigger == trigger }
                .forEach { automation ->
                    CombinedActionExecutor.executeAll(context, automation.actionList)
                }
        }
    }

    fun evaluateState(
        context: Context,
        moduleId: String,
        isOpenPredicate: (Automation) -> Boolean,
    ) {
        scope.launch(Dispatchers.IO) {
            val automations = DIYRepository.automations.value.filter { it.isEnabled && it.type == Automation.Type.STATE }
            val previouslyOpen = DIYRuntimeStore.openStateIds(moduleId)
            val currentlyOpen = mutableSetOf<String>()

            automations.forEach { automation ->
                val isOpen = isOpenPredicate(automation)
                if (isOpen) {
                    currentlyOpen.add(automation.id)
                    if (!previouslyOpen.contains(automation.id)) {
                        // State just became active -> execute entry actions
                        CombinedActionExecutor.executeAll(context, automation.entryActionList)
                    }
                } else {
                    if (previouslyOpen.contains(automation.id)) {
                        // State just became inactive -> execute exit actions
                        CombinedActionExecutor.executeAll(context, automation.exitActionList)
                    }
                }
            }

            DIYRuntimeStore.setOpenStateIds(moduleId, currentlyOpen)
        }
    }

    private fun stopAllModules() {
        val context = applicationContext ?: return
        activeModules.values.forEach { it.stop(context) }
        activeModules.clear()
    }

    private fun refreshModules(automations: List<Automation>) {
        val context = applicationContext ?: return
        val enabledAutomations = automations.filter { it.isEnabled }

        val powerRules =
            enabledAutomations.filter { rule ->
                rule.trigger is Trigger.ChargerConnected || rule.trigger is Trigger.ChargerDisconnected ||
                    rule.trigger is Trigger.PowerSavingOn || rule.trigger is Trigger.PowerSavingOff ||
                    rule.state is DiyState.Charging ||
                    rule.state is DiyState.BatteryLevel ||
                    rule.state is DiyState.PowerSaving
            }

        val displayRules =
            enabledAutomations.filter { rule ->
                rule.trigger is Trigger.ScreenOn || rule.trigger is Trigger.ScreenOff || rule.trigger is Trigger.DeviceUnlock ||
                    rule.state is DiyState.ScreenOn
            }

        val timeRules =
            enabledAutomations.filter { rule ->
                rule.trigger is Trigger.Schedule || rule.state is DiyState.TimePeriod
            }

        val bluetoothRules =
            enabledAutomations.filter { rule ->
                rule.trigger is Trigger.BluetoothConnected || rule.trigger is Trigger.BluetoothDisconnected
            }

        val wifiRules =
            enabledAutomations.filter { rule ->
                rule.trigger is Trigger.WifiConnected || rule.trigger is Trigger.WifiDisconnected
            }

        val androidAutoRules =
            enabledAutomations.filter { rule ->
                rule.trigger is Trigger.AndroidAutoConnected || rule.trigger is Trigger.AndroidAutoDisconnected
            }

        updateModule("power", powerRules, PowerModule())
        updateModule("display", displayRules, DisplayModule())
        updateModule("time", timeRules, TimeModule())
        updateModule("bluetooth", bluetoothRules, BluetoothModule())
        updateModule("wifi", wifiRules, WifiModule())
        updateModule("android_auto", androidAutoRules, AndroidAutoModule())

        // Start or stop background service based on whether any automations exist
        if (enabledAutomations.isNotEmpty()) {
            ensureServiceStarted(context)
        }
    }

    private fun updateModule(
        key: String,
        rules: List<Automation>,
        moduleFactory: AutomationModule,
    ) {
        val context = applicationContext ?: return
        if (rules.isNotEmpty()) {
            val module = activeModules.getOrPut(key) { moduleFactory }
            module.start(context, rules)
        } else {
            activeModules.remove(key)?.stop(context)
        }
    }

    private fun ensureServiceStarted(context: Context) {
        try {
            val intent =
                Intent(context, AutomationService::class.java).apply {
                    putExtra("is_foreground_start", true)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot start AutomationService", e)
        }
    }
}
