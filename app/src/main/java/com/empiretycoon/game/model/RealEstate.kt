package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

enum class PropertyType(
    val displayName: String,
    val emoji: String,
    val basePrice: Double,
    val baseRentPerDay: Double
) {
    SMALL_APT("Piso pequeño", "\uD83C\uDFE0", 80_000.0, 90.0),
    LARGE_APT("Apartamento", "\uD83C\uDFE2", 180_000.0, 220.0),
    HOUSE("Casa", "\uD83C\uDFE1", 320_000.0, 360.0),
    LUXURY("Villa de lujo", "\uD83C\uDFF0", 950_000.0, 1_100.0),
    COMMERCIAL("Local comercial", "\uD83C\uDFEA", 520_000.0, 620.0),
    SKYSCRAPER("Rascacielos", "\uD83C\uDF06", 4_500_000.0, 6_200.0)
}

@Serializable
data class Property(
    val id: String,
    val type: PropertyType,
    val nickname: String,
    val purchasePrice: Double,
    val rentPerDay: Double,
    val occupied: Boolean = true,
    val maintenancePerDay: Double = 0.0
)

@Serializable
data class RealEstatePortfolio(
    val owned: List<Property> = emptyList(),
    val available: List<Property> = emptyList()
) {
    val dailyNet: Double get() = owned.sumOf {
        (if (it.occupied) it.rentPerDay else 0.0) - it.maintenancePerDay
    }
    val totalValue: Double get() = owned.sumOf { it.purchasePrice }
}
