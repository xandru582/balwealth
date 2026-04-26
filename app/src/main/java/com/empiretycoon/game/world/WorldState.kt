package com.empiretycoon.game.world

import kotlinx.serialization.Serializable

/**
 * Raíz serializable del mundo 2D explorable. Vive *dentro* de
 * [com.empiretycoon.game.model.GameState] como `world: WorldState`.
 *
 * - [grid] geometría + payloads de baldosas.
 * - [avatar] posición y look del jugador.
 * - [currentDistrict] etiqueta lógica del barrio (downtown, harbour, etc.).
 *   El renderer puede usarla para skybox / paleta ambient.
 * - [lastInteractTick] anti-rebote: evita disparar la misma interacción dos
 *   veces en frames consecutivos. El motor lo compara contra `state.tick`.
 * - [npcsPath] ids de NPCs que el jugador ha "tageado" para que le sigan.
 */
@Serializable
data class WorldState(
    val grid: WorldGrid,
    val avatar: Avatar,
    val currentDistrict: String = "downtown",
    val lastInteractTick: Long = 0,
    val npcsPath: List<String> = emptyList()
) {
    companion object {
        /**
         * Mundo inicial: 64x64 todo de hierba, avatar en el centro mirando
         * hacia abajo. El generador real de ciudades lo reescribirá luego;
         * de momento basta para arrancar el renderer y la cámara.
         */
        fun fresh(): WorldState {
            val grid = CityBlueprint.build()
            // Spawn en el centro (DOWNTOWN). Buscar un tile caminable cercano.
            val cx = grid.width / 2
            val cy = grid.height / 2
            var sx = cx
            var sy = cy
            for (r in 0..6) {
                if (grid.walkableAt(cx + r, cy)) { sx = cx + r; sy = cy; break }
                if (grid.walkableAt(cx - r, cy)) { sx = cx - r; sy = cy; break }
                if (grid.walkableAt(cx, cy + r)) { sx = cx; sy = cy + r; break }
                if (grid.walkableAt(cx, cy - r)) { sx = cx; sy = cy - r; break }
            }
            return WorldState(
                grid = grid,
                avatar = Avatar(x = sx + 0.5f, y = sy + 0.5f, facing = Facing.DOWN),
                currentDistrict = "downtown"
            )
        }
    }
}
