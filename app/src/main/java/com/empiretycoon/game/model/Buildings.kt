package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/** Tipos de edificio que el jugador puede construir para producir. */
enum class BuildingType(
    val displayName: String,
    val emoji: String,
    val baseCost: Double,
    val workerCapacityBase: Int,
    val description: String
) {
    FARM("Granja", "\uD83D\uDC68\u200D\uD83C\uDF3E", 1_200.0, 3, "Cultivos y ganadería."),
    SAWMILL("Aserradero", "\uD83E\uDEB5", 1_800.0, 3, "Madera y tablones."),
    MINE("Mina", "\u26CF\uFE0F", 4_500.0, 4, "Minerales y combustibles."),
    BAKERY("Panadería", "\uD83E\uDDC1", 3_200.0, 3, "Alimentos y harina."),
    SMELTER("Fundición", "\u2699\uFE0F", 12_000.0, 5, "Metales y vidrio."),
    REFINERY("Refinería", "\uD83C\uDFED", 28_000.0, 6, "Plásticos y químicos."),
    FACTORY("Fábrica", "\uD83D\uDE97", 55_000.0, 8, "Ensambla bienes complejos."),
    OFFICE("Oficina", "\uD83C\uDFE2", 18_000.0, 6, "Servicios y software."),
    JEWELRY("Taller de joyería", "\uD83D\uDC8D", 48_000.0, 3, "Objetos de lujo."),
    SHIPYARD("Astillero", "\u26F5", 220_000.0, 10, "Yates y embarcaciones."),
    WAREHOUSE("Almacén", "\uD83C\uDFEC", 6_000.0, 0, "Aumenta capacidad de stock.");

    /** Coste de construcción para el nivel N (escalado exponencial suave). */
    fun costAtLevel(level: Int): Double = baseCost * Math.pow(1.8, (level - 1).toDouble())

    /** Productividad relativa (multiplicador de velocidad) por nivel del edificio. */
    fun productivityAtLevel(level: Int): Double = 1.0 + (level - 1) * 0.25

    /** Capacidad de trabajadores por nivel. */
    fun workerCapacity(level: Int): Int = workerCapacityBase + (level - 1) * 2
}

/**
 * Instancia de un edificio poseído por el jugador.
 * `currentRecipeId` vacío = inactivo.
 * `progressSeconds` acumula segundos de simulación cuando hay recursos.
 */
@Serializable
data class Building(
    val id: String,
    val type: BuildingType,
    val level: Int = 1,
    val assignedWorkers: Int = 0,
    val currentRecipeId: String? = null,
    val progressSeconds: Double = 0.0,
    val autoRestart: Boolean = true
) {
    val name: String get() = "${type.displayName} Nv.$level"
    val workerCapacity: Int get() = type.workerCapacity(level)
    val productivity: Double get() = type.productivityAtLevel(level)
}
