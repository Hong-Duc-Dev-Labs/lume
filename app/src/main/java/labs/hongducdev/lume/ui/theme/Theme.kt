package labs.hongducdev.lume.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import labs.hongducdev.lume.utils.HapticUtil

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
    )

@Composable
fun ToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pitchBlackTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                val dynamicScheme =
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                if (darkTheme && pitchBlackTheme) {
                    dynamicScheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color.Black,
                    )
                } else {
                    dynamicScheme
                }
            }

            darkTheme -> {
                if (pitchBlackTheme) {
                    DarkColorScheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color.Black,
                    )
                } else {
                    DarkColorScheme
                }
            }

            else -> {
                LightColorScheme
            }
        }

    val context = LocalContext.current
    val gatedHaptics = HapticUtil.gate(context, LocalHapticFeedback.current)

    CompositionLocalProvider(LocalHapticFeedback provides gatedHaptics) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ToolkitTypography,
            shapes = ToolkitShapes,
            content = content,
        )
    }
}
