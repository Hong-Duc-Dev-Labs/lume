package labs.hongducdev.lume.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.util.TypedValue
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import labs.hongducdev.lume.data.LunarDisplayPolicy
import labs.hongducdev.lume.data.LunarSettings
import labs.hongducdev.lume.data.LunarSettingsRepository
import labs.hongducdev.lume.lunar.LunarCalendar
import labs.hongducdev.lume.lunar.LunarFormatter
import java.time.Instant
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Hình nền vẽ ảnh nền do người dùng chọn kèm dòng lịch âm theo mẫu đã nhập. */
class LunarWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = LunarEngine()

    private inner class LunarEngine : Engine() {
        private val repository = LunarSettingsRepository(applicationContext)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 0f, 2f, 0x99000000.toInt())
        }

        private var settings = LunarSettings()
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var background: Bitmap? = null
        private var loadedBackgroundVersion = -1L
        private var loadedForWidth = 0
        private var loadedForHeight = 0
        private var receiverRegistered = false

        private val dateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                draw()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            scope.launch {
                repository.settings.collect { value ->
                    settings = value
                    reloadBackgroundIfNeeded()
                    draw()
                }
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
            reloadBackgroundIfNeeded()
            draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                registerDateReceiver()
                draw()
            } else {
                unregisterDateReceiver()
            }
        }

        override fun onDestroy() {
            unregisterDateReceiver()
            scope.cancel()
            background = null
            super.onDestroy()
        }

        private fun registerDateReceiver() {
            if (receiverRegistered) return
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            ContextCompat.registerReceiver(
                this@LunarWallpaperService,
                dateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }

        private fun unregisterDateReceiver() {
            if (!receiverRegistered) return
            unregisterReceiver(dateReceiver)
            receiverRegistered = false
        }

        private fun reloadBackgroundIfNeeded() {
            if (surfaceWidth == 0 || surfaceHeight == 0) return
            val unchanged = loadedBackgroundVersion == settings.backgroundVersion &&
                loadedForWidth == surfaceWidth && loadedForHeight == surfaceHeight
            if (unchanged) return
            loadedBackgroundVersion = settings.backgroundVersion
            loadedForWidth = surfaceWidth
            loadedForHeight = surfaceHeight
            background = if (settings.backgroundVersion == 0L) null else decodeBackground()
        }

        private fun decodeBackground(): Bitmap? {
            val file = repository.backgroundFile()
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= surfaceWidth &&
                bounds.outHeight / (sample * 2) >= surfaceHeight
            ) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeFile(file.absolutePath, options)
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = holder.lockCanvas() ?: return
            try {
                render(canvas)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun render(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
            background?.let { drawCenterCrop(canvas, it) }

            if (!shouldDrawText()) return
            val lunar = LunarCalendar.fromInstant(Instant.now())
            val text = LunarFormatter.format(settings.template, lunar)
            if (text.isBlank()) return

            textPaint.color = settings.colorArgb
            textPaint.textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                settings.textSizeSp,
                resources.displayMetrics,
            )
            val lineHeight = textPaint.fontSpacing
            val x = canvas.width * settings.xFraction
            var y = canvas.height * settings.yFraction
            text.split('\n').forEach { line ->
                canvas.drawText(line, x, y, textPaint)
                y += lineHeight
            }
        }

        private fun shouldDrawText(): Boolean {
            val now = System.currentTimeMillis()
            val hookActive = LunarDisplayPolicy.isHookActive(
                lastSeenMillis = settings.hookLastSeen,
                bootMillis = now - SystemClock.elapsedRealtime(),
                nowMillis = now,
            )
            return LunarDisplayPolicy.wallpaperShouldShow(settings.displayMode, hookActive)
        }

        private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap) {
            val scale = max(
                canvas.width.toFloat() / bitmap.width,
                canvas.height.toFloat() / bitmap.height,
            )
            val dx = (canvas.width - bitmap.width * scale) / 2f
            val dy = (canvas.height - bitmap.height * scale) / 2f
            val matrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            }
            canvas.drawBitmap(bitmap, matrix, null)
        }
    }
}
