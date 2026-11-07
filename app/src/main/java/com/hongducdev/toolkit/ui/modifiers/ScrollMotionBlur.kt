package com.hongducdev.toolkit.ui.modifiers

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import org.intellij.lang.annotations.Language
import kotlin.math.abs

@Language("AGSL")
private const val DIRECTIONAL_BLUR_AGSL = """
    uniform shader composable;
    uniform float2 resolution;
    uniform float scrollVelocity;
    uniform float isHorizontal;

    half4 main(float2 fragCoord) {
        const int SAMPLES = 10;
        half4 color = half4(0.0);
        float totalWeight = 0.0;
        float blurMagnitude = clamp(scrollVelocity * 22.0, -60.0, 60.0);

        for (int i = 0; i < SAMPLES; i++) {
            float offset = (float(i) / float(SAMPLES - 1) - 0.5) * blurMagnitude;
            float2 sampleCoord;
            if (isHorizontal > 0.5) {
                sampleCoord = float2(clamp(fragCoord.x + offset, 0.0, resolution.x), fragCoord.y);
            } else {
                sampleCoord = float2(fragCoord.x, clamp(fragCoord.y + offset, 0.0, resolution.y));
            }
            float weight = 1.0 - abs(offset / (abs(blurMagnitude) + 0.001)) * 0.5;
            color += composable.eval(sampleCoord) * weight;
            totalWeight += weight;
        }
        return color / totalWeight;
    }
"""

/** Directional motion blur driven by the scroll velocity of a pager (horizontal). */
fun Modifier.scrollMotionBlur(pagerState: PagerState): Modifier =
    composed {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed Modifier
        val velocity = remember { Animatable(0f) }
        LaunchedEffect(pagerState) {
            trackVelocity(velocity, { pagerState.isScrollInProgress }) {
                (pagerState.currentPage + pagerState.currentPageOffsetFraction) * 400f
            }
        }
        scrollBlurLayer(velocity, isHorizontal = true)
    }

/** Directional motion blur driven by the scroll velocity of a lazy list (vertical). */
fun Modifier.scrollMotionBlur(listState: LazyListState): Modifier =
    composed {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@composed Modifier
        val velocity = remember { Animatable(0f) }
        LaunchedEffect(listState) {
            trackVelocity(velocity, { listState.isScrollInProgress }) {
                listState.firstVisibleItemIndex * 80f + listState.firstVisibleItemScrollOffset
            }
        }
        scrollBlurLayer(velocity, isHorizontal = false)
    }

private suspend fun trackVelocity(
    velocity: Animatable<Float, *>,
    isScrolling: () -> Boolean,
    position: () -> Float,
) {
    var prevPosition = position()
    var prevTimeNanos = 0L
    while (true) {
        var next = 0f
        withFrameNanos { frameNanos ->
            val current = position()
            if (prevTimeNanos != 0L) {
                val dtMs = (frameNanos - prevTimeNanos) / 1_000_000f
                if (dtMs in 1f..100f) {
                    val delta = current - prevPosition
                    val target = (delta / dtMs).coerceIn(-3f, 3f)
                    next =
                        if (abs(delta) > 0.1f && isScrolling()) {
                            velocity.value * 0.35f + target * 0.65f
                        } else {
                            (velocity.value * 0.45f).let { if (abs(it) < 0.01f) 0f else it }
                        }
                }
            }
            prevPosition = current
            prevTimeNanos = frameNanos
        }
        velocity.snapTo(next)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Modifier.scrollBlurLayer(
    velocity: Animatable<Float, *>,
    isHorizontal: Boolean,
): Modifier =
    composed {
        val shader = remember { RuntimeShader(DIRECTIONAL_BLUR_AGSL) }
        graphicsLayer {
            val v = velocity.value
            renderEffect =
                if (abs(v) > 0.05f) {
                    shader.setFloatUniform("resolution", size.width, size.height)
                    shader.setFloatUniform("scrollVelocity", v)
                    shader.setFloatUniform("isHorizontal", if (isHorizontal) 1f else 0f)
                    RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
                } else {
                    null
                }
        }
    }
