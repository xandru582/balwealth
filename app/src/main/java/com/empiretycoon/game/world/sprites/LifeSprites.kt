package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.empiretycoon.game.world.PetSpecies
import com.empiretycoon.game.world.UfoSighting
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawPet(species: PetSpecies, x: Float, y: Float, tileSize: Float, walkPhase: Float) {
    val size = tileSize * 0.5f
    when (species) {
        PetSpecies.DOG -> drawDog(x, y, size, walkPhase)
        PetSpecies.CAT -> drawCat(x, y, size, walkPhase)
        PetSpecies.BIRD -> drawBird(x, y, size, walkPhase)
        PetSpecies.HAMSTER -> drawHamster(x, y, size, walkPhase)
        PetSpecies.RABBIT -> drawRabbit(x, y, size, walkPhase)
        PetSpecies.FOX -> drawFox(x, y, size, walkPhase)
        PetSpecies.DRAGON -> drawDragon(x, y, size, walkPhase)
    }
}

private fun DrawScope.drawDog(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x55000000), radius = s * 0.4f, center = Offset(x, y + s * 0.5f))
    val body = Color(0xFF8D6E63)
    val ears = Color(0xFF6D4C41)
    val tailWag = sin((phase * 6.28f).toDouble()).toFloat() * s * 0.2f
    // Cuerpo
    drawRect(color = body, topLeft = Offset(x - s * 0.4f, y - s * 0.1f), size = Size(s * 0.8f, s * 0.5f))
    // Cabeza
    drawCircle(color = body, radius = s * 0.3f, center = Offset(x + s * 0.4f, y - s * 0.05f))
    // Orejas
    drawCircle(color = ears, radius = s * 0.12f, center = Offset(x + s * 0.55f, y - s * 0.25f))
    // Hocico
    drawCircle(color = Color(0xFF000000), radius = s * 0.05f, center = Offset(x + s * 0.65f, y))
    // Ojo
    drawCircle(color = Color(0xFFFFFFFF), radius = s * 0.06f, center = Offset(x + s * 0.5f, y - s * 0.1f))
    drawCircle(color = Color(0xFF000000), radius = s * 0.03f, center = Offset(x + s * 0.51f, y - s * 0.1f))
    // Cola wagging
    drawRect(color = body, topLeft = Offset(x - s * 0.5f + tailWag, y - s * 0.15f), size = Size(s * 0.15f, s * 0.05f))
}

private fun DrawScope.drawCat(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x55000000), radius = s * 0.4f, center = Offset(x, y + s * 0.5f))
    val body = Color(0xFF424242)
    drawRect(color = body, topLeft = Offset(x - s * 0.35f, y - s * 0.05f), size = Size(s * 0.7f, s * 0.45f))
    drawCircle(color = body, radius = s * 0.28f, center = Offset(x + s * 0.35f, y - s * 0.05f))
    // Orejas triangulares
    drawArc(color = body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x + s * 0.2f, y - s * 0.4f), size = Size(s * 0.15f, s * 0.2f))
    drawArc(color = body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x + s * 0.45f, y - s * 0.4f), size = Size(s * 0.15f, s * 0.2f))
    // Ojo
    drawCircle(color = Color(0xFF8BC34A), radius = s * 0.06f, center = Offset(x + s * 0.45f, y - s * 0.1f))
    // Cola larga curvada
    val tailY = sin((phase * 6.28f).toDouble()).toFloat() * s * 0.1f
    drawArc(color = body, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(x - s * 0.6f, y - s * 0.3f + tailY), size = Size(s * 0.4f, s * 0.4f),
        style = Stroke(width = s * 0.08f))
}

private fun DrawScope.drawBird(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x55000000), radius = s * 0.25f, center = Offset(x, y + s * 0.5f))
    val flap = sin((phase * 12f).toDouble()).toFloat() * s * 0.15f
    val body = Color(0xFFFFC107)
    drawCircle(color = body, radius = s * 0.25f, center = Offset(x, y + s * 0.05f))
    drawCircle(color = body, radius = s * 0.18f, center = Offset(x + s * 0.2f, y - s * 0.1f))
    // Pico
    drawRect(color = Color(0xFFFB8C00), topLeft = Offset(x + s * 0.35f, y - s * 0.1f), size = Size(s * 0.1f, s * 0.05f))
    // Ojo
    drawCircle(color = Color(0xFF000000), radius = s * 0.03f, center = Offset(x + s * 0.25f, y - s * 0.12f))
    // Alas batiendo
    drawArc(color = body.copy(alpha = 0.8f), startAngle = -180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x - s * 0.3f, y - s * 0.05f + flap), size = Size(s * 0.4f, s * 0.2f))
}

private fun DrawScope.drawHamster(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x55000000), radius = s * 0.25f, center = Offset(x, y + s * 0.4f))
    val body = Color(0xFFFFE0B2)
    drawCircle(color = body, radius = s * 0.3f, center = Offset(x, y + s * 0.1f))
    drawCircle(color = Color(0xFFFFFFFF), radius = s * 0.2f, center = Offset(x, y + s * 0.18f))
    // Orejas redondas
    drawCircle(color = body, radius = s * 0.06f, center = Offset(x - s * 0.18f, y - s * 0.08f))
    drawCircle(color = body, radius = s * 0.06f, center = Offset(x + s * 0.18f, y - s * 0.08f))
    drawCircle(color = Color(0xFF000000), radius = s * 0.04f, center = Offset(x - s * 0.1f, y + s * 0.05f))
    drawCircle(color = Color(0xFF000000), radius = s * 0.04f, center = Offset(x + s * 0.1f, y + s * 0.05f))
}

private fun DrawScope.drawRabbit(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x55000000), radius = s * 0.3f, center = Offset(x, y + s * 0.5f))
    val body = Color(0xFFFFFFFF)
    drawCircle(color = body, radius = s * 0.25f, center = Offset(x, y + s * 0.15f))
    drawCircle(color = body, radius = s * 0.2f, center = Offset(x, y - s * 0.1f))
    // Orejas largas
    val earWiggle = sin((phase * 4f).toDouble()).toFloat() * s * 0.05f
    drawRect(color = body, topLeft = Offset(x - s * 0.15f + earWiggle, y - s * 0.45f), size = Size(s * 0.06f, s * 0.3f))
    drawRect(color = body, topLeft = Offset(x + s * 0.1f - earWiggle, y - s * 0.45f), size = Size(s * 0.06f, s * 0.3f))
    drawCircle(color = Color(0xFF000000), radius = s * 0.04f, center = Offset(x - s * 0.08f, y - s * 0.12f))
    drawCircle(color = Color(0xFF000000), radius = s * 0.04f, center = Offset(x + s * 0.08f, y - s * 0.12f))
    drawCircle(color = Color(0xFFE91E63), radius = s * 0.025f, center = Offset(x, y - s * 0.05f))
}

private fun DrawScope.drawFox(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x55000000), radius = s * 0.4f, center = Offset(x, y + s * 0.5f))
    val body = Color(0xFFFB8C00)
    val white = Color(0xFFFFFFFF)
    drawRect(color = body, topLeft = Offset(x - s * 0.4f, y - s * 0.05f), size = Size(s * 0.8f, s * 0.45f))
    drawCircle(color = body, radius = s * 0.32f, center = Offset(x + s * 0.4f, y - s * 0.05f))
    // Hocico blanco
    drawRect(color = white, topLeft = Offset(x + s * 0.55f, y), size = Size(s * 0.2f, s * 0.15f))
    // Orejas puntiagudas
    drawArc(color = body, startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x + s * 0.25f, y - s * 0.45f), size = Size(s * 0.18f, s * 0.25f))
    // Cola enorme y peluda
    val tailWag = sin((phase * 3f).toDouble()).toFloat() * s * 0.2f
    drawCircle(color = body, radius = s * 0.18f, center = Offset(x - s * 0.55f + tailWag, y - s * 0.05f))
    drawCircle(color = white, radius = s * 0.08f, center = Offset(x - s * 0.6f + tailWag, y - s * 0.1f))
    drawCircle(color = Color(0xFF000000), radius = s * 0.05f, center = Offset(x + s * 0.5f, y - s * 0.15f))
}

private fun DrawScope.drawDragon(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x55000000), radius = s * 0.5f, center = Offset(x, y + s * 0.6f))
    val body = Color(0xFF43A047)
    val belly = Color(0xFF81C784)
    drawRect(color = body, topLeft = Offset(x - s * 0.5f, y - s * 0.1f), size = Size(s * 1.0f, s * 0.6f))
    drawRect(color = belly, topLeft = Offset(x - s * 0.4f, y + s * 0.1f), size = Size(s * 0.8f, s * 0.4f))
    // Cabeza
    drawCircle(color = body, radius = s * 0.35f, center = Offset(x + s * 0.5f, y - s * 0.1f))
    // Cuernos
    drawArc(color = Color(0xFF4E342E), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x + s * 0.4f, y - s * 0.55f), size = Size(s * 0.1f, s * 0.2f))
    drawArc(color = Color(0xFF4E342E), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x + s * 0.6f, y - s * 0.55f), size = Size(s * 0.1f, s * 0.2f))
    // Alas
    val flap = sin((phase * 4f).toDouble()).toFloat() * s * 0.2f
    drawArc(color = body.copy(alpha = 0.9f), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x - s * 0.2f, y - s * 0.55f + flap), size = Size(s * 0.5f, s * 0.4f))
    // Ojo amarillo
    drawCircle(color = Color(0xFFFFEB3B), radius = s * 0.08f, center = Offset(x + s * 0.55f, y - s * 0.15f))
    drawCircle(color = Color(0xFF000000), radius = s * 0.04f, center = Offset(x + s * 0.55f, y - s * 0.15f))
    // Llamita por la nariz
    drawCircle(color = Color(0xFFFF5722), radius = s * 0.06f, center = Offset(x + s * 0.85f, y))
    drawCircle(color = Color(0xFFFFEB3B), radius = s * 0.03f, center = Offset(x + s * 0.85f, y))
}

// =====================================================================
//                              UFO
// =====================================================================

fun DrawScope.drawUfo(ufo: UfoSighting, screenX: Float, screenY: Float, tileSize: Float) {
    val s = tileSize * 1.2f
    when (ufo.type) {
        "saucer" -> drawSaucer(screenX, screenY, s, ufo.phase)
        "triangle" -> drawTriangleShip(screenX, screenY, s, ufo.phase)
        "cigar" -> drawCigar(screenX, screenY, s, ufo.phase)
        "orb" -> drawOrb(screenX, screenY, s, ufo.phase)
        else -> drawMothership(screenX, screenY, s, ufo.phase)
    }
}

private fun DrawScope.drawSaucer(x: Float, y: Float, s: Float, phase: Float) {
    // Brillo halo
    drawCircle(color = Color(0x4400FF00), radius = s * (0.6f + sin((phase*6f).toDouble()).toFloat() * 0.1f),
        center = Offset(x, y))
    // Cuerpo (elipse aplastada — usar arc trick)
    drawArc(color = Color(0xFF455A64), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(x - s * 0.5f, y - s * 0.15f), size = Size(s, s * 0.3f))
    // Cúpula
    drawArc(color = Color(0xCC00ACC1), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x - s * 0.25f, y - s * 0.3f), size = Size(s * 0.5f, s * 0.4f))
    // Luces parpadeantes
    val lightOn = (phase * 8f) % 1f < 0.5f
    val light = if (lightOn) Color(0xFFFFEB3B) else Color(0xFFFB8C00)
    for (i in 0 until 5) {
        val ang = i * 1.2566f
        drawCircle(color = light,
            radius = s * 0.04f,
            center = Offset(x + cos(ang.toDouble()).toFloat() * s * 0.4f, y + s * 0.05f))
    }
    // Rayo de abducción ocasional
    if (phase > 0.4f && phase < 0.7f) {
        drawArc(color = Color(0x6600FF00),
            startAngle = 80f, sweepAngle = 20f, useCenter = true,
            topLeft = Offset(x - s * 0.3f, y),
            size = Size(s * 0.6f, s * 4f))
    }
}

private fun DrawScope.drawTriangleShip(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x4400FFFF), radius = s * 0.6f, center = Offset(x, y))
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(x, y - s * 0.4f)
        lineTo(x + s * 0.5f, y + s * 0.2f)
        lineTo(x - s * 0.5f, y + s * 0.2f)
        close()
    }
    drawPath(path, color = Color(0xFF263238))
    drawPath(path, color = Color(0xFF00BCD4), style = Stroke(width = 2f))
    drawCircle(color = Color(0xFFFFEB3B), radius = s * 0.05f, center = Offset(x, y - s * 0.2f))
    drawCircle(color = Color(0xFFFF1744), radius = s * 0.05f, center = Offset(x + s * 0.3f, y + s * 0.1f))
    drawCircle(color = Color(0xFF00E676), radius = s * 0.05f, center = Offset(x - s * 0.3f, y + s * 0.1f))
}

private fun DrawScope.drawCigar(x: Float, y: Float, s: Float, phase: Float) {
    drawCircle(color = Color(0x44FFEB3B), radius = s * 0.7f, center = Offset(x, y))
    drawArc(color = Color(0xFFB0BEC5), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(x - s * 0.7f, y - s * 0.1f), size = Size(s * 1.4f, s * 0.2f))
    for (i in 0 until 6) {
        drawCircle(color = Color(0xFFFFEB3B), radius = s * 0.03f,
            center = Offset(x - s * 0.6f + i * s * 0.24f, y))
    }
}

private fun DrawScope.drawOrb(x: Float, y: Float, s: Float, phase: Float) {
    val pulse = (sin((phase * 12f).toDouble()).toFloat() * 0.5f + 0.5f)
    drawCircle(color = Color(0x88FF1744).copy(alpha = pulse * 0.6f),
        radius = s * (0.4f + pulse * 0.3f), center = Offset(x, y))
    drawCircle(color = Color(0xFFFFFFFF), radius = s * 0.25f, center = Offset(x, y))
    drawCircle(color = Color(0xFFFFEB3B), radius = s * 0.12f, center = Offset(x, y))
}

private fun DrawScope.drawMothership(x: Float, y: Float, s: Float, phase: Float) {
    val sBig = s * 2.5f
    drawCircle(color = Color(0x44E91E63), radius = sBig * 0.6f, center = Offset(x, y))
    drawArc(color = Color(0xFF263238), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(x - sBig * 0.5f, y - sBig * 0.2f), size = Size(sBig, sBig * 0.4f))
    // Doble cúpula
    drawArc(color = Color(0xCCAB47BC), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(x - sBig * 0.4f, y - sBig * 0.5f), size = Size(sBig * 0.8f, sBig * 0.6f))
    // Anillo de luces 12 puntos
    for (i in 0 until 12) {
        val ang = i * 0.5236f
        val on = ((phase * 8f).toInt() + i) % 2 == 0
        drawCircle(
            color = if (on) Color(0xFFFFEB3B) else Color(0xFF666666),
            radius = sBig * 0.025f,
            center = Offset(x + cos(ang.toDouble()).toFloat() * sBig * 0.45f,
                            y + sin(ang.toDouble()).toFloat() * sBig * 0.18f + sBig * 0.05f)
        )
    }
}

// =====================================================================
//                       FOLLOWER NPC
// =====================================================================

fun DrawScope.drawFollowerHalo(x: Float, y: Float, tileSize: Float, animPhase: Float) {
    // Halo amarillo pulsante para distinguir al follower
    val pulse = (sin((animPhase * 6.28f).toDouble()).toFloat() * 0.5f + 0.5f)
    drawCircle(
        color = Color(0xFFFFEB3B).copy(alpha = 0.3f + pulse * 0.3f),
        radius = tileSize * (0.55f + pulse * 0.1f),
        center = Offset(x, y)
    )
    // Burbuja "?" encima
    drawCircle(color = Color(0xFFFFFFFF), radius = tileSize * 0.18f,
        center = Offset(x + tileSize * 0.2f, y - tileSize * 0.6f))
    drawCircle(color = Color(0xFF263238), radius = tileSize * 0.15f,
        center = Offset(x + tileSize * 0.2f, y - tileSize * 0.6f),
        style = Stroke(width = 2f))
}
