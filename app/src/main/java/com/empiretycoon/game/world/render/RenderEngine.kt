package com.empiretycoon.game.world.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.empiretycoon.game.world.PetSpecies
import com.empiretycoon.game.world.PropKind
import com.empiretycoon.game.world.UfoSighting
import com.empiretycoon.game.world.Vehicle
import com.empiretycoon.game.world.sprites.drawFollowerHalo
import com.empiretycoon.game.world.sprites.drawNpc
import com.empiretycoon.game.world.sprites.drawPet
import com.empiretycoon.game.world.sprites.drawProp
import com.empiretycoon.game.world.sprites.drawUfo
import com.empiretycoon.game.world.sprites.drawVehicle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Motor de render propio para BalWealth.
 *
 * Características:
 *  - **Depth sorting**: dibuja objetos por su `worldY` (los del fondo primero,
 *    los del frente después). Olvídate de avatar bajo paredes.
 *  - **Iluminación nocturna real**: cada farol/faro/ventana proyecta un halo
 *    radial cálido sobre la escena en horas oscuras.
 *  - **Sombras direccionales**: cada objeto proyecta una sombra elíptica que
 *    sigue al sol según la hora.
 *  - **Cielo cinemático**: gradiente vertical que cambia con la hora real
 *    (azul día → naranja atardecer → púrpura crepúsculo → ámbar amanecer).
 *  - **Post-FX**: vignette suave + atmósfera tintada según hora/clima.
 */

// =====================================================================
//                      OBJETOS RENDERIZABLES (con Z)
// =====================================================================

sealed class RenderObject {
    abstract val worldX: Float       // baldosa
    abstract val worldY: Float       // baldosa — usado para depth sort
    abstract val zBoost: Float       // ajuste del orden (0 default)

    /** Dibuja en pantalla en (screenX, screenY) con tileSize. */
    abstract fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float)

    /** Si tiene sombra direccional, devuelve su radio aprox. en baldosas. */
    open val shadowRadius: Float get() = 0.4f
    open val castsShadow: Boolean get() = true
}

data class AvatarObject(
    override val worldX: Float,
    override val worldY: Float,
    override val zBoost: Float = 0f,
    val drawer: (DrawScope, Float, Float, Float, Float) -> Unit
) : RenderObject() {
    override fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float) {
        drawer(scope, screenX, screenY, tileSize, animPhase)
    }
    override val shadowRadius: Float get() = 0.5f
}

data class NpcObject(
    override val worldX: Float,
    override val worldY: Float,
    override val zBoost: Float = 0f,
    val drawer: (DrawScope, Float, Float, Float) -> Unit
) : RenderObject() {
    override fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float) {
        drawer(scope, screenX, screenY, tileSize)
    }
    override val shadowRadius: Float get() = 0.45f
}

data class VehicleObject(
    val vehicle: Vehicle,
    override val worldX: Float = vehicle.x,
    override val worldY: Float = vehicle.y,
    override val zBoost: Float = -0.1f
) : RenderObject() {
    override fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float) {
        with(scope) { drawVehicle(vehicle, screenX, screenY, tileSize) }
    }
    override val shadowRadius: Float get() = 0.7f
}

data class PropObject(
    val kind: PropKind,
    override val worldX: Float,
    override val worldY: Float,
    override val zBoost: Float = 0f
) : RenderObject() {
    override fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float) {
        with(scope) {
            drawProp(kind, screenX - tileSize / 2f, screenY - tileSize / 2f, tileSize, animPhase)
        }
    }
    // Los árboles tienen sombra grande, los faroles pequeña
    override val shadowRadius: Float
        get() = when (kind) {
            PropKind.TREE_OAK, PropKind.TREE_PINE, PropKind.TREE_BIRCH, PropKind.TREE_AUTUMN -> 0.45f
            PropKind.TREE_PALM -> 0.35f
            PropKind.LAMP_POST, PropKind.HYDRANT, PropKind.MAILBOX -> 0.15f
            PropKind.FOUNTAIN -> 0.6f
            PropKind.PARKED_CAR_RED, PropKind.PARKED_CAR_BLUE -> 0.65f
            PropKind.BUS_STOP_SHELTER, PropKind.NEWSPAPER_KIOSK -> 0.5f
            PropKind.CAFE_TABLE, PropKind.MARKET_STALL_RED, PropKind.MARKET_STALL_BLUE -> 0.4f
            else -> 0.25f
        }
}

data class PetObject(
    val species: PetSpecies,
    override val worldX: Float,
    override val worldY: Float,
    val walkPhase: Float,
    override val zBoost: Float = 0.05f
) : RenderObject() {
    override fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float) {
        with(scope) { drawPet(species, screenX, screenY, tileSize, walkPhase) }
    }
    override val shadowRadius: Float get() = 0.3f
}

data class UfoObject(
    val ufo: UfoSighting,
    override val worldX: Float = ufo.x,
    override val worldY: Float = ufo.y,
    override val zBoost: Float = 999f       // SIEMPRE encima de todo
) : RenderObject() {
    override fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float) {
        with(scope) { drawUfo(ufo, screenX, screenY, tileSize) }
    }
    override val castsShadow: Boolean get() = false
}

data class FollowerObject(
    val follower: com.empiretycoon.game.world.FollowerNpc,
    override val worldX: Float = follower.x,
    override val worldY: Float = follower.y,
    override val zBoost: Float = 0.5f,
    val animPhaseRef: () -> Float
) : RenderObject() {
    override fun draw(scope: DrawScope, screenX: Float, screenY: Float, tileSize: Float, animPhase: Float) {
        with(scope) {
            drawFollowerHalo(screenX, screenY, tileSize, animPhase)
            drawNpc(
                seed = follower.portraitSeed,
                x = screenX - tileSize / 2f,
                y = screenY - tileSize * 0.6f,
                scale = tileSize / 16f,
                walkPhase = 0f,
                facing = com.empiretycoon.game.world.Facing.DOWN
            )
        }
    }
    override val shadowRadius: Float get() = 0.45f
}

// =====================================================================
//                          CONTEXTO DE RENDER
// =====================================================================

data class RenderContext(
    val originTileX: Float,
    val originTileY: Float,
    val tileSize: Float,
    val animPhase: Float,
    val hour: Int,
    val weather: String,
    val sunAngleRad: Float,           // ángulo del sol — sombras
    val ambientLight: Float,          // 0..1 — multiplicador ambient global
    val viewW: Int,
    val viewH: Int
) {
    fun worldToScreen(wx: Float, wy: Float): Pair<Float, Float> =
        ((wx - originTileX) * tileSize) to ((wy - originTileY) * tileSize)
}

// =====================================================================
//                           SOL Y CIELO
// =====================================================================

object SkyEngine {
    /** Devuelve el gradient vertical del cielo según la hora (0-23). */
    fun gradientFor(hour: Int, minuteFrac: Float = 0f): Brush {
        val h = hour + minuteFrac
        val (top, mid, bot) = when {
            h < 5 -> Triple(Color(0xFF0A0E27), Color(0xFF1B1F3A), Color(0xFF2D1B5C))
            h < 6 -> Triple(Color(0xFF1B1F3A), Color(0xFF6A4480), Color(0xFFD68B5B))    // amanecer 1
            h < 7 -> Triple(Color(0xFF7E96B8), Color(0xFFE8A87C), Color(0xFFFFD49A))    // amanecer 2
            h < 11 -> Triple(Color(0xFF87CEEB), Color(0xFFB0E0E6), Color(0xFFE0F6FF))   // mañana
            h < 17 -> Triple(Color(0xFF4A90E2), Color(0xFF87CEEB), Color(0xFFC4E1F0))   // mediodía
            h < 19 -> Triple(Color(0xFF4A90E2), Color(0xFFFFB347), Color(0xFFFFD89B))   // tarde dorada
            h < 20 -> Triple(Color(0xFF6B4E8A), Color(0xFFE85D55), Color(0xFFF8B500))   // atardecer 1
            h < 21 -> Triple(Color(0xFF2C3E50), Color(0xFF8E44AD), Color(0xFFE74C3C))   // atardecer 2
            h < 22 -> Triple(Color(0xFF0A0E27), Color(0xFF1B1F3A), Color(0xFF4A148C))   // crepúsculo
            else -> Triple(Color(0xFF050516), Color(0xFF0A0E27), Color(0xFF1B1F3A))     // noche
        }
        return Brush.verticalGradient(colors = listOf(top, mid, bot))
    }

    /** Devuelve el ángulo del sol (rad) — para proyectar sombras. */
    fun sunAngle(hour: Int): Float {
        // Sol al este al amanecer (PI), cenit a mediodía (-PI/2), oeste atardecer (0)
        val h = hour.coerceIn(6, 20)
        val frac = (h - 6).toFloat() / 14f   // 0..1 a lo largo del día
        // Mapeo: 0 → PI/4 (sombra hacia oeste), 1 → 3PI/4 (este)
        return (PI.toFloat() * 0.25f) + frac * (PI.toFloat() * 0.5f)
    }

    /** Luz ambiental — cuán claro/oscuro está. */
    fun ambientFor(hour: Int): Float = when (hour) {
        in 11..16 -> 1.0f
        in 7..10 -> 0.9f
        in 17..18 -> 0.8f
        in 19..20 -> 0.55f
        in 21..22 -> 0.32f
        in 5..6 -> 0.5f
        else -> 0.18f                  // noche profunda
    }

    /** Dibuja gradient de cielo completo + nubes + sol/luna + estrellas. */
    fun DrawScope.drawSky(hour: Int, animPhase: Float) {
        // Gradient base
        drawRect(
            brush = gradientFor(hour),
            topLeft = Offset.Zero,
            size = size
        )

        val isDay = hour in 6..19
        val isDusk = hour in 5..6 || hour in 19..21
        val isNight = hour >= 21 || hour < 6

        // SOL grande con halo (siempre visible de día, intensidad varía)
        if (isDay) {
            // Posición del sol cruza de izquierda a derecha durante el día
            val frac = ((hour - 6) + animPhase * 0.05f).coerceIn(0f, 14f) / 14f
            val sunX = size.width * (0.15f + frac * 0.7f)
            val sunY = size.height * (0.30f - sin((frac * 3.14159f).toDouble()).toFloat() * 0.18f)
            val sunColor = when {
                hour in 11..15 -> Color(0xFFFFF59D)            // amarillo brillante
                hour in 7..10 || hour in 16..18 -> Color(0xFFFFCC80)  // amarillento cálido
                else -> Color(0xFFFF7043)                       // naranja
            }
            // Halo amplio + halo medio + núcleo
            drawCircle(color = sunColor.copy(alpha = 0.10f), radius = 120f, center = Offset(sunX, sunY))
            drawCircle(color = sunColor.copy(alpha = 0.20f), radius = 70f, center = Offset(sunX, sunY))
            drawCircle(color = sunColor.copy(alpha = 0.35f), radius = 45f, center = Offset(sunX, sunY))
            drawCircle(color = sunColor, radius = 26f, center = Offset(sunX, sunY))
            drawCircle(color = Color(0xCCFFFFFF), radius = 14f, center = Offset(sunX - 4f, sunY - 4f))
        }

        // NUBES procedurales — solo durante el día / amanecer
        if (isDay || isDusk) {
            drawClouds(animPhase, hour)
        }

        // PÁJAROS volando ocasionalmente (de día solo)
        if (isDay && hour in 7..18) {
            drawBirds(animPhase)
        }

        // ESTRELLAS + LUNA si es de noche
        if (isNight) {
            val seed = 42
            for (i in 0 until 80) {
                val px = ((i * 137 + seed) % 1000).toFloat() / 1000f * size.width
                val py = ((i * 251 + seed) % 800).toFloat() / 800f * (size.height * 0.55f)
                val twinkle = (sin((animPhase * 6.28f + i * 0.7f).toDouble()).toFloat() * 0.5f + 0.5f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f + twinkle * 0.5f),
                    radius = 0.8f + twinkle * 1.2f,
                    center = Offset(px, py)
                )
            }
            // Estrella fugaz ocasional
            val shoot = ((animPhase * 7f) % 4f)
            if (shoot < 0.3f) {
                val sx = size.width * (0.2f + shoot * 0.5f)
                val sy = size.height * (0.1f + shoot * 0.3f)
                drawLine(
                    color = Color(0xCCFFFFFF),
                    start = Offset(sx, sy),
                    end = Offset(sx + 40f, sy + 15f),
                    strokeWidth = 1.5f
                )
            }
            // Luna grande con cráteres
            val moonX = size.width * 0.78f
            val moonY = size.height * 0.18f
            drawCircle(color = Color(0xFFFFFFFF).copy(alpha = 0.15f), radius = 50f, center = Offset(moonX, moonY))
            drawCircle(color = Color(0xFFFFFFFF).copy(alpha = 0.25f), radius = 32f, center = Offset(moonX, moonY))
            drawCircle(color = Color(0xFFFFFCE5), radius = 24f, center = Offset(moonX, moonY))
            drawCircle(color = Color(0xFFE0DAA0), radius = 5f, center = Offset(moonX - 7f, moonY - 5f))
            drawCircle(color = Color(0xFFE0DAA0), radius = 3.5f, center = Offset(moonX + 6f, moonY + 4f))
            drawCircle(color = Color(0xFFE0DAA0), radius = 2.5f, center = Offset(moonX + 2f, moonY - 8f))
        }

        // RAYOS DE LUZ durante amanecer/atardecer (god rays sutiles)
        if (hour in 6..7 || hour in 18..20) {
            drawGodRays(animPhase, hour)
        }
    }

    /** Nubes procedurales que se mueven lento por el cielo. */
    private fun DrawScope.drawClouds(animPhase: Float, hour: Int) {
        val cloudColor = when {
            hour in 19..20 -> Color(0xCCFFCCAA)  // tinted naranja
            hour in 6..7 -> Color(0xCCFFE0CC)    // amanecer rosado
            else -> Color(0xCCFFFFFF)            // blanco normal
        }
        val drift = animPhase * 60f  // drift horizontal
        for (i in 0 until 6) {
            val baseX = ((i * 197) % 1000).toFloat() / 1000f * size.width
            val baseY = size.height * (0.08f + (i % 3) * 0.05f)
            val cx = ((baseX + drift) % (size.width + 200f)) - 100f
            val cy = baseY + sin((animPhase * 0.8f + i).toDouble()).toFloat() * 5f
            // Tres óvalos solapados forman una nube
            drawOval(
                color = cloudColor.copy(alpha = 0.55f),
                topLeft = Offset(cx, cy),
                size = Size(80f, 28f)
            )
            drawOval(
                color = cloudColor.copy(alpha = 0.85f),
                topLeft = Offset(cx + 18f, cy - 14f),
                size = Size(70f, 30f)
            )
            drawOval(
                color = cloudColor.copy(alpha = 0.7f),
                topLeft = Offset(cx + 50f, cy - 6f),
                size = Size(55f, 22f)
            )
            // Sombra de nube en su parte inferior
            drawOval(
                color = Color(0x44000000),
                topLeft = Offset(cx + 5f, cy + 14f),
                size = Size(75f, 12f)
            )
        }
    }

    private fun DrawScope.drawOval(color: Color, topLeft: Offset, size: Size) {
        drawArc(
            color = color,
            startAngle = 0f, sweepAngle = 360f, useCenter = true,
            topLeft = topLeft, size = size
        )
    }

    /** Bandadas de pájaros pequeños volando — V-formación. */
    private fun DrawScope.drawBirds(animPhase: Float) {
        val flockX = ((animPhase * 90f) % (size.width + 200f)) - 100f
        val flockY = size.height * 0.15f
        for (i in 0 until 5) {
            val bx = flockX - i * 12f
            val by = flockY + abs(i - 2) * 4f
            val flap = sin((animPhase * 18f + i).toDouble()).toFloat()
            val wingY = if (flap > 0f) -3f else 3f
            // Cuerpo
            drawCircle(color = Color(0xFF263238), radius = 1.5f, center = Offset(bx, by))
            // Alas con flap
            drawLine(
                color = Color(0xFF263238),
                start = Offset(bx - 5f, by + wingY),
                end = Offset(bx, by),
                strokeWidth = 1.2f
            )
            drawLine(
                color = Color(0xFF263238),
                start = Offset(bx, by),
                end = Offset(bx + 5f, by + wingY),
                strokeWidth = 1.2f
            )
        }
    }

    /** God rays diagonales suaves desde el sol — solo amanecer/atardecer. */
    private fun DrawScope.drawGodRays(animPhase: Float, hour: Int) {
        val frac = if (hour in 6..7) (hour - 6 + animPhase * 0.05f) / 1.5f
                   else (hour - 18 + animPhase * 0.05f) / 2f
        val sunX = size.width * (0.15f + frac.coerceIn(0f, 1f) * 0.7f)
        val sunY = size.height * 0.18f
        val rayColor = if (hour < 12) Color(0x33FFE0CC) else Color(0x33FFB890)
        for (i in 0 until 5) {
            val angle = (i - 2) * 0.18f + 0.2f
            val len = size.height * 0.7f
            val ex = sunX + cos((angle + 3.14159f / 2f).toDouble()).toFloat() * len
            val ey = sunY + sin((angle + 3.14159f / 2f).toDouble()).toFloat() * len
            drawLine(
                color = rayColor,
                start = Offset(sunX, sunY),
                end = Offset(ex, ey),
                strokeWidth = 18f
            )
        }
    }
}

// =====================================================================
//                           SOMBRAS DIRECCIONALES
// =====================================================================

object ShadowEngine {
    /** Dibuja sombra elíptica de un objeto en (screenX, screenY) según ángulo solar. */
    fun DrawScope.drawObjectShadow(
        screenX: Float,
        screenY: Float,
        radiusPx: Float,
        sunAngle: Float,
        ambient: Float
    ) {
        if (ambient < 0.4f) return  // de noche no hay sombra direccional
        // Cuanto más bajo el sol, más larga la sombra
        val sunHigh = sin(sunAngle.toDouble()).toFloat()    // cenit = 1, horizonte = 0
        val len = radiusPx * (1.5f + (1f - sunHigh) * 2.5f)
        val offsetX = -cos(sunAngle.toDouble()).toFloat() * len * 0.5f
        val offsetY = (1f - sunHigh) * radiusPx * 0.6f + radiusPx * 0.3f
        // Elipse aplastada usando arc 360
        drawArc(
            color = Color(0x44000000).copy(alpha = 0.25f * ambient),
            startAngle = 0f, sweepAngle = 360f, useCenter = true,
            topLeft = Offset(screenX + offsetX - len / 2f, screenY + offsetY - radiusPx * 0.3f),
            size = Size(len, radiusPx * 0.6f)
        )
    }
}

// =====================================================================
//                           ILUMINACIÓN NOCTURNA
// =====================================================================

object LightingEngine {
    /** Dibuja luces puntuales (faroles, ventanas, faros) sobre el frame. Solo se ve si está oscuro. */
    fun DrawScope.drawLights(
        lights: List<PointLight>,
        ambient: Float,
        screenSize: Size
    ) {
        if (ambient > 0.7f) return  // de día las luces son invisibles
        val lightStrength = (1f - ambient).coerceIn(0f, 1f)
        for (light in lights) {
            // Halo amplio
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        light.color.copy(alpha = light.intensity * lightStrength),
                        light.color.copy(alpha = 0f)
                    ),
                    center = Offset(light.x, light.y),
                    radius = light.radius
                ),
                radius = light.radius,
                center = Offset(light.x, light.y),
                blendMode = BlendMode.Screen
            )
            // Núcleo brillante
            drawCircle(
                color = light.color.copy(alpha = light.intensity * 0.8f * lightStrength),
                radius = light.radius * 0.18f,
                center = Offset(light.x, light.y),
                blendMode = BlendMode.Plus
            )
        }
    }
}

data class PointLight(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color = Color(0xFFFFEB3B),
    val intensity: Float = 0.9f
)

// =====================================================================
//                          POST-PROCESSING (vignette + tint)
// =====================================================================

object PostFx {
    /** Vignette suave en los bordes — añade profundidad cinematográfica. */
    fun DrawScope.drawVignette(strength: Float = 0.55f) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color(0x88000000).copy(alpha = strength)
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.maxDimension * 0.7f
            ),
            topLeft = Offset.Zero, size = size
        )
    }

    /**
     * Partículas ambientales (polen/polvo en el aire) — añaden vida.
     * Determinista vía hash, animadas con animPhase.
     */
    fun DrawScope.drawAmbientParticles(animPhase: Float, hour: Int) {
        val isDay = hour in 6..19
        if (!isDay) return  // de noche no se ven motas
        val particleColor = when {
            hour in 6..8 -> Color(0xFFFFE0B2)  // dorado amanecer
            hour in 17..19 -> Color(0xFFFFCC80) // ámbar atardecer
            else -> Color(0xFFFFF9C4)          // crema día
        }
        for (i in 0 until 32) {
            // Posición base determinista
            val baseX = ((i * 197 + 43) % 1000).toFloat() / 1000f * size.width
            val baseY = ((i * 251 + 89) % 1000).toFloat() / 1000f * size.height
            // Drift muy lento horizontal + bobbing vertical
            val driftX = (animPhase * 18f + i * 13f) % size.width
            val px = (baseX + driftX) % size.width
            val py = baseY + sin((animPhase * 3.0f + i).toDouble()).toFloat() * 8f
            val size = 1.2f + ((i and 0x3) * 0.5f)
            val alpha = 0.35f + sin((animPhase * 4f + i * 0.7f).toDouble()).toFloat() * 0.20f
            drawCircle(
                color = particleColor.copy(alpha = alpha),
                radius = size,
                center = Offset(px, py)
            )
        }
    }

    /**
     * Niebla atmosférica de profundidad — un velo MUY sutil que oscurece el
     * fondo, simulando perspectiva aérea. Se aplica solo en clima nublado o
     * de madrugada.
     */
    fun DrawScope.drawAtmosphericFog(weather: String, hour: Int) {
        val intensity = when {
            weather == "FOG" -> 0.35f
            weather == "RAIN" || weather == "STORM" -> 0.15f
            hour in 5..6 -> 0.18f                   // niebla matinal
            hour in 19..21 -> 0.10f                 // bruma del atardecer
            else -> 0f
        }
        if (intensity <= 0f) return
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE0E8F0).copy(alpha = intensity * 0.6f),
                    Color(0xFFE0E8F0).copy(alpha = intensity),
                    Color(0xFFE0E8F0).copy(alpha = intensity * 0.4f),
                    Color.Transparent
                ),
                startY = 0f, endY = size.height
            ),
            topLeft = Offset.Zero, size = size,
            blendMode = BlendMode.Screen
        )
    }

    /** Tinte atmosférico según hora del día. */
    fun DrawScope.drawAtmosphereTint(hour: Int) {
        val (color, alpha) = when {
            hour in 19..20 -> Color(0xFFFF8C42) to 0.12f         // hora dorada
            hour in 21..22 -> Color(0xFF6E5499) to 0.18f         // crepúsculo púrpura
            hour >= 23 || hour < 5 -> Color(0xFF1A237E) to 0.30f // noche azul
            hour in 5..6 -> Color(0xFFE8A87C) to 0.15f           // amanecer cálido
            else -> Color.Transparent to 0f
        }
        if (alpha > 0f) {
            drawRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset.Zero, size = size,
                blendMode = BlendMode.Multiply
            )
        }
    }

    /** Lluvia/nieve overlay extra (encima de todo). */
    fun DrawScope.drawWeatherFx(weather: String, animPhase: Float) {
        // El WeatherEngine ya dibuja gotas, esto añade glow extra
        if (weather == "STORM") {
            // Flash de relámpago muy ocasional
            if ((animPhase % 0.15f) < 0.008f) {
                drawRect(
                    color = Color(0x88FFFFFF),
                    topLeft = Offset.Zero, size = size
                )
            }
        }
    }
}
