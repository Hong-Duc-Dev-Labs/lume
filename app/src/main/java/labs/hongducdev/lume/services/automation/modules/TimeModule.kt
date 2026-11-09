package labs.hongducdev.lume.services.automation.modules

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import labs.hongducdev.lume.domain.diy.Automation
import labs.hongducdev.lume.domain.diy.DiyState
import labs.hongducdev.lume.domain.diy.Trigger
import labs.hongducdev.lume.services.automation.receivers.TimeAutomationReceiver
import java.util.Calendar

class TimeModule : AutomationModule {
    companion object {
        const val TAG = "TimeModule"

        fun rescheduleNext(
            context: Context,
            automation: Automation,
            isEntry: Boolean,
        ) {
            when (automation.type) {
                Automation.Type.TRIGGER -> {
                    (automation.trigger as? Trigger.Schedule)?.let {
                        scheduleAlarm(context, automation.id, it.hour, it.minute, it.days, true)
                    }
                }

                Automation.Type.STATE -> {
                    (automation.state as? DiyState.TimePeriod)?.let {
                        if (isEntry) {
                            scheduleAlarm(context, automation.id, it.startHour, it.startMinute, it.days, true)
                        } else {
                            scheduleAlarm(context, automation.id, it.endHour, it.endMinute, it.days, false)
                        }
                    }
                }

                else -> {}
            }
        }

        fun scheduleAlarm(
            context: Context,
            id: String,
            hour: Int,
            minute: Int,
            days: Set<Int>,
            isEntry: Boolean,
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent =
                Intent(context, TimeAutomationReceiver::class.java).apply {
                    action = TimeAutomationReceiver.ACTION_TRIGGER
                    putExtra(TimeAutomationReceiver.EXTRA_AUTOMATION_ID, id)
                    putExtra(TimeAutomationReceiver.EXTRA_IS_ENTRY, isEntry)
                }

            val requestCode = (id + isEntry.toString()).hashCode()
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val calendar = calculateNextOccurrence(hour, minute, days)
            Log.d(TAG, "Scheduling alarm for $id at ${calendar.time}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent,
                            )
                        } else {
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                calendar.timeInMillis,
                                pendingIntent,
                            )
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent,
                        )
                    }
                } catch (e: Exception) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }

        fun calculateNextOccurrence(
            hour: Int,
            minute: Int,
            days: Set<Int>,
        ): Calendar {
            val now = Calendar.getInstance()
            val target =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

            if (days.isEmpty()) {
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target
            }

            for (i in 0..7) {
                val candidate = (target.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
                val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
                if (days.contains(dayOfWeek)) {
                    if (candidate.after(now)) {
                        return candidate
                    }
                }
            }

            target.add(Calendar.DAY_OF_YEAR, 1)
            return target
        }
    }

    override fun start(
        context: Context,
        automations: List<Automation>,
    ) {
        automations.forEach { automation ->
            rescheduleNext(context, automation, true)
            if (automation.type == Automation.Type.STATE) {
                rescheduleNext(context, automation, false)
            }
        }
    }

    override fun stop(context: Context) {
        // Alarms are cleaned up when replaced or triggered
    }
}
