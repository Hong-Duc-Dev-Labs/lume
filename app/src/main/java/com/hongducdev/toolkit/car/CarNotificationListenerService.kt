package com.hongducdev.toolkit.car

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarNotificationListenerService : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        mainHandler.post {
            attachToFirstActiveController(controllers)
        }
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            mainHandler.post { refreshMediaState() }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            mainHandler.post { refreshMediaState() }
        }

        override fun onSessionDestroyed() {
            mainHandler.post {
                activeController = null
                _mediaState.value = _mediaState.value.copy(
                    isAvailable = false,
                    isPlaying = false,
                    title = "",
                    artist = "",
                    album = "",
                    albumArt = null,
                    durationMs = 0L,
                    positionMs = 0L
                )
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "CarNotificationListener connected")
        instance = this
        initSessionManager()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "CarNotificationListener disconnected")
        detachController()
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        instance = null
        _mediaState.value = _mediaState.value.copy(
            isAvailable = false,
            isPlaying = false,
            isNotificationListenerEnabled = false
        )
    }

    private fun initSessionManager() {
        try {
            sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, CarNotificationListenerService::class.java)
            sessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            val initialControllers = sessionManager?.getActiveSessions(component)
            attachToFirstActiveController(initialControllers)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaSessionManager", e)
        }
    }

    private fun attachToFirstActiveController(controllers: List<MediaController>?) {
        detachController()
        val target = controllers?.firstOrNull()
        if (target != null) {
            activeController = target
            target.registerCallback(mediaCallback, mainHandler)
            refreshMediaState()
        } else {
            _mediaState.value = _mediaState.value.copy(
                isAvailable = false,
                isPlaying = false,
                isNotificationListenerEnabled = true
            )
        }
    }

    private fun detachController() {
        try {
            activeController?.unregisterCallback(mediaCallback)
        } catch (ignored: Exception) {
        }
        activeController = null
    }

    private fun refreshMediaState() {
        val ctrl = activeController
        if (ctrl == null) {
            _mediaState.value = _mediaState.value.copy(
                isAvailable = false,
                isPlaying = false,
                isNotificationListenerEnabled = true
            )
            return
        }

        val metadata = ctrl.metadata
        val pbState = ctrl.playbackState
        val isPlaying = pbState?.state == PlaybackState.STATE_PLAYING

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = pbState?.position ?: 0L

        val pkgName = ctrl.packageName ?: ""
        var appLabel = pkgName
        try {
            val appInfo = packageManager.getApplicationInfo(pkgName, 0)
            appLabel = packageManager.getApplicationLabel(appInfo).toString()
        } catch (ignored: PackageManager.NameNotFoundException) {
        }

        _mediaState.value = CarMediaState(
            isAvailable = true,
            isPlaying = isPlaying,
            title = title,
            artist = artist,
            album = album,
            albumArt = art,
            appPackage = pkgName,
            appName = appLabel,
            durationMs = duration,
            positionMs = position,
            isNotificationListenerEnabled = true
        )
    }

    companion object {
        private const val TAG = "CarNotificationListener"

        private val _mediaState = MutableStateFlow(CarMediaState())
        val mediaState: StateFlow<CarMediaState> = _mediaState.asStateFlow()

        private var instance: CarNotificationListenerService? = null

        fun isConnected(): Boolean = instance != null

        fun play() {
            try {
                instance?.activeController?.transportControls?.play()
            } catch (e: Exception) {
                Log.e(TAG, "play failed", e)
            }
        }

        fun pause() {
            try {
                instance?.activeController?.transportControls?.pause()
            } catch (e: Exception) {
                Log.e(TAG, "pause failed", e)
            }
        }

        fun skipToNext() {
            try {
                instance?.activeController?.transportControls?.skipToNext()
            } catch (e: Exception) {
                Log.e(TAG, "skipToNext failed", e)
            }
        }

        fun skipToPrevious() {
            try {
                instance?.activeController?.transportControls?.skipToPrevious()
            } catch (e: Exception) {
                Log.e(TAG, "skipToPrevious failed", e)
            }
        }

        fun seekTo(positionMs: Long) {
            try {
                instance?.activeController?.transportControls?.seekTo(positionMs)
            } catch (e: Exception) {
                Log.e(TAG, "seekTo failed", e)
            }
        }

        fun checkPermissionStatus(context: Context) {
            val enabledListeners = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            val myComponent = ComponentName(context, CarNotificationListenerService::class.java).flattenToString()
            val isEnabled = enabledListeners.contains(myComponent)
            _mediaState.value = _mediaState.value.copy(
                isNotificationListenerEnabled = isEnabled
            )
        }
    }
}
