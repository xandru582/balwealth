package com.empiretycoon.game.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Registro composicional que mapea un identificador de widget (`String`)
 * a su `Rect` global en pantalla. Usado por el sistema de tutorial para
 * dibujar coachmarks y tooltips encima de elementos concretos.
 *
 * El registro es un `MutableState<Map<String, Rect>>`; al recomponer una
 * pantalla, los widgets vuelven a llamarse a `Modifier.anchor(id)` y el mapa
 * se mantiene consistente.
 */
val LocalAnchorRegistry = compositionLocalOf<MutableState<Map<String, Rect>>> {
    error("AnchorRegistry not provided. Envuelve la UI con AnchorRegistry { ... }.")
}

/**
 * Provee un `MutableState<Map<String, Rect>>` a la composición. Los widgets
 * descendientes usan `Modifier.anchor(id)` para registrarse.
 */
@Composable
fun AnchorRegistry(content: @Composable () -> Unit) {
    val state = remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    androidx.compose.runtime.CompositionLocalProvider(LocalAnchorRegistry provides state) {
        content()
    }
}

/**
 * Registra el `Rect` global del componente bajo el id dado. Si la composición
 * desaparece, la entrada permanece (la próxima ronda la sobrescribe). Esto
 * evita parpadeos al cambiar de pestaña.
 */
fun Modifier.anchor(id: String): Modifier = composed {
    val registryState = LocalAnchorRegistry.current
    onGloballyPositioned { coords ->
        val rect = coords.boundsInRoot()
        val current = registryState.value
        val previous = current[id]
        if (previous == null || previous != rect) {
            registryState.value = current + (id to rect)
        }
    }
}

/** Lee el rect actual de un id (o null si no se ha registrado todavía). */
@Composable
fun rememberAnchor(id: String?): Rect? {
    if (id == null) return null
    val registryState = LocalAnchorRegistry.current
    return registryState.value[id]
}
