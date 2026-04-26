package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Niveles de calidad de los productos manufacturados.
 *
 * El multiplicador `mult` se aplica al precio de venta en el mercado
 * cuando se vende un lote de esa calidad. Las calidades se obtienen
 * tirando un dado modulado por nivel del edificio, skill medio de los
 * empleados asignados y bonos de investigación.
 */
enum class QualityTier(
    val mult: Double,
    val label: String,
    val emoji: String
) {
    POOR(0.7, "Pobre", "⚫"),
    STANDARD(1.0, "Estándar", "🔘"),
    GOOD(1.25, "Buena", "🔗"),
    PREMIUM(1.6, "Premium", "⭐"),
    ULTRA(2.2, "Ultra", "💎"),
    MASTERWORK(3.0, "Obra Maestra", "🏆");

    companion object {
        /** Lista en orden ascendente de calidad. */
        val ascending: List<QualityTier> = values().sortedBy { it.mult }

        /** Devuelve el siguiente tier (o el mismo si ya estamos en el máximo). */
        fun stepUp(t: QualityTier): QualityTier {
            val idx = ascending.indexOf(t)
            return ascending.getOrNull(idx + 1) ?: t
        }

        /** Tier por defecto cuando no hay datos. */
        val Default: QualityTier = STANDARD

        /** Lookup tolerante por nombre (para deserialización flexible). */
        fun fromNameOrDefault(name: String?): QualityTier =
            values().firstOrNull { it.name == name } ?: Default
    }
}

/**
 * Inventario por calidad. Mapa anidado: resourceId -> tierName -> cantidad.
 * Usamos `String` como clave del tier para que sea trivialmente serializable
 * con kotlinx.serialization (no necesitamos un serializer custom para el enum).
 *
 * Es un complemento al `Company.inventory` clásico y no lo reemplaza:
 * mantenemos la suma total en el inventario tradicional para no romper
 * el resto del motor (capacidad, comprobaciones de receta, etc.).
 */
@Serializable
data class QualityInventory(
    val byTier: Map<String, Map<String, Int>> = emptyMap()
) {
    /** Cantidad total de un recurso (todas las calidades). */
    fun totalOf(resId: String): Int =
        byTier[resId]?.values?.sum() ?: 0

    /** Cantidad de un recurso en una calidad concreta. */
    fun amountOf(resId: String, tier: QualityTier): Int =
        byTier[resId]?.get(tier.name) ?: 0

    /** Añade `n` unidades de `resId` en `tier`. */
    fun addQty(resId: String, tier: QualityTier, n: Int): QualityInventory {
        if (n == 0) return this
        val perRes = byTier[resId]?.toMutableMap() ?: mutableMapOf()
        perRes[tier.name] = (perRes[tier.name] ?: 0) + n
        // limpia entradas a 0 para no engordar el save
        if ((perRes[tier.name] ?: 0) <= 0) perRes.remove(tier.name)
        val out = byTier.toMutableMap()
        if (perRes.isEmpty()) out.remove(resId) else out[resId] = perRes
        return copy(byTier = out)
    }

    /**
     * Retira `n` unidades del recurso, empezando por la calidad más baja.
     * Devuelve el nuevo inventario y la cantidad realmente retirada.
     */
    fun takeAny(resId: String, n: Int): Pair<QualityInventory, Int> {
        if (n <= 0) return this to 0
        val perRes = byTier[resId] ?: return this to 0
        var remaining = n
        val mut = perRes.toMutableMap()
        for (t in QualityTier.ascending) {
            if (remaining == 0) break
            val have = mut[t.name] ?: 0
            if (have <= 0) continue
            val take = minOf(have, remaining)
            mut[t.name] = have - take
            if ((mut[t.name] ?: 0) <= 0) mut.remove(t.name)
            remaining -= take
        }
        val out = byTier.toMutableMap()
        if (mut.isEmpty()) out.remove(resId) else out[resId] = mut
        return copy(byTier = out) to (n - remaining)
    }

    /** Retira `n` unidades de una calidad concreta. */
    fun takeTier(resId: String, tier: QualityTier, n: Int): Pair<QualityInventory, Int> {
        if (n <= 0) return this to 0
        val have = amountOf(resId, tier)
        if (have <= 0) return this to 0
        val take = minOf(have, n)
        return addQty(resId, tier, -take) to take
    }

    /** Resumen ordenado: (resId, tier, qty) sólo entradas > 0. */
    fun flatList(): List<Triple<String, QualityTier, Int>> =
        byTier.flatMap { (rid, perTier) ->
            perTier.mapNotNull { (tn, q) ->
                if (q <= 0) null
                else Triple(rid, QualityTier.fromNameOrDefault(tn), q)
            }
        }

    companion object {
        val Empty: QualityInventory = QualityInventory(emptyMap())
    }
}

/**
 * Tirada de calidad encapsulada. Se construye a partir de los modificadores
 * y al instanciar resuelve la calidad final.
 */
data class QualityRoll(
    val rng: Random,
    val baseTier: QualityTier = QualityTier.STANDARD,
    val employeeSkillBonus: Double = 0.0,
    val buildingLevelBonus: Double = 0.0,
    val researchBonus: Double = 0.0
) {
    /** Resuelve la tirada en una calidad concreta, mezclando todos los bonos. */
    fun resolve(): QualityTier {
        // El "score" determina cuántos pasos sube/baja respecto al baseTier.
        // Cada 0.20 de score sube un peldaño; cada -0.25 baja uno.
        val score = (employeeSkillBonus + buildingLevelBonus + researchBonus) +
                rng.nextDouble(-0.18, 0.22)
        val baseIdx = QualityTier.ascending.indexOf(baseTier).coerceAtLeast(0)
        val steps = when {
            score >= 1.0 -> 5
            score >= 0.78 -> 4
            score >= 0.55 -> 3
            score >= 0.35 -> 2
            score >= 0.18 -> 1
            score >= -0.10 -> 0
            score >= -0.25 -> -1
            else -> -2
        }
        val targetIdx = (baseIdx + steps).coerceIn(0, QualityTier.ascending.lastIndex)
        return QualityTier.ascending[targetIdx]
    }
}
