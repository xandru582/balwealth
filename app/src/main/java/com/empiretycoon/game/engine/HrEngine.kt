package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.random.Random

/**
 * Motor del subsistema RRHH. Mantiene el patrón funcional del resto del
 * proyecto: GameState in -> GameState out. No hace IO.
 *
 * Convenciones:
 *  - El `Employee` clásico permanece intacto (skill, monthlySalary, loyalty).
 *  - El `EmployeeProfile` extiende cada empleado vía HrState.profiles[id].
 *  - Las acciones del jugador llaman aquí desde GameViewModel.
 */
object HrEngine {

    // -------------------------- 1. CONTRATACIÓN --------------------------

    /**
     * Convierte un `JobApplicant` en `Employee` + `EmployeeProfile`.
     * Cobra `expectedSalary*0.5 + askingBonus` como prima de fichaje.
     */
    fun hireApplicant(state: GameState, appId: String): GameState {
        val app = state.hrState.applicants.find { it.id == appId } ?: return state
        val signing = app.expectedSalary * 0.5 + app.askingBonus
        if (state.company.cash < signing) {
            return notify(state, NotificationKind.ERROR, "Fichaje fallido",
                "No puedes pagar la prima de ${"%,.0f".format(signing)}.")
        }

        val empId = "emp_${state.tick}_${System.nanoTime() % 1_000_000}"
        val employee = Employee(
            id = empId,
            name = app.name,
            skill = (app.startingSkill() * 100).toInt() / 100.0,
            monthlySalary = app.expectedSalary,
            loyalty = 1.0
        )
        val profile = EmployeeProfile(
            employeeId = empId,
            role = app.role,
            level = 1 + (app.prevExperienceYears / 3).coerceAtMost(4),
            xp = app.startingXp(),
            traits = app.traits,
            education = app.education,
            hiredAtTick = state.tick,
            lastPromotionTick = state.tick
        )

        val newCompany = state.company.copy(
            cash = state.company.cash - signing,
            employees = state.company.employees + employee
        )
        val newHr = state.hrState.copy(
            profiles = state.hrState.profiles + (empId to profile),
            applicants = state.hrState.applicants - app
        )
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Fichaje cerrado",
            message = "${app.name} se incorpora como ${RoleCatalog.get(app.role).displayName}."
        )
        return state.copy(
            company = newCompany,
            hrState = newHr,
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    // -------------------------- 2. FORMACIÓN --------------------------

    /**
     * Inicia un programa de formación para `employeeIds`. Cobra coste por persona.
     */
    fun startTraining(state: GameState, programId: String, employeeIds: List<String>): GameState {
        val program = TrainingCatalog.byId(programId) ?: return state
        if (employeeIds.isEmpty()) return state

        val activeIds = state.hrState.training.active.flatMap { it.employeeIds }.toSet()
        val eligible = employeeIds.filter { id ->
            val p = state.hrState.profiles[id] ?: return@filter false
            id !in activeIds &&
                p.education.rank >= program.minimumEducation.rank &&
                p.level >= program.minimumLevel &&
                (program.role == null || program.role == p.role)
        }
        if (eligible.isEmpty()) {
            return notify(state, NotificationKind.WARNING, "Formación rechazada",
                "Ningún candidato cumple los requisitos del programa.")
        }
        val totalCost = program.costPerEmployee * eligible.size
        if (state.company.cash < totalCost) {
            return notify(state, NotificationKind.ERROR, "Sin fondos",
                "La formación cuesta ${"%,.0f".format(totalCost)}.")
        }

        val active = ActiveTraining(
            programId = program.id,
            employeeIds = eligible,
            startedAtTick = state.tick,
            endsAtTick = state.tick + program.durationDays * 1_440L
        )
        val newHr = state.hrState.copy(
            training = state.hrState.training.copy(
                active = state.hrState.training.active + active
            )
        )
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.INFO,
            title = "Formación en marcha",
            message = "${program.name} para ${eligible.size} empleados (${program.durationDays}d)."
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - totalCost),
            hrState = newHr,
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    /**
     * Avanza programas de formación. Si un programa finaliza, aplica boost
     * a empleado y perfil, y suma 1 al historial.
     *
     * Pensado para llamarse en tick (o cada N ticks) desde GameEngine.
     */
    fun tickTraining(state: GameState): GameState {
        val tr = state.hrState.training
        if (tr.active.isEmpty()) return state
        val tick = state.tick

        val finished = tr.active.filter { tick >= it.endsAtTick }
        if (finished.isEmpty()) return state

        var employees = state.company.employees
        var profiles = state.hrState.profiles
        var historyDelta = 0
        val notifs = mutableListOf<GameNotification>()

        for (active in finished) {
            val program = TrainingCatalog.byId(active.programId) ?: continue
            historyDelta += 1
            for (id in active.employeeIds) {
                val emp = employees.find { it.id == id } ?: continue
                val profile = profiles[id] ?: continue
                val newEmp = emp.copy(
                    skill = (emp.skill + program.statBoost.skillDelta).coerceIn(0.4, 3.0)
                )
                val updatedProfile = profile.addXp(program.statBoost.xpDelta)
                    .withMood(
                        satDelta = program.statBoost.satisfactionDelta,
                        burnDelta = program.statBoost.burnoutDelta
                    )
                val withEdu = if (program.grantsEducation != null &&
                    program.grantsEducation.rank > updatedProfile.education.rank
                ) updatedProfile.copy(education = program.grantsEducation) else updatedProfile
                employees = employees.map { if (it.id == id) newEmp else it }
                profiles = profiles + (id to withEdu)
            }
            notifs += GameNotification(
                id = System.nanoTime() + program.id.hashCode().toLong(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.SUCCESS,
                title = "Formación completada",
                message = "${program.name}: +${"%.2f".format(program.statBoost.skillDelta)} skill, " +
                    "${active.employeeIds.size} empleados graduados."
            )
        }

        val newTr = tr.copy(
            active = tr.active - finished.toSet(),
            history = tr.history + historyDelta
        )
        return state.copy(
            company = state.company.copy(employees = employees),
            hrState = state.hrState.copy(profiles = profiles, training = newTr),
            notifications = (state.notifications + notifs).takeLast(40)
        )
    }

    // -------------------------- 3. ASCENSOS --------------------------

    /**
     * Promociona al empleado al siguiente rol según RoleCatalog.promotionPath.
     * Requiere XP suficiente (>=50% del próximo nivel) y subida automática
     * de salario al 130% del salario actual o al baseline del nuevo rol (el mayor).
     */
    fun promote(state: GameState, employeeId: String): GameState {
        val emp = state.company.employees.find { it.id == employeeId } ?: return state
        val profile = state.hrState.profiles[employeeId] ?: return state
        val path = RoleCatalog.promotionPath(profile.role)
        if (path.isEmpty()) {
            return notify(state, NotificationKind.WARNING, "Sin promoción",
                "${emp.name} ya está en la cima de su carrera.")
        }
        if (state.tick - profile.lastPromotionTick < 1_440L * 7) {
            return notify(state, NotificationKind.WARNING, "Demasiado pronto",
                "Aún no ha pasado una semana desde el último ascenso.")
        }

        val nextRole = path.first()
        if (nextRole == EmployeeRole.CXO) {
            return notify(state, NotificationKind.WARNING, "Ascenso C-Suite",
                "Asigna a ${emp.name} a un slot ejecutivo en su lugar.")
        }
        val nextProfile = RoleCatalog.get(nextRole)
        val newSalary = maxOf(
            emp.monthlySalary * 1.30,
            nextProfile.salaryAtLevel(profile.level)
        )

        val newEmp = emp.copy(monthlySalary = (newSalary.toInt()).toDouble())
        val newProf = profile.copy(
            role = nextRole,
            lastPromotionTick = state.tick,
            satisfactionScore = (profile.satisfactionScore + 12).coerceAtMost(100)
        )
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Ascenso aprobado",
            message = "${emp.name} ahora es ${nextProfile.displayName}."
        )
        return state.copy(
            company = state.company.copy(
                employees = state.company.employees.map { if (it.id == employeeId) newEmp else it }
            ),
            hrState = state.hrState.copy(
                profiles = state.hrState.profiles + (employeeId to newProf)
            ),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    // -------------------------- 4. EJECUTIVOS --------------------------

    /**
     * Asigna a un empleado a un slot CXO (CFO/COO/CTO/CMO).
     * Solo válido si el rol previo es DIRECTOR o EXECUTIVE_ASSISTANT.
     * El rol del empleado pasa a CXO; si el slot estaba ocupado, libera al previo
     * (y le devuelve el rol DIRECTOR).
     */
    fun assignToExec(state: GameState, role: String, employeeId: String?): GameState {
        val slot = ExecSlot.fromKey(role)
            ?: return notify(state, NotificationKind.ERROR, "Slot inválido",
                "Slot CXO desconocido: $role")

        val team = state.hrState.executives
        var profiles = state.hrState.profiles
        var employees = state.company.employees

        // Si nos piden vaciar, soltamos al ocupante.
        if (employeeId == null) {
            val prev = team.assignedIds()
                .firstOrNull { id -> when (slot) {
                    ExecSlot.CFO -> team.cfo == id
                    ExecSlot.COO -> team.coo == id
                    ExecSlot.CTO -> team.cto == id
                    ExecSlot.CMO -> team.cmo == id
                } }
            if (prev != null) profiles = demoteFromCxo(profiles, prev)
            return state.copy(
                hrState = state.hrState.copy(
                    profiles = profiles,
                    executives = team.withSlot(slot, null)
                )
            )
        }

        val candidate = profiles[employeeId]
            ?: return notify(state, NotificationKind.ERROR, "Empleado sin perfil",
                "No se puede ascender a CXO.")
        if (candidate.role != EmployeeRole.DIRECTOR &&
            candidate.role != EmployeeRole.EXECUTIVE_ASSISTANT &&
            candidate.role != EmployeeRole.CXO
        ) {
            return notify(state, NotificationKind.WARNING, "Promoción no válida",
                "Sólo Directores o Asistentes Ejecutivos pueden ser CXO.")
        }
        // Liberar al ocupante anterior, si existe
        val prevId = when (slot) {
            ExecSlot.CFO -> team.cfo
            ExecSlot.COO -> team.coo
            ExecSlot.CTO -> team.cto
            ExecSlot.CMO -> team.cmo
        }
        if (prevId != null && prevId != employeeId) profiles = demoteFromCxo(profiles, prevId)

        // Si el empleado ya estaba en otro slot, lo liberamos antes
        val cleanedTeam = team.release(employeeId).withSlot(slot, employeeId)

        // Actualizar perfil y salario
        val cxoProfile = candidate.copy(
            role = EmployeeRole.CXO,
            lastPromotionTick = state.tick,
            satisfactionScore = (candidate.satisfactionScore + 18).coerceAtMost(100)
        )
        profiles = profiles + (employeeId to cxoProfile)
        val cxoSalary = RoleCatalog.get(EmployeeRole.CXO)
            .salaryAtLevel(candidate.level)
            .coerceAtLeast(state.company.employees.find { it.id == employeeId }?.monthlySalary ?: 0.0)
        employees = employees.map {
            if (it.id == employeeId) it.copy(monthlySalary = cxoSalary) else it
        }

        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Nuevo ${slot.displayName}",
            message = "${candidate.employeeId.let { id ->
                state.company.employees.find { it.id == id }?.name ?: id
            }} se une al C-Suite."
        )

        return state.copy(
            company = state.company.copy(employees = employees),
            hrState = state.hrState.copy(
                profiles = profiles,
                executives = cleanedTeam
            ),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    private fun demoteFromCxo(
        profiles: Map<String, EmployeeProfile>,
        employeeId: String
    ): Map<String, EmployeeProfile> {
        val p = profiles[employeeId] ?: return profiles
        return profiles + (employeeId to p.copy(role = EmployeeRole.DIRECTOR))
    }

    /**
     * Devuelve el mapa de bonos globales agregados por la cúpula.
     * Las claves son consumidas por el motor:
     *   - "production"          -> multiplicador velocidad de producción
     *   - "research"            -> multiplicador velocidad de I+D
     *   - "market_sell"         -> multiplicador precio de venta
     *   - "capital_efficiency"  -> reducción de costes financieros
     */
    fun aggregateExecBonus(state: GameState): Map<String, Double> {
        val team = state.hrState.executives
        val out = HashMap<String, Double>()
        out[ExecSlot.CFO.bonusKey] = if (team.cfo != null) ExecSlot.CFO.bonus else 0.0
        out[ExecSlot.COO.bonusKey] = if (team.coo != null) ExecSlot.COO.bonus else 0.0
        out[ExecSlot.CTO.bonusKey] = if (team.cto != null) ExecSlot.CTO.bonus else 0.0
        out[ExecSlot.CMO.bonusKey] = if (team.cmo != null) ExecSlot.CMO.bonus else 0.0
        return out
    }

    // -------------------------- 5. ROTACIÓN --------------------------

    /**
     * Aplicar churn HR diario: empleados con burnout muy alto o satisfacción
     * muy baja pueden dejar la empresa. Se invoca una vez al día desde el motor.
     */
    fun churnHr(state: GameState, rng: Random): GameState {
        if (state.company.employees.isEmpty()) return state

        var employees = state.company.employees
        var profiles = state.hrState.profiles
        var team = state.hrState.executives
        val notifs = mutableListOf<GameNotification>()

        // Drift natural: sube burnout si llevan tiempo sin descanso
        profiles = profiles.mapValues { (_, p) ->
            val deltaSat = if (p.vacationDaysLeft <= 0) -2 else 0
            val deltaBurn = if (p.role == EmployeeRole.CXO) +2 else +1
            p.withMood(satDelta = deltaSat, burnDelta = deltaBurn)
        }

        // Cribar bajas
        val leavers = mutableListOf<String>()
        for (emp in employees) {
            val p = profiles[emp.id] ?: continue
            val burnFactor = if (p.isBurnedOut) 0.18 else 0.0
            val unhappyFactor = if (p.satisfactionScore < 25) 0.12 else 0.0
            val loyaltyFactor = if (emp.loyalty < 0.3) 0.10 else 0.0
            val pLeave = (burnFactor + unhappyFactor + loyaltyFactor).coerceAtMost(0.45)
            if (pLeave > 0 && rng.nextDouble() < pLeave) {
                leavers += emp.id
            }
        }
        if (leavers.isEmpty()) {
            return state.copy(hrState = state.hrState.copy(profiles = profiles))
        }
        for (id in leavers) {
            val name = employees.find { it.id == id }?.name ?: id
            notifs += GameNotification(
                id = System.nanoTime() + id.hashCode().toLong(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.WARNING,
                title = "Renuncia",
                message = "$name ha presentado su dimisión."
            )
            employees = employees.filterNot { it.id == id }
            profiles = profiles - id
            team = team.release(id)
        }
        return state.copy(
            company = state.company.copy(employees = employees),
            hrState = state.hrState.copy(
                profiles = profiles,
                executives = team
            ),
            notifications = (state.notifications + notifs).takeLast(40)
        )
    }

    // -------------------------- 6. SOLICITANTES --------------------------

    /**
     * Refresca el pool de candidatos. Se llama al inicio del juego y a diario.
     * Las ofertas vencidas (`expiresAtTick < tick`) se descartan.
     */
    fun refreshApplicants(state: GameState, rng: Random): GameState {
        val pool = ApplicantGenerator.pool(
            reputation = state.company.reputation,
            level = state.company.level,
            rng = rng,
            currentTick = state.tick
        )
        val newHr = state.hrState.copy(
            applicants = pool,
            lastApplicantRefreshTick = state.tick
        )
        return state.copy(hrState = newHr)
    }

    // -------------------------- 7. UTIL --------------------------

    /**
     * Garantiza que cada `Employee` tenga un `EmployeeProfile`. Útil al cargar
     * partidas antiguas (donde los Employee pre-existían sin HrState).
     */
    fun ensureProfilesForLegacyEmployees(state: GameState): GameState {
        val missing = state.company.employees.filter { it.id !in state.hrState.profiles }
        if (missing.isEmpty()) return state
        val newProfiles = missing.associate { emp ->
            emp.id to EmployeeProfile(
                employeeId = emp.id,
                role = EmployeeRole.LABORER,
                level = 1,
                xp = 0,
                traits = emptyList(),
                education = Education.HIGHSCHOOL,
                hiredAtTick = state.tick,
                lastPromotionTick = state.tick
            )
        }
        return state.copy(
            hrState = state.hrState.copy(
                profiles = state.hrState.profiles + newProfiles
            )
        )
    }

    /** Limpia perfiles cuyo Employee ya no existe (despidos o renuncias). */
    fun cleanupOrphanProfiles(state: GameState): GameState {
        val live = state.company.employees.map { it.id }.toSet()
        val orphans = state.hrState.profiles.keys - live
        if (orphans.isEmpty()) return state
        return state.copy(
            hrState = state.hrState.copy(
                profiles = state.hrState.profiles - orphans,
                executives = orphans.fold(state.hrState.executives) { team, id -> team.release(id) }
            )
        )
    }

    private fun notify(state: GameState, kind: NotificationKind, title: String, msg: String): GameState {
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = kind,
            title = title,
            message = msg
        )
        return state.copy(notifications = (state.notifications + n).takeLast(40))
    }
}
