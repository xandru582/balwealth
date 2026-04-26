package com.empiretycoon.game.world

import kotlinx.serialization.Serializable

/**
 * Catálogo cerrado de tipos de baldosa en el mundo 2D.
 *
 * El renderer (agente separado) usa `decoColor1` / `decoColor2` como base
 * para pintar cada celda; `decoIndex` en [Tile] selecciona variantes
 * (p. ej. parche de hierba alta, charco en una calzada, etc.).
 *
 * `interactKind` es `null` si la baldosa NO es interactiva *por sí misma*
 * (un parque o una carretera); las baldosas interactivas se identifican
 * desde el motor de movimiento mirando al tile en frente del avatar.
 */
enum class TileType(
    val walkable: Boolean,
    val decoColor1: Long,
    val decoColor2: Long,
    val interactKind: TileInteractKind? = null
) {
    GRASS         (walkable = true,  decoColor1 = 0xFF4CAF50, decoColor2 = 0xFF388E3C),
    ROAD          (walkable = true,  decoColor1 = 0xFF3A3F47, decoColor2 = 0xFF2C3038),
    ROAD_LINE_H   (walkable = true,  decoColor1 = 0xFF3A3F47, decoColor2 = 0xFFFFE082),
    ROAD_LINE_V   (walkable = true,  decoColor1 = 0xFF3A3F47, decoColor2 = 0xFFFFE082),
    SIDEWALK      (walkable = true,  decoColor1 = 0xFFB0BEC5, decoColor2 = 0xFF90A4AE),
    PLAZA_TILE    (walkable = true,  decoColor1 = 0xFFD7CCC8, decoColor2 = 0xFFA1887F),
    WATER         (walkable = false, decoColor1 = 0xFF2196F3, decoColor2 = 0xFF1565C0),
    SAND          (walkable = true,  decoColor1 = 0xFFFFE082, decoColor2 = 0xFFFFCA28),
    WALL          (walkable = false, decoColor1 = 0xFF5D4037, decoColor2 = 0xFF3E2723),
    FLOOR_INDOOR  (walkable = true,  decoColor1 = 0xFFEFEBE9, decoColor2 = 0xFFD7CCC8),
    DOOR          (walkable = true,  decoColor1 = 0xFF8D6E63, decoColor2 = 0xFF4E342E,
                   interactKind = TileInteractKind.ENTER_BUILDING),
    FOREST_FLOOR  (walkable = true,  decoColor1 = 0xFF2E7D32, decoColor2 = 0xFF1B5E20),
    RAIL          (walkable = false, decoColor1 = 0xFF424242, decoColor2 = 0xFF9E9E9E),
    BRIDGE        (walkable = true,  decoColor1 = 0xFF8D6E63, decoColor2 = 0xFF6D4C41);
}

/**
 * Acciones contextuales que el avatar puede ejecutar al pulsar "interactuar"
 * mirando una baldosa concreta. El payload se interpreta en función del kind.
 */
enum class TileInteractKind {
    ENTER_BUILDING,   // payload = building.id (engancha con [com.empiretycoon.game.model.Building])
    READ_SIGN,        // payload = clave de cadena localizable / texto literal
    TALK_NPC,         // payload = npc.id
    BUS_STOP,         // payload = id de línea / ruta
    FAST_TRAVEL,      // payload = districtId destino
    ATM,              // payload = banco/sucursal id
    SHOP,             // payload = shop.id
    BENCH             // payload = null (descansa, +energía)
}

/**
 * Celda individual del [WorldGrid].
 *
 * - [decoIndex] permite al renderer elegir variantes visuales (0..15) sin
 *   inflar el catálogo de [TileType].
 * - [buildingRefId] enlaza la baldosa con un [com.empiretycoon.game.model.Building]
 *   poseído por el jugador (típicamente puertas).
 * - [interactPayload] es opcional: si la baldosa tiene `interactKind`, este
 *   campo guarda el target (ver enum arriba).
 */
@Serializable
data class Tile(
    val type: TileType,
    val decoIndex: Int = 0,
    val buildingRefId: String? = null,
    val interactPayload: String? = null
) {
    val walkable: Boolean get() = type.walkable
    val interactKind: TileInteractKind? get() = type.interactKind
}
