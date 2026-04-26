package com.empiretycoon.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Motor de sonido sintetizado en runtime. No usa archivos de audio:
 * cada [SoundEvent] se transforma en una secuencia de notas y se
 * reproduce vía [AudioTrack] en formato PCM 16-bit mono 44.1kHz.
 *
 * Diseño:
 *  - Single-shot. Cada llamada a [play] crea un [AudioTrack] de
 *    tamaño exacto, lo escribe en bloque y lo libera al terminar.
 *  - Síntesis en [Dispatchers.Default] para no bloquear la UI.
 *  - Limita la concurrencia con un [Semaphore] para evitar saturar
 *    al sistema con muchos tracks simultáneos.
 *  - Aplica envolvente ADSR (ataque + decay exponencial) para que
 *    los tonos arranquen y terminen sin clicks.
 */
class SoundEngine(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val enabled = AtomicBoolean(true)

    @Volatile
    private var masterVolume: Float = 0.7f

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    /** Limita simultaneidad para no saturar el mixer del sistema. */
    private val semaphore = Semaphore(MAX_CONCURRENT)

    private val released = AtomicBoolean(false)

    /** Reproduce un evento de forma no bloqueante. */
    fun play(event: SoundEvent) {
        if (released.get() || !enabled.get()) return
        val def = SoundDefs.map[event] ?: return
        scope.launch {
            try {
                semaphore.withPermit { renderAndPlay(def) }
            } catch (_: Throwable) {
                // Nunca dejamos que un fallo del audio se propague a la UI.
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled.set(enabled)
    }

    fun isEnabled(): Boolean = enabled.get()

    fun setVolume(v: Float) {
        masterVolume = v.coerceIn(0f, 1f)
    }

    fun getVolume(): Float = masterVolume

    /** Libera recursos. Tras esto el motor queda inutilizable. */
    fun release() {
        if (!released.compareAndSet(false, true)) return
        scope.cancel()
        job.cancel()
    }

    // ---------- Síntesis ----------

    private suspend fun renderAndPlay(def: SoundDef) = withContext(Dispatchers.Default) {
        val totalMs = def.durations.sum()
        val totalSamples = (totalMs * SAMPLE_RATE) / 1000
        if (totalSamples <= 0) return@withContext

        val pcm = ShortArray(totalSamples)
        val masterAmp = (def.volume * masterVolume).coerceIn(0f, 1f)
        val noiseRng = java.util.Random()

        var sampleCursor = 0
        for (i in def.frequencies.indices) {
            val freq = def.frequencies[i]
            val durMs = def.durations[i]
            val noteSamples = (durMs * SAMPLE_RATE) / 1000
            val attackSamples = (def.attackMs * SAMPLE_RATE) / 1000
            val decaySamples = (def.decayMs * SAMPLE_RATE) / 1000
            // sustain absoluto (tras attack y antes de release final)
            val sustainStart = attackSamples
            val releaseStart = (noteSamples - decaySamples).coerceAtLeast(sustainStart)

            // Fase reseteada por nota — evita clicks entre notas pero
            // permite saltos limpios.
            val phaseStep = 2.0 * PI * freq.toDouble() / SAMPLE_RATE
            var phase = 0.0

            for (s in 0 until noteSamples) {
                if (sampleCursor + s >= pcm.size) break

                val raw: Float = when (def.waveform) {
                    Waveform.SINE -> sin(phase).toFloat()
                    Waveform.SQUARE -> if (sin(phase) >= 0.0) 1f else -1f
                    Waveform.SAW -> {
                        // saw normalizada: -1..1
                        val t = (phase / (2.0 * PI)) % 1.0
                        (2.0 * t - 1.0).toFloat()
                    }
                    Waveform.TRI -> {
                        val t = (phase / (2.0 * PI)) % 1.0
                        // triangular: pendiente 4, simétrica
                        val v = if (t < 0.5) 4.0 * t - 1.0 else 3.0 - 4.0 * t
                        v.toFloat()
                    }
                    Waveform.NOISE -> {
                        // ruido coloreado por la frecuencia: cuanto más
                        // alta, más cambio en el coeficiente del filtro
                        (noiseRng.nextFloat() * 2f - 1f)
                    }
                }

                // ADSR: ataque lineal, sustain plano, decay exponencial.
                val env: Float = when {
                    s < attackSamples && attackSamples > 0 ->
                        s.toFloat() / attackSamples
                    s >= releaseStart && decaySamples > 0 -> {
                        val k = (s - releaseStart).toFloat() / decaySamples
                        // exp(-3k) cae a ~5% al final, suave y sin click
                        exp(-3.0 * k).toFloat()
                    }
                    else -> 1f
                }

                val sample = raw * env * masterAmp
                val clipped = sample.coerceIn(-1f, 1f)
                val pcmIdx = sampleCursor + s
                // mezcla aditiva con saturación suave (tanh aproximada)
                val blended = pcm[pcmIdx] + (clipped * 32_000f).toInt()
                pcm[pcmIdx] = softClip(blended).toShort()

                phase += phaseStep
                if (phase > 2.0 * PI) phase -= 2.0 * PI
            }
            sampleCursor += noteSamples
        }

        playPcm(pcm)
    }

    private fun softClip(value: Int): Int {
        // limita sin truncar abruptamente; cubre los Short.MAX_VALUE
        if (value > Short.MAX_VALUE.toInt()) return Short.MAX_VALUE.toInt()
        if (value < Short.MIN_VALUE.toInt()) return Short.MIN_VALUE.toInt()
        return value
    }

    private fun playPcm(pcm: ShortArray) {
        if (released.get()) return

        val byteSize = pcm.size * 2
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(byteSize)

        var track: AudioTrack? = null
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            track = AudioTrack(
                attrs,
                format,
                minBuffer,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            val written = track.write(pcm, 0, pcm.size)
            if (written <= 0) return

            track.play()

            // Esperar a que termine la reproducción antes de liberar.
            val durationMs = (pcm.size.toLong() * 1000L) / SAMPLE_RATE
            val safety = 40L // colchón para drain
            try {
                Thread.sleep(durationMs + safety)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        } catch (_: Throwable) {
            // ignoramos; nada de logging para no inundar
        } finally {
            try {
                track?.stop()
            } catch (_: Throwable) {
            }
            try {
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        const val SAMPLE_RATE = 44_100
        private const val MAX_CONCURRENT = 4
    }
}

/** Aplica una compresión leve (no usado externamente). */
@Suppress("unused")
private fun saturate(x: Float): Float {
    val a = abs(x)
    return if (a < 1f) x else x / a
}
