package labs.hongducdev.lume.car.auto

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import labs.hongducdev.lume.car.CarLocationManager
import labs.hongducdev.lume.car.CarRoutingManager
import labs.hongducdev.lume.car.CarSpeedAlertManager
import labs.hongducdev.lume.car.DestinationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Điểm vào Android Auto: được host của xe/Desktop Head Unit gắn vào khi điện thoại
 * kết nối. Khai báo category NAVIGATION để được vẽ HUD lên bề mặt bản đồ.
 */
class ToolkitCarAppService : CarAppService() {

    // App sideload cá nhân (không phát hành trên Play): chấp nhận mọi host để dùng được
    // với Android Auto ở chế độ nhà phát triển và Desktop Head Unit. Nếu phát hành rộng
    // rãi, hãy thay bằng danh sách host (package + chữ ký SHA-256) của Google.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = ToolkitCarSession()
}

/**
 * Dịch vụ dùng chung giữa màn hình trên xe (Android Auto) và màn hình kiểu Android Auto
 * trên điện thoại: định vị, cảnh báo tốc độ, tìm kiếm và lộ trình.
 */
internal class CarAppServices(context: Context) {
    val location = CarLocationManager(context)
    val alerts = CarSpeedAlertManager(context)
    val routing = CarRoutingManager(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Phát snapshot mỗi khi vị trí, cảnh báo hoặc lộ trình thay đổi, kèm cập nhật cảnh báo tốc độ. */
    fun collectSnapshots(onSnapshot: (HudSnapshot) -> Unit) {
        scope.launch {
            combine(location.locationState, alerts.alertState, routing.activeRoute) { loc, alert, route ->
                HudSnapshot(loc, alert, route)
            }.collect { snapshot ->
                alerts.onSpeedChanged(snapshot.location.speedKmh)
                onSnapshot(snapshot)
            }
        }
    }

    fun close() {
        scope.cancel()
        location.stopTracking()
        alerts.cleanup()
    }
}

internal class ToolkitCarSession : Session() {

    private lateinit var services: CarAppServices
    private lateinit var renderer: CarHudRenderer

    override fun onCreateScreen(intent: Intent): Screen {
        services = CarAppServices(carContext)
        renderer = CarHudRenderer(carContext)
        carContext.getCarService(androidx.car.app.AppManager::class.java)
            .setSurfaceCallback(renderer)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) = services.close()
        })
        startLocation()
        services.collectSnapshots { renderer.update(it) }

        val root = CarNavigationScreen(carContext, services)
        parseNavigateIntent(intent)?.let { destination ->
            carContext.getCarService(androidx.car.app.ScreenManager::class.java).push(root)
            return CarRoutePreviewScreen(carContext, services, destination)
        }
        return root
    }

    private fun startLocation() {
        val permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (permissions.any { carContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            services.location.startTracking()
        } else {
            carContext.requestPermissions(permissions) { granted, _ ->
                if (granted.isNotEmpty()) services.location.startTracking()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        val destination = parseNavigateIntent(intent) ?: return
        carContext.getCarService(androidx.car.app.ScreenManager::class.java)
            .push(CarRoutePreviewScreen(carContext, services, destination))
    }

    /** Đọc điểm đến từ intent `geo:lat,lng?q=tên` do host/Google Assistant gửi. */
    private fun parseNavigateIntent(intent: Intent): DestinationItem? {
        if (intent.action != CarContext.ACTION_NAVIGATE) return null
        val uri: Uri = intent.data ?: return null
        val coords = uri.schemeSpecificPart.substringBefore('?').split(',')
        val lat = coords.getOrNull(0)?.toDoubleOrNull() ?: return null
        val lng = coords.getOrNull(1)?.toDoubleOrNull() ?: return null
        val name = uri.getQueryParameter("q")?.takeIf { it.isNotBlank() && it.toDoubleOrNull() == null }
            ?: "Điểm đến"
        return DestinationItem(name = name, subtitle = "", latitude = lat, longitude = lng)
    }
}
