package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * TraitTreeEngine — talentos permanentes (v17 — siguientes tandas).
 *
 * 60 traits en 5 ramas (12 por rama). Cada trait tiene:
 *  - prerequisito lineal: trait i requiere haber comprado el i-1.
 *  - coste creciente en `resilienceXp` (que ya genera DisasterEngine).
 *  - un effect type con un value multiplicativo que se acumula con el
 *    resto de traits del jugador.
 *
 * Filosofía:
 *  - Persistente a través del prestige (cuando se reinicie con
 *    PrestigeEngine, los traits comprados se mantienen).
 *  - Consumible único: una vez comprado un trait, no se puede recuperar.
 *  - Ramas independientes: puedes especializarte en una sola, o ir
 *    cogiendo lo bajito de cada una.
 *
 * Otras engines consultan los modificadores acumulados via
 * `state.traitTree.totalEffectOf(type)`.
 */

/** Rama del árbol. */
@Serializable
enum class TraitBranch(
    val displayName: String,
    val emoji: String,
    val tagline: String,
    val accentArgb: Long
) {
    MAGNATE("Magnate", "💼", "Cash y producción", 0xFFFFD166),
    VISIONARY("Visionario", "🔮", "XP y velocidad de I+D", 0xFF118AB2),
    POLITICIAN("Político", "🎩", "Reputación y contratos", 0xFF06D6A0),
    ARTIST("Artista", "🎨", "Sueldos y felicidad", 0xFFEF476F),
    OUTLAW("Outlaw", "🦹", "Heists y heat", 0xFF8E24AA)
}

/** Tipos de efectos que un trait puede aplicar. */
@Serializable
enum class TraitEffectType(val displayName: String) {
    CASH_GAIN_MUL("ganancia de cash"),
    PRODUCTION_MUL("velocidad de producción"),
    PLAYER_XP_MUL("XP del jugador"),
    RESEARCH_SPEED_MUL("velocidad de I+D"),
    REPUTATION_GAIN_MUL("ganancia de reputación"),
    CONTRACT_REVENUE_MUL("ingresos de contratos"),
    JOB_WAGE_MUL("salario de oficios"),
    HAPPINESS_GAIN_MUL("ganancia de felicidad"),
    HEIST_SUCCESS_MUL("éxito en heists"),
    HEAT_DECAY_MUL("decay de heat")
}

/** Definición estática de un trait. */
@Serializable
data class TraitDefinition(
    val id: String,
    val branch: TraitBranch,
    val tier: Int,                    // 1..12 dentro de la rama
    val displayName: String,
    val description: String,
    val cost: Int,                    // resilienceXp
    val effectType: TraitEffectType,
    val effectValue: Double           // p. ej. 0.05 = +5%
)

/** Estado serializable del árbol. */
@Serializable
data class TraitTreeState(
    val unlockedIds: Set<String> = emptySet()
) {
    /** Suma de effectValue de todos los traits unlocked de un type. */
    fun totalEffectOf(type: TraitEffectType): Double {
        var sum = 0.0
        for (id in unlockedIds) {
            val t = TraitCatalog.byId(id) ?: continue
            if (t.effectType == type) sum += t.effectValue
        }
        return sum
    }

    /** Multiplicador final para usar (1.0 + total). */
    fun multiplierFor(type: TraitEffectType): Double = 1.0 + totalEffectOf(type)

    fun isUnlocked(id: String): Boolean = id in unlockedIds

    /** Cuántos traits de una rama lleva el jugador. */
    fun unlockedInBranch(branch: TraitBranch): Int =
        TraitCatalog.byBranch(branch).count { it.id in unlockedIds }
}

object TraitCatalog {
    /** 60 traits generados con un patrón equilibrado por rama. */
    val all: List<TraitDefinition> = run {
        // Para cada rama, 12 traits con nombres temáticos + efectos rotativos
        // por tier dentro de un pool propio de la rama.
        val byBranchEffects = mapOf(
            TraitBranch.MAGNATE to listOf(
                TraitEffectType.CASH_GAIN_MUL,
                TraitEffectType.PRODUCTION_MUL,
                TraitEffectType.CONTRACT_REVENUE_MUL
            ),
            TraitBranch.VISIONARY to listOf(
                TraitEffectType.PLAYER_XP_MUL,
                TraitEffectType.RESEARCH_SPEED_MUL,
                TraitEffectType.PRODUCTION_MUL
            ),
            TraitBranch.POLITICIAN to listOf(
                TraitEffectType.REPUTATION_GAIN_MUL,
                TraitEffectType.CONTRACT_REVENUE_MUL,
                TraitEffectType.HAPPINESS_GAIN_MUL
            ),
            TraitBranch.ARTIST to listOf(
                TraitEffectType.JOB_WAGE_MUL,
                TraitEffectType.HAPPINESS_GAIN_MUL,
                TraitEffectType.PLAYER_XP_MUL
            ),
            TraitBranch.OUTLAW to listOf(
                TraitEffectType.HEIST_SUCCESS_MUL,
                TraitEffectType.HEAT_DECAY_MUL,
                TraitEffectType.CASH_GAIN_MUL
            )
        )
        val nameByBranch = mapOf(
            TraitBranch.MAGNATE to listOf(
                "Mente comercial", "Negociador", "Olfato del mercado", "Magnate junior",
                "Magnate", "Magnate senior", "Tycoon", "Imperio", "Monarca del mercado",
                "Oligarca", "Visionario del cash", "Leyenda viva"
            ),
            TraitBranch.VISIONARY to listOf(
                "Curiosidad", "Estudiante", "Erudito", "Inventor",
                "Pensador", "Polímata", "Genio", "Lumbrera", "Visionario",
                "Sabio", "Maestro del oficio", "Mente brillante"
            ),
            TraitBranch.POLITICIAN to listOf(
                "Habla bonito", "Diplomático", "Conector", "Influencer",
                "Notable", "Político local", "Político regional", "Líder", "Estadista",
                "Embajador", "Constructor de coaliciones", "Ícono público"
            ),
            TraitBranch.ARTIST to listOf(
                "Aprendiz", "Bohemio", "Creativo", "Artesano",
                "Performer", "Artista", "Maestro", "Virtuoso", "Genio creativo",
                "Ícono cultural", "Renovador del arte", "Inmortal"
            ),
            TraitBranch.OUTLAW to listOf(
                "Pillo", "Carterista", "Cuatrero", "Bandolero",
                "Outlaw", "Forajido", "Hampa", "Capo menor", "Capo",
                "Jefe del crimen", "Sombra", "Leyenda criminal"
            )
        )
        val list = mutableListOf<TraitDefinition>()
        for (branch in TraitBranch.values()) {
            val pool = byBranchEffects.getValue(branch)
            val names = nameByBranch.getValue(branch)
            for (tier in 1..12) {
                val effectType = pool[(tier - 1) % pool.size]
                // Coste creciente exponencial: 10, 16, 26, 41, 65, 104, 167, 268, 428, 685, 1097, 1755
                val cost = (10.0 * (1.6).pow(tier - 1)).toInt().coerceAtLeast(10)
                // Efecto creciente lineal: 0.02..0.30 (2%..30%)
                val effectValue = 0.02 + (tier - 1) * 0.025
                val description = "+${(effectValue * 100).toInt()}% ${effectType.displayName}"
                list += TraitDefinition(
                    id = "${branch.name}_$tier",
                    branch = branch,
                    tier = tier,
                    displayName = names[tier - 1],
                    description = description,
                    cost = cost,
                    effectType = effectType,
                    effectValue = effectValue
                )
            }
        }
        list
    }

    private val byIdMap: Map<String, TraitDefinition> = all.associateBy { it.id }
    fun byId(id: String): TraitDefinition? = byIdMap[id]
    fun byBranch(branch: TraitBranch): List<TraitDefinition> =
        all.filter { it.branch == branch }.sortedBy { it.tier }

    /** ID del trait inmediatamente anterior (prerequisito). Null si tier=1. */
    fun previousOf(trait: TraitDefinition): String? =
        if (trait.tier <= 1) null else "${trait.branch.name}_${trait.tier - 1}"
}
