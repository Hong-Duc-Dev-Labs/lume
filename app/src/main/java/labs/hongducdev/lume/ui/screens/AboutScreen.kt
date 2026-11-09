package labs.hongducdev.lume.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import labs.hongducdev.lume.ui.components.PullTitleHeader
import labs.hongducdev.lume.ui.components.rememberPullTitleHeaderState
import labs.hongducdev.lume.ui.modifiers.scrollMotionBlur
import labs.hongducdev.lume.ui.core.cards.SettingsItem
import labs.hongducdev.lume.ui.core.containers.RoundedCardContainer

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val context = LocalContext.current
    val headerState = rememberPullTitleHeaderState()
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(headerState.nestedScrollConnection)
                .scrollMotionBlur(lazyListState),
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = 150.dp,
                start = 16.dp,
                end = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PullTitleHeader(
                title = "About",
                caption = "ROM Custom Lume • Essentials Edition",
                state = headerState,
            )
        }

        item {
            RoundedCardContainer(title = "Thiết bị & Hệ điều hành") {
                SettingsItem(
                    title = "Thiết bị mục tiêu",
                    description = "Samsung Galaxy S10 5G (SM-G977B/N)",
                    icon = Icons.Rounded.Devices,
                    valueText = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
                    showChevron = false,
                )
                SettingsItem(
                    title = "Nền tảng Android",
                    description = "Tương thích Android 10+ (tối ưu Android 15-16, API 36)",
                    icon = Icons.Rounded.Security,
                    valueText = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    showChevron = false,
                )
            }
        }

        item {
            RoundedCardContainer(title = "Thông tin bản quyền") {
                SettingsItem(
                    title = "Tác giả & Phát triển",
                    description = "Nguyễn Hồng Đức (hongducdev)",
                    icon = Icons.Rounded.Info,
                    valueText = "hongducdev",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hongducdev"))
                        context.startActivity(intent)
                    },
                )
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}
