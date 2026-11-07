package com.hongducdev.toolkit.utils

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType as ComposeHapticFeedbackType

enum class AppHapticMode { DISABLED, ENABLED, STRONGER }

object HapticUtil {
    val hapticMode = mutableStateOf(AppHapticMode.ENABLED)
    val isAppHapticsEnabled = mutableStateOf(true)

    private fun getVibrator(context: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    fun gate(
        context: Context,
        delegate: HapticFeedback,
    ): HapticFeedback =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: ComposeHapticFeedbackType) {
                if (!isAppHapticsEnabled.value) return
                if (hapticMode.value == AppHapticMode.STRONGER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    performCustomHaptic(context, composeStrength(hapticFeedbackType))
                } else {
                    delegate.performHapticFeedback(hapticFeedbackType)
                }
            }
        }

    private fun composeStrength(type: ComposeHapticFeedbackType): Float =
        when (type) {
            ComposeHapticFeedbackType.LongPress -> 0.7f
            ComposeHapticFeedbackType.TextHandleMove -> 0.3f
            else -> 0.5f
        }

    private fun playStronger(
        view: View,
        strength: Float,
    ): Boolean {
        if (hapticMode.value != AppHapticMode.STRONGER || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        performCustomHaptic(view.context, strength)
        return true
    }

    private fun performCustomHaptic(
        context: Context,
        strength: Float,
    ) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context)
        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val amplitude = (strength * 255).toInt().coerceIn(1, 255)
            val effect = VibrationEffect.createOneShot(15, amplitude)
            runCatching { vibrator.vibrate(effect) }
        }
    }

    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.5f)) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun performVirtualKeyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.7f)) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.3f)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun performHeavyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.85f)) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performSliderHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        if (playStronger(view, 0.25f)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
}
