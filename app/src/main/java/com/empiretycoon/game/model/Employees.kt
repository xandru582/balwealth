package com.empiretycoon.game.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * Empleado contratable. La `skill` (0.5–2.0) multiplica productividad del edificio
 * donde esté asignado. `loyalty` cae con malas decisiones (impago de salario).
 */
@Serializable
data class Employee(
    val id: String,
    val name: String,
    val skill: Double,              // multiplicador 0.5–2.0
    val monthlySalary: Double,      // coste cada 30 días de juego
    val loyalty: Double = 1.0,      // 0.0–1.0
    val assignedBuildingId: String? = null
) {
    fun effectiveOutput(): Double = skill * (0.6 + 0.4 * loyalty)

    fun withLoyalty(new: Double) = copy(loyalty = max(0.0, min(1.0, new)))
}

/** Generación pseudo-aleatoria de candidatos según el nivel/reputación de la empresa. */
object EmployeeFactory {
    private val firstNames = listOf(
        "Luis","María","Ana","Jorge","Carla","Pedro","Sara","Iván",
        "Lucía","Diego","Marta","Miguel","Elena","David","Laura",
        "Alba","Rafa","Sofía","Tomás","Olga","Javier","Paula"
    )
    private val lastNames = listOf(
        "García","Rodríguez","López","Martínez","Hernández","Pérez",
        "Gómez","Sánchez","Romero","Torres","Ramírez","Vargas",
        "Navarro","Ortiz","Ruiz","Castro","Iglesias","Cano"
    )

    fun generateCandidates(tier: Int, rng: kotlin.random.Random, count: Int = 6): List<Employee> {
        return (0 until count).map { idx ->
            val skillBase = 0.6 + 0.15 * tier
            val skill = (skillBase + rng.nextDouble(-0.2, 0.45)).coerceIn(0.4, 2.2)
            val salary = (300.0 + skill * (250.0 + tier * 80)) * rng.nextDouble(0.8, 1.25)
            Employee(
                id = "emp_${System.nanoTime()}_$idx",
                name = "${firstNames.random(rng)} ${lastNames.random(rng)}",
                skill = (skill * 100).toInt() / 100.0,
                monthlySalary = (salary.toInt()).toDouble()
            )
        }
    }
}
