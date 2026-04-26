package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor de producción: avanza edificios 1 segundo. Consume inputs al
 * iniciar un ciclo, produce outputs al completarlo. Maneja auto-restart.
 *
 * FIX BUG-07-01: ahora usa AdvancedRecipeCatalog.byId que abarca recetas
 * adv_* además de las base. Sin este cambio las líneas con receta avanzada
 * eran ignoradas (b.currentRecipeId no resolvía y nunca producían).
 *
 * FIX BUG-07-02: invoca QualityEngine cuando una receta avanzada completa
 * un ciclo, depositando el lote en qualityInventory además del inventario
 * clásico (mantiene compatibilidad con el motor existente).
 */
object Production {

    data class Tick(
        val company: Company,
        val qualityInventory: QualityInventory,
        val notifications: List<GameNotification> = emptyList(),
        val xpEarned: Long = 0
    )

    /** Avanza 1 segundo todos los edificios. */
    fun advance(
        company: Company,
        research: ResearchState,
        player: Player,
        qualityInventory: QualityInventory,
        seed: Long
    ): Tick {
        val notifs = mutableListOf<GameNotification>()
        var inv = HashMap(company.inventory)
        var qInv = qualityInventory
        var xp = 0L
        val newBuildings = ArrayList<Building>(company.buildings.size)
        val rng = Random(seed)

        // bono global por investigaciones completadas
        val prodBonus = 1.0 +
            research.completed.sumOf { id -> TechCatalog.byId(id)?.productionBonus ?: 0.0 } +
            // bonus por dex del jugador (hasta +40%)
            (player.stats.dexterity.coerceAtMost(100) * 0.004)

        // FIX BUG-14-01: NO reutilizar productionBonus aquí — ya está aplicado
        // en `prodBonus` y duplicarlo dispararía la velocidad. Para calidad,
        // derivamos un bonus pequeño basado en cantidad de techs completadas
        // (cada tech aporta +0.02, máximo 0.30).
        val qualityResearchBonus = (research.completed.size * 0.02).coerceIn(0.0, 0.30)

        // felicidad reduce productividad si baja de 50
        val happinessFactor = if (player.happiness >= 50) 1.0
        else 0.5 + (player.happiness / 100.0)

        for (b in company.buildings) {
            val recipe = b.currentRecipeId?.let { AdvancedRecipeCatalog.byId(it) }
            if (recipe == null) { newBuildings.add(b); continue }

            // sin trabajadores no hay producción (edificios tipo warehouse ignorados)
            if (b.type != BuildingType.WAREHOUSE && b.assignedWorkers == 0) {
                newBuildings.add(b); continue
            }

            // ¿tiene ingredientes? si no, esperar sin progresar
            val hasInputs = recipe.inputs.all { (id, q) -> (inv[id] ?: 0) >= q }
            if (!hasInputs && b.progressSeconds == 0.0) {
                newBuildings.add(b); continue
            }

            // Consumir ingredientes al iniciar
            if (b.progressSeconds == 0.0 && hasInputs) {
                for ((id, q) in recipe.inputs) {
                    inv[id] = (inv[id] ?: 0) - q
                }
            }

            // Multiplicador de empleados asignados
            val avgSkill = averageSkillOfAssigned(company, b)
            val speed = b.productivity * prodBonus * happinessFactor *
                avgSkill * (b.assignedWorkers.coerceAtMost(b.workerCapacity) /
                    b.workerCapacity.toDouble().coerceAtLeast(1.0))

            var progress = b.progressSeconds + speed

            // ¿Ciclo completado?
            if (progress >= recipe.seconds) {
                // producir outputs, respetando almacén
                val freeSpace = (company.effectiveCapacity() - inv.values.sum())
                    .coerceAtLeast(0)
                val outputsSum = recipe.outputs.values.sum()
                if (outputsSum <= freeSpace) {
                    val isAdvanced = AdvancedRecipeCatalog.isAdvanced(recipe.id)
                    val tier = if (isAdvanced) {
                        QualityEngine.rollQuality(
                            buildingLevel = b.level,
                            avgSkill = avgSkill,
                            researchBonus = qualityResearchBonus,
                            rng = rng
                        )
                    } else QualityTier.STANDARD

                    for ((id, q) in recipe.outputs) {
                        inv[id] = (inv[id] ?: 0) + q
                        if (isAdvanced) {
                            qInv = qInv.addQty(id, tier, q)
                        }
                    }
                    xp += (5 + recipe.seconds / 10).toLong()
                    progress = 0.0

                    if (!b.autoRestart) {
                        newBuildings.add(b.copy(progressSeconds = 0.0, currentRecipeId = null))
                        continue
                    }
                    // si no hay suficientes ingredientes, detener silenciosamente
                    if (!recipe.inputs.all { (id, q) -> (inv[id] ?: 0) >= q }) {
                        newBuildings.add(b.copy(progressSeconds = 0.0))
                        continue
                    } else {
                        for ((id, q) in recipe.inputs) {
                            inv[id] = (inv[id] ?: 0) - q
                        }
                    }
                } else {
                    // sin espacio, hold
                    progress = recipe.seconds.toDouble()
                }
            }

            newBuildings.add(b.copy(progressSeconds = progress))
        }

        return Tick(
            company = company.copy(buildings = newBuildings, inventory = inv),
            qualityInventory = qInv,
            notifications = notifs,
            xpEarned = xp
        )
    }

    private fun averageSkillOfAssigned(company: Company, b: Building): Double {
        val assigned = company.employees.filter { it.assignedBuildingId == b.id }
        if (assigned.isEmpty()) return 0.9
        val avg = assigned.sumOf { it.effectiveOutput() } / assigned.size
        return avg.coerceIn(0.4, 2.2)
    }
}
