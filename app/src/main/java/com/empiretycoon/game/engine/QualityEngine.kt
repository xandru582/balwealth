package com.empiretycoon.game.engine

import com.empiretycoon.game.model.QualityInventory
import com.empiretycoon.game.model.QualityRoll
import com.empiretycoon.game.model.QualityTier
import kotlin.random.Random

/**
 * Motor de calidad: traduce nivel de edificio + skill medio + bonos de
 * investigación en una calidad concreta para cada lote.
 *
 * El motor es puro y stateless. La aleatoriedad va por argumento.
 */
object QualityEngine {

    /**
     * Realiza una tirada de calidad.
     *
     * @param buildingLevel  nivel del edificio (1+).
     * @param avgSkill       skill medio efectivo de los empleados asignados (0..2.5).
     * @param researchBonus  bono adicional acumulado por techs (0..1.0).
     * @param rng            generador aleatorio reproducible.
     */
    fun rollQuality(
        buildingLevel: Int,
        avgSkill: Double,
        researchBonus: Double,
        rng: Random
    ): QualityTier {
        // Convertimos cada eje a un score acotado para no romper la curva.
        val skillBonus = ((avgSkill - 1.0) * 0.30).coerceIn(-0.30, 0.50)
        val levelBonus = ((buildingLevel - 1) * 0.06).coerceIn(0.0, 0.60)
        val resBonus = researchBonus.coerceIn(0.0, 0.60)

        return QualityRoll(
            rng = rng,
            baseTier = QualityTier.STANDARD,
            employeeSkillBonus = skillBonus,
            buildingLevelBonus = levelBonus,
            researchBonus = resBonus
        ).resolve()
    }

    /** Conveniencia: aplica `qty` unidades del recurso al inventario en `tier`. */
    fun applyToInventory(
        inv: QualityInventory,
        resId: String,
        qty: Int,
        tier: QualityTier
    ): QualityInventory = inv.addQty(resId, tier, qty)

    /**
     * Multiplicador de precio combinado para una venta agregada de tier mixtos.
     * Se calcula la media ponderada de `tier.mult` por cantidad.
     */
    fun blendedPriceMultiplier(
        breakdown: List<Pair<QualityTier, Int>>
    ): Double {
        val total = breakdown.sumOf { it.second }
        if (total <= 0) return 1.0
        val weighted = breakdown.sumOf { (t, q) -> t.mult * q }
        return weighted / total
    }
}
