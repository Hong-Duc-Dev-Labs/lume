package com.hongducdev.toolkit.car.auto

import android.content.Intent
import android.net.Uri
import androidx.activity.OnBackPressedCallback
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import com.hongducdev.toolkit.car.CarRoutingManager
import com.hongducdev.toolkit.car.DestinationItem
import com.hongducdev.toolkit.car.RouteInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 400L

/** Màn hình chính: bản đồ/HUD tốc độ do [CarHudRenderer] vẽ, kèm nút tìm đường và bật/tắt giọng nói. */
internal class CarNavigationScreen(
    carContext: CarContext,
    private val services: CarAppServices
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val voiceOn = services.alerts.alertState.value.isVoiceAlertEnabled
        val actions = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Tìm đường")
                    .setOnClickListener { screenManager.push(CarSearchScreen(carContext, services)) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle(if (voiceOn) "Tắt cảnh báo" else "Bật cảnh báo")
                    .setOnClickListener {
                        services.alerts.toggleVoiceAlert()
                        invalidate()
                    }
                    .build()
            )
            .build()
        return NavigationTemplate.Builder().setActionStrip(actions).build()
    }
}

/** Tìm địa điểm bằng bàn phím/giọng nói của xe, kèm các gợi ý nhanh cho tài xế. */
internal class CarSearchScreen(
    carContext: CarContext,
    private val services: CarAppServices
) : Screen(carContext) {

    private var results: List<DestinationItem> = emptyList()
    private var isLoading = false
    private var hasQuery = false
    private var searchJob: Job? = null

    override fun onGetTemplate(): Template {
        val builder = SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            override fun onSearchTextChanged(searchText: String) {
                if (searchText.trim().length >= 2) search(searchText, debounce = true)
            }

            override fun onSearchSubmitted(searchText: String) = search(searchText, debounce = false)
        })
            .setHeaderAction(Action.BACK)
            .setShowKeyboardByDefault(true)
            .setSearchHint("Nhập địa chỉ, tên đường, sân bay...")

        if (isLoading) {
            builder.setLoading(true)
        } else {
            builder.setItemList(buildList())
        }
        return builder.build()
    }

    private fun buildList(): ItemList {
        val list = ItemList.Builder()
        if (hasQuery && results.isEmpty()) {
            list.setNoItemsMessage("Không tìm thấy địa điểm phù hợp")
        } else if (!hasQuery) {
            CarRoutingManager.QUICK_SEARCH_CHIPS.forEach { label ->
                list.addItem(
                    Row.Builder()
                        .setTitle(label)
                        .setOnClickListener { search(label, debounce = false) }
                        .build()
                )
            }
        } else {
            results.forEach { item ->
                val row = Row.Builder().setTitle(item.name)
                if (item.subtitle.isNotBlank()) row.addText(item.subtitle)
                list.addItem(row.setOnClickListener { select(item) }.build())
            }
        }
        return list.build()
    }

    private fun search(query: String, debounce: Boolean) {
        searchJob?.cancel()
        searchJob = services.scope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            val loc = services.location.locationState.value
            isLoading = true
            invalidate()
            results = services.routing.searchPlaces(query, loc.latitude, loc.longitude)
            hasQuery = true
            isLoading = false
            invalidate()
        }
    }

    private fun select(item: DestinationItem) {
        screenManager.push(CarRoutePreviewScreen(carContext, services, item))
    }
}

/** Tính lộ trình tới điểm đến và hiển thị khoảng cách, thời gian, nút bắt đầu dẫn đường. */
internal class CarRoutePreviewScreen(
    carContext: CarContext,
    private val services: CarAppServices,
    private val destination: DestinationItem
) : Screen(carContext) {

    private var route: RouteInfo? = null
    private var failed = false

    init {
        carContext.onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = cancelRoute()
        })
        services.scope.launch {
            val loc = services.location.locationState.value
            route = services.routing.calculateRoute(loc.latitude, loc.longitude, destination)
            failed = route == null
            if (failed) {
                CarToast.makeText(this@CarRoutePreviewScreen.carContext, "Không tính được lộ trình", CarToast.LENGTH_LONG).show()
            }
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val current = route
        val builder = RoutePreviewNavigationTemplate.Builder()
            .setHeaderAction(Action.BACK)
            .setTitle(destination.name)

        if (current == null) {
            return builder.setLoading(!failed).apply {
                if (failed) {
                    setItemList(ItemList.Builder().setNoItemsMessage("Không tính được lộ trình").build())
                }
            }.build()
        }

        val row = Row.Builder()
            .setTitle(current.destinationName)
            .addText("${current.distanceDisplay} · ${current.durationDisplay}")
            .build()
        return builder
            .setItemList(ItemList.Builder().addItem(row).setOnSelectedListener { }.build())
            .setNavigateAction(
                Action.Builder()
                    .setTitle("Bắt đầu")
                    .setOnClickListener { startNavigation(current) }
                    .build()
            )
            .build()
    }

    private fun cancelRoute() {
        services.routing.clearRoute()
        screenManager.pop()
    }

    /** Chuyển dẫn đường từng chặng cho ứng dụng điều hướng mặc định của xe (ví dụ Google Maps). */
    private fun startNavigation(target: RouteInfo) {
        val geo = Uri.parse("geo:${target.destinationLat},${target.destinationLng}")
        try {
            carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, geo))
        } catch (e: Exception) {
            CarToast.makeText(carContext, "Không mở được ứng dụng dẫn đường", CarToast.LENGTH_LONG).show()
        }
    }
}
