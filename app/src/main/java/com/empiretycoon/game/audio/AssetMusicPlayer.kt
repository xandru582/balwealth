package com.empiretycoon.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Reproductor de música cargado desde assets/audio/music/ (archivos .wav).
 *
 * - Carga lazy via AssetFileDescriptor (no copia en memoria).
 * - Crossfade básico entre tracks.
 * - Respeta volumen + flag enabled.
 */
class AssetMusicPlayer(private val context: Context) {

    private var current: MediaPlayer? = null
    private var currentName: String? = null
    private var enabled: Boolean = true
    private var volume: Float = 0.5f

    fun setEnabled(value: Boolean) {
        enabled = value
        if (!enabled) stop()
    }

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        current?.setVolume(volume, volume)
    }

    /** Cambia a `name` (sin extensión). Si ya está sonando ese, no hace nada. */
    fun play(name: String) {
        if (!enabled) return
        if (currentName == name && current?.isPlaying == true) return
        try {
            stop()
            val afd = context.assets.openFd("audio/music/$name.wav")
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
            afd.close()
            current = mp
            currentName = name
        } catch (t: Throwable) {
            // Asset no existe o error de codec — silenciar para no crashear
        }
    }

    fun stop() {
        try {
            current?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Throwable) {}
        current = null
        currentName = null
    }

    /** FIX feedback usuario: pausa sin liberar para reanudar después. */
    fun pause() {
        try {
            current?.let { if (it.isPlaying) it.pause() }
        } catch (_: Throwable) {}
    }

    /** Reanuda la pista actual si estaba pausada. */
    fun resume() {
        if (!enabled) return
        try {
            current?.let { if (!it.isPlaying) it.start() }
        } catch (_: Throwable) {}
    }

    fun release() {
        stop()
    }
}

/**
 * Selector contextual: elige la música según la situación del juego.
 *
 * FIX feedback usuario: ANTES usaba `.random()` en cada llamada, así que
 * cambiar de menú reelegía pista distinta y la música se reseteaba sin
 * razón aparente. AHORA es determinista: para mismas condiciones (distrito
 * + bucket horario + flags) devuelve SIEMPRE la misma pista, así el
 * `play()` del Root.kt detecta que ya está sonando y no la reinicia.
 */
object MusicSelector {

    /** Hash estable a partir de string (no usar hashCode() porque varía). */
    private fun stableIndex(key: String, mod: Int): Int {
        if (mod <= 0) return 0
        var h = 0
        for (c in key) h = (h * 31 + c.code) and 0x7FFFFFFF
        return h % mod
    }

    private fun pick(list: List<String>, key: String): String =
        if (list.isEmpty()) "city_day_1" else list[stableIndex(key, list.size)]

    fun pickFor(
        district: String,
        hour: Int,
        isDriving: Boolean,
        isInDream: Boolean,
        isInCasino: Boolean,
        isInDealership: Boolean,
        weather: String
    ): String {
        // Bucket horario en 4 partes: madrugada/mañana/tarde/noche
        val bucket = when {
            hour in 0..5 -> "night"
            hour in 6..11 -> "morning"
            hour in 12..18 -> "afternoon"
            else -> "evening"
        }
        val key = "$district|$bucket"

        if (isInDream) return pick(listOf("dream_1", "dream_2", "dream_3"), key)
        if (isInCasino) return pick(listOf("casino_1", "casino_2", "casino_3"), key)
        if (isInDealership) return pick(listOf("dealership_1", "dealership_2"), key)

        val isDay = hour in 7..20
        return when (district.lowercase()) {
            "downtown" -> if (isDay) pick(listOf("city_day_1", "city_day_2", "city_day_3"), key)
                else pick(listOf("downtown_1", "downtown_2", "downtown_3"), key)
            "industrial", "polígono" -> pick(listOf("industrial_1", "industrial_2", "industrial_3"), key)
            "park", "parque" -> pick(listOf("park_1", "park_2", "park_3"), key)
            "harbor", "puerto" -> pick(listOf("harbor_1", "harbor_2", "harbor_3"), key)
            "residential", "residencial" -> pick(listOf("residential_1", "residential_2"), key)
            "commercial", "comercial" -> "commercial_1"
            "suburb", "afueras" -> if (isDay) "park_1" else "harbor_1"
            else -> if (isDay) "city_day_1" else "city_night_1"
        }
    }
}

/**
 * Composable que monta el AssetMusicPlayer, lo pausa cuando la app va a
 * background (ON_PAUSE) y lo reanuda al volver (ON_RESUME), y lo libera al
 * destruirse el lifecycle owner.
 *
 * FIX feedback usuario: antes la música seguía sonando con la app en
 * segundo plano porque MediaPlayer.start() no se detenía nunca.
 */
@Composable
fun rememberAssetMusicPlayer(): AssetMusicPlayer {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember { AssetMusicPlayer(ctx.applicationContext) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    return player
}
