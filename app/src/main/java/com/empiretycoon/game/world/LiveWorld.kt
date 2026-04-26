package com.empiretycoon.game.world

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================
//                              CLIMA
// =====================================================================

enum class Weather(val displayName: String, val emoji: String, val brightness: Float, val particleColor: Long, val particleCount: Int) {
    SUNNY    ("Soleado",     "☀️",  1.10f, 0x00000000L,         0),
    CLOUDY   ("Nublado",     "☁️",  0.85f, 0x00000000L,         0),
    RAINY    ("Lluvia",      "🌧",   0.70f, 0xCCB3E5FCL,         80),
    STORM    ("Tormenta",    "⛈",  0.55f, 0xFFB3E5FCL,         150),
    FOG      ("Niebla",      "🌫",   0.85f, 0x66FFFFFFL,         40),
    SNOW     ("Nieve",       "❄️",   0.95f, 0xFFFFFFFFL,         60);

    val tintAlpha: Float get() = (1f - brightness).coerceIn(-0.5f, 0.5f)
}

@Serializable
data class WeatherState(
    val current: String = Weather.SUNNY.name,
    val nextChangeTick: Long = 1_440L,
    val intensity: Float = 1f
) {
    fun currentWeather(): Weather = runCatching { Weather.valueOf(current) }.getOrDefault(Weather.SUNNY)
}

object WeatherEngine {
    /** Rotación de clima cada ~10 minutos in-game (600 ticks). */
    fun tick(state: WeatherState, currentTick: Long, rng: Random): WeatherState {
        if (currentTick < state.nextChangeTick) return state
        val choices = Weather.values()
        // Probabilidades suaves: sol más frecuente
        val weights = floatArrayOf(0.35f, 0.25f, 0.15f, 0.05f, 0.10f, 0.10f)
        val r = rng.nextFloat()
        var acc = 0f
        var idx = 0
        for (i in weights.indices) {
            acc += weights[i]
            if (r < acc) { idx = i; break }
        }
        val pick = choices[idx]
        return state.copy(
            current = pick.name,
            nextChangeTick = currentTick + (300 + rng.nextInt(600)),  // 5-15 min in-game
            intensity = (0.7f + rng.nextFloat() * 0.6f).coerceIn(0.3f, 1.5f)
        )
    }
}

// =====================================================================
//                              TRÁFICO
// =====================================================================

enum class VehicleKind(val color: Long) {
    CAR_RED(0xFFE53935), CAR_BLUE(0xFF1E88E5), CAR_BLACK(0xFF263238),
    CAR_GREEN(0xFF43A047), CAR_YELLOW(0xFFFBC02D), TAXI(0xFFFFEB3B),
    POLICE(0xFF1565C0), AMBULANCE(0xFFFFFFFF), TRUCK(0xFF6D4C41), BUS(0xFFEF6C00)
}

@Serializable
data class Vehicle(
    val id: String,
    val kind: String,                  // VehicleKind.name
    val x: Float,
    val y: Float,
    val dx: Float,                     // dirección normalizada
    val dy: Float,
    val speed: Float = 2.5f,
    val onRoadVertical: Boolean = false
) {
    fun vehicleKind(): VehicleKind = runCatching { VehicleKind.valueOf(kind) }.getOrDefault(VehicleKind.CAR_BLUE)
}

@Serializable
data class TrafficState(
    val vehicles: List<Vehicle> = emptyList()
)

object TrafficEngine {
    /** Spawn inicial de coches en posiciones aleatorias sobre carreteras. */
    fun ensurePopulated(state: TrafficState, grid: WorldGrid, target: Int = 18, rng: Random = Random(123)): TrafficState {
        if (state.vehicles.size >= target) return state
        val list = state.vehicles.toMutableList()
        var attempts = 0
        while (list.size < target && attempts < 800) {
            attempts++
            val x = rng.nextInt(grid.width)
            val y = rng.nextInt(grid.height)
            val type = grid.tileAt(x, y)
            val isRoad = type == TileType.ROAD || type == TileType.ROAD_LINE_H || type == TileType.ROAD_LINE_V
            if (!isRoad) continue
            val vertical = (type == TileType.ROAD_LINE_V) || rng.nextBoolean()
            val (dx, dy) = if (vertical) {
                0f to (if (rng.nextBoolean()) 1f else -1f)
            } else {
                (if (rng.nextBoolean()) 1f else -1f) to 0f
            }
            val kind = VehicleKind.values().random(rng)
            list.add(
                Vehicle(
                    id = "veh_${list.size}_${System.nanoTime()}",
                    kind = kind.name,
                    x = x + 0.5f,
                    y = y + 0.5f,
                    dx = dx,
                    dy = dy,
                    speed = 2.0f + rng.nextFloat() * 2.5f,
                    onRoadVertical = vertical
                )
            )
        }
        return state.copy(vehicles = list)
    }

    fun tick(state: TrafficState, grid: WorldGrid, deltaSec: Float): TrafficState {
        val updated = state.vehicles.map { v ->
            val nx = v.x + v.dx * v.speed * deltaSec
            val ny = v.y + v.dy * v.speed * deltaSec
            // Si va a salir del mapa, wrap
            val wx = when {
                nx < 0 -> grid.width.toFloat() - 1f
                nx >= grid.width -> 0f
                else -> nx
            }
            val wy = when {
                ny < 0 -> grid.height.toFloat() - 1f
                ny >= grid.height -> 0f
                else -> ny
            }
            // Si la siguiente baldosa NO es carretera, gira 90º
            val nextTileX = wx.toInt()
            val nextTileY = wy.toInt()
            val nextTile = grid.tileAt(nextTileX, nextTileY)
            val onRoad = nextTile == TileType.ROAD || nextTile == TileType.ROAD_LINE_H || nextTile == TileType.ROAD_LINE_V
            val (newDx, newDy) = if (!onRoad) {
                // Girar: cambiar dirección
                if (v.onRoadVertical) (if (v.x.toInt() % 2 == 0) 1f else -1f) to 0f
                else 0f to (if (v.y.toInt() % 2 == 0) 1f else -1f)
            } else v.dx to v.dy
            v.copy(x = wx, y = wy, dx = newDx, dy = newDy)
        }
        return state.copy(vehicles = updated)
    }
}

// =====================================================================
//                              EVENTOS WORLD
// =====================================================================

/** Encuentro aleatorio "estilo Pokemon" cuando avatar pisa cierto tile. */
@Serializable
data class WorldEvent(
    val id: String,
    val title: String,
    val body: String,
    val emoji: String,
    val choices: List<WorldEventChoice>
)

@Serializable
data class WorldEventChoice(
    val label: String,
    val cashDelta: Double = 0.0,
    val xpDelta: Long = 0,
    val happinessDelta: Int = 0,
    val energyDelta: Int = 0,
    val karmaDelta: Int = 0,
    val reputationDelta: Int = 0,
    val resultMessage: String = ""
)

@Serializable
data class WorldEventState(
    val activeEventId: String? = null,
    val lastEventTick: Long = 0L,
    val seenIds: Set<String> = emptySet(),
    // FIX BUG-18-05: timestamp en el que se levantó el evento. Si el jugador
    // ignora el modal por > EXPIRE_TICKS, se descarta sin recompensa.
    val activatedAtTick: Long = 0L
) {
    companion object {
        const val EXPIRE_TICKS: Long = 600L  // 10 minutos in-game
    }
}

object WorldEventCatalog {
    val all: List<WorldEvent> = listOf(
        WorldEvent("we_lost_wallet", "Cartera en el suelo", "Encuentras una cartera tirada en la acera. Tiene 200 €.", "👛",
            listOf(
                WorldEventChoice("Quedártela", cashDelta = 200.0, karmaDelta = -3, resultMessage = "200 € extra. Tu karma baja un poco."),
                WorldEventChoice("Buscar al dueño", karmaDelta = +5, reputationDelta = +2, xpDelta = 30, resultMessage = "El dueño te lo agradece. La gente lo cuenta.")
            )),
        WorldEvent("we_beggar", "Mendigo en la esquina", "Un mendigo te pide ayuda con la mirada perdida.", "🥺",
            listOf(
                WorldEventChoice("Darle 50 €", cashDelta = -50.0, karmaDelta = +4, happinessDelta = +5, resultMessage = "Le compras una comida caliente."),
                WorldEventChoice("Ignorar", karmaDelta = -1, happinessDelta = -3, resultMessage = "Sigues caminando. Pesa un poco la conciencia.")
            )),
        WorldEvent("we_busker", "Músico callejero", "Un músico toca una versión genial de tu canción favorita.", "🎸",
            listOf(
                WorldEventChoice("Lanzar 20 €", cashDelta = -20.0, happinessDelta = +6, karmaDelta = +1, resultMessage = "El músico sonríe agradecido."),
                WorldEventChoice("Pasar de largo", resultMessage = "Sigues tu camino sin más.")
            )),
        WorldEvent("we_tip", "Soplo en el callejón", "Un tipo te susurra: \"AGRX caerá en 2 días. Vende.\"", "🤫",
            listOf(
                WorldEventChoice("Creerle (riesgo)", xpDelta = 50, resultMessage = "Recordarás esto para luego."),
                WorldEventChoice("Pasar", resultMessage = "Decides ignorar al desconocido.")
            )),
        WorldEvent("we_journalist", "Periodista te aborda", "\"¿Unas declaraciones para mañana?\"", "📰",
            listOf(
                WorldEventChoice("Hablar bien", reputationDelta = +5, energyDelta = -10, resultMessage = "Mañana sales en portada."),
                WorldEventChoice("Sin comentarios", resultMessage = "El periodista anota algo y se va."),
                WorldEventChoice("Insultarle", reputationDelta = -8, karmaDelta = -3, resultMessage = "Mañana saldrás como un ogro.")
            )),
        WorldEvent("we_stray_dog", "Perro callejero", "Un perrito te sigue moviendo el rabo.", "🐕",
            listOf(
                WorldEventChoice("Adoptarlo", happinessDelta = +12, karmaDelta = +3, resultMessage = "Se llamará Tycoon."),
                WorldEventChoice("Llevarlo a la perrera", cashDelta = -30.0, karmaDelta = +1, resultMessage = "Al menos estará a salvo."),
                WorldEventChoice("Espantarlo", karmaDelta = -2, resultMessage = "Se aleja con la cola entre las patas.")
            )),
        WorldEvent("we_old_friend", "Viejo amigo", "Te encuentras a un viejo amigo del instituto.", "👋",
            listOf(
                WorldEventChoice("Tomar algo", cashDelta = -25.0, happinessDelta = +8, energyDelta = -5, resultMessage = "Buenos recuerdos."),
                WorldEventChoice("Excusarte", happinessDelta = -2, resultMessage = "Quedas en verlo pronto. Pero sabes que no.")
            )),
        WorldEvent("we_lottery", "Lotería rascada", "Encuentras un boleto en el suelo. Lo rascas.", "🎫",
            listOf(
                WorldEventChoice("Premio: 500 €", cashDelta = 500.0, happinessDelta = +6, resultMessage = "¡Suerte!"),
                WorldEventChoice("Nada", happinessDelta = -1, resultMessage = "Otra vez será.")
            )),
        WorldEvent("we_rival_spotted", "Avistas a un rival", "Tu rival empresarial pasa en una limusina y te ignora.", "💼",
            listOf(
                WorldEventChoice("Apretar dientes", happinessDelta = -3, xpDelta = 20, resultMessage = "Convertirás esa rabia en motivación."),
                WorldEventChoice("Saludar con clase", reputationDelta = +2, karmaDelta = +1, resultMessage = "Te ven sereno.")
            )),
        WorldEvent("we_protest", "Manifestación pacífica", "Hay una manifestación contra los grandes empresarios. Tú eres uno.", "📣",
            listOf(
                WorldEventChoice("Donar 1.000 € a la causa", cashDelta = -1_000.0, karmaDelta = +6, reputationDelta = +4, resultMessage = "La gente te aplaude."),
                WorldEventChoice("Pasar agachado", reputationDelta = -2, resultMessage = "Te marchas sin mirar."),
                WorldEventChoice("Llamar a la policía", karmaDelta = -10, reputationDelta = -5, resultMessage = "Te has ganado enemigos.")
            )),
        WorldEvent("we_artist", "Artista callejero", "Una pintora te ofrece un retrato gratis.", "🎨",
            listOf(
                WorldEventChoice("Aceptar", happinessDelta = +5, xpDelta = 20, resultMessage = "Tienes un cuadro nuevo en la cabeza."),
                WorldEventChoice("Pagar 100 € por uno bueno", cashDelta = -100.0, happinessDelta = +10, karmaDelta = +2, resultMessage = "Es un retrato realmente bueno.")
            )),
        WorldEvent("we_tax_inspector", "Inspector de Hacienda", "Te aborda con sospecha: \"¿Tiene un momento?\"", "🧾",
            listOf(
                WorldEventChoice("Sobornar (300 €)", cashDelta = -300.0, karmaDelta = -5, resultMessage = "Acepta el sobre. Te miras incómodos."),
                WorldEventChoice("Cooperar", energyDelta = -15, reputationDelta = +1, resultMessage = "30 minutos de su vida.")
            ))
    )

    fun random(rng: Random, exclude: Set<String>): WorldEvent? {
        val pool = all.filter { it.id !in exclude }
        if (pool.isEmpty()) return all.random(rng)
        return pool.random(rng)
    }
}
