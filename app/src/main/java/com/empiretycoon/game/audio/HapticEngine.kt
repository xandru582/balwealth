package com.empiretycoon.game.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Catálogo de efectos hápticos. Cada uno define un patrón distinto
 * de [VibrationEffect] que se ejecuta en el [HapticEngine].
 */
enum class HapticEffect {
    LIGHT_TAP,
    MEDIUM_TAP,
    HEAVY_TAP,
    SUCCESS,
    ERROR,
    WARNING,
    GESTURE,
    LEVEL_UP,
    ACHIEVEMENT,
    COIN,
    EXPLOSION,
    PURCHASE
}

/**
 * Motor de vibración. Usa [VibratorManager] en API 31+ y [Vibrator]
 * deprecado como fallback. Si el dispositivo no tiene actuador o no
 * soporta amplitudes, hace un degradación silenciosa.
 */
class HapticEngine(context: Context) {

    private val enabled = AtomicBoolean(true)

    private val vibrator: Vibrator? = run {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private val supportsAmplitude: Boolean =
        try { vibrator?.hasAmplitudeControl() == true } catch (_: Throwable) { false }

    fun setEnabled(enabled: Boolean) {
        this.enabled.set(enabled)
        if (!enabled) {
            try { vibrator?.cancel() } catch (_: Throwable) {}
        }
    }

    fun isEnabled(): Boolean = enabled.get()

    /**
     * Reproduce el efecto. Idempotente y seguro de llamar muchas
     * veces seguidas — los efectos cortos cancelan automáticamente
     * cualquier vibración previa al lanzar uno nuevo.
     */
    fun perform(effect: HapticEffect) {
        if (!enabled.get()) return
        val v = vibrator ?: return
        try {
            val ve = buildEffect(effect) ?: return
            v.cancel()
            v.vibrate(ve)
        } catch (_: Throwable) {
            // graceful no-op
        }
    }

    private fun buildEffect(effect: HapticEffect): VibrationEffect? {
        return when (effect) {
            // Pulsos simples — usar createOneShot con amplitud si se soporta.
            HapticEffect.LIGHT_TAP -> oneShot(durationMs = 14, amplitude = 60)
            HapticEffect.MEDIUM_TAP -> oneShot(durationMs = 22, amplitude = 130)
            HapticEffect.HEAVY_TAP -> oneShot(durationMs = 38, amplitude = 220)

            HapticEffect.SUCCESS -> waveform(
                timings = longArrayOf(0, 22, 60, 22),
                amplitudes = intArrayOf(0, 160, 0, 200)
            )

            HapticEffect.ERROR -> oneShot(durationMs = 200, amplitude = 220)

            HapticEffect.WARNING -> waveform(
                timings = longArrayOf(0, 30, 60, 30),
                amplitudes = intArrayOf(0, 200, 0, 200)
            )

            HapticEffect.GESTURE -> oneShot(durationMs = 12, amplitude = 80)

            // Tres pulsos ascendentes en intensidad.
            HapticEffect.LEVEL_UP -> waveform(
                timings = longArrayOf(0, 30, 60, 40, 60, 60),
                amplitudes = intArrayOf(0, 120, 0, 180, 0, 240)
            )

            // Logro: tres pulsos crecientes + cierre fuerte.
            HapticEffect.ACHIEVEMENT -> waveform(
                timings = longArrayOf(0, 25, 50, 35, 50, 45, 70, 90),
                amplitudes = intArrayOf(0, 110, 0, 160, 0, 210, 0, 255)
            )

            HapticEffect.COIN -> waveform(
                timings = longArrayOf(0, 14, 28, 14),
                amplitudes = intArrayOf(0, 130, 0, 90)
            )

            HapticEffect.EXPLOSION -> waveform(
                timings = longArrayOf(0, 60, 30, 80, 30, 100),
                amplitudes = intArrayOf(0, 255, 0, 200, 0, 150)
            )

            HapticEffect.PURCHASE -> waveform(
                timings = longArrayOf(0, 18, 30, 28),
                amplitudes = intArrayOf(0, 140, 0, 200)
            )
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect {
        val a = amplitude.coerceIn(1, 255)
        return if (supportsAmplitude) {
            VibrationEffect.createOneShot(durationMs, a)
        } else {
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    private fun waveform(timings: LongArray, amplitudes: IntArray): VibrationEffect {
        require(timings.size == amplitudes.size) { "timings y amplitudes deben tener el mismo tamaño" }
        return if (supportsAmplitude) {
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            // Fallback: ignorar amplitudes, sólo timings.
            VibrationEffect.createWaveform(timings, -1)
        }
    }
}
