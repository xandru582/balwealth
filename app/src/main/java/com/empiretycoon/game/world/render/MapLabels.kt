package com.empiretycoon.game.world.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.empiretycoon.game.model.Building
import com.empiretycoon.game.model.BuildingType
import com.empiretycoon.game.world.CityBlueprint
import com.empiretycoon.game.world.PlaceKind
import com.empiretycoon.game.world.emoji
import com.empiretycoon.game.world.isLandmark

/**
 * Etiquetas de lugares + iconos de landmarks sobre el mapa 2D.
 *
 * Compose Canvas no expone drawText nativamente — usamos drawIntoCanvas
 * con Paint nativo de Android para texto + emojis con sombra para que
 * la etiqueta se vea sobre cualquier tile.
 *
 * Filosofía:
 *  - **Solo el nombre** flota encima del tile de "puerta" del lugar
 *    (`CityPlace.name`). 1 tile por encima del tile, centrado horizontal.
 *  - **Landmarks** (STATUE_MYSTERIOUS, BEACHED_UFO, ICE_CREAM_VAN) además
 *    pintan un emoji grande (🗿/🛸/🍦) en el tile mismo, así son
 *    visualmente distintos al resto del mapa.
 *  - **Slots ocupados por el jugador** (FACTORY_SLOT, MINE_SLOT, FARM_SLOT
 *    asignados a un Building del jugador) muestran el nombre del edificio
 *    con un emoji de propiedad (👑) y color dorado.
 *  - Cull: solo procesa lugares dentro del viewport con un tile de margen.
 *  - Llamar al FINAL del pipeline (después de vignette) para que las
 *    etiquetas siempre se vean nítidas sin atenuación atmosférica.
 *
 * Mapeo Slot → Building:
 *  - Determinista por orden: el Nº-iésimo Building de tipo FACTORY se
 *    asigna al N-ésimo FACTORY_SLOT (en orden de la lista CityBlueprint.places).
 *  - Idem para MINE / FARM / BAKERY (los SAWMILL/SMELTER/etc. caen sobre
 *    los slots fabriles disponibles).
 */
/** FIX P1: cache de Paints reutilizables — antes se creaban 3 Paint() por
 *  frame (~180 allocations/seg). Ahora solo si cambia densityScale. */
private object MapLabelPaintCache {
    @Volatile var lastDensity: Float = -1f
    val labelPaint = Paint()
    val ownedPaint = Paint()
    val emojiPaint = Paint()

    fun ensure(densityScale: Float) {
        if (lastDensity == densityScale) return
        lastDensity = densityScale
        val textSize = (11f * densityScale).coerceAtLeast(22f)
        val emojiSize = (28f * densityScale).coerceAtLeast(46f)
        val shadowR1 = 3f * densityScale.coerceAtLeast(1f)
        val shadowR2 = 4f * densityScale.coerceAtLeast(1f)
        val shadowDy = 1f * densityScale.coerceAtLeast(1f)

        labelPaint.apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            this.textSize = textSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(shadowR1, 0f, shadowDy, android.graphics.Color.argb(220, 0, 0, 0))
        }
        ownedPaint.apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(255, 255, 209, 102)
            this.textSize = textSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(shadowR2, 0f, shadowDy, android.graphics.Color.argb(240, 0, 0, 0))
        }
        emojiPaint.apply {
            isAntiAlias = true
            this.textSize = emojiSize
            textAlign = Paint.Align.CENTER
        }
    }
}

fun DrawScope.drawPlaceLabels(
    originTileX: Float,
    originTileY: Float,
    tileSize: Float,
    viewW: Int,
    viewH: Int,
    densityScale: Float,
    playerBuildings: List<Building> = emptyList()
) {
    MapLabelPaintCache.ensure(densityScale)
    val labelPaint = MapLabelPaintCache.labelPaint
    val ownedPaint = MapLabelPaintCache.ownedPaint
    val emojiPaint = MapLabelPaintCache.emojiPaint

    // Pre-calcula el mapping placeId → Building (deterministic order).
    val ownedById: Map<String, Building> = if (playerBuildings.isEmpty()) emptyMap()
    else buildOwnedSlotMap(playerBuildings)

    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        for (place in CityBlueprint.places) {
            // Cull viewport con margen de 1 tile.
            if (place.x < originTileX - 1 || place.x > originTileX + viewW + 1) continue
            if (place.y < originTileY - 1 || place.y > originTileY + viewH + 1) continue

            val sx = (place.x.toFloat() - originTileX) * tileSize + tileSize / 2f
            val sy = (place.y.toFloat() - originTileY) * tileSize

            // Si es landmark, dibujar emoji grande dentro del tile.
            if (place.kind.isLandmark) {
                nc.drawText(
                    place.kind.emoji,
                    sx,
                    sy + tileSize * 0.85f,
                    emojiPaint
                )
            }

            // Nombre del lugar: si está ocupado por un edificio del jugador,
            // usa el nombre del edificio + emoji 👑 y paint dorado.
            val owned = ownedById[place.id]
            val displayLabel: String
            val paintToUse: Paint
            if (owned != null) {
                displayLabel = "👑 ${owned.type.emoji} ${owned.name}"
                paintToUse = ownedPaint
            } else {
                displayLabel = place.name
                paintToUse = labelPaint
            }
            val labelY = sy - tileSize * 0.15f
            nc.drawText(
                displayLabel,
                sx,
                labelY,
                paintToUse
            )
        }
    }
}

/**
 * Asigna cada Building a un CityPlace slot del tipo correspondiente,
 * en orden de aparición. Los tipos sin slot directo (SAWMILL, SMELTER,
 * REFINERY, OFFICE, JEWELRY, SHIPYARD, BAKERY, WAREHOUSE) caen sobre los
 * slots fabriles disponibles tras los FACTORY.
 */
private fun buildOwnedSlotMap(buildings: List<Building>): Map<String, Building> {
    val factorySlots = CityBlueprint.places.filter { it.kind == PlaceKind.FACTORY_SLOT }.map { it.id }
    val mineSlots = CityBlueprint.places.filter { it.kind == PlaceKind.MINE_SLOT }.map { it.id }
    val farmSlots = CityBlueprint.places.filter { it.kind == PlaceKind.FARM_SLOT }.map { it.id }

    val mines = buildings.filter { it.type == BuildingType.MINE }
    val farms = buildings.filter { it.type == BuildingType.FARM }
    val rest = buildings.filter { it.type != BuildingType.MINE && it.type != BuildingType.FARM }

    val out = HashMap<String, Building>()
    mines.forEachIndexed { i, b -> if (i < mineSlots.size) out[mineSlots[i]] = b }
    farms.forEachIndexed { i, b -> if (i < farmSlots.size) out[farmSlots[i]] = b }
    rest.forEachIndexed { i, b -> if (i < factorySlots.size) out[factorySlots[i]] = b }
    return out
}
