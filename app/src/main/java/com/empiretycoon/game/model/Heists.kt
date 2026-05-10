package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Mini-juego roguelike de atracos. Inspiración: GTA Online, Payday.
 * Cada heist:
 *  - Reclutas tripulación (4 roles).
 *  - Eliges enfoque (loud / stealth / negotiate).
 *  - Inviertes en equipo.
 *  - Resolución probabilística → 4 outcomes.
 *  - Sube heat policial → eventos de policía si se acumula.
 */

@Serializable
enum class HeistType(val emoji: String, val displayName: String) {
    SHOP_ROBBERY("🏪", "Robo a tienda"),
    BANK_HEIST("🏦", "Atraco al banco"),
    CORPORATE_HACK("🐛", "Hack corporativo"),
    CASINO_HEIST("🎰", "Asalto al casino"),
    AIRPORT_HEIST("✈️", "Heist al aeropuerto"),
    DEALERSHIP_THEFT("🚗", "Robo en concesionario"),
    YACHT_RAID("🛥️", "Atraco al yate"),
    THE_BIG_ONE("👑", "The Big One")
}

@Serializable
enum class CrewRole(val emoji: String, val displayName: String) {
    LEADER("🎩", "Líder"),
    HACKER("💻", "Hacker"),
    DRIVER("🚗", "Conductor"),
    SHARPSHOOTER("🎯", "Francotirador")
}

@Serializable
enum class HeistApproach(val emoji: String, val displayName: String) {
    LOUD("💥", "Ruidoso"),
    STEALTH("🥷", "Sigiloso"),
    NEGOTIATE("🤝", "Negociación")
}

@Serializable
enum class HeistOutcome(val emoji: String, val displayName: String) {
    PERFECT("🏆", "Éxito perfecto"),
    SUCCESS("✅", "Éxito"),
    ESCAPE("🏃", "Escape (parcial)"),
    DISASTER("💀", "Desastre")
}

@Serializable
enum class HeistStatus { LOCKED, AVAILABLE, PLANNING, EXECUTING, COMPLETED, COOLDOWN }

@Serializable
data class CrewMember(
    val id: String,
    val name: String,
    val role: CrewRole,
    /** Skill 30-99. */
    val skill: Int,
    /** Coste de reclutamiento (cobrado al fichar). */
    val recruitFee: Double,
    /** Cut: porcentaje del botín que pide. */
    val cutPct: Double,
    /** Notoriedad: si cae, sube el heat. */
    val notoriety: Int = 1,
    /** Si está disponible (no muerto/preso). */
    val available: Boolean = true
)

@Serializable
data class HeistPlan(
    val heistId: String,
    val crewIds: List<String>,
    val approach: HeistApproach,
    /** Cuánto se gastó en equipamiento (mejora skill efectiva). */
    val gearSpent: Double = 0.0
)

@Serializable
data class HeistDef(
    val type: HeistType,
    val baseReward: Double,
    val baseDifficulty: Int,        // 0-100
    val unlockLevel: Int,
    val unlockReputation: Int,
    /** Roles requeridos (puede repetirse, e.g. 2 sharpshooters). */
    val requiredRoles: List<CrewRole>,
    val description: String,
    val karmaImpact: Int = -3,
    val heatBase: Int = 20
)

@Serializable
data class HeistInstance(
    val id: String,
    val type: HeistType,
    val status: HeistStatus = HeistStatus.AVAILABLE,
    val plan: HeistPlan? = null,
    val outcome: HeistOutcome? = null,
    val payoutCash: Double = 0.0,
    val cooldownUntilTick: Long = 0L
)

@Serializable
data class HeistState(
    val unlocked: Boolean = false,
    /** Lista de heists con su estado actual. */
    val heists: List<HeistInstance> = emptyList(),
    /** Pool actual de tripulación reclutable + ya fichados. */
    val crewPool: List<CrewMember> = emptyList(),
    val recruitedCrew: List<String> = emptyList(),
    /** Heat policial 0-100. >70 = policía persiguiéndote. */
    val heat: Int = 0,
    /** Estadísticas. */
    val totalHeists: Int = 0,
    val totalLoot: Double = 0.0,
    val perfectRuns: Int = 0,
    val disasters: Int = 0,
    /** Tick del último refresh del crew pool. */
    val lastCrewRefreshTick: Long = -1L
)

object HeistCatalog {
    val all = listOf(
        HeistDef(
            type = HeistType.SHOP_ROBBERY,
            baseReward = 50_000.0,
            baseDifficulty = 25,
            unlockLevel = 5,
            unlockReputation = 0,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.DRIVER),
            description = "Asalto rápido a una tienda 24h. Pequeño, fácil, con poca atención.",
            karmaImpact = -2,
            heatBase = 12
        ),
        HeistDef(
            type = HeistType.DEALERSHIP_THEFT,
            baseReward = 120_000.0,
            baseDifficulty = 35,
            unlockLevel = 8,
            unlockReputation = 0,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.DRIVER, CrewRole.HACKER),
            description = "Sustrae un coche premium y una bolsa de cash del despacho.",
            karmaImpact = -3,
            heatBase = 18
        ),
        HeistDef(
            type = HeistType.BANK_HEIST,
            baseReward = 800_000.0,
            baseDifficulty = 55,
            unlockLevel = 15,
            unlockReputation = 20,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.HACKER, CrewRole.DRIVER, CrewRole.SHARPSHOOTER),
            description = "Atraco a la sucursal central. Altos riesgos, alto botín.",
            karmaImpact = -8,
            heatBase = 35
        ),
        HeistDef(
            type = HeistType.CORPORATE_HACK,
            baseReward = 1_200_000.0,
            baseDifficulty = 60,
            unlockLevel = 18,
            unlockReputation = 25,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.HACKER, CrewRole.HACKER),
            description = "Penetras en un rival corporativo. Botín en cripto + dossier.",
            karmaImpact = -5,
            heatBase = 22
        ),
        HeistDef(
            type = HeistType.CASINO_HEIST,
            baseReward = 4_000_000.0,
            baseDifficulty = 75,
            unlockLevel = 25,
            unlockReputation = 40,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.HACKER, CrewRole.DRIVER, CrewRole.SHARPSHOOTER),
            description = "Boveda del casino. Cámaras everywhere. Solo los mejores.",
            karmaImpact = -10,
            heatBase = 50
        ),
        HeistDef(
            type = HeistType.AIRPORT_HEIST,
            baseReward = 6_000_000.0,
            baseDifficulty = 80,
            unlockLevel = 30,
            unlockReputation = 50,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.HACKER, CrewRole.DRIVER, CrewRole.SHARPSHOOTER, CrewRole.SHARPSHOOTER),
            description = "Avión privado en pista. Cargamento + jet privado de regalo si va bien.",
            karmaImpact = -12,
            heatBase = 60
        ),
        HeistDef(
            type = HeistType.YACHT_RAID,
            baseReward = 15_000_000.0,
            baseDifficulty = 88,
            unlockLevel = 40,
            unlockReputation = 60,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.HACKER, CrewRole.DRIVER, CrewRole.SHARPSHOOTER),
            description = "Yate privado de un magnate. Caja fuerte oceánica.",
            karmaImpact = -15,
            heatBase = 70
        ),
        HeistDef(
            type = HeistType.THE_BIG_ONE,
            baseReward = 50_000_000.0,
            baseDifficulty = 95,
            unlockLevel = 50,
            unlockReputation = 70,
            requiredRoles = listOf(CrewRole.LEADER, CrewRole.HACKER, CrewRole.HACKER, CrewRole.DRIVER, CrewRole.SHARPSHOOTER, CrewRole.SHARPSHOOTER),
            description = "El golpe definitivo. Imperio rival cae. Una sola oportunidad.",
            karmaImpact = -25,
            heatBase = 90
        )
    )

    fun byType(type: HeistType): HeistDef? = all.find { it.type == type }

    /** Plantilla inicial de heists con status LOCKED. */
    fun freshHeists(): List<HeistInstance> = all.map {
        HeistInstance(id = "heist_${it.type.name}", type = it.type, status = HeistStatus.LOCKED)
    }
}

/** Generador procedural de tripulación para el pool. */
object CrewGenerator {
    private val firstNames = listOf("Vega", "Mara", "Ari", "Río", "Lex", "Yuri", "Nico", "Sasha",
        "Kai", "Iván", "Eli", "Tomás", "Coral", "Greta", "Hugo", "Ada", "Bruno", "Lía", "Olmo", "Reina")
    private val nicknames = listOf("\"Sombra\"", "\"El Búho\"", "\"Cero\"", "\"Eco\"", "\"Acero\"",
        "\"Ghost\"", "\"Trapo\"", "\"Lince\"", "\"Trueno\"", "\"Frío\"")

    fun generate(rng: kotlin.random.Random, seed: Int): List<CrewMember> {
        return (0 until 8).map { i ->
            val role = CrewRole.values().random(rng)
            val name = "${firstNames.random(rng)} ${nicknames.random(rng)}"
            val skill = 35 + rng.nextInt(60)
            val recruitFee = (8_000 + skill * 1_500 + rng.nextInt(20_000)).toDouble()
            val cut = 0.05 + (skill / 99.0) * 0.10 + rng.nextDouble() * 0.03
            CrewMember(
                id = "crew_${seed}_$i",
                name = name,
                role = role,
                skill = skill,
                recruitFee = recruitFee,
                cutPct = cut,
                notoriety = (skill / 25).coerceIn(1, 4)
            )
        }
    }
}
