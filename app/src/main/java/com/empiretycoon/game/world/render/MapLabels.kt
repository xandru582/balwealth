package com.empiretycoon.game.world.render

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
 *  - Cull: solo procesa lugares dentro del viewport con un tile de margen.
 *  - Llamar al FINAL del pipeline (después de vignette) para que las
 *    etiquetas siempre se vean nítidas sin atenuación atmosférica.
 */
fun DrawScope.drawPlaceLabels(
    originTileX: Float,
    originTileY: Float,
    tileSize: Float,
    viewW: Int,
    viewH: Int,
    densityScale: Float
) {
    // Tamaño de fuente proporcional pero con mínimos legibles.
    val labelTextSize = (11f * densityScale).coerceAtLeast(22f)
    val landmarkEmojiSize = (28f * densityScale).coerceAtLeast(46f)

    val labelPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = labelTextSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(
            3f * densityScale.coerceAtLeast(1f),
            0f,
            1f * densityScale.coerceAtLeast(1f),
            android.graphics.Color.argb(220, 0, 0, 0)
        )
    }
    val emojiPaint = Paint().apply {
        isAntiAlias = true
        textSize = landmarkEmojiSize
        textAlign = Paint.Align.CENTER
    }

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

            // Nombre del lugar 1 línea por encima del tile.
            val labelY = sy - tileSize * 0.15f
            nc.drawText(
                place.name,
                sx,
                labelY,
                labelPaint
            )
        }
    }
}
