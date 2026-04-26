package com.empiretycoon.game.world

import kotlinx.serialization.Serializable

/**
 * Mobiliario urbano estático que se renderiza sobre el grid sin afectar
 * caminabilidad. Da volumen y vida visual a la ciudad.
 */
enum class PropKind {
    TREE_OAK, TREE_PINE, TREE_PALM, TREE_BIRCH, TREE_AUTUMN,
    LAMP_POST,
    BENCH,
    FOUNTAIN,
    TRASH_CAN,
    PLANTER,
    NEWSPAPER_KIOSK,
    MAILBOX,
    HYDRANT,
    PARKED_CAR_RED, PARKED_CAR_BLUE,
    CAFE_TABLE,
    BUS_STOP_SHELTER,
    STREET_SIGN,
    MARKET_STALL_RED, MARKET_STALL_BLUE,
    CHIMNEY_SMOKE,
    BUSH,
    FLOWER_BED
}

@Serializable
data class WorldProp(
    val id: String,
    val kind: String,            // PropKind.name
    val x: Float,                // posición en baldosas (decimales OK)
    val y: Float
) {
    fun propKind(): PropKind = runCatching { PropKind.valueOf(kind) }.getOrDefault(PropKind.LAMP_POST)
}

@Serializable
data class CityPropsState(
    val props: List<WorldProp> = emptyList()
)

/**
 * Generador de mobiliario urbano según el blueprint de la ciudad.
 * Coloca decoración densa en parques, sutil en industrial, ornamental en
 * downtown, etc. Determinista para que el mismo seed dé el mismo resultado.
 */
object CityPropsGenerator {

    fun generate(grid: WorldGrid): CityPropsState {
        val list = mutableListOf<WorldProp>()
        var counter = 0
        fun add(kind: PropKind, x: Float, y: Float) {
            // Solo si la baldosa base es caminable o es césped (no encima de carreteras o agua)
            val tx = x.toInt(); val ty = y.toInt()
            if (!grid.inBounds(tx, ty)) return
            val type = grid.tileAt(tx, ty)
            // Algunas decoraciones (faroles) aparecen en aceras; árboles en césped
            when (kind) {
                PropKind.LAMP_POST,
                PropKind.HYDRANT,
                PropKind.MAILBOX,
                PropKind.PARKED_CAR_RED, PropKind.PARKED_CAR_BLUE,
                PropKind.NEWSPAPER_KIOSK,
                PropKind.BUS_STOP_SHELTER,
                PropKind.STREET_SIGN -> {
                    if (type != TileType.SIDEWALK && type != TileType.PLAZA_TILE) return
                }
                PropKind.TREE_OAK, PropKind.TREE_PINE, PropKind.TREE_PALM, PropKind.TREE_BIRCH, PropKind.TREE_AUTUMN,
                PropKind.BUSH, PropKind.FLOWER_BED -> {
                    if (type != TileType.GRASS && type != TileType.FOREST_FLOOR && type != TileType.SAND) return
                }
                PropKind.FOUNTAIN -> {
                    if (type != TileType.PLAZA_TILE && type != TileType.GRASS) return
                }
                else -> {
                    if (type == TileType.WATER || type == TileType.WALL) return
                }
            }
            list.add(WorldProp(id = "prop_${counter++}", kind = kind.name, x = x, y = y))
        }

        // Recorremos el grid sembrando decoraciones según distrito (banda Y)
        for (y in 0 until grid.height) {
            val districtBand = when {
                y < 18 -> "PARK"
                y < 30 -> "RESIDENTIAL"
                y < 50 -> "DOWNTOWN"
                y < 62 -> "COMMERCIAL"
                y < 76 -> "INDUSTRIAL"
                else -> "HARBOR"
            }
            for (x in 0 until grid.width) {
                val type = grid.tileAt(x, y)
                val seed = (x * 73 + y * 131) and 0xFF

                when (districtBand) {
                    "PARK" -> {
                        if (type == TileType.GRASS) {
                            // Árboles y flores muy densos
                            if (seed < 28) add(PropKind.TREE_OAK, x + 0.5f, y + 0.5f)
                            else if (seed < 38) add(PropKind.TREE_PINE, x + 0.5f, y + 0.5f)
                            else if (seed < 48) add(PropKind.TREE_BIRCH, x + 0.5f, y + 0.5f)
                            else if (seed < 60) add(PropKind.BUSH, x + 0.5f, y + 0.5f)
                            else if (seed < 68) add(PropKind.FLOWER_BED, x + 0.5f, y + 0.5f)
                        }
                        if (type == TileType.SIDEWALK && seed < 16) add(PropKind.LAMP_POST, x + 0.5f, y + 0.5f)
                        if (type == TileType.SIDEWALK && seed in 16..22) add(PropKind.BENCH, x + 0.5f, y + 0.5f)
                        if (type == TileType.PLAZA_TILE && seed < 4) add(PropKind.FOUNTAIN, x + 0.5f, y + 0.5f)
                    }
                    "RESIDENTIAL" -> {
                        if (type == TileType.GRASS) {
                            if (seed < 20) add(PropKind.TREE_OAK, x + 0.5f, y + 0.5f)
                            else if (seed < 30) add(PropKind.BUSH, x + 0.5f, y + 0.5f)
                            else if (seed < 40) add(PropKind.FLOWER_BED, x + 0.5f, y + 0.5f)
                        }
                        if (type == TileType.SIDEWALK) {
                            if (seed < 12) add(PropKind.LAMP_POST, x + 0.5f, y + 0.5f)
                            else if (seed in 12..18) add(PropKind.MAILBOX, x + 0.5f, y + 0.5f)
                            else if (seed in 18..24) add(PropKind.HYDRANT, x + 0.5f, y + 0.5f)
                            else if (seed in 24..32) add(PropKind.PARKED_CAR_RED, x + 0.5f, y + 0.5f)
                            else if (seed in 32..40) add(PropKind.PARKED_CAR_BLUE, x + 0.5f, y + 0.5f)
                            else if (seed in 40..50) add(PropKind.PLANTER, x + 0.5f, y + 0.5f)
                        }
                    }
                    "DOWNTOWN" -> {
                        if (type == TileType.SIDEWALK) {
                            if (seed < 18) add(PropKind.LAMP_POST, x + 0.5f, y + 0.5f)
                            else if (seed in 18..28) add(PropKind.NEWSPAPER_KIOSK, x + 0.5f, y + 0.5f)
                            else if (seed in 28..36) add(PropKind.STREET_SIGN, x + 0.5f, y + 0.5f)
                            else if (seed in 36..46) add(PropKind.PARKED_CAR_BLUE, x + 0.5f, y + 0.5f)
                            else if (seed in 46..56) add(PropKind.TRASH_CAN, x + 0.5f, y + 0.5f)
                            else if (seed in 56..64) add(PropKind.CAFE_TABLE, x + 0.5f, y + 0.5f)
                            else if (seed in 64..70) add(PropKind.PLANTER, x + 0.5f, y + 0.5f)
                        }
                        if (type == TileType.PLAZA_TILE && seed < 4) add(PropKind.FOUNTAIN, x + 0.5f, y + 0.5f)
                        if (type == TileType.GRASS && seed < 25) add(PropKind.TREE_BIRCH, x + 0.5f, y + 0.5f)
                    }
                    "COMMERCIAL" -> {
                        if (type == TileType.SIDEWALK) {
                            if (seed < 14) add(PropKind.LAMP_POST, x + 0.5f, y + 0.5f)
                            else if (seed in 14..24) add(PropKind.MARKET_STALL_RED, x + 0.5f, y + 0.5f)
                            else if (seed in 24..34) add(PropKind.MARKET_STALL_BLUE, x + 0.5f, y + 0.5f)
                            else if (seed in 34..44) add(PropKind.CAFE_TABLE, x + 0.5f, y + 0.5f)
                            else if (seed in 44..54) add(PropKind.TRASH_CAN, x + 0.5f, y + 0.5f)
                        }
                        if (type == TileType.GRASS && seed < 25) add(PropKind.TREE_PALM, x + 0.5f, y + 0.5f)
                    }
                    "INDUSTRIAL" -> {
                        if (type == TileType.SIDEWALK) {
                            if (seed < 14) add(PropKind.LAMP_POST, x + 0.5f, y + 0.5f)
                            else if (seed in 14..28) add(PropKind.HYDRANT, x + 0.5f, y + 0.5f)
                            else if (seed in 28..40) add(PropKind.TRASH_CAN, x + 0.5f, y + 0.5f)
                        }
                        if (type == TileType.GRASS && seed < 10) add(PropKind.TREE_AUTUMN, x + 0.5f, y + 0.5f)
                    }
                    "HARBOR" -> {
                        if (type == TileType.SAND && seed < 10) add(PropKind.TREE_PALM, x + 0.5f, y + 0.5f)
                        if (type == TileType.SIDEWALK && seed < 18) add(PropKind.LAMP_POST, x + 0.5f, y + 0.5f)
                    }
                }
            }
        }
        return CityPropsState(props = list)
    }
}
