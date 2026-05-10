package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Estado corporativo del jugador. `reputation` modula ofertas de empleo,
 * contratos y eventos. `xp` y `level` desbloquean edificios avanzados.
 */
@Serializable
data class Company(
    val name: String = "Nueva Empresa S.L.",
    val slogan: String = "Construyendo el futuro, ladrillo a ladrillo.",
    val cash: Double = 10_000.0,
    val reputation: Int = 30,      // 0–100
    val level: Int = 1,
    val xp: Long = 0,
    val founded: Long = 0,         // tick founded
    val buildings: List<Building> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val inventory: Map<String, Int> = emptyMap(),
    val storageCapacity: Int = 200
) {
    val totalWorkers: Int get() = employees.count { it.assignedBuildingId != null }
    val totalSalaries: Double get() = employees.sumOf { it.monthlySalary }

    fun inventoryCount(): Int = inventory.values.sum()

    /** Capacidad efectiva: base + bonos por almacenes. */
    fun effectiveCapacity(): Int {
        val warehouseBonus = buildings
            .filter { it.type == BuildingType.WAREHOUSE }
            .sumOf { 150 + (it.level - 1) * 150 }
        return storageCapacity + warehouseBonus
    }

    fun xpForNextLevel(): Long = (500 * Math.pow(1.5, (level - 1).toDouble())).toLong()

    fun addXp(amount: Long): Company {
        if (amount <= 0) return this
        var xpNew = xp + amount
        var lvlNew = level
        while (true) {
            val cost = (500 * Math.pow(1.5, (lvlNew - 1).toDouble())).toLong()
            if (cost <= 0 || xpNew < cost) break
            xpNew -= cost
            lvlNew++
            // Hard cap para evitar overflow en runs muy largos.
            if (lvlNew >= 999) break
        }
        return copy(xp = xpNew, level = lvlNew)
    }
}
