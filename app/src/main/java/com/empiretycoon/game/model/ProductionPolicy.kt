package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Política de producción configurable por edificio. Permite al jugador
 * automatizar comportamiento de venta, control de calidad mínima,
 * priorización de contratos, recompra automática de insumos, etc.
 *
 * `qualityFloor` se almacena como string del enum para mantener el save
 * forward-compatible si en el futuro se añaden niveles intermedios.
 */
@Serializable
data class ProductionPolicy(
    val maxBatchesPerCycle: Int = 1,
    val sellAutomatically: Boolean = false,
    /** Factor mínimo del precio "fair" al que se acepta autovender (1.0 = precio normal). */
    val sellAtMinFactor: Double = 1.0,
    /** Calidad mínima por debajo de la cual se descarta o no se autovende. */
    val qualityFloorName: String = QualityTier.POOR.name,
    /** Si true, al haber contrato pendiente prioriza producción para él. */
    val prioritizeContracts: Boolean = true,
    /** Umbral de stock por debajo del cual se considera "stock bajo". */
    val lowStockThreshold: Int = 0,
    /** Si true, comprará insumos en mercado cuando estén por debajo del umbral. */
    val autoBuyInputs: Boolean = false
) {
    val qualityFloor: QualityTier get() =
        QualityTier.fromNameOrDefault(qualityFloorName)

    fun withQualityFloor(t: QualityTier): ProductionPolicy =
        copy(qualityFloorName = t.name)

    companion object {
        val Default: ProductionPolicy = ProductionPolicy()
    }
}

/**
 * Conjunto de políticas indexado por id de edificio.
 *
 * Vive en GameState como una sola pieza serializable; cuando un edificio
 * no tiene política definida se aplica `ProductionPolicy.Default`.
 */
@Serializable
data class ProductionPolicies(
    val byBuildingId: Map<String, ProductionPolicy> = emptyMap()
) {
    fun policyFor(buildingId: String): ProductionPolicy =
        byBuildingId[buildingId] ?: ProductionPolicy.Default

    fun set(buildingId: String, policy: ProductionPolicy): ProductionPolicies =
        copy(byBuildingId = byBuildingId + (buildingId to policy))

    fun reset(buildingId: String): ProductionPolicies =
        copy(byBuildingId = byBuildingId - buildingId)

    companion object {
        val Empty: ProductionPolicies = ProductionPolicies(emptyMap())
    }
}
