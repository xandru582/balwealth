package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Sistema de desastres dinámicos. Crisis que aparecen cada 30-60 días in-game.
 * El jugador puede mitigar daños con seguros, evacuaciones, o donaciones.
 */

/** Categoría del desastre. Determina qué se daña. */
@Serializable
enum class DisasterKind {
    EARTHQUAKE,    // 🌍 daña edificios físicos
    FLOOD,         // 🌊 daña edificios + recursos perecederos
    FIRE,          // 🔥 destruye 1-2 edificios al azar
    PANDEMIC,      // 🦠 -50% productividad 14 días
    BLACKOUT,      // ⚡ -100% prod 1 día, gasto adicional 50k
    HURRICANE,     // 🌪️ daños mixtos + cierre puerto
    HACK,          // 🐛 robo de cash 5-15%
    FLASH_CRASH,   // 📉 stocks -25% instantáneo
    HYPERINFLATION,// 💸 todos los precios suben 60%, salarios x2
    LOGISTICS_STRIKE, // 🚛 ventas -70% durante 7 días
    BOYCOTT,       // 🛒 reputación -20, ventas -40% 10 días
    ARMED_ROBBERY  // 🔫 cash personal -50%
}

@Serializable
enum class DisasterSeverity { LOW, MEDIUM, HIGH, CATASTROPHIC }

@Serializable
data class ActiveDisaster(
    val id: String,
    val kind: DisasterKind,
    val severity: DisasterSeverity,
    val startedAtTick: Long,
    /** En cuántos ticks termina el efecto (0 si es inmediato). */
    val expiresAtTick: Long,
    val title: String,
    val description: String,
    /** Multiplicador a producción (1.0 = sin efecto). */
    val productionMul: Double = 1.0,
    /** Multiplicador a precios de venta. */
    val sellPriceMul: Double = 1.0,
    /** Multiplicador a precios de compra. */
    val buyPriceMul: Double = 1.0,
    /** Si el jugador ya ha actuado (mitigación). */
    val mitigated: Boolean = false,
    /** Fase: PENDING_RESPONSE (tiene 24h para mitigar) → ACTIVE → RESOLVED. */
    val phase: DisasterPhase = DisasterPhase.PENDING_RESPONSE,
    /** Tick a partir del cual deja de aceptar mitigación. */
    val mitigationDeadlineTick: Long
)

@Serializable
enum class DisasterPhase { PENDING_RESPONSE, ACTIVE, RESOLVED }

@Serializable
data class DisasterHistory(
    val id: String,
    val kind: DisasterKind,
    val severity: DisasterSeverity,
    val day: Int,
    val mitigated: Boolean,
    val cashLost: Double,
    val outcome: String
)

/** Estado del subsistema. */
@Serializable
data class DisasterState(
    val active: List<ActiveDisaster> = emptyList(),
    val history: List<DisasterHistory> = emptyList(),
    val lastDisasterDay: Int = 0,
    /** Días entre desastres (cooldown). */
    val cooldownDays: Int = 30,
    /** XP de resiliencia: 1 punto por desastre superado. */
    val resilienceXp: Int = 0,
    /** Si el jugador tiene seguro contratado. */
    val insuranceActive: Boolean = false,
    /** Coste diario del seguro. */
    val insuranceDailyCost: Double = 5_000.0,
    /** Cobertura: porcentaje de daño que paga el seguro 0..1. */
    val insuranceCoverage: Double = 0.50
)

/** Catálogo de desastres con plantillas. */
object DisasterCatalog {

    private fun template(kind: DisasterKind): Triple<String, String, String> = when (kind) {
        DisasterKind.EARTHQUAKE -> Triple("🌍", "Terremoto", "Sismo grado %s sacude la ciudad. Edificios afectados.")
        DisasterKind.FLOOD -> Triple("🌊", "Inundación", "Río desbordado. Inventarios anegados, edificios dañados.")
        DisasterKind.FIRE -> Triple("🔥", "Incendio en planta", "Llamas devoran una de tus fábricas.")
        DisasterKind.PANDEMIC -> Triple("🦠", "Pandemia", "Ola epidémica. Empleados con baja masiva durante semanas.")
        DisasterKind.BLACKOUT -> Triple("⚡", "Apagón total", "Sin red eléctrica. Producción en cero hasta restaurar.")
        DisasterKind.HURRICANE -> Triple("🌪️", "Huracán", "Vientos de categoría %s. Cierre de puertos y caos logístico.")
        DisasterKind.HACK -> Triple("🐛", "Ataque ransomware", "Tus servidores cifrados. Pagas o pierdes datos.")
        DisasterKind.FLASH_CRASH -> Triple("📉", "Flash crash", "El mercado se desploma sin previo aviso.")
        DisasterKind.HYPERINFLATION -> Triple("💸", "Hiperinflación", "Inflación galopante. Precios y salarios disparados.")
        DisasterKind.LOGISTICS_STRIKE -> Triple("🚛", "Huelga logística", "Transportistas en huelga. Ventas paralizadas.")
        DisasterKind.BOYCOTT -> Triple("🛒", "Boicot ciudadano", "La opinión pública te crucifica. Reputación en caída libre.")
        DisasterKind.ARMED_ROBBERY -> Triple("🔫", "Atraco armado", "Asaltan tu oficina. Pierdes parte de tu cash personal.")
    }

    fun build(kind: DisasterKind, severity: DisasterSeverity, tick: Long): ActiveDisaster {
        val (emoji, name, desc) = template(kind)
        val sevLabel = when (severity) {
            DisasterSeverity.LOW -> "leve"
            DisasterSeverity.MEDIUM -> "moderado"
            DisasterSeverity.HIGH -> "grave"
            DisasterSeverity.CATASTROPHIC -> "catastrófico"
        }
        val (prodMul, sellMul, buyMul, durationDays) = when (kind) {
            DisasterKind.EARTHQUAKE -> Quad(0.6, 0.95, 1.05, 5)
            DisasterKind.FLOOD -> Quad(0.5, 0.90, 1.05, 7)
            DisasterKind.FIRE -> Quad(0.85, 1.0, 1.0, 3)
            DisasterKind.PANDEMIC -> Quad(0.5, 0.85, 1.10, 14)
            DisasterKind.BLACKOUT -> Quad(0.0, 1.0, 1.0, 1)
            DisasterKind.HURRICANE -> Quad(0.5, 0.80, 1.20, 4)
            DisasterKind.HACK -> Quad(0.95, 1.0, 1.0, 1)
            DisasterKind.FLASH_CRASH -> Quad(1.0, 1.0, 1.0, 1)
            DisasterKind.HYPERINFLATION -> Quad(0.85, 1.6, 1.6, 21)
            DisasterKind.LOGISTICS_STRIKE -> Quad(0.7, 0.30, 1.0, 7)
            DisasterKind.BOYCOTT -> Quad(0.85, 0.60, 1.0, 10)
            DisasterKind.ARMED_ROBBERY -> Quad(1.0, 1.0, 1.0, 0)
        }
        val sevMul = when (severity) {
            DisasterSeverity.LOW -> 0.6
            DisasterSeverity.MEDIUM -> 1.0
            DisasterSeverity.HIGH -> 1.4
            DisasterSeverity.CATASTROPHIC -> 1.9
        }
        return ActiveDisaster(
            id = "dis_${tick}_${kind.name}",
            kind = kind,
            severity = severity,
            startedAtTick = tick,
            expiresAtTick = tick + durationDays.toLong() * 1_440L,
            title = "$emoji $name $sevLabel",
            description = desc.format(sevLabel),
            productionMul = applyMul(prodMul, sevMul),
            sellPriceMul = applyMul(sellMul, sevMul),
            buyPriceMul = applyMul(buyMul, sevMul),
            mitigationDeadlineTick = tick + 1_440L
        )
    }

    private fun applyMul(base: Double, sevMul: Double): Double {
        if (base == 1.0) return 1.0
        return if (base > 1.0) {
            1.0 + (base - 1.0) * sevMul
        } else {
            1.0 - (1.0 - base) * sevMul
        }
    }
}

private data class Quad(val a: Double, val b: Double, val c: Double, val d: Int)
