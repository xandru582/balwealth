package com.empiretycoon.game.world

import kotlinx.serialization.Serializable

/**
 * Malla 2D row-major persistente del mundo explorable.
 *
 * Para que el grid sea barato de serializar y rápido de copiar (data class
 * inmutable), se almacena como una `List<Int>` empaquetada:
 *
 * ```
 *  bits  | 31..16    | 15..0
 *        | TileType  | decoIndex
 * ```
 *
 * `decoMap` guarda payloads "raros" — building ids, npc ids, texto de cartel —
 * indexados por `(y * width + x).toLong()`, manteniendo el array principal
 * compacto (un solo Int por celda).
 *
 * Operaciones in-place se evitan a propósito: las mutaciones devuelven un
 * nuevo [WorldGrid] (es coherente con el patrón Redux del [GameEngine]).
 */
@Serializable
data class WorldGrid(
    val width: Int,
    val height: Int,
    val tilesPacked: List<Int>,
    val decoMap: Map<Long, String> = emptyMap()
) {
    init {
        require(width > 0 && height > 0) { "WorldGrid dims must be > 0" }
        require(tilesPacked.size == width * height) {
            "tilesPacked size ${tilesPacked.size} != width*height=${width * height}"
        }
    }

    fun inBounds(x: Int, y: Int): Boolean =
        x in 0 until width && y in 0 until height

    private fun idx(x: Int, y: Int): Int = y * width + x

    /** Tipo de baldosa en (x, y). Devuelve [TileType.WALL] si fuera de límites. */
    fun tileAt(x: Int, y: Int): TileType {
        if (!inBounds(x, y)) return TileType.WALL
        return unpackType(tilesPacked[idx(x, y)])
    }

    /** Variante visual (0..65535) en (x, y). */
    fun decoAt(x: Int, y: Int): Int {
        if (!inBounds(x, y)) return 0
        return unpackDeco(tilesPacked[idx(x, y)])
    }

    /** True si el avatar puede pisar (x, y). Bordes -> false. */
    fun walkableAt(x: Int, y: Int): Boolean {
        if (!inBounds(x, y)) return false
        return unpackType(tilesPacked[idx(x, y)]).walkable
    }

    /** Reconstruye la celda completa, fusionando type+deco+payload. */
    fun fullTileAt(x: Int, y: Int): Tile {
        if (!inBounds(x, y)) return Tile(TileType.WALL)
        val packed = tilesPacked[idx(x, y)]
        val type = unpackType(packed)
        val deco = unpackDeco(packed)
        val key = idx(x, y).toLong()
        val payload = decoMap[key]
        // Si el tipo es DOOR y hay payload, lo tratamos como buildingRefId.
        val (buildingRefId, interactPayload) = when (type.interactKind) {
            TileInteractKind.ENTER_BUILDING -> payload to payload
            null -> null to null
            else -> null to payload
        }
        return Tile(
            type = type,
            decoIndex = deco,
            buildingRefId = buildingRefId,
            interactPayload = interactPayload
        )
    }

    /** Copia con la celda (x, y) remplazada. */
    fun setTile(x: Int, y: Int, type: TileType, decoIndex: Int = 0): WorldGrid {
        if (!inBounds(x, y)) return this
        val list = tilesPacked.toMutableList()
        list[idx(x, y)] = pack(type, decoIndex)
        return copy(tilesPacked = list)
    }

    /** Copia con un payload asociado a la celda. `value == null` lo elimina. */
    fun setPayload(x: Int, y: Int, value: String?): WorldGrid {
        if (!inBounds(x, y)) return this
        val key = idx(x, y).toLong()
        val newMap = if (value == null) decoMap - key else decoMap + (key to value)
        return copy(decoMap = newMap)
    }

    companion object {
        // ------- packing -------
        private const val TYPE_SHIFT = 16
        private const val DECO_MASK = 0xFFFF

        fun pack(type: TileType, decoIndex: Int): Int =
            (type.ordinal shl TYPE_SHIFT) or (decoIndex and DECO_MASK)

        fun unpackType(packed: Int): TileType {
            val ord = (packed ushr TYPE_SHIFT) and DECO_MASK
            val values = TileType.values()
            return if (ord in values.indices) values[ord] else TileType.GRASS
        }

        fun unpackDeco(packed: Int): Int = packed and DECO_MASK

        // ------- factories -------

        /** Mapa vacío todo de hierba (para `WorldState.fresh`). */
        fun blank(width: Int = 64, height: Int = 64, fill: TileType = TileType.GRASS): WorldGrid {
            val packedFill = pack(fill, 0)
            return WorldGrid(
                width = width,
                height = height,
                tilesPacked = List(width * height) { packedFill }
            )
        }
    }
}
