package labs.hongducdev.lume.services.automation.modules

import android.content.Context
import labs.hongducdev.lume.domain.diy.Automation

interface AutomationModule {
    fun start(
        context: Context,
        automations: List<Automation>,
    )

    fun stop(context: Context)
}
