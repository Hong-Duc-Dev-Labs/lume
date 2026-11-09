package labs.hongducdev.lume.car.auto

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Surface
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import labs.hongducdev.lume.car.CarLocationState
import labs.hongducdev.lume.car.CarSpeedAlertState
import labs.hongducdev.lume.car.RouteInfo
import labs.hongducdev.lume.car.SignType

internal data class HudSnapshot(
    val location: CarLocationState,
    val alert: CarSpeedAlertState,
    val route: RouteInfo?
)

/**
 * Vẽ HUD lên bề mặt bản đồ của Android Auto: tốc độ hiện tại, biển giới hạn tốc độ,
 * cảnh báo biển báo/camera và đường đi đang chọn. Chỉ vẽ trong vùng nhìn thấy
 * (visible area) để không bị các thành phần của host che.
 */
internal class CarHudRenderer(private val carContext: CarContext) : SurfaceCallback {

    private var surface: Surface? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var visibleArea: Rect? = null
    private var snapshot: HudSnapshot? = null

    private var cachedRouteJson: String? = null
    private var cachedRoutePoints: List<GeoPoint> = emptyList()

    private val density = carContext.resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD }

    fun update(newSnapshot: HudSnapshot) {
        snapshot = newSnapshot
        draw()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        surface = surfaceContainer.surface
        surfaceWidth = surfaceContainer.width
        surfaceHeight = surfaceContainer.height
        draw()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        surface = null
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        this.visibleArea = visibleArea
        draw()
    }

    private fun dp(value: Float) = value * density

    private fun draw() {
        val target = surface?.takeIf { it.isValid } ?: return
        val state = snapshot ?: return
        val canvas = try {
            target.lockCanvas(null)
        } catch (e: Exception) {
            return
        }
        try {
            render(canvas, state)
        } finally {
            target.unlockCanvasAndPost(canvas)
        }
    }

    private fun render(canvas: Canvas, state: HudSnapshot) {
        val dark = carContext.isDarkMode
        canvas.drawColor(if (dark) Color.rgb(14, 16, 22) else Color.rgb(232, 236, 241))

        val area = visibleArea?.takeIf { !it.isEmpty }
            ?: Rect(0, 0, surfaceWidth, surfaceHeight)
        val pad = dp(16f)
        val content = RectF(
            area.left + pad,
            area.top + pad,
            area.right - pad,
            area.bottom - pad
        )

        drawRoute(canvas, state, content, dark)
        drawSpeedPanel(canvas, state, content, dark)
        drawApproachingSign(canvas, state, content, dark)
        drawStatus(canvas, state, content, dark)
    }

    private fun drawRoute(canvas: Canvas, state: HudSnapshot, content: RectF, dark: Boolean) {
        val route = state.route ?: return
        if (route.geoJson != cachedRouteJson) {
            cachedRouteJson = route.geoJson
            cachedRoutePoints = CarRouteProjection.parseLineString(route.geoJson)
        }
        val line = cachedRoutePoints
        if (line.size < 2) return

        val user = GeoPoint(state.location.latitude, state.location.longitude)
        val destination = GeoPoint(route.destinationLat, route.destinationLng)
        // Chừa chỗ cho cụm tốc độ ở bên trái.
        val mapRect = RectF(
            content.left + dp(220f).coerceAtMost(content.width() * 0.4f),
            content.top + dp(24f),
            content.right - dp(24f),
            content.bottom - dp(24f)
        )
        if (mapRect.width() <= 0f || mapRect.height() <= 0f) return
        val projection = CarRouteProjection(
            line + user + destination,
            mapRect.left, mapRect.top, mapRect.width(), mapRect.height()
        )

        val path = Path()
        line.forEachIndexed { i, p ->
            val x = projection.x(p)
            val y = projection.y(p)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        stroke.strokeWidth = dp(9f)
        stroke.color = if (dark) Color.argb(90, 0, 229, 255) else Color.argb(90, 41, 121, 255)
        canvas.drawPath(path, stroke)
        stroke.strokeWidth = dp(5f)
        stroke.color = if (dark) Color.rgb(0, 229, 255) else Color.rgb(41, 121, 255)
        canvas.drawPath(path, stroke)

        fill.color = Color.rgb(255, 82, 82)
        canvas.drawCircle(projection.x(destination), projection.y(destination), dp(8f), fill)
        fill.color = Color.WHITE
        canvas.drawCircle(projection.x(user), projection.y(user), dp(11f), fill)
        fill.color = Color.rgb(41, 121, 255)
        canvas.drawCircle(projection.x(user), projection.y(user), dp(7f), fill)
    }

    private fun drawSpeedPanel(canvas: Canvas, state: HudSnapshot, content: RectF, dark: Boolean) {
        val over = state.alert.isOverSpeed
        val panel = RectF(
            content.left,
            content.top,
            content.left + dp(200f),
            content.top + dp(112f)
        )
        fill.color = when {
            over -> Color.argb(235, 183, 28, 28)
            dark -> Color.argb(225, 16, 18, 24)
            else -> Color.argb(235, 255, 255, 255)
        }
        canvas.drawRoundRect(panel, dp(20f), dp(20f), fill)

        val primary = if (over || dark) Color.WHITE else Color.rgb(20, 22, 28)
        text.color = primary
        text.textSize = dp(54f)
        text.textAlign = Paint.Align.LEFT
        canvas.drawText("${state.location.speedKmh}", panel.left + dp(16f), panel.top + dp(66f), text)

        text.color = if (over) Color.rgb(255, 205, 210) else Color.rgb(0, 200, 110)
        text.textSize = dp(15f)
        canvas.drawText("km/h", panel.left + dp(16f), panel.top + dp(90f), text)

        // Thanh tỷ lệ tốc độ / giới hạn.
        val ratio = (state.location.speedKmh.toFloat() / state.alert.speedLimit.coerceAtLeast(1)).coerceIn(0f, 1.25f)
        val track = RectF(panel.left + dp(16f), panel.bottom - dp(14f), panel.right - dp(16f), panel.bottom - dp(9f))
        fill.color = Color.argb(60, 128, 128, 128)
        canvas.drawRoundRect(track, dp(3f), dp(3f), fill)
        fill.color = when {
            over -> Color.rgb(255, 23, 68)
            ratio > 0.85f -> Color.rgb(255, 179, 0)
            else -> Color.rgb(0, 230, 118)
        }
        val filled = RectF(track.left, track.top, track.left + track.width() * (ratio / 1.25f).coerceAtLeast(0.05f), track.bottom)
        canvas.drawRoundRect(filled, dp(3f), dp(3f), fill)

        drawSpeedLimitSign(canvas, state.alert.speedLimit, over, panel.right + dp(16f) + dp(36f), panel.centerY())
    }

    private fun drawSpeedLimitSign(canvas: Canvas, limit: Int, over: Boolean, cx: Float, cy: Float) {
        val radius = dp(36f)
        if (over) {
            stroke.strokeWidth = dp(5f)
            stroke.color = Color.argb(140, 255, 23, 68)
            canvas.drawCircle(cx, cy, radius + dp(5f), stroke)
        }
        fill.color = Color.WHITE
        canvas.drawCircle(cx, cy, radius, fill)
        stroke.strokeWidth = dp(7f)
        stroke.color = Color.rgb(211, 47, 47)
        canvas.drawCircle(cx, cy, radius - dp(3.5f), stroke)
        text.color = Color.rgb(26, 26, 26)
        text.textSize = dp(28f)
        text.textAlign = Paint.Align.CENTER
        canvas.drawText("$limit", cx, cy + dp(10f), text)
        text.textAlign = Paint.Align.LEFT
    }

    private fun drawApproachingSign(canvas: Canvas, state: HudSnapshot, content: RectF, dark: Boolean) {
        val sign = state.alert.approachingSign ?: return
        val accent = when (sign.type) {
            SignType.SPEED_CAMERA -> Color.rgb(255, 145, 0)
            SignType.RESIDENTIAL_AREA -> Color.rgb(0, 229, 255)
            SignType.END_RESIDENTIAL -> Color.rgb(0, 230, 118)
            SignType.SPEED_LIMIT -> Color.rgb(255, 214, 0)
        }
        val label = if (sign.distanceMeters > 0) "${sign.title} · ${sign.distanceMeters} m" else sign.title
        text.textSize = dp(16f)
        val textWidth = text.measureText(label)
        val pill = RectF(
            content.left,
            content.top + dp(128f),
            content.left + textWidth + dp(40f),
            content.top + dp(128f) + dp(44f)
        )
        fill.color = if (dark) Color.argb(230, 20, 22, 32) else Color.argb(240, 255, 255, 255)
        canvas.drawRoundRect(pill, dp(14f), dp(14f), fill)
        stroke.strokeWidth = dp(1.5f)
        stroke.color = accent
        canvas.drawRoundRect(pill, dp(14f), dp(14f), stroke)
        fill.color = accent
        canvas.drawCircle(pill.left + dp(18f), pill.centerY(), dp(6f), fill)
        text.color = if (dark) Color.WHITE else Color.rgb(20, 22, 28)
        canvas.drawText(label, pill.left + dp(32f), pill.centerY() + dp(6f), text)
    }

    private fun drawStatus(canvas: Canvas, state: HudSnapshot, content: RectF, dark: Boolean) {
        val message = when {
            !state.location.isPermissionGranted -> "Chưa cấp quyền vị trí"
            !state.location.hasGpsFix -> "Đang chờ tín hiệu GPS..."
            state.route != null -> "${state.route.destinationName} · ${state.route.distanceDisplay} · ${state.route.durationDisplay}"
            else -> return
        }
        text.textSize = dp(15f)
        text.color = if (dark) Color.rgb(176, 190, 197) else Color.rgb(69, 90, 100)
        text.textAlign = Paint.Align.LEFT
        canvas.drawText(message, content.left, content.bottom - dp(4f), text)
    }
}
