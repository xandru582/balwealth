package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Slots de la cúpula directiva. El CEO es siempre el jugador (no se asigna).
 * Cada slot guarda el `Employee.id` de quien lo ocupa, o null si vacante.
 *
 * Bonos GLOBALES asociados a cada CXO (aplicados por HrEngine.aggregateExecBonus):
 *  - CFO  -> +5%  capital efficiency  (key: "capital_efficiency")
 *  - COO  -> +8%  production           (key: "production")
 *  - CTO  -> +10% research speed       (key: "research")
 *  - CMO  -> +6%  market sell price    (key: "market_sell")
 *
 * Reglas de asignación (HrEngine.assignToExec):
 *  - Sólo se permite EXECUTIVE_ASSISTANT o DIRECTOR como CXO
 *  - Al asignar, el rol del empleado se promociona a CXO automáticamente
 */
@Serializable
data class ExecutiveTeam(
    val cfo: String? = null,
    val coo: String? = null,
    val cto: String? = null,
    val cmo: String? = null
) {

    /** Ids de empleados ocupando algún slot. */
    fun assignedIds(): Set<String> =
        listOfNotNull(cfo, coo, cto, cmo).toSet()

    fun isOccupied(employeeId: String): Boolean =
        employeeId in assignedIds()

    /** Devuelve copia con `slot` ocupado por `employeeId` (o null para vaciar). */
    fun withSlot(slot: ExecSlot, employeeId: String?): ExecutiveTeam = when (slot) {
        ExecSlot.CFO -> copy(cfo = employeeId)
        ExecSlot.COO -> copy(coo = employeeId)
        ExecSlot.CTO -> copy(cto = employeeId)
        ExecSlot.CMO -> copy(cmo = employeeId)
    }

    /** Si `employeeId` ocupa algún slot, lo libera. */
    fun release(employeeId: String): ExecutiveTeam {
        var t = this
        if (cfo == employeeId) t = t.copy(cfo = null)
        if (coo == employeeId) t = t.copy(coo = null)
        if (cto == employeeId) t = t.copy(cto = null)
        if (cmo == employeeId) t = t.copy(cmo = null)
        return t
    }
}

/** Identificadores de cada slot CXO (excluyendo CEO = jugador). */
enum class ExecSlot(
    val key: String,
    val displayName: String,
    val emoji: String,
    val bonusKey: String,
    val bonus: Double,
    val description: String
) {
    CFO("CFO", "CFO", "💰",
        "capital_efficiency", 0.05,
        "+5% eficiencia de capital (intereses, gastos)."),
    COO("COO", "COO", "🏭",
        "production", 0.08,
        "+8% producción global."),
    CTO("CTO", "CTO", "🧪",
        "research", 0.10,
        "+10% velocidad de I+D."),
    CMO("CMO", "CMO", "📣",
        "market_sell", 0.06,
        "+6% precio de venta en mercado.");

    companion object {
        fun fromKey(key: String): ExecSlot? = values().firstOrNull { it.key.equals(key, true) }
        val all: List<ExecSlot> = values().toList()
    }
}

/** Útil para que la UI pinte el slot del CEO (jugador) sin confundirlo con uno C-suite. */
object CeoSlot {
    const val key: String = "CEO"
    const val displayName: String = "CEO"
    const val emoji: String = "👑"
    const val description: String = "Tú diriges la compañía."
}
