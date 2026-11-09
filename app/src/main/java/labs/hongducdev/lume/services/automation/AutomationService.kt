package labs.hongducdev.lume.services.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AutomationService : Service() {
    companion object {
        private const val CHANNEL_ID = "toolkit_automation_service_channel"
        private const val NOTIFICATION_ID = 888
        var isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val isForegroundStart = intent?.getBooleanExtra("is_foreground_start", false) ?: false
        if (isForegroundStart) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        } else {
                            0
                        },
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        AutomationManager.onServiceConnected(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        AutomationManager.onServiceDisconnected(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Lume Tự động hóa",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Dịch vụ duy trì các quy tắc tự động hóa ngầm"
                }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Lume Tự động hóa")
            .setContentText("Các quy tắc tự động hóa đang hoạt động ngầm")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
}
