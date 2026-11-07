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
import com.hongducdev.toolkit.ui.LockscreenLunarScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    ToolkitApp(Modifier.safeDrawingPadding())
                }
            }
        }
    }
}

private enum class Screen { HOME, LOCKSCREEN_LUNAR }

@Composable
private fun ToolkitApp(modifier: Modifier = Modifier) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    when (screen) {
        Screen.HOME -> HomeScreen(modifier, onOpenLockscreenLunar = { screen = Screen.LOCKSCREEN_LUNAR })
        Screen.LOCKSCREEN_LUNAR -> LockscreenLunarScreen(modifier)
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, onOpenLockscreenLunar: () -> Unit) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Toolkit", style = MaterialTheme.typography.headlineMedium)
        FeatureCard(
            title = "Lịch âm màn hình khóa",
            description = "Hiển thị lịch âm Việt Nam trên màn hình khóa: trong dòng ngày nếu có root, " +
                "hoặc vẽ lên hình nền nếu không.",
            onClick = onOpenLockscreenLunar,
        )
    }
}

@Composable
private fun FeatureCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
