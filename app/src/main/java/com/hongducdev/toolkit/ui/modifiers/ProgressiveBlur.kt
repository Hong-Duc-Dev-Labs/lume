package com.hongducdev.toolkit.ui.modifiers

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

/**
 * Progressive edge blur: the content is recorded once, drawn sharp, and drawn again through a
 * native blur that is masked so it fades out toward the center. A translucent surface scrim
 * over the blurred edge keeps overlaid text readable, like sameerasw/essentials.
 */
fun Modifier.progressiveBlur(
    topHeightPx: Float,
    bottomHeightPx: Float,
    radiusPx: Float = 72f,
): Modifier =
    composed {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return@composed Modifier
        val scrim = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f)
        val sharpLayer = rememberGraphicsLayer()
        val blurLayer = rememberGraphicsLayer()
        val edgeLayer = rememberGraphicsLayer()

        drawWithContent {
            sharpLayer.record { this@drawWithContent.drawContent() }

            blurLayer.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)
            blurLayer.record { drawLayer(sharpLayer) }

            edgeLayer.compositingStrategy = CompositingStrategy.Offscreen
            edgeLayer.record {
                drawLayer(blurLayer)
                drawEdgeMaskAndScrim(topHeightPx, bottomHeightPx, scrim)
            }

            drawLayer(sharpLayer)
            drawLayer(edgeLayer)
        }
    }

private fun DrawScope.drawEdgeMaskAndScrim(
    topHeight: Float,
    bottomHeight: Float,
    scrim: Color,
) {
    val top = topHeight.coerceIn(0f, size.height)
    val bottom = bottomHeight.coerceIn(0f, size.height - top)

    // Keep the blurred pixels only inside the edge strips, fading toward the center.
    drawRect(
        color = Color.Black,
        topLeft = Offset(0f, top),
        size = Size(size.width, size.height - top - bottom),
        blendMode = BlendMode.Clear,
    )
    if (top > 0f) {
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Black, 0.45f to Color.Black, 1f to Color.Transparent, startY = 0f, endY = top),
            size = Size(size.width, top),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            brush = Brush.verticalGradient(0f to scrim, 0.45f to scrim, 1f to Color.Transparent, startY = 0f, endY = top),
            size = Size(size.width, top),
        )
    }
    if (bottom > 0f) {
        val start = size.height - bottom
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Black, 1f to Color.Black, startY = start, endY = size.height),
            topLeft = Offset(0f, start),
            size = Size(size.width, bottom),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            brush = Brush.verticalGradient(0f to Color.Transparent, 0.55f to scrim, 1f to scrim, startY = start, endY = size.height),
            topLeft = Offset(0f, start),
            size = Size(size.width, bottom),
        )
    }
}
