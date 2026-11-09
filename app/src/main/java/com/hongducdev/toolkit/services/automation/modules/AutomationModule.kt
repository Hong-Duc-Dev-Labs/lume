package com.hongducdev.toolkit.services.automation.modules

import android.content.Context
import com.hongducdev.toolkit.domain.diy.Automation

interface AutomationModule {
    fun start(
        context: Context,
        automations: List<Automation>,
    )

    fun stop(context: Context)
}
