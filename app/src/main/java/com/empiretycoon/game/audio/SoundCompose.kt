package com.empiretycoon.game.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext

/**
 * CompositionLocals que distribuyen los engines a toda la jerarquía
 * Compose. Si no hay [SoundProvider] arriba, el valor es null y los
 * helpers degradan silenciosamente.
 */
val LocalSoundEngine = staticCompositionLocalOf<SoundEngine?> { null }
val LocalHapticEngine = staticCompositionLocalOf<HapticEngine?> { null }

/**
 * Crea ambos engines y los provee al árbol Compose. Libera recursos
 * cuando el Composable sale del árbol (cambio de proceso, finish…).
 *
 * El parámetro [context] es opcional: si se omite, usa [LocalContext].
 */
@Composable
fun SoundProvider(
    context: android.content.Context? = null,
    content: @Composable () -> Unit
) {
    val ctx = context ?: LocalContext.current
    val sound = remember(ctx) { SoundEngine(ctx.applicationContext) }
    val haptic = remember(ctx) { HapticEngine(ctx.applicationContext) }

    DisposableEffect(sound, haptic) {
        onDispose {
            sound.release()
        }
    }

    CompositionLocalProvider(
        LocalSoundEngine provides sound,
        LocalHapticEngine provides haptic
    ) {
        content()
    }
}

/**
 * Versión "click + sound + haptic" de [Modifier.clickable].
 *
 * - Toca el [SoundEvent] indicado (si hay [LocalSoundEngine]).
 * - Dispara el [HapticEffect] (si hay [LocalHapticEngine]).
 * - Llama a [onClick] al final.
 *
 * Mantiene la indicación visual por defecto del tema, así que es
 * apto para superficies y filas que no traen su propio ripple.
 */
fun Modifier.clickWithSound(
    sound: SoundEvent? = SoundEvent.BUTTON_CLICK,
    haptic: HapticEffect? = HapticEffect.LIGHT_TAP,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit
): Modifier = composed {
    val soundEngine = LocalSoundEngine.current
    val hapticEngine = LocalHapticEngine.current
    this.clickable(
        enabled = enabled,
        onClickLabel = onClickLabel
    ) {
        if (sound != null) soundEngine?.play(sound)
        if (haptic != null) hapticEngine?.perform(haptic)
        onClick()
    }
}

/**
 * Variante sin indicación visual (útil sobre componentes que ya
 * pintan su propia respuesta táctil, como [androidx.compose.material3.Button]).
 * Se usa principalmente para decorar `Modifier`s en sitios donde
 * no queremos un segundo ripple.
 */
fun Modifier.tapWithSound(
    sound: SoundEvent? = SoundEvent.BUTTON_CLICK,
    haptic: HapticEffect? = HapticEffect.LIGHT_TAP,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val soundEngine = LocalSoundEngine.current
    val hapticEngine = LocalHapticEngine.current
    val interactionSource = remember { MutableInteractionSource() }
    this.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled
    ) {
        if (sound != null) soundEngine?.play(sound)
        if (haptic != null) hapticEngine?.perform(haptic)
        onClick()
    }
}

/**
 * Helper de uso directo desde un onClick existente. Útil cuando no
 * quieres reemplazar el modifier:
 * ```
 *   Button(onClick = playSoundThen(SoundEvent.MARKET_BUY) { vm.buy(...) })
 * ```
 */
@Composable
fun playSoundThen(
    sound: SoundEvent? = SoundEvent.BUTTON_CLICK,
    haptic: HapticEffect? = HapticEffect.LIGHT_TAP,
    onClick: () -> Unit
): () -> Unit {
    val soundEngine = LocalSoundEngine.current
    val hapticEngine = LocalHapticEngine.current
    return {
        if (sound != null) soundEngine?.play(sound)
        if (haptic != null) hapticEngine?.perform(haptic)
        onClick()
    }
}
