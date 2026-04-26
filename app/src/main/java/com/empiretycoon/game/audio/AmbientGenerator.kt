package com.empiretycoon.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generador de ambient procedural muy ligero. La filosofía es:
 *
 *  - Generar un *drone* (sostenido grave) suavemente filtrado en
 *    bloques de 250 ms y enviarlos en streaming a un único
 *    [AudioTrack] en modo STREAM.
 *  - Cada ~30 s mover sutilmente la nota base entre {A2, C3, D3}
 *    para crear una sensación de progresión sin melodía.
 *  - Cada 8-16 s añadir una "campanita" aleatoria que se mezcla
 *    encima del drone.
 *  - Hacer fade-in/fade-out de ~1.5 s al iniciar / parar.
 */
class AmbientPlayer(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val scope: CoroutineScope
) {

    private val running = AtomicBoolean(false)

    /** Cuando es true, el bucle está en fade-out activo y se cerrará solo. */
    private val stopping = AtomicBoolean(false)

    private var loopJob: Job? = null

    @Volatile
    private var volume: Float = 0.35f

    @Volatile
    private var track: AudioTrack? = null

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        try {
            track?.setVolume(volume)
        } catch (_: Throwable) {
        }
    }

    fun isRunning(): Boolean = running.get()

    /** Arranca el bucle si no estaba ya activo. Idempotente. */
    fun start() {
        if (!running.compareAndSet(false, true)) return
        stopping.set(false)
        loopJob = scope.launch(Dispatchers.Default) { runLoop() }
    }

    /**
     * Marca fade-out. El bucle interno irá bajando el volumen y se
     * cerrará. No bloquea al caller.
     */
    fun stop() {
        if (!running.get()) return
        stopping.set(true)
    }

    /**
     * Cancela inmediatamente sin fade. Llamar al destruir.
     */
    fun release() {
        stopping.set(true)
        running.set(false)
        loopJob?.cancel()
        loopJob = null
        try {
            track?.stop()
        } catch (_: Throwable) {
        }
        try {
            track?.release()
        } catch (_: Throwable) {
        }
        track = null
    }

    // ---------- bucle interno ----------

    private suspend fun runLoop() = withContext(Dispatchers.Default) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(BLOCK_SAMPLES * 2 * 2)

        val tk = try {
            AudioTrack(
                attrs, format, minBuf,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } catch (_: Throwable) {
            running.set(false)
            return@withContext
        }
        track = tk

        try {
            tk.setVolume(0f)
            tk.play()
        } catch (_: Throwable) {
            tk.release()
            track = null
            running.set(false)
            return@withContext
        }

        val rng = Random(System.nanoTime())
        val tonic = floatArrayOf(110f, 130.81f, 146.83f) // A2, C3, D3
        var tonicIdx = 0
        var tonicChangeIn = TONIC_CHANGE_BLOCKS
        var bellCountdown = rng.nextInt(BELL_MIN_BLOCKS, BELL_MAX_BLOCKS + 1)

        var phaseFund = 0.0
        var phaseFifth = 0.0
        var lpState = 0f

        val buf = ShortArray(BLOCK_SAMPLES)

        var fadeInRemaining = FADE_BLOCKS
        var fadeOutRemaining = -1

        try {
            while (isActive) {
                ensureActive()

                if (stopping.get() && fadeOutRemaining < 0) {
                    fadeOutRemaining = FADE_BLOCKS
                }

                val gain: Float = when {
                    fadeOutRemaining >= 0 -> {
                        val g = (fadeOutRemaining.toFloat() / FADE_BLOCKS).coerceIn(0f, 1f)
                        fadeOutRemaining -= 1
                        g * volume
                    }
                    fadeInRemaining > 0 -> {
                        val g = 1f - (fadeInRemaining.toFloat() / FADE_BLOCKS).coerceIn(0f, 1f)
                        fadeInRemaining -= 1
                        g * volume
                    }
                    else -> volume
                }

                try { tk.setVolume(gain) } catch (_: Throwable) {}

                val baseFreq = tonic[tonicIdx].toDouble()
                val fifthFreq = baseFreq * 1.4983
                val stepFund = 2.0 * PI * baseFreq / SAMPLE_RATE
                val stepFifth = 2.0 * PI * fifthFreq / SAMPLE_RATE

                val bellThisBlock = bellCountdown <= 0 && fadeOutRemaining < 0
                val bellFreq: Int? = if (bellThisBlock) {
                    bellCountdown = rng.nextInt(BELL_MIN_BLOCKS, BELL_MAX_BLOCKS + 1)
                    BELL_NOTES[rng.nextInt(BELL_NOTES.size)]
                } else null
                val bellStep = if (bellFreq != null) {
                    2.0 * PI * bellFreq.toDouble() / SAMPLE_RATE
                } else 0.0
                var bellPhase = 0.0
                val bellDecaySamples = (BELL_DECAY_MS * SAMPLE_RATE) / 1000

                for (i in 0 until BLOCK_SAMPLES) {
                    val tFund = (phaseFund / (2.0 * PI)) % 1.0
                    val sawFund = (2.0 * tFund - 1.0).toFloat()
                    val tFifth = (phaseFifth / (2.0 * PI)) % 1.0
                    val sawFifth = (2.0 * tFifth - 1.0).toFloat() * 0.5f

                    var s = (sawFund + sawFifth) * 0.45f

                    // Filtro paso-bajo de un polo (~1 kHz cutoff dependiendo de a)
                    val a = 0.04f
                    lpState += a * (s - lpState)
                    s = lpState

                    if (bellFreq != null && i < bellDecaySamples) {
                        val k = i.toFloat() / bellDecaySamples
                        val env = exp(-3.5 * k).toFloat()
                        val bell = sin(bellPhase).toFloat() * env * 0.18f
                        s += bell
                        bellPhase += bellStep
                    }

                    val clipped = (s.coerceIn(-1f, 1f) * 28_000f).toInt()
                    buf[i] = clipped.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()

                    phaseFund += stepFund
                    phaseFifth += stepFifth
                    if (phaseFund > 2.0 * PI) phaseFund -= 2.0 * PI
                    if (phaseFifth > 2.0 * PI) phaseFifth -= 2.0 * PI
                }

                try {
                    val written = tk.write(buf, 0, buf.size)
                    if (written < 0) break
                } catch (_: Throwable) {
                    break
                }

                bellCountdown -= 1
                tonicChangeIn -= 1
                if (tonicChangeIn <= 0) {
                    tonicChangeIn = TONIC_CHANGE_BLOCKS
                    tonicIdx = (tonicIdx + 1) % tonic.size
                }

                if (fadeOutRemaining == 0) break
            }
        } finally {
            try { tk.stop() } catch (_: Throwable) {}
            try { tk.release() } catch (_: Throwable) {}
            if (track === tk) track = null
            running.set(false)
            stopping.set(false)
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22_050               // ahorro CPU vs 44.1k
        private const val BLOCK_SAMPLES = SAMPLE_RATE / 4    // 250 ms
        private const val FADE_BLOCKS = 6                    // ~1.5 s
        private const val TONIC_CHANGE_BLOCKS = 120          // ~30 s
        private const val BELL_MIN_BLOCKS = 32               // ~8 s
        private const val BELL_MAX_BLOCKS = 64               // ~16 s
        private const val BELL_DECAY_MS = 1_400

        private val BELL_NOTES = intArrayOf(
            659, 784, 988, 1175 // E5, G5, B5, D6
        )
    }
}
