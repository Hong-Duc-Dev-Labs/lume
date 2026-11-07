package com.hongducdev.toolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hongducdev.toolkit.car.CarDashboardScreen
import com.hongducdev.toolkit.car.CarMapHttpConfig
import com.hongducdev.toolkit.ui.LockscreenLunarScreen
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo MapLibre và HTTP network client kháng lỗi SELinux/DNS sớm nhất
        MapLibre.getInstance(applicationContext)
        CarMapHttpConfig.initialize(applicationContext)
        MapLibre.setConnected(true)

        val startScreen = when (intent?.getStringExtra("screen")) {
            "car" -> Screen.CAR_DASHBOARD
            "lunar" -> Screen.LOCKSCREEN_LUNAR
            else -> Screen.HOME
        }

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    ToolkitApp(defaultScreen = startScreen)
                }
            }
        }
    }
}

private enum class Screen { HOME, LOCKSCREEN_LUNAR, CAR_DASHBOARD }

@Composable
private fun ToolkitApp(defaultScreen: Screen = Screen.HOME) {
    var screen by rememberSaveable { mutableStateOf(defaultScreen) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            onOpenLunar = { screen = Screen.LOCKSCREEN_LUNAR },
            onOpenCarDashboard = { screen = Screen.CAR_DASHBOARD }
        )
        Screen.LOCKSCREEN_LUNAR -> LockscreenLunarScreen(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        )
        Screen.CAR_DASHBOARD -> CarDashboardScreen(
            onExit = { screen = Screen.HOME }
        )
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenLunar: () -> Unit,
    onOpenCarDashboard: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Toolkit",
            style = MaterialTheme.typography.headlineMedium
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLunar)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Lịch âm màn hình khóa",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Hiển thị lịch âm Việt Nam trên màn hình khóa: trong dòng ngày nếu có root, hoặc vẽ lên hình nền nếu không.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenCarDashboard)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Car Dashboard (Chế độ lái xe)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Giao diện Android Auto Coolwalk: Bản đồ nhúng MapLibre định vị thời gian thực, điều khiển nhạc trực tiếp (Spotify/Zing MP3), đồng hồ tốc độ GPS và dock điều khiển.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
