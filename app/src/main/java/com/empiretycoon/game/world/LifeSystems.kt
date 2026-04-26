package com.empiretycoon.game.world

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================
//                           MASCOTAS
// =====================================================================

enum class PetSpecies(val displayName: String, val emoji: String, val cost: Double, val happinessPerDay: Int) {
    DOG("Perro", "🐕", 200.0, 4),
    CAT("Gato", "🐈", 150.0, 3),
    BIRD("Loro", "🦜", 80.0, 2),
    HAMSTER("Hámster", "🐹", 30.0, 1),
    RABBIT("Conejo", "🐰", 60.0, 2),
    FOX("Zorro", "🦊", 800.0, 5),
    DRAGON("Dragoncito", "🐉", 5_000.0, 10)
}

@Serializable
data class Pet(
    val id: String,
    val species: String,
    val name: String,
    val x: Float,
    val y: Float,
    val facing: String = Facing.DOWN.name,
    val walkPhase: Float = 0f,
    val hunger: Int = 50,         // 0..100, baja con tiempo
    val happiness: Int = 70,
    val lastFedTick: Long = 0L
) {
    fun spec(): PetSpecies = runCatching { PetSpecies.valueOf(species) }.getOrDefault(PetSpecies.DOG)
}

@Serializable
data class PetsState(
    val owned: List<Pet> = emptyList(),
    val activePetId: String? = null   // mascota que te sigue actualmente
) {
    fun active(): Pet? = owned.find { it.id == activePetId }
}

object PetEngine {
    /**
     * Tick: la mascota activa sigue al avatar suavemente.
     *
     * FIX BUG-03-PET-01: si `grid != null`, el movimiento respeta walkable —
     * la mascota nunca cruza paredes/agua. Si la celda objetivo no es
     * caminable, mantiene posición pero igual actualiza facing/animación.
     */
    fun tick(
        state: PetsState,
        avatarX: Float,
        avatarY: Float,
        deltaSec: Float,
        currentTick: Long,
        grid: WorldGrid? = null
    ): PetsState {
        val active = state.active() ?: return state
        val dx = avatarX - active.x
        val dy = avatarY - active.y
        val dist = hypot(dx, dy)
        if (dist < 0.6f) return state.copy(
            owned = state.owned.map { if (it.id == active.id) it.copy(walkPhase = 0f) else it }
        )
        val speed = (dist * 1.5f).coerceAtMost(4.0f)
        val tnx = active.x + (dx / dist) * speed * deltaSec
        val tny = active.y + (dy / dist) * speed * deltaSec

        // Walkable check: ejes separados para permitir slide contra muros.
        val (nx, ny) = if (grid == null) {
            tnx to tny
        } else {
            val canX = grid.walkableAt(tnx.toInt(), active.y.toInt())
            val canY = grid.walkableAt(active.x.toInt(), tny.toInt())
            val rx = if (canX) tnx else active.x
            val ry = if (canY) tny else active.y
            // si tampoco se puede en ninguno, teleport relativo: tirón hacia el avatar
            // a la celda walkable más cercana — anti-stuck.
            if (!canX && !canY && dist > 4f) {
                val fx = (avatarX - dx / dist).toInt()
                val fy = (avatarY - dy / dist).toInt()
                if (grid.walkableAt(fx, fy)) fx.toFloat() to fy.toFloat() else rx to ry
            } else rx to ry
        }

        val newFacing = when {
            abs(dx) > abs(dy) && dx > 0 -> Facing.RIGHT
            abs(dx) > abs(dy) -> Facing.LEFT
            dy > 0 -> Facing.DOWN
            else -> Facing.UP
        }
        val newHunger = if (currentTick % 60 == 0L) (active.hunger - 1).coerceAtLeast(0)
            else active.hunger
        return state.copy(
            owned = state.owned.map {
                if (it.id == active.id) it.copy(
                    x = nx, y = ny,
                    facing = newFacing.name,
                    walkPhase = (it.walkPhase + deltaSec * 2f) % 1f,
                    hunger = newHunger
                ) else it
            }
        )
    }

    fun feedActive(state: PetsState, currentTick: Long): PetsState {
        val active = state.active() ?: return state
        return state.copy(
            owned = state.owned.map {
                if (it.id == active.id) it.copy(
                    hunger = (it.hunger + 30).coerceAtMost(100),
                    happiness = (it.happiness + 5).coerceAtMost(100),
                    lastFedTick = currentTick
                ) else it
            }
        )
    }
}

// =====================================================================
//                     NPC SEGUIDOR (con timeout)
// =====================================================================

/**
 * NPC que se aproxima al jugador con UNA pregunta. Auto-desaparece tras
 * [TIMEOUT_TICKS] segundos o si el jugador responde. NUNCA hay más de UNO
 * activo a la vez — anti-bucle.
 */
@Serializable
data class FollowerNpc(
    val id: String,
    val name: String,
    val portraitSeed: Long,
    val x: Float,
    val y: Float,
    val question: String,
    val choiceA: String,
    val choiceB: String,
    val effectACashDelta: Double = 0.0,
    val effectAHappiness: Int = 0,
    val effectAKarma: Int = 0,
    val effectBCashDelta: Double = 0.0,
    val effectBHappiness: Int = 0,
    val effectBKarma: Int = 0,
    val spawnedAtTick: Long
) {
    companion object {
        const val TIMEOUT_TICKS = 35L  // se rinde tras 35 segundos reales
    }
}

@Serializable
data class FollowerState(
    val current: FollowerNpc? = null,
    val cooldownUntilTick: Long = 0L,   // anti-spam: sin nuevo hasta este tick
    val resolvedCount: Int = 0
)

object FollowerEngine {

    private val NAMES = listOf("Marta", "Luis", "Ana", "Pedro", "Carla", "Diego", "Sofía", "Iván",
        "Lucía", "Pablo", "Olga", "Hugo", "Eva", "Marcos", "Paula")

    private val QUESTIONS = listOf(
        Triple("Disculpa, ¿tienes un € para el bus?", "Darle 5 €", "No, gracias")
            to Triple(Triple(-5.0, 1, 1), Triple(0.0, 0, -1), null),
        Triple("¡Eh! ¿Sabes dónde queda la biblioteca?", "Indicarle", "Lo siento, no")
            to Triple(Triple(0.0, 3, 2), Triple(0.0, -1, 0), null),
        Triple("¿Me ayudas a llevar esto al coche?", "Ayudar", "Tengo prisa")
            to Triple(Triple(0.0, 5, 2), Triple(0.0, -2, -1), null),
        Triple("Soy del barrio, ¿una propina por el rap?", "Dar 3 €", "Pasar")
            to Triple(Triple(-3.0, 4, 1), Triple(0.0, -1, 0), null),
        Triple("¿Me firmas un autógrafo?", "Firmar", "Otro día")
            to Triple(Triple(0.0, 8, 1), Triple(0.0, -3, 0), null),
        Triple("¿Me cambias 50 € en monedas?", "Cambiar", "No tengo")
            to Triple(Triple(0.0, 2, 0), Triple(0.0, 0, 0), null),
        Triple("Perdona, ¿conoces algún hotel barato?", "Recomendar", "No")
            to Triple(Triple(0.0, 2, 1), Triple(0.0, 0, 0), null),
        Triple("¿Te gusta nuestra ciudad?", "Sí, mucho", "Está bien")
            to Triple(Triple(0.0, 5, 2), Triple(0.0, 1, 0), null)
    )

    /**
     * Spawn ocasional de un seguidor — solo si nadie activo y cooldown pasado.
     *
     * FIX BUG-03-FOL-01: si `grid != null`, intenta hasta 8 ángulos buscando
     * una celda walkable. Si tras 8 intentos no la hay, aborta el spawn (mejor
     * sin follower que un follower atrapado en agua/edificio).
     */
    fun maybeSpawn(
        state: FollowerState,
        avatarX: Float,
        avatarY: Float,
        currentTick: Long,
        rng: Random,
        grid: WorldGrid? = null
    ): FollowerState {
        if (state.current != null) return state
        if (currentTick < state.cooldownUntilTick) return state
        if (rng.nextInt(600) != 0) return state

        val (qPair, effs) = QUESTIONS.random(rng)
        val (q, a, b) = qPair
        val (effA, effB, _) = effs

        var nx = 0f; var ny = 0f; var found = false
        for (attempt in 0 until 8) {
            val angle = rng.nextDouble() * 6.28318
            val dist = 4.0
            val tx = (avatarX + cos(angle) * dist).toFloat()
            val ty = (avatarY + sin(angle) * dist).toFloat()
            if (grid == null || grid.walkableAt(tx.toInt(), ty.toInt())) {
                nx = tx; ny = ty; found = true; break
            }
        }
        if (!found) return state.copy(cooldownUntilTick = currentTick + 60L)

        return state.copy(
            current = FollowerNpc(
                id = "follower_${System.nanoTime()}",
                name = NAMES.random(rng),
                portraitSeed = rng.nextLong(),
                x = nx, y = ny,
                question = q,
                choiceA = a, choiceB = b,
                effectACashDelta = effA.first,
                effectAHappiness = effA.second,
                effectAKarma = effA.third,
                effectBCashDelta = effB.first,
                effectBHappiness = effB.second,
                effectBKarma = effB.third,
                spawnedAtTick = currentTick
            )
        )
    }

    /**
     * Tick: el follower se acerca al avatar pero PARA a 1.5 baldosas.
     *
     * FIX BUG-03-FOL-02: respeta walkable. Si la siguiente celda es bloque,
     * no avanza pero no se pierde su estado.
     */
    fun tick(
        state: FollowerState,
        avatarX: Float,
        avatarY: Float,
        currentTick: Long,
        grid: WorldGrid? = null
    ): FollowerState {
        val cur = state.current ?: return state
        if (currentTick - cur.spawnedAtTick > FollowerNpc.TIMEOUT_TICKS) {
            return state.copy(current = null, cooldownUntilTick = currentTick + 60L)
        }
        val dx = avatarX - cur.x
        val dy = avatarY - cur.y
        val dist = hypot(dx, dy)
        if (dist < 1.5f) return state
        val speed = 2.0f
        val step = 0.1f * speed
        val tnx = cur.x + (dx / dist) * step
        val tny = cur.y + (dy / dist) * step
        val (nx, ny) = if (grid == null) {
            tnx to tny
        } else {
            val canX = grid.walkableAt(tnx.toInt(), cur.y.toInt())
            val canY = grid.walkableAt(cur.x.toInt(), tny.toInt())
            (if (canX) tnx else cur.x) to (if (canY) tny else cur.y)
        }
        return state.copy(current = cur.copy(x = nx, y = ny))
    }

    fun resolve(state: FollowerState, choice: Int, currentTick: Long): Triple<FollowerState, Double, Triple<Int, Int, String>> {
        val cur = state.current ?: return Triple(state, 0.0, Triple(0, 0, ""))
        val (cash, happy, karma) = when (choice) {
            0 -> Triple(cur.effectACashDelta, cur.effectAHappiness, cur.effectAKarma)
            else -> Triple(cur.effectBCashDelta, cur.effectBHappiness, cur.effectBKarma)
        }
        return Triple(
            state.copy(
                current = null,
                cooldownUntilTick = currentTick + 240L,  // 4 min in-game antes de otro
                resolvedCount = state.resolvedCount + 1
            ),
            cash,
            Triple(happy, karma, cur.name)
        )
    }
}

// =====================================================================
//                            UFO EVENTS
// =====================================================================

@Serializable
data class UfoSighting(
    val id: String,
    val x: Float,
    val y: Float,
    val phase: Float = 0f,        // animación 0..1
    val spawnedAtTick: Long,
    val durationTicks: Long = 25L,
    val type: String = "saucer"
)

@Serializable
data class UfoState(
    val active: UfoSighting? = null,
    val totalSeen: Int = 0,
    val nextEarliestTick: Long = 0L
)

object UfoEngine {
    fun tick(state: UfoState, avatarX: Float, avatarY: Float, currentTick: Long, rng: Random): UfoState {
        val active = state.active
        // Si ya hay uno, avanza fase y comprueba expiración
        if (active != null) {
            val elapsed = currentTick - active.spawnedAtTick
            if (elapsed >= active.durationTicks) {
                return state.copy(
                    active = null,
                    totalSeen = state.totalSeen + 1,
                    nextEarliestTick = currentTick + 1_440L  // mínimo un día entre OVNIs
                )
            }
            val newPhase = (elapsed.toFloat() / active.durationTicks.toFloat()).coerceIn(0f, 1f)
            // Vuelan por encima del avatar, cruzan en línea recta
            val nx = active.x + 0.3f
            val ny = active.y + sin((newPhase * 6.28).toFloat()) * 0.1f
            return state.copy(active = active.copy(x = nx, y = ny, phase = newPhase))
        }
        // Sin OVNI activo: posible spawn
        if (currentTick < state.nextEarliestTick) return state
        // 1/3000 chance por tick = 1 cada ~50 min in-game
        if (rng.nextInt(3000) != 0) return state
        val type = listOf("saucer", "triangle", "cigar", "orb", "mothership").random(rng)
        return state.copy(
            active = UfoSighting(
                id = "ufo_${System.nanoTime()}",
                x = avatarX - 8f,
                y = avatarY - 4f + rng.nextFloat() * 2f,
                spawnedAtTick = currentTick,
                durationTicks = 30L + rng.nextLong(40),
                type = type
            )
        )
    }
}

// =====================================================================
//                       FAMILIA + CASA
// =====================================================================

@Serializable
data class Spouse(
    val name: String,
    val portraitSeed: Long,
    val happiness: Int = 70,
    val daysWith: Int = 0,
    val likes: List<String> = emptyList()
)

@Serializable
data class Child(
    val id: String,
    val name: String,
    val ageDays: Int = 0,
    val portraitSeed: Long,
    val happiness: Int = 80
)

@Serializable
data class FamilyState(
    val spouse: Spouse? = null,
    val children: List<Child> = emptyList(),
    val proposedAt: Long = 0L
)

enum class FurnitureKind(val displayName: String, val emoji: String, val price: Double, val sizeX: Int, val sizeY: Int) {
    BED("Cama", "🛏️", 400.0, 2, 2),
    SOFA("Sofá", "🛋️", 600.0, 3, 1),
    TABLE("Mesa", "🪑", 200.0, 2, 1),
    TV("Televisión", "📺", 800.0, 1, 1),
    BOOKSHELF("Estantería", "📚", 250.0, 1, 2),
    PLANT("Planta", "🪴", 80.0, 1, 1),
    LAMP("Lámpara", "💡", 120.0, 1, 1),
    PIANO("Piano", "🎹", 3_500.0, 3, 1),
    AQUARIUM("Acuario", "🐠", 600.0, 2, 1),
    PAINTING("Cuadro", "🖼️", 350.0, 1, 1),
    CARPET("Alfombra", "🟫", 200.0, 2, 2),
    GUITAR("Guitarra", "🎸", 800.0, 1, 1),
    GAME_CONSOLE("Consola", "🎮", 500.0, 1, 1),
    REFRIGERATOR("Nevera", "🧊", 700.0, 1, 2),
    KITCHEN_STOVE("Cocina", "🔥", 800.0, 2, 1),
    CHANDELIER("Araña", "💎", 5_000.0, 1, 1),
    WINE_CABINET("Bodega", "🍷", 1_200.0, 2, 1)
}

@Serializable
data class PlacedFurniture(
    val id: String,
    val kind: String,
    val x: Int,
    val y: Int
) {
    fun spec(): FurnitureKind = runCatching { FurnitureKind.valueOf(kind) }.getOrDefault(FurnitureKind.SOFA)
}

@Serializable
data class HouseState(
    val width: Int = 12,
    val height: Int = 9,
    val furniture: List<PlacedFurniture> = emptyList(),
    val wallpaperColor: Long = 0xFFEFEBE9,
    val floorColor: Long = 0xFFD7CCC8
) {
    fun isOccupied(x: Int, y: Int): Boolean = furniture.any { f ->
        x in f.x until (f.x + f.spec().sizeX) && y in f.y until (f.y + f.spec().sizeY)
    }

    /**
     * FIX BUG-03-HOU-01: comprueba si TODA el área que ocuparía un mueble
     * `kind` colocado en (x,y) está libre. Antes se chequeaba solo el origen,
     * permitiendo overlap parcial con muebles existentes.
     */
    fun areaFree(kind: FurnitureKind, x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x + kind.sizeX > width || y + kind.sizeY > height) return false
        for (dy in 0 until kind.sizeY) {
            for (dx in 0 until kind.sizeX) {
                if (isOccupied(x + dx, y + dy)) return false
            }
        }
        return true
    }
}
