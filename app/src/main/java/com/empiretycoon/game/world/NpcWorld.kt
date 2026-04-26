package com.empiretycoon.game.world

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * NPC simple en el mundo 2D. Camina haciendo loops alrededor de un punto base.
 * Está vinculado a un NPC del catálogo del modelo principal por [npcId], pero
 * NO duplica su personalidad — solo mantiene posición, dirección y fase.
 */
@Serializable
data class NpcWalker(
    val id: String,
    val npcId: String?,
    val homeX: Float,
    val homeY: Float,
    val x: Float = homeX,
    val y: Float = homeY,
    val facing: Facing = Facing.DOWN,
    val walkPhase: Float = 0f,
    val orbitRadius: Float = 3f,
    val orbitSpeed: Float = 0.18f,
    val seedColor: Long = 0L
)

@Serializable
data class NpcWorldState(
    val walkers: List<NpcWalker> = emptyList(),
    val activeDialogueNpcId: String? = null,
    val activeDialogueLine: String = ""
)

object NpcWorldEngine {

    /** Inicializa con N NPCs aleatorios sobre tiles caminables. */
    fun ensurePopulated(state: NpcWorldState, grid: WorldGrid, target: Int = 12, rng: Random = Random(42)): NpcWorldState {
        if (state.walkers.size >= target) return state
        val walkers = state.walkers.toMutableList()
        var attempts = 0
        while (walkers.size < target && attempts < target * 20) {
            attempts++
            val rx = rng.nextInt(grid.width).toFloat() + 0.5f
            val ry = rng.nextInt(grid.height).toFloat() + 0.5f
            if (!grid.walkableAt(rx.toInt(), ry.toInt())) continue
            walkers.add(
                NpcWalker(
                    id = "walker_${walkers.size}_${System.nanoTime()}",
                    npcId = null,
                    homeX = rx,
                    homeY = ry,
                    seedColor = rng.nextLong(),
                    orbitRadius = (1.5f + rng.nextFloat() * 3.5f),
                    orbitSpeed = 0.08f + rng.nextFloat() * 0.18f
                )
            )
        }
        return state.copy(walkers = walkers)
    }

    /** Avanza todos los walkers en una órbita suave en torno a su home. */
    fun tick(state: NpcWorldState, grid: WorldGrid, deltaSec: Float, currentTick: Long): NpcWorldState {
        val updated = state.walkers.map { w ->
            val phase = (currentTick * 0.05f + w.id.hashCode().toFloat() * 0.001f) * w.orbitSpeed
            val nx = w.homeX + cos(phase) * w.orbitRadius
            val ny = w.homeY + sin(phase) * w.orbitRadius
            val moveX = nx - w.x
            val moveY = ny - w.y
            val newFacing = when {
                abs(moveX) > abs(moveY) && moveX > 0 -> Facing.RIGHT
                abs(moveX) > abs(moveY) -> Facing.LEFT
                moveY > 0 -> Facing.DOWN
                else -> Facing.UP
            }
            val targetX = if (grid.walkableAt(nx.toInt(), w.y.toInt())) nx else w.x
            val targetY = if (grid.walkableAt(w.x.toInt(), ny.toInt())) ny else w.y
            w.copy(
                x = targetX,
                y = targetY,
                facing = newFacing,
                walkPhase = (w.walkPhase + deltaSec * 1.5f) % 1f
            )
        }
        return state.copy(walkers = updated)
    }

    /** Devuelve el walker más cercano al avatar dentro del radio dado. */
    fun nearest(state: NpcWorldState, avatarX: Float, avatarY: Float, radius: Float = 1.5f): NpcWalker? {
        return state.walkers
            .map { it to (it.x - avatarX) * (it.x - avatarX) + (it.y - avatarY) * (it.y - avatarY) }
            .filter { it.second <= radius * radius }
            .minByOrNull { it.second }
            ?.first
    }
}
