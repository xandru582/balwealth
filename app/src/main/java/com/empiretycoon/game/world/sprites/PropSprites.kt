package com.empiretycoon.game.world.sprites

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.empiretycoon.game.world.PropKind
import kotlin.math.sin

/** Render de un mobiliario urbano en (x, y) en píxeles, con tamaño base [tileSize]. */
fun DrawScope.drawProp(kind: PropKind, x: Float, y: Float, tileSize: Float, animPhase: Float = 0f) {
    when (kind) {
        PropKind.TREE_OAK -> drawTreeOak(x, y, tileSize)
        PropKind.TREE_PINE -> drawTreePine(x, y, tileSize)
        PropKind.TREE_PALM -> drawTreePalm(x, y, tileSize, animPhase)
        PropKind.TREE_BIRCH -> drawTreeBirch(x, y, tileSize)
        PropKind.TREE_AUTUMN -> drawTreeAutumn(x, y, tileSize)
        PropKind.LAMP_POST -> drawLamp(x, y, tileSize, lit = true)
        PropKind.BENCH -> drawBenchProp(x, y, tileSize)
        PropKind.FOUNTAIN -> drawFountainProp(x, y, tileSize, animPhase)
        PropKind.TRASH_CAN -> drawTrashCan(x, y, tileSize)
        PropKind.PLANTER -> drawPlanter(x, y, tileSize)
        PropKind.NEWSPAPER_KIOSK -> drawKiosk(x, y, tileSize)
        PropKind.MAILBOX -> drawMailbox(x, y, tileSize)
        PropKind.HYDRANT -> drawHydrant(x, y, tileSize)
        PropKind.PARKED_CAR_RED -> drawParkedCar(x, y, tileSize, Color(0xFFE53935))
        PropKind.PARKED_CAR_BLUE -> drawParkedCar(x, y, tileSize, Color(0xFF1E88E5))
        PropKind.CAFE_TABLE -> drawCafeTable(x, y, tileSize)
        PropKind.BUS_STOP_SHELTER -> drawBusStop(x, y, tileSize)
        PropKind.STREET_SIGN -> drawSignPost(x, y, tileSize)
        PropKind.MARKET_STALL_RED -> drawMarketStall(x, y, tileSize, Color(0xFFE53935))
        PropKind.MARKET_STALL_BLUE -> drawMarketStall(x, y, tileSize, Color(0xFF1E88E5))
        PropKind.CHIMNEY_SMOKE -> drawSmoke(x, y, tileSize, animPhase)
        PropKind.BUSH -> drawBush(x, y, tileSize)
        PropKind.FLOWER_BED -> drawFlowerBed(x, y, tileSize)
    }
}

// ===== Árboles =====

private fun DrawScope.drawTreeOak(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    // Sombra elíptica
    drawArc(
        color = Color(0x55000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(cx - t * 0.4f, y + t * 0.78f), size = Size(t * 0.8f, t * 0.18f)
    )
    // Tronco con gradiente y vetas
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFF4E342E), Color(0xFF6D4C41), Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0xFF4E342E))
        ),
        topLeft = Offset(cx - t * 0.07f, y + t * 0.48f),
        size = Size(t * 0.14f, t * 0.42f)
    )
    // Vetas verticales
    drawLine(color = Color(0xFF3E2723), start = Offset(cx - t * 0.03f, y + t * 0.5f),
        end = Offset(cx - t * 0.03f, y + t * 0.88f), strokeWidth = 1f)
    drawLine(color = Color(0xFF3E2723), start = Offset(cx + t * 0.03f, y + t * 0.55f),
        end = Offset(cx + t * 0.03f, y + t * 0.85f), strokeWidth = 1f)
    // Sombra base bajo la copa (debajo, oscurece)
    drawCircle(color = Color(0x66000000), radius = t * 0.34f, center = Offset(cx, y + t * 0.5f))
    // Copa: 6 clusters de hojas en distintas tonalidades para dar volumen
    val leafGreens = listOf(
        Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C), Color(0xFF43A047), Color(0xFF66BB6A)
    )
    val clusters = listOf(
        Triple(-0.16f, 0.42f, 0.30f),  // izquierda baja
        Triple(0.16f, 0.42f, 0.30f),   // derecha baja
        Triple(-0.10f, 0.30f, 0.28f),  // izq media
        Triple(0.10f, 0.30f, 0.28f),   // der media
        Triple(0.0f, 0.18f, 0.30f),    // top centro
        Triple(0.0f, 0.32f, 0.24f)     // centro mid
    )
    for ((i, c) in clusters.withIndex()) {
        drawCircle(
            color = leafGreens[i % leafGreens.size],
            radius = t * c.third,
            center = Offset(cx + t * c.first, y + t * c.second)
        )
    }
    // Hojas individuales pequeñas (textura)
    for (i in 0 until 12) {
        val ang = i * 0.524f
        val rx = cx + sin(ang.toDouble()).toFloat() * t * 0.30f
        val ry = y + t * 0.30f - sin((ang + 1.57f).toDouble()).toFloat() * t * 0.18f
        drawCircle(
            color = if (i % 2 == 0) Color(0xFF66BB6A) else Color(0xFF81C784),
            radius = t * 0.025f,
            center = Offset(rx, ry)
        )
    }
    // Highlight superior izquierdo (luz del sol)
    drawCircle(color = Color(0x66FFFFFF), radius = t * 0.10f, center = Offset(cx - t * 0.12f, y + t * 0.20f))
    drawCircle(color = Color(0x33FFFFFF), radius = t * 0.18f, center = Offset(cx - t * 0.10f, y + t * 0.22f))
}

private fun DrawScope.drawTreePine(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    // Sombra base elíptica
    drawArc(
        color = Color(0x55000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(cx - t * 0.32f, y + t * 0.85f), size = Size(t * 0.64f, t * 0.16f)
    )
    // Tronco corto con gradiente
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFF4E342E), Color(0xFF6D4C41), Color(0xFF4E342E))
        ),
        topLeft = Offset(cx - t * 0.06f, y + t * 0.62f),
        size = Size(t * 0.12f, t * 0.32f)
    )
    // 4 capas de copa triangular (más realista que 3)
    for (i in 0 until 4) {
        val py = y + t * (0.10f + i * 0.16f)
        val w = t * (0.55f - i * 0.10f)
        // Capa de sombra (oscura, debajo)
        drawArc(
            color = Color(0xFF0D3B11),
            startAngle = -180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(cx - w / 2f, py + t * 0.02f),
            size = Size(w, t * 0.24f)
        )
        // Capa principal
        drawArc(
            color = Color(0xFF1B5E20),
            startAngle = -180f, sweepAngle = 180f, useCenter = true,
            topLeft = Offset(cx - w / 2f, py),
            size = Size(w, t * 0.24f)
        )
        // Highlight (luz del sol desde la izquierda)
        drawArc(
            color = Color(0xFF388E3C),
            startAngle = -180f, sweepAngle = 90f, useCenter = true,
            topLeft = Offset(cx - w / 2f, py),
            size = Size(w, t * 0.24f)
        )
        // Highlight más claro
        drawArc(
            color = Color(0xFF66BB6A),
            startAngle = -170f, sweepAngle = 50f, useCenter = true,
            topLeft = Offset(cx - w * 0.45f, py),
            size = Size(w * 0.7f, t * 0.18f)
        )
    }
    // Punta final con corona
    drawCircle(color = Color(0xFFFFD54F).copy(alpha = 0.4f),
        radius = t * 0.04f, center = Offset(cx, y + t * 0.08f))
}

private fun DrawScope.drawTreePalm(x: Float, y: Float, t: Float, animPhase: Float) {
    val cx = x + t / 2f
    drawCircle(color = Color(0x55000000), radius = t * 0.28f, center = Offset(cx, y + t * 0.9f))
    drawRect(color = Color(0xFF8D6E63), topLeft = Offset(cx - t * 0.04f, y + t * 0.4f), size = Size(t * 0.08f, t * 0.55f))
    // 5 hojas que se mecen
    val sway = sin((animPhase * 6.28f).toDouble()).toFloat() * t * 0.05f
    for (i in 0 until 5) {
        val ang = i * 1.2566f - 0.6f
        val ex = cx + sin(ang.toDouble()).toFloat() * t * 0.3f + sway
        val ey = y + t * 0.35f - sin((ang + 1.5708f).toDouble()).toFloat() * t * 0.2f
        drawArc(
            color = Color(0xFF43A047),
            startAngle = -90f + i * 72f, sweepAngle = 60f, useCenter = true,
            topLeft = Offset(cx - t * 0.32f, y + t * 0.1f),
            size = Size(t * 0.65f, t * 0.65f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = t * 0.06f)
        )
    }
    // Cocos
    drawCircle(color = Color(0xFF6D4C41), radius = t * 0.04f, center = Offset(cx - t * 0.06f, y + t * 0.4f))
    drawCircle(color = Color(0xFF6D4C41), radius = t * 0.04f, center = Offset(cx + t * 0.06f, y + t * 0.42f))
}

private fun DrawScope.drawTreeBirch(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    drawArc(
        color = Color(0x55000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(cx - t * 0.30f, y + t * 0.83f), size = Size(t * 0.60f, t * 0.14f)
    )
    // Tronco blanco con vetas (estilo abedul plateado) y gradiente
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFFBDBDBD), Color(0xFFEEEEEE), Color(0xFFFFFFFF), Color(0xFFEEEEEE), Color(0xFFBDBDBD))
        ),
        topLeft = Offset(cx - t * 0.06f, y + t * 0.40f),
        size = Size(t * 0.12f, t * 0.50f)
    )
    // Vetas negras estilo abedul
    drawRect(color = Color(0xFF424242), topLeft = Offset(cx - t * 0.06f, y + t * 0.48f), size = Size(t * 0.12f, t * 0.025f))
    drawRect(color = Color(0xFF212121), topLeft = Offset(cx - t * 0.04f, y + t * 0.62f), size = Size(t * 0.08f, t * 0.020f))
    drawRect(color = Color(0xFF424242), topLeft = Offset(cx - t * 0.06f, y + t * 0.75f), size = Size(t * 0.12f, t * 0.025f))
    drawCircle(color = Color(0xFF212121), radius = t * 0.012f, center = Offset(cx + t * 0.02f, y + t * 0.68f))
    drawCircle(color = Color(0xFF212121), radius = t * 0.010f, center = Offset(cx - t * 0.02f, y + t * 0.55f))
    // Copa: hojas amarillo-verdes en clusters
    drawCircle(color = Color(0xFF8BC34A), radius = t * 0.28f, center = Offset(cx - t * 0.10f, y + t * 0.30f))
    drawCircle(color = Color(0xFFAED581), radius = t * 0.28f, center = Offset(cx + t * 0.10f, y + t * 0.30f))
    drawCircle(color = Color(0xFFC5E1A5), radius = t * 0.25f, center = Offset(cx, y + t * 0.18f))
    drawCircle(color = Color(0xFFDCEDC8), radius = t * 0.18f, center = Offset(cx + t * 0.08f, y + t * 0.20f))
    // Hojitas pequeñas individuales
    for (i in 0 until 8) {
        val ang = i * 0.785f
        val rx = cx + sin(ang.toDouble()).toFloat() * t * 0.28f
        val ry = y + t * 0.25f - sin((ang + 1.57f).toDouble()).toFloat() * t * 0.16f
        drawCircle(color = Color(0xFFFFEB3B), radius = t * 0.020f, center = Offset(rx, ry))
    }
    // Highlight superior
    drawCircle(color = Color(0x77FFFFFF), radius = t * 0.08f, center = Offset(cx - t * 0.10f, y + t * 0.14f))
}

private fun DrawScope.drawTreeAutumn(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    drawArc(
        color = Color(0x66000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(cx - t * 0.40f, y + t * 0.78f), size = Size(t * 0.80f, t * 0.18f)
    )
    // Tronco
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
            listOf(Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFF8D6E63), Color(0xFF5D4037), Color(0xFF3E2723))
        ),
        topLeft = Offset(cx - t * 0.07f, y + t * 0.48f),
        size = Size(t * 0.14f, t * 0.42f)
    )
    drawLine(color = Color(0xFF2E1A14), start = Offset(cx, y + t * 0.50f),
        end = Offset(cx, y + t * 0.88f), strokeWidth = 1.5f)
    // Sombra base bajo la copa
    drawCircle(color = Color(0x66000000), radius = t * 0.34f, center = Offset(cx, y + t * 0.50f))
    // Copa otoñal: mezcla de naranjas, rojos, amarillos en clusters
    val autumnColors = listOf(
        Color(0xFFD84315), Color(0xFFE65100), Color(0xFFEF6C00),
        Color(0xFFFB8C00), Color(0xFFFFA726), Color(0xFFFFC107)
    )
    val clusters = listOf(
        Triple(-0.16f, 0.42f, 0.30f),
        Triple(0.16f, 0.42f, 0.30f),
        Triple(-0.10f, 0.30f, 0.28f),
        Triple(0.10f, 0.30f, 0.28f),
        Triple(0.0f, 0.18f, 0.30f),
        Triple(0.0f, 0.32f, 0.24f)
    )
    for ((i, c) in clusters.withIndex()) {
        drawCircle(
            color = autumnColors[i % autumnColors.size],
            radius = t * c.third,
            center = Offset(cx + t * c.first, y + t * c.second)
        )
    }
    // Hojas individuales rojizas
    for (i in 0 until 14) {
        val ang = i * 0.448f
        val rx = cx + sin(ang.toDouble()).toFloat() * t * 0.32f
        val ry = y + t * 0.30f - sin((ang + 1.57f).toDouble()).toFloat() * t * 0.18f
        drawCircle(
            color = if (i % 3 == 0) Color(0xFFD32F2F)
                    else if (i % 3 == 1) Color(0xFFFF5722)
                    else Color(0xFFFFEB3B),
            radius = t * 0.025f,
            center = Offset(rx, ry)
        )
    }
    // Highlight cálido superior
    drawCircle(color = Color(0x77FFE0B2), radius = t * 0.10f, center = Offset(cx - t * 0.12f, y + t * 0.18f))
    // Algunas hojas que caen
    for (i in 0 until 3) {
        val fx = cx + (i - 1) * t * 0.18f
        val fy = y + t * (0.78f + i * 0.04f)
        drawCircle(color = Color(0xFFFB8C00), radius = t * 0.020f, center = Offset(fx, fy))
    }
}

// ===== Mobiliario =====

private fun DrawScope.drawLamp(x: Float, y: Float, t: Float, lit: Boolean) {
    val cx = x + t / 2f
    val bulbX = cx + t * 0.20f
    val bulbY = y + t * 0.27f

    // Cono de luz proyectado en el suelo (radial gradient elíptico)
    if (lit) {
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFEB3B).copy(alpha = 0.30f),
                    Color(0xFFFFEB3B).copy(alpha = 0.18f),
                    Color(0xFFFFEB3B).copy(alpha = 0f)
                ),
                center = Offset(bulbX, y + t * 0.95f),
                radius = t * 0.55f
            ),
            radius = t * 0.55f,
            center = Offset(bulbX, y + t * 0.95f)
        )
    }

    // Sombra base elíptica
    drawArc(color = Color(0x55000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(cx - t * 0.13f, y + t * 0.85f), size = Size(t * 0.26f, t * 0.10f))

    // Base ornamental
    drawRect(color = Color(0xFF263238), topLeft = Offset(cx - t * 0.08f, y + t * 0.86f), size = Size(t * 0.16f, t * 0.06f))
    drawRect(color = Color(0xFF455A64), topLeft = Offset(cx - t * 0.06f, y + t * 0.83f), size = Size(t * 0.12f, t * 0.04f))

    // Poste con highlight + sombra
    drawRect(color = Color(0xFF1B262C), topLeft = Offset(cx - t * 0.035f, y + t * 0.25f), size = Size(t * 0.07f, t * 0.60f))
    drawRect(color = Color(0xFF455A64), topLeft = Offset(cx - t * 0.025f, y + t * 0.25f), size = Size(t * 0.018f, t * 0.60f))

    // Brazo curvo (rectángulos escalonados para efecto "L")
    drawRect(color = Color(0xFF263238), topLeft = Offset(cx, y + t * 0.23f), size = Size(t * 0.20f, t * 0.04f))
    drawRect(color = Color(0xFF263238), topLeft = Offset(cx + t * 0.18f, y + t * 0.23f), size = Size(t * 0.04f, t * 0.05f))

    // Capucha de la lámpara (forma cónica)
    drawArc(color = Color(0xFF1B262C), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(bulbX - t * 0.10f, bulbY - t * 0.10f), size = Size(t * 0.20f, t * 0.12f))

    // Bombilla con halo cálido
    val bulbColor = if (lit) Color(0xFFFFF59D) else Color(0xFF616161)
    if (lit) {
        // Halo amplio
        drawCircle(color = Color(0xFFFFEB3B).copy(alpha = 0.30f), radius = t * 0.28f, center = Offset(bulbX, bulbY))
        drawCircle(color = Color(0xFFFFEB3B).copy(alpha = 0.45f), radius = t * 0.16f, center = Offset(bulbX, bulbY))
    }
    drawCircle(color = bulbColor, radius = t * 0.08f, center = Offset(bulbX, bulbY))
    if (lit) {
        // Núcleo blanco brillante
        drawCircle(color = Color(0xFFFFFFFF), radius = t * 0.04f, center = Offset(bulbX, bulbY))
    }
}

private fun DrawScope.drawBenchProp(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    // Sombra base
    drawArc(color = Color(0x55000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(x + t * 0.05f, y + t * 0.86f), size = Size(t * 0.90f, t * 0.10f))

    // Patas metálicas con highlight
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.18f, y + t * 0.67f), size = Size(t * 0.06f, t * 0.22f))
    drawRect(color = Color(0xFF455A64), topLeft = Offset(x + t * 0.18f, y + t * 0.67f), size = Size(t * 0.018f, t * 0.22f))
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.76f, y + t * 0.67f), size = Size(t * 0.06f, t * 0.22f))
    drawRect(color = Color(0xFF455A64), topLeft = Offset(x + t * 0.76f, y + t * 0.67f), size = Size(t * 0.018f, t * 0.22f))

    // Asiento (3 tablones de madera con grano)
    for (i in 0 until 3) {
        val py = y + t * (0.55f + i * 0.04f)
        drawRect(color = Color(0xFF8D6E63), topLeft = Offset(x + t * 0.10f, py), size = Size(t * 0.80f, t * 0.038f))
        drawRect(color = Color(0xFF6D4C41), topLeft = Offset(x + t * 0.10f, py + t * 0.030f), size = Size(t * 0.80f, t * 0.008f))
        // Veta de madera
        drawLine(color = Color(0xFF5D4037),
            start = Offset(x + t * 0.20f, py + t * 0.018f),
            end = Offset(x + t * 0.70f, py + t * 0.018f), strokeWidth = 0.8f)
    }

    // Respaldo (3 listones verticales)
    drawRect(color = Color(0xFF6D4C41), topLeft = Offset(x + t * 0.10f, y + t * 0.40f), size = Size(t * 0.80f, t * 0.06f))
    for (i in 0 until 5) {
        val sx = x + t * (0.16f + i * 0.165f)
        drawRect(color = Color(0xFF5D4037), topLeft = Offset(sx, y + t * 0.40f), size = Size(t * 0.025f, t * 0.16f))
    }

    // Highlights (luz desde arriba-izq)
    drawRect(color = Color(0x77FFFFFF), topLeft = Offset(x + t * 0.12f, y + t * 0.40f), size = Size(t * 0.40f, 1.5f))
}

private fun DrawScope.drawFountainProp(x: Float, y: Float, t: Float, animPhase: Float) {
    val cx = x + t / 2f
    val cy = y + t * 0.62f

    // Sombra elíptica grande
    drawArc(color = Color(0x77000000), startAngle = 0f, sweepAngle = 360f, useCenter = true,
        topLeft = Offset(cx - t * 0.55f, cy + t * 0.10f), size = Size(t * 1.10f, t * 0.20f))

    // Cuenco con bordes biselados (3 capas de gris para depth)
    drawCircle(color = Color(0xFF78909C), radius = t * 0.52f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFB0BEC5), radius = t * 0.48f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF607D8B), radius = t * 0.42f, center = Offset(cx, cy))

    // Borde decorativo del cuenco con textura de piedra
    for (i in 0 until 12) {
        val ang = i * 0.5236f  // 30 grados
        val rx = cx + sin(ang.toDouble()).toFloat() * t * 0.45f
        val ry = cy + sin((ang + 1.57f).toDouble()).toFloat() * t * 0.20f
        drawCircle(color = Color(0xFF455A64), radius = t * 0.025f, center = Offset(rx, ry))
        drawCircle(color = Color(0xFFCFD8DC), radius = t * 0.012f, center = Offset(rx - 1f, ry - 1f))
    }

    // Agua con gradiente radial (más oscuro en bordes)
    drawCircle(
        brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(Color(0xFF81D4FA), Color(0xFF42A5F5), Color(0xFF1565C0)),
            center = Offset(cx, cy),
            radius = t * 0.38f
        ),
        radius = t * 0.38f,
        center = Offset(cx, cy)
    )

    // Cáusticos animados sobre el agua
    for (i in 0 until 4) {
        val ang = animPhase * 6.28f + i * 1.57f
        val rr = t * 0.20f * (sin((animPhase * 4f + i).toDouble()).toFloat() * 0.3f + 0.7f)
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = 0.20f),
            radius = rr,
            center = Offset(
                cx + sin(ang.toDouble()).toFloat() * t * 0.10f,
                cy + sin((ang + 1.57f).toDouble()).toFloat() * t * 0.05f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
    }

    // Pedestal central con highlight
    drawRect(color = Color(0xFF607D8B), topLeft = Offset(cx - t * 0.075f, y + t * 0.18f), size = Size(t * 0.15f, t * 0.45f))
    drawRect(color = Color(0xFFB0BEC5), topLeft = Offset(cx - t * 0.06f, y + t * 0.18f), size = Size(t * 0.04f, t * 0.45f))
    drawRect(color = Color(0xFF455A64), topLeft = Offset(cx + t * 0.04f, y + t * 0.18f), size = Size(t * 0.035f, t * 0.45f))
    // Capitel decorativo
    drawRect(color = Color(0xFFCFD8DC), topLeft = Offset(cx - t * 0.10f, y + t * 0.16f), size = Size(t * 0.20f, t * 0.04f))

    // Surtidor central — chorro vertical animado con partículas
    val splash = (sin((animPhase * 6.28f).toDouble()) * 0.5 + 0.5).toFloat()
    val jetH = t * (0.18f + splash * 0.05f)
    val jetTopY = y + t * 0.16f - jetH
    // Chorro principal (gradient)
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color(0x88FFFFFF), Color(0xCC81D4FA))
        ),
        topLeft = Offset(cx - t * 0.025f, jetTopY),
        size = Size(t * 0.05f, jetH)
    )
    // Cabeza del chorro
    drawCircle(color = Color(0xFFFFFFFF), radius = t * (0.05f + splash * 0.025f), center = Offset(cx, jetTopY))
    drawCircle(color = Color(0xCC81D4FA), radius = t * (0.07f + splash * 0.035f), center = Offset(cx, jetTopY))

    // 8 gotas de spray cayendo en distintas direcciones
    for (i in 0 until 8) {
        val ang = i * 0.785f
        val phase = ((animPhase + i * 0.13f) % 1f)
        val dist = t * (0.10f + phase * 0.30f)
        val dropX = cx + sin(ang.toDouble()).toFloat() * dist
        val dropY = jetTopY + phase * t * 0.25f - sin((phase * 3.14f).toDouble()).toFloat() * t * 0.15f
        drawCircle(
            color = Color(0xCC81D4FA).copy(alpha = (1f - phase) * 0.8f),
            radius = t * 0.018f * (1f - phase * 0.5f),
            center = Offset(dropX, dropY)
        )
    }

    // Salpicaduras sobre el agua del cuenco
    for (i in 0 until 5) {
        val ang = i * 1.257f + animPhase * 3f
        val ringPhase = ((animPhase + i * 0.2f) % 1f)
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = (1f - ringPhase) * 0.5f),
            radius = t * (0.04f + ringPhase * 0.10f),
            center = Offset(
                cx + sin(ang.toDouble()).toFloat() * t * 0.15f,
                cy + sin((ang + 1.57f).toDouble()).toFloat() * t * 0.08f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawTrashCan(x: Float, y: Float, t: Float) {
    drawRect(color = Color(0xFF455A64), topLeft = Offset(x + t * 0.35f, y + t * 0.45f), size = Size(t * 0.3f, t * 0.45f))
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.32f, y + t * 0.42f), size = Size(t * 0.36f, t * 0.06f))
    drawRect(color = Color(0xFFFFEB3B), topLeft = Offset(x + t * 0.38f, y + t * 0.55f), size = Size(t * 0.1f, t * 0.1f))
}

private fun DrawScope.drawPlanter(x: Float, y: Float, t: Float) {
    drawRect(color = Color(0xFF8D6E63), topLeft = Offset(x + t * 0.2f, y + t * 0.55f), size = Size(t * 0.6f, t * 0.3f))
    drawRect(color = Color(0xFF6D4C41), topLeft = Offset(x + t * 0.2f, y + t * 0.55f), size = Size(t * 0.6f, t * 0.05f))
    // Plantas
    drawCircle(color = Color(0xFF388E3C), radius = t * 0.12f, center = Offset(x + t * 0.35f, y + t * 0.45f))
    drawCircle(color = Color(0xFF2E7D32), radius = t * 0.12f, center = Offset(x + t * 0.55f, y + t * 0.43f))
    drawCircle(color = Color(0xFFE91E63), radius = t * 0.04f, center = Offset(x + t * 0.42f, y + t * 0.4f))
}

private fun DrawScope.drawKiosk(x: Float, y: Float, t: Float) {
    drawRect(color = Color(0xFFE53935), topLeft = Offset(x + t * 0.15f, y + t * 0.3f), size = Size(t * 0.7f, t * 0.6f))
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.15f, y + t * 0.3f), size = Size(t * 0.7f, t * 0.08f))
    drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(x + t * 0.22f, y + t * 0.42f), size = Size(t * 0.56f, t * 0.3f))
    // "PRENSA"
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.3f, y + t * 0.5f), size = Size(t * 0.4f, t * 0.04f))
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.3f, y + t * 0.58f), size = Size(t * 0.4f, t * 0.04f))
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.3f, y + t * 0.66f), size = Size(t * 0.4f, t * 0.04f))
}

private fun DrawScope.drawMailbox(x: Float, y: Float, t: Float) {
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.45f, y + t * 0.5f), size = Size(t * 0.06f, t * 0.4f))
    drawRect(color = Color(0xFFE53935), topLeft = Offset(x + t * 0.32f, y + t * 0.35f), size = Size(t * 0.32f, t * 0.22f))
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.4f, y + t * 0.4f), size = Size(t * 0.16f, t * 0.04f))
}

private fun DrawScope.drawHydrant(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    drawRect(color = Color(0xFFD32F2F), topLeft = Offset(cx - t * 0.06f, y + t * 0.55f), size = Size(t * 0.12f, t * 0.3f))
    drawCircle(color = Color(0xFFD32F2F), radius = t * 0.08f, center = Offset(cx, y + t * 0.55f))
    drawRect(color = Color(0xFFFFD700), topLeft = Offset(cx - t * 0.02f, y + t * 0.5f), size = Size(t * 0.04f, t * 0.04f))
    drawRect(color = Color(0xFF424242), topLeft = Offset(cx - t * 0.1f, y + t * 0.85f), size = Size(t * 0.2f, t * 0.04f))
}

private fun DrawScope.drawParkedCar(x: Float, y: Float, t: Float, color: Color) {
    drawRect(color = Color(0x55000000), topLeft = Offset(x + t * 0.05f, y + t * 0.7f), size = Size(t * 0.9f, t * 0.18f))
    drawRect(color = color, topLeft = Offset(x + t * 0.08f, y + t * 0.45f), size = Size(t * 0.84f, t * 0.32f))
    drawRect(color = color.darken(0.2f), topLeft = Offset(x + t * 0.08f, y + t * 0.55f), size = Size(t * 0.84f, t * 0.04f))
    // Ventanas
    drawRect(color = Color(0xCC81D4FA), topLeft = Offset(x + t * 0.15f, y + t * 0.38f), size = Size(t * 0.7f, t * 0.12f))
    drawRect(color = Color(0xFF1B1B3A), topLeft = Offset(x + t * 0.5f, y + t * 0.38f), size = Size(t * 0.02f, t * 0.12f))
    // Ruedas
    drawCircle(color = Color(0xFF263238), radius = t * 0.08f, center = Offset(x + t * 0.22f, y + t * 0.78f))
    drawCircle(color = Color(0xFF263238), radius = t * 0.08f, center = Offset(x + t * 0.78f, y + t * 0.78f))
    drawCircle(color = Color(0xFF9E9E9E), radius = t * 0.04f, center = Offset(x + t * 0.22f, y + t * 0.78f))
    drawCircle(color = Color(0xFF9E9E9E), radius = t * 0.04f, center = Offset(x + t * 0.78f, y + t * 0.78f))
}

private fun DrawScope.drawCafeTable(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    drawRect(color = Color(0x55000000), topLeft = Offset(cx - t * 0.25f, y + t * 0.78f), size = Size(t * 0.5f, t * 0.05f))
    drawRect(color = Color(0xFFB0BEC5), topLeft = Offset(cx - t * 0.03f, y + t * 0.55f), size = Size(t * 0.06f, t * 0.3f))
    drawCircle(color = Color(0xFFFFFFFF), radius = t * 0.18f, center = Offset(cx, y + t * 0.5f))
    drawCircle(color = Color(0xFF607D8B), radius = t * 0.18f, center = Offset(cx, y + t * 0.5f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
    // Sombrilla
    drawArc(color = Color(0xFFD32F2F), startAngle = 180f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(cx - t * 0.3f, y + t * 0.05f), size = Size(t * 0.6f, t * 0.4f))
}

private fun DrawScope.drawBusStop(x: Float, y: Float, t: Float) {
    drawRect(color = Color(0xFF263238), topLeft = Offset(x + t * 0.1f, y + t * 0.85f), size = Size(t * 0.8f, t * 0.05f))
    // Postes
    drawRect(color = Color(0xFF424242), topLeft = Offset(x + t * 0.12f, y + t * 0.2f), size = Size(t * 0.04f, t * 0.65f))
    drawRect(color = Color(0xFF424242), topLeft = Offset(x + t * 0.84f, y + t * 0.2f), size = Size(t * 0.04f, t * 0.65f))
    // Techo
    drawRect(color = Color(0xFFFFD166), topLeft = Offset(x + t * 0.05f, y + t * 0.18f), size = Size(t * 0.9f, t * 0.06f))
    // Cristal
    drawRect(color = Color(0x55BBDEFB), topLeft = Offset(x + t * 0.16f, y + t * 0.3f), size = Size(t * 0.66f, t * 0.5f))
    // Banco interior
    drawRect(color = Color(0xFF6D4C41), topLeft = Offset(x + t * 0.2f, y + t * 0.7f), size = Size(t * 0.6f, t * 0.08f))
}

private fun DrawScope.drawSignPost(x: Float, y: Float, t: Float) {
    val cx = x + t / 2f
    drawRect(color = Color(0xFF424242), topLeft = Offset(cx - t * 0.03f, y + t * 0.3f), size = Size(t * 0.06f, t * 0.6f))
    drawRect(color = Color(0xFF1E88E5), topLeft = Offset(x + t * 0.1f, y + t * 0.3f), size = Size(t * 0.8f, t * 0.18f))
    drawRect(color = Color(0xFFFFFFFF), topLeft = Offset(x + t * 0.15f, y + t * 0.36f), size = Size(t * 0.7f, t * 0.06f))
}

private fun DrawScope.drawMarketStall(x: Float, y: Float, t: Float, awningColor: Color) {
    drawRect(color = Color(0xFF6D4C41), topLeft = Offset(x + t * 0.1f, y + t * 0.55f), size = Size(t * 0.8f, t * 0.3f))
    // Rayas en el toldo
    for (i in 0 until 4) {
        val sx = x + t * 0.05f + i * t * 0.225f
        drawRect(
            color = if (i % 2 == 0) awningColor else Color(0xFFFFFFFF),
            topLeft = Offset(sx, y + t * 0.3f),
            size = Size(t * 0.225f, t * 0.18f)
        )
    }
    // Mostrador con frutas/objetos
    drawCircle(color = Color(0xFFE53935), radius = t * 0.05f, center = Offset(x + t * 0.3f, y + t * 0.6f))
    drawCircle(color = Color(0xFF43A047), radius = t * 0.05f, center = Offset(x + t * 0.5f, y + t * 0.62f))
    drawCircle(color = Color(0xFFFFC107), radius = t * 0.05f, center = Offset(x + t * 0.7f, y + t * 0.6f))
}

private fun DrawScope.drawSmoke(x: Float, y: Float, t: Float, animPhase: Float) {
    for (i in 0 until 4) {
        val phase = (animPhase + i * 0.25f) % 1f
        drawCircle(
            color = Color(0x88AAAAAA).copy(alpha = (1f - phase) * 0.6f),
            radius = t * (0.08f + phase * 0.18f),
            center = Offset(x + t * 0.5f + sin((phase * 6.28f).toDouble()).toFloat() * t * 0.1f, y + t * 0.5f - phase * t * 0.6f)
        )
    }
}

private fun DrawScope.drawBush(x: Float, y: Float, t: Float) {
    drawCircle(color = Color(0x55000000), radius = t * 0.25f, center = Offset(x + t / 2f, y + t * 0.85f))
    drawCircle(color = Color(0xFF1B5E20), radius = t * 0.22f, center = Offset(x + t * 0.4f, y + t * 0.65f))
    drawCircle(color = Color(0xFF2E7D32), radius = t * 0.22f, center = Offset(x + t * 0.6f, y + t * 0.65f))
    drawCircle(color = Color(0xFF388E3C), radius = t * 0.18f, center = Offset(x + t * 0.5f, y + t * 0.55f))
}

private fun DrawScope.drawFlowerBed(x: Float, y: Float, t: Float) {
    drawRect(color = Color(0xFF6D4C41), topLeft = Offset(x + t * 0.1f, y + t * 0.75f), size = Size(t * 0.8f, t * 0.12f))
    // 6 flores variadas
    val colors = listOf(
        Color(0xFFE91E63), Color(0xFFFFEB3B), Color(0xFFFFFFFF),
        Color(0xFFFB8C00), Color(0xFFAB47BC), Color(0xFFEF5350)
    )
    for (i in 0 until 6) {
        val fx = x + t * 0.18f + i * t * 0.12f
        val fy = y + t * 0.72f
        drawCircle(color = Color(0xFF388E3C), radius = t * 0.025f, center = Offset(fx, fy + t * 0.08f))
        drawCircle(color = colors[i], radius = t * 0.045f, center = Offset(fx, fy))
        drawCircle(color = Color(0xFFFFEB3B), radius = t * 0.015f, center = Offset(fx, fy))
    }
}
