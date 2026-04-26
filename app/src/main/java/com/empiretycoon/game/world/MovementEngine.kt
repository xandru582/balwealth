package com.empiretycoon.game.world

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Vector de entrada del joystick (componentes en [-1, 1]). Magnitud > 1 se
 * normaliza para que las diagonales no sean más rápidas.
 */
data class MoveInput(val dx: Float, val dy: Float) {
    val isMoving: Boolean get() = abs(dx) > DEAD_ZONE || abs(dy) > DEAD_ZONE

    /** Vector normalizado si supera la deadzone, null en otro caso. */
    fun normalized(): Pair<Float, Float>? {
        val mag = sqrt(dx * dx + dy * dy)
        if (mag <= DEAD_ZONE) return null
        val s = if (mag > 1f) 1f / mag else 1f
        return (dx * s) to (dy * s)
    }

    companion object {
        const val DEAD_ZONE = 0.12f
        val ZERO = MoveInput(0f, 0f)
    }
}

/** Lo que el avatar tiene "delante" cuando se pulsa interactuar. */
data class InteractionRequest(
    val kind: TileInteractKind,
    val payload: String?
)

/**
 * Movimiento puro: no hace IO ni toca Compose. Recibe `WorldState` y devuelve
 * el siguiente. La velocidad se expresa en *baldosas / segundo* — coherente
 * con el sistema de coordenadas del [Avatar].
 */
object MovementEngine {

    private const val AVATAR_RADIUS = 0.30f  // colisión: caja en torno al centro
    private const val WALK_PHASE_HZ = 1.6f    // ciclos por segundo a vel. plena

    /**
     * Aplica un paso de simulación al mundo. Eje a eje para permitir deslizar
     * a lo largo de paredes. Actualiza también el `facing` (priorizando el
     * eje con mayor magnitud absoluta) y la fase de animación.
     */
    fun applyMovement(
        world: WorldState,
        input: MoveInput,
        deltaSeconds: Float,
        walkSpeed: Float = 4.5f
    ): WorldState {
        if (deltaSeconds <= 0f) return world
        val norm = input.normalized() ?: return world.copy(
            avatar = world.avatar.copy(walking = false)
        )
        val (nx, ny) = norm
        val avatar = world.avatar
        val grid = world.grid
        val step = walkSpeed * deltaSeconds

        // 1) eje X
        val targetX = avatar.x + nx * step
        val resolvedX = resolveAxis(
            grid = grid,
            from = avatar.x,
            to = targetX,
            otherAxis = avatar.y,
            isXAxis = true
        )

        // 2) eje Y (usa la X ya resuelta para colisión correcta en esquinas)
        val targetY = avatar.y + ny * step
        val resolvedY = resolveAxis(
            grid = grid,
            from = avatar.y,
            to = targetY,
            otherAxis = resolvedX,
            isXAxis = false
        )

        val movedX = resolvedX - avatar.x
        val movedY = resolvedY - avatar.y
        val moved = abs(movedX) > 1e-4f || abs(movedY) > 1e-4f

        // 3) facing: prioriza el eje del input dominante (no el movimiento real,
        //    así el jugador "encara" la pared aunque no avance).
        val newFacing = facingFor(nx, ny, avatar.facing)

        // 4) walkPhase: avanza proporcional al desplazamiento real
        val phaseAdv = if (moved) {
            val movedDist = sqrt(movedX * movedX + movedY * movedY)
            (movedDist / walkSpeed) * WALK_PHASE_HZ
        } else 0f
        val newPhase = ((avatar.walkPhase + phaseAdv) % 1f + 1f) % 1f

        return world.copy(
            avatar = avatar.copy(
                x = resolvedX,
                y = resolvedY,
                facing = newFacing,
                walking = moved,
                walkPhase = newPhase
            )
        )
    }

    /**
     * Devuelve la baldosa interactiva delante del avatar (si la hay), o
     * `null`. No muta el mundo: la lógica de "qué pasa" la decide el bridge
     * VM o un engine especializado (banco, tienda, NPC...) consumiendo el
     * [InteractionRequest].
     */
    fun interact(world: WorldState): InteractionRequest? {
        val (fx, fy) = world.avatar.frontTile()
        if (!world.grid.inBounds(fx, fy)) return null
        val tile = world.grid.fullTileAt(fx, fy)
        val kind = tile.interactKind ?: return null
        return InteractionRequest(kind = kind, payload = tile.interactPayload)
    }

    // -------- helpers --------

    private fun facingFor(nx: Float, ny: Float, fallback: Facing): Facing {
        if (abs(nx) < 1e-4f && abs(ny) < 1e-4f) return fallback
        return if (abs(nx) >= abs(ny)) {
            if (nx >= 0f) Facing.RIGHT else Facing.LEFT
        } else {
            if (ny >= 0f) Facing.DOWN else Facing.UP
        }
    }

    /**
     * Mueve a lo largo de un eje y revierte al borde de la baldosa más cercana
     * si la caja del avatar penetraría una baldosa no caminable.
     */
    private fun resolveAxis(
        grid: WorldGrid,
        from: Float,
        to: Float,
        otherAxis: Float,
        isXAxis: Boolean
    ): Float {
        if (to == from) return from
        val r = AVATAR_RADIUS
        // Esquinas que ocupará la caja del avatar
        val ox0 = (otherAxis - r)
        val ox1 = (otherAxis + r)
        val low = if (to > from) (to + r) else (to - r)
        val nx0 = if (isXAxis) low else ox0
        val nx1 = if (isXAxis) low else ox1
        val ny0 = if (isXAxis) ox0 else low
        val ny1 = if (isXAxis) ox1 else low
        val cells = listOf(
            nx0.toIntFloor() to ny0.toIntFloor(),
            nx1.toIntFloor() to ny0.toIntFloor(),
            nx0.toIntFloor() to ny1.toIntFloor(),
            nx1.toIntFloor() to ny1.toIntFloor()
        )
        val blocked = cells.any { (cx, cy) -> !grid.walkableAt(cx, cy) }
        if (!blocked) return to
        // Encaja contra el borde: si me muevo a la derecha, el centro queda
        // pegado al borde izquierdo de la baldosa bloqueante (cell.toFloat - r - eps).
        val eps = 1e-3f
        return if (to > from) {
            val cell = low.toIntFloor()
            (cell.toFloat() - r - eps).coerceAtLeast(from)
        } else {
            val cell = low.toIntFloor()
            (cell + 1f + r + eps).coerceAtMost(from)
        }
    }

    private fun Float.toIntFloor(): Int = if (this >= 0f) toInt() else toInt() - 1
}
