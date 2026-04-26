package com.empiretycoon.game.engine

import com.empiretycoon.game.model.*
import kotlin.math.max
import kotlin.random.Random

/**
 * Motor de Formula Manager.
 *
 *  - Inicialización lazy (al desbloquear o entrar por primera vez).
 *  - Simulación de carrera basada en (skill piloto * coche * suerte).
 *  - Pago diario de salarios + sponsor income.
 *  - Auto-tick semanal de la próxima carrera del calendario.
 *  - Acciones del jugador: comprar equipo, fichar/despedir, mejorar coche.
 */
object RacingEngine {

    // =====================================================================
    //                      INICIALIZACIÓN
    // =====================================================================

    /** Bootstraps el subsistema si está vacío. Idempotente. */
    fun ensureInitialized(state: GameState, currentDay: Int): GameState {
        val r = state.racing
        if (r.teams.isNotEmpty()) return state
        val seed = (currentDay.toLong() * 8191L) xor 0x5A17C0DEL
        val rng = Random(seed)
        val calendar = generateSeasonCalendar(currentDay + 7, rng)
        return state.copy(
            racing = r.copy(
                unlocked = true,
                teams = TeamCatalog.starter,
                drivers = DriverPool.all,
                calendar = calendar,
                nextRaceIndex = 0,
                currentSeason = 1,
                racesThisSeason = 0,
                lastSimulatedDay = currentDay
            )
        )
    }

    private fun generateSeasonCalendar(startDay: Int, rng: Random): List<CalendarRace> {
        val circuits = CircuitCatalog.all.shuffled(rng).take(RacingConstants.RACES_PER_SEASON)
        return circuits.mapIndexed { idx, c ->
            CalendarRace(
                raceIndex = idx,
                circuitId = c.id,
                raceDay = startDay + idx * RacingConstants.DAYS_BETWEEN_RACES
            )
        }
    }

    // =====================================================================
    //                      ACCIONES DEL JUGADOR
    // =====================================================================

    /** Comprar el equipo `teamId`. Requiere cash >= price. */
    fun buyTeam(state: GameState, teamId: String): GameState {
        val r = state.racing
        if (r.ownedTeamId != null) return notify(state, NotificationKind.WARNING,
            "Ya tienes equipo", "Vende tu equipo actual antes de comprar otro.")
        val team = r.teams.find { it.id == teamId }
            ?: return notify(state, NotificationKind.ERROR, "Equipo inexistente",
                "El equipo seleccionado no existe.")
        if (state.company.cash < team.price) return notify(state, NotificationKind.ERROR,
            "Sin fondos", "Necesitas ${"%,.0f".format(team.price)} € en caja.")

        val newTeams = r.teams.map { if (it.id == teamId) it.copy(ownedByPlayer = true) else it }
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "🏁 Has comprado ${team.flag} ${team.name}",
            message = "Pagaste ${"%,.0f".format(team.price)} €. ¡Bienvenido al circo de la velocidad!"
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - team.price).addXp(2_000),
            racing = r.copy(ownedTeamId = teamId, teams = newTeams),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    /** Vender el equipo del jugador (recupera 80% del precio). */
    fun sellTeam(state: GameState): GameState {
        val r = state.racing
        val team = r.ownedTeam() ?: return state
        val refund = team.price * 0.80
        val newTeams = r.teams.map { if (it.id == team.id) it.copy(ownedByPlayer = false) else it }
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.INFO,
            title = "Has vendido ${team.name}",
            message = "Recuperas ${"%,.0f".format(refund)} € (80% del precio)."
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash + refund),
            racing = r.copy(ownedTeamId = null, teams = newTeams),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    enum class CarPart(val displayName: String, val baseCost: Double) {
        ENGINE("Motor", 250_000.0),
        AERO("Aerodinámica", 220_000.0),
        RELIABILITY("Fiabilidad", 180_000.0),
        TYRES("Neumáticos", 120_000.0)
    }

    /** Mejora la parte indicada en +3..+5 (escalado por nivel actual). */
    fun upgradeCarPart(state: GameState, part: CarPart): GameState {
        val r = state.racing
        val team = r.ownedTeam() ?: return state
        val car = team.car
        val level = when (part) {
            CarPart.ENGINE -> car.engine
            CarPart.AERO -> car.aero
            CarPart.RELIABILITY -> car.reliability
            CarPart.TYRES -> car.tyres
        }
        if (level >= 99) return notify(state, NotificationKind.WARNING,
            "Ya está al máximo", "${part.displayName} no puede mejorarse más.")
        // Cuanto más alto el nivel, más caro
        val cost = part.baseCost * (1.0 + level * 0.05)
        if (state.company.cash < cost) return notify(state, NotificationKind.ERROR,
            "Sin fondos", "Necesitas ${"%,.0f".format(cost)} €.")
        val gain = (5 - level / 25).coerceAtLeast(2)
        val newCar = when (part) {
            CarPart.ENGINE -> car.copy(engine = (car.engine + gain).coerceAtMost(99))
            CarPart.AERO -> car.copy(aero = (car.aero + gain).coerceAtMost(99))
            CarPart.RELIABILITY -> car.copy(reliability = (car.reliability + gain).coerceAtMost(99))
            CarPart.TYRES -> car.copy(tyres = (car.tyres + gain).coerceAtMost(99))
        }.copy(totalUpgradeSpend = car.totalUpgradeSpend + cost)
        val newTeam = team.copy(car = newCar)
        val newTeams = r.teams.map { if (it.id == team.id) newTeam else it }
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "🔧 ${part.displayName} mejorada (+$gain)",
            message = "Coste: ${"%,.0f".format(cost)} €. Nuevo nivel: ${level + gain}."
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - cost),
            racing = r.copy(teams = newTeams),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    /** Contratar piloto al equipo del jugador en slot 1 ó 2. */
    fun signDriver(state: GameState, driverId: String, slot: Int): GameState {
        val r = state.racing
        val team = r.ownedTeam() ?: return state
        val driver = DriverPool.byId(driverId) ?: return state
        // No puede estar en otro equipo (excepto el tuyo)
        val occupied = r.teams.any {
            it.id != team.id && (it.driver1Id == driverId || it.driver2Id == driverId)
        }
        if (occupied) return notify(state, NotificationKind.WARNING,
            "Piloto ocupado", "${driver.name} ya tiene contrato con otro equipo.")
        // Cuesta una prima de fichaje = 30 días de salario
        val signOnFee = driver.salaryPerDay * 30
        if (state.company.cash < signOnFee) return notify(state, NotificationKind.ERROR,
            "Sin fondos", "Prima de fichaje: ${"%,.0f".format(signOnFee)} €.")
        val newTeam = if (slot == 1) team.copy(driver1Id = driverId)
                      else team.copy(driver2Id = driverId)
        val newTeams = r.teams.map { if (it.id == team.id) newTeam else it }
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "✍️ Fichaje confirmado",
            message = "${driver.flag} ${driver.name} se une a ${team.name}."
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - signOnFee),
            racing = r.copy(teams = newTeams),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    // === SPONSORSHIPS ===

    /** Firma un contrato con un patrocinador (paga prima up-front). */
    fun signSponsor(state: GameState, sponsorId: String): GameState {
        val r = state.racing
        val team = r.ownedTeam() ?: return notify(state, NotificationKind.WARNING,
            "Sin equipo", "Necesitas un equipo de carreras para firmar patrocinios.")
        val sponsor = SponsorCatalog.byId(sponsorId)
            ?: return notify(state, NotificationKind.ERROR, "Patrocinador inexistente", "")
        if (r.activeSponsorships.any { it.sponsorId == sponsorId }) return notify(state,
            NotificationKind.WARNING, "Ya patrocinado", "Ese contrato sigue activo.")
        if (team.brandValue < sponsor.minBrandRequired) return notify(state,
            NotificationKind.ERROR, "Brand insuficiente",
            "${sponsor.brand} exige brand ${sponsor.minBrandRequired} (tienes ${team.brandValue}).")
        if (state.company.cash < sponsor.signOnFee) return notify(state, NotificationKind.ERROR,
            "Sin fondos", "Prima de firma: ${"%,.0f".format(sponsor.signOnFee)} €.")
        val newContract = ActiveSponsorship(
            sponsorId = sponsorId,
            signedOnDay = state.day,
            expiresOnDay = state.day + sponsor.contractDays
        )
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "✍️ ${sponsor.tier.emoji} ${sponsor.brand}",
            message = "Contrato ${sponsor.contractDays} días. Prima: ${"%,.0f".format(sponsor.signOnFee)} €."
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - sponsor.signOnFee),
            racing = r.copy(activeSponsorships = r.activeSponsorships + newContract),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    /** Cancela un sponsorship activo (penalización: 30% de lo que falta del contrato). */
    fun cancelSponsor(state: GameState, sponsorId: String): GameState {
        val r = state.racing
        val active = r.activeSponsorships.find { it.sponsorId == sponsorId } ?: return state
        val sponsor = SponsorCatalog.byId(sponsorId) ?: return state
        val daysLeft = (active.expiresOnDay - state.day).coerceAtLeast(0)
        val penalty = sponsor.baseDailyPay * daysLeft * 0.30
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.WARNING,
            title = "Contrato roto",
            message = "${sponsor.brand} cobra ${"%,.0f".format(penalty)} € por ruptura."
        )
        return state.copy(
            company = state.company.copy(cash = (state.company.cash - penalty).coerceAtLeast(0.0)),
            racing = r.copy(activeSponsorships = r.activeSponsorships.filterNot { it.sponsorId == sponsorId }),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    // === STAFF ===

    fun hireStaff(state: GameState, staffId: String): GameState {
        val r = state.racing
        val team = r.ownedTeam() ?: return notify(state, NotificationKind.WARNING,
            "Sin equipo", "Compra un equipo antes de contratar personal.")
        val staff = StaffPool.byId(staffId) ?: return state
        if (r.hiredStaff.contains(staffId)) return notify(state, NotificationKind.WARNING,
            "Ya contratado", "${staff.name} ya está en plantilla.")
        // Solo uno por rol
        if (r.hiredStaff.mapNotNull { StaffPool.byId(it) }.any { it.role == staff.role })
            return notify(state, NotificationKind.WARNING,
                "Rol ocupado", "Ya tienes un ${staff.role.label} contratado.")
        val signOnFee = staff.salaryPerDay * 30
        if (state.company.cash < signOnFee) return notify(state, NotificationKind.ERROR,
            "Sin fondos", "Prima de contratación: ${"%,.0f".format(signOnFee)} €.")
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "${staff.role.emoji} ${staff.role.label}",
            message = "${staff.flag} ${staff.name} (${staff.rating}/100) se une a ${team.name}."
        )
        return state.copy(
            company = state.company.copy(cash = state.company.cash - signOnFee),
            racing = r.copy(hiredStaff = r.hiredStaff + staffId),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    fun fireStaff(state: GameState, staffId: String): GameState {
        val r = state.racing
        val staff = StaffPool.byId(staffId) ?: return state
        val severance = staff.salaryPerDay * 60
        return state.copy(
            company = state.company.copy(cash = (state.company.cash - severance).coerceAtLeast(0.0)),
            racing = r.copy(hiredStaff = r.hiredStaff - staffId),
            notifications = (state.notifications + GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.INFO,
                title = "Despido de ${staff.role.label}",
                message = "${staff.name} se va. Indemnización ${"%,.0f".format(severance)} €."
            )).takeLast(40)
        )
    }

    /** Despedir al piloto en slot 1 ó 2 (cuesta indemnización = 60 días de salario). */
    fun fireDriver(state: GameState, slot: Int): GameState {
        val r = state.racing
        val team = r.ownedTeam() ?: return state
        val driverId = if (slot == 1) team.driver1Id else team.driver2Id
        val driver = driverId?.let { DriverPool.byId(it) } ?: return state
        val severance = driver.salaryPerDay * 60
        val newTeam = if (slot == 1) team.copy(driver1Id = null) else team.copy(driver2Id = null)
        val newTeams = r.teams.map { if (it.id == team.id) newTeam else it }
        val n = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.WARNING,
            title = "Piloto despedido",
            message = "${driver.name} se va. Indemnización: ${"%,.0f".format(severance)} €."
        )
        return state.copy(
            company = state.company.copy(cash = (state.company.cash - severance).coerceAtLeast(0.0)),
            racing = r.copy(teams = newTeams),
            notifications = (state.notifications + n).takeLast(40)
        )
    }

    // =====================================================================
    //                      TICK PERIÓDICO
    // =====================================================================

    /**
     * Llamar cada cambio de día. Si el jugador es dueño de un equipo,
     * cobra sponsor income y resta salarios. Si toca carrera, simula.
     */
    fun dailyTick(state: GameState, currentDay: Int, rng: Random): GameState {
        val r = state.racing
        if (!r.unlocked) return state

        var s = state
        // Pago diario al / del equipo del jugador (sponsor base + sponsorships + staff)
        val owned = r.ownedTeam()
        if (owned != null) {
            val sponsorshipIncome = r.totalSponsorDailyIncome()
            val staffCost = r.totalStaffDailyCost()
            val income = owned.sponsorIncomePerDay + sponsorshipIncome
            val cost = owned.totalDailyCost() + staffCost
            val net = income - cost
            s = s.copy(company = s.company.copy(cash = s.company.cash + net))
            val updatedTeam = owned.copy(budget = owned.budget + net)
            val newTeams = s.racing.teams.map { if (it.id == owned.id) updatedTeam else it }
            s = s.copy(racing = s.racing.copy(
                teams = newTeams,
                totalSponsorEarnings = s.racing.totalSponsorEarnings + sponsorshipIncome
            ))
            // Expirar sponsorships vencidos
            val expired = r.activeSponsorships.filter { it.expiresOnDay <= currentDay }
            if (expired.isNotEmpty()) {
                val notifs = expired.mapNotNull { exp ->
                    SponsorCatalog.byId(exp.sponsorId)?.let { sp ->
                        GameNotification(
                            id = System.nanoTime() + exp.sponsorId.hashCode(),
                            timestamp = System.currentTimeMillis(),
                            kind = NotificationKind.INFO,
                            title = "Contrato finalizado",
                            message = "${sp.brand} ya no patrocina. Total ganado: ${"%,.0f".format(exp.totalEarned)} €."
                        )
                    }
                }
                s = s.copy(
                    racing = s.racing.copy(
                        activeSponsorships = s.racing.activeSponsorships - expired.toSet()
                    ),
                    notifications = (s.notifications + notifs).takeLast(40)
                )
            }
        }

        // ¿Toca carrera hoy?
        val nextRace = s.racing.nextRace()
        if (nextRace != null && currentDay >= nextRace.raceDay) {
            s = simulateRace(s, nextRace, rng)
        }

        // ¿Fin de temporada? (todas las carreras corridas)
        if (s.racing.nextRaceIndex >= s.racing.calendar.size && s.racing.calendar.isNotEmpty()) {
            s = endOfSeason(s, currentDay, rng)
        }

        return s.copy(racing = s.racing.copy(lastSimulatedDay = currentDay))
    }

    /** Simula una carrera completa y aplica resultados al estado. */
    fun simulateRace(state: GameState, race: CalendarRace, rng: Random): GameState {
        val r = state.racing
        val circuit = CircuitCatalog.byId(race.circuitId) ?: return state
        val weather = state.weather.current
        // Bonus de staff del equipo del jugador (multiplicador a su performance)
        val playerStaffMult = if (r.ownedTeamId != null) {
            val staffMembers = r.hiredStaff.mapNotNull { StaffPool.byId(it) }
            // Cada rol aporta su bonus, capped a +20% total
            val combined = staffMembers.map { it.bonusMultiplier() - 1.0 }.sum()
            (1.0 + combined.coerceAtMost(0.20))
        } else 1.0

        // Recogemos todos los pilotos activos
        val starters = mutableListOf<Triple<String, String, Double>>()
        for (team in r.teams) {
            for (did in listOfNotNull(team.driver1Id, team.driver2Id)) {
                val driver = state.racing.drivers.find { it.id == did } ?: DriverPool.byId(did) ?: continue
                val mult = if (team.id == r.ownedTeamId) playerStaffMult else 1.0
                starters += Triple(did, team.id, computePerformance(driver, team, circuit, weather, rng, mult))
            }
        }
        val ordered = starters.sortedByDescending { it.third }
        // Probabilidad de DNF basada en fiabilidad
        val finishOrder = mutableListOf<Pair<String, String>>()
        val dnfs = mutableListOf<Pair<String, String>>()
        for ((did, tid, _) in ordered) {
            val team = r.teams.find { it.id == tid } ?: continue
            val dnfChance = ((100 - team.car.reliability) * 0.001 * circuit.difficulty).coerceIn(0.0, 0.10)
            if (rng.nextDouble() < dnfChance) {
                dnfs += did to tid
            } else {
                finishOrder += did to tid
            }
        }

        // Asignar puntos del top 10
        val points = HashMap<String, Int>()       // driverId -> +pts
        val teamPoints = HashMap<String, Int>()   // teamId -> +pts
        for ((idx, pair) in finishOrder.withIndex()) {
            if (idx >= RacingConstants.POINTS_PER_POSITION.size) break
            val (did, tid) = pair
            val pts = RacingConstants.POINTS_PER_POSITION[idx]
            points[did] = (points[did] ?: 0) + pts
            teamPoints[tid] = (teamPoints[tid] ?: 0) + pts
        }

        // Bonus por vuelta rápida (random entre top 5)
        val fastestLap = if (finishOrder.size >= 5)
            finishOrder.subList(0, 5).random(rng).first
        else finishOrder.firstOrNull()?.first
        val pole = if (finishOrder.size >= 3)
            finishOrder.subList(0, 3).random(rng).first
        else finishOrder.firstOrNull()?.first
        if (fastestLap != null) {
            points[fastestLap] = (points[fastestLap] ?: 0) + RacingConstants.FASTEST_LAP_BONUS
            // bonus a su equipo también
            val tid = finishOrder.find { it.first == fastestLap }?.second
            if (tid != null) teamPoints[tid] = (teamPoints[tid] ?: 0) + RacingConstants.FASTEST_LAP_BONUS
        }

        // Actualizar driverStats (stats detalladas)
        val baseDrivers = if (r.drivers.isNotEmpty()) r.drivers else DriverPool.all
        val updatedDrivers = baseDrivers.map { d ->
            val gained = points[d.id] ?: 0
            val pos = finishOrder.indexOfFirst { it.first == d.id } + 1
            val didDnf = dnfs.any { it.first == d.id }
            val didStart = pos > 0 || didDnf
            if (!didStart) return@map d
            val isWin = pos == 1
            val isPodium = pos in 1..3
            val isPole = d.id == pole
            val isFastest = d.id == fastestLap
            val newWinStreak = if (isWin) d.currentWinStreak + 1 else 0
            d.copy(
                seasonPoints = d.seasonPoints + gained,
                careerStarts = d.careerStarts + 1,
                careerWins = d.careerWins + if (isWin) 1 else 0,
                careerPodiums = d.careerPodiums + if (isPodium) 1 else 0,
                careerPoles = d.careerPoles + if (isPole) 1 else 0,
                careerFastestLaps = d.careerFastestLaps + if (isFastest) 1 else 0,
                careerDNFs = d.careerDNFs + if (didDnf) 1 else 0,
                careerPoints = d.careerPoints + gained,
                bestSeasonPoints = maxOf(d.bestSeasonPoints, d.seasonPoints + gained),
                currentWinStreak = newWinStreak,
                longestWinStreak = maxOf(d.longestWinStreak, newWinStreak),
                avgFinishSum = if (pos > 0) d.avgFinishSum + pos else d.avgFinishSum,
                finishCount = if (pos > 0) d.finishCount + 1 else d.finishCount,
                morale = (d.morale + when {
                    isWin -> 6
                    isPodium -> 3
                    gained > 0 -> 1
                    didDnf -> -3
                    else -> -1
                }).coerceIn(0, 100)
            )
        }

        // Actualizar teamPoints + premio en metálico al equipo del jugador (si terminó top 10)
        val newTeams = r.teams.map { team ->
            val gained = teamPoints[team.id] ?: 0
            // Premio: para todos según posición de su mejor piloto
            val bestPos = finishOrder.withIndex()
                .filter { it.value.second == team.id }
                .minOfOrNull { it.index + 1 } ?: 99
            val prize = when {
                bestPos == 1 -> circuit.basePrize
                bestPos == 2 -> circuit.basePrize * 0.65
                bestPos == 3 -> circuit.basePrize * 0.45
                bestPos in 4..6 -> circuit.basePrize * 0.25
                bestPos in 7..10 -> circuit.basePrize * 0.10
                else -> 0.0
            }
            val newBudget = team.budget + prize
            // Si era el equipo del jugador, también suma a su cash
            team.copy(seasonPoints = team.seasonPoints + gained, budget = newBudget)
        }

        // Premio en cash al jugador
        val playerTeam = r.ownedTeam()
        var company = state.company
        var notifications = state.notifications
        var playerFinishMap: Map<String, Int> = emptyMap()
        if (playerTeam != null) {
            val playerDrivers = listOfNotNull(playerTeam.driver1Id, playerTeam.driver2Id)
            val finishes = HashMap<String, Int>()
            for (pid in playerDrivers) {
                val pos = finishOrder.indexOfFirst { it.first == pid } + 1
                finishes[pid] = if (pos <= 0) 0 else pos
            }
            playerFinishMap = finishes
            val bestPos = finishes.values.filter { it > 0 }.minOrNull() ?: 99
            val prize = when {
                bestPos == 1 -> circuit.basePrize
                bestPos == 2 -> circuit.basePrize * 0.65
                bestPos == 3 -> circuit.basePrize * 0.45
                bestPos in 4..6 -> circuit.basePrize * 0.25
                bestPos in 7..10 -> circuit.basePrize * 0.10
                else -> 0.0
            }
            if (prize > 0) {
                company = company.copy(cash = company.cash + prize).addXp(300L + (1100L / bestPos.coerceAtLeast(1)))
            }
            // Notificación principal del resultado
            val title = when (bestPos) {
                1 -> "🥇 ¡Victoria en ${circuit.name}!"
                2 -> "🥈 P2 en ${circuit.name}"
                3 -> "🥉 P3 en ${circuit.name}"
                in 4..10 -> "🏁 P$bestPos en ${circuit.name}"
                else -> "❌ Sin puntos en ${circuit.name}"
            }
            val msg = if (prize > 0) "Premio: ${"%,.0f".format(prize)} €."
                      else "Tu equipo no entró en zona de puntos."
            notifications = (notifications + GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = if (bestPos <= 3) NotificationKind.SUCCESS else NotificationKind.INFO,
                title = title,
                message = msg
            )).takeLast(40)
        }

        val result = RaceResult(
            raceIndex = race.raceIndex,
            circuitId = race.circuitId,
            day = state.day,
            finishOrder = finishOrder,
            fastestLapDriver = fastestLap,
            poleDriver = pole,
            playerDriverFinish = playerFinishMap
        )

        // === SPONSOR BONUSES (player team) ===
        var newActiveSponsorships = r.activeSponsorships
        if (playerTeam != null) {
            val playerWon = finishOrder.firstOrNull()?.second == playerTeam.id
            val playerPodium = finishOrder.take(3).any { it.second == playerTeam.id }
            val playerPoints = teamPoints[playerTeam.id] ?: 0
            var bonusTotal = 0.0
            newActiveSponsorships = r.activeSponsorships.map { sp ->
                val sponsor = SponsorCatalog.byId(sp.sponsorId) ?: return@map sp
                val winB = if (playerWon) sponsor.winBonus else 0.0
                val podB = if (playerPodium && !playerWon) sponsor.podiumBonus else 0.0
                val ptsB = sponsor.pointsBonus * playerPoints
                val total = winB + podB + ptsB
                bonusTotal += total
                sp.copy(
                    totalEarned = sp.totalEarned + total,
                    winsDuringContract = sp.winsDuringContract + if (playerWon) 1 else 0,
                    podiumsDuringContract = sp.podiumsDuringContract + if (playerPodium) 1 else 0
                )
            }
            if (bonusTotal > 0) {
                company = company.copy(cash = company.cash + bonusTotal)
                notifications = (notifications + GameNotification(
                    id = System.nanoTime() + 17,
                    timestamp = System.currentTimeMillis(),
                    kind = NotificationKind.SUCCESS,
                    title = "💰 Bonificación de patrocinadores",
                    message = "Has recibido ${"%,.0f".format(bonusTotal)} € extra por la actuación."
                )).takeLast(40)
            }
        }

        // === LAP RECORD + WINNER per circuit ===
        val winnerPair = finishOrder.firstOrNull()
        val newRecord: CircuitRecord = run {
            val existing = r.recordOf(circuit.id)
            // Tiempo de vuelta simulado (no es real pero sirve para compararse en el tiempo)
            val baseLapSec = (circuit.lengthKm / circuit.avgTopSpeedKmh.toDouble()) * 3600.0
            val simulatedLapTime = baseLapSec * (1.0 - (rng.nextDouble() * 0.04 - 0.02))
            val isNewRecord = existing == null || simulatedLapTime < existing.lapRecordTimeSec - 0.001
            val winsByDriver = (existing?.winsByDriver ?: emptyMap()).toMutableMap()
            val winsByTeam = (existing?.winsByTeam ?: emptyMap()).toMutableMap()
            if (winnerPair != null) {
                winsByDriver[winnerPair.first] = (winsByDriver[winnerPair.first] ?: 0) + 1
                winsByTeam[winnerPair.second] = (winsByTeam[winnerPair.second] ?: 0) + 1
            }
            CircuitRecord(
                circuitId = circuit.id,
                lapRecordHolder = if (isNewRecord && fastestLap != null) fastestLap else existing?.lapRecordHolder,
                lapRecordTimeSec = if (isNewRecord) simulatedLapTime else (existing?.lapRecordTimeSec ?: simulatedLapTime),
                lapRecordSeason = if (isNewRecord) r.currentSeason else (existing?.lapRecordSeason ?: r.currentSeason),
                lastWinner = winnerPair?.first,
                lastWinnerTeam = winnerPair?.second,
                winsByDriver = winsByDriver,
                winsByTeam = winsByTeam
            )
        }
        val newRecords = if (r.circuitRecords.any { it.circuitId == circuit.id })
            r.circuitRecords.map { if (it.circuitId == circuit.id) newRecord else it }
        else r.circuitRecords + newRecord

        return state.copy(
            company = company,
            notifications = notifications,
            racing = r.copy(
                drivers = updatedDrivers,
                teams = newTeams,
                nextRaceIndex = r.nextRaceIndex + 1,
                racesThisSeason = r.racesThisSeason + 1,
                resultsHistory = (r.resultsHistory + result).takeLast(40),
                activeSponsorships = newActiveSponsorships,
                circuitRecords = newRecords
            )
        )
    }

    private fun computePerformance(
        driver: RaceDriver, team: RacingTeam, circuit: RaceCircuit,
        weather: String, rng: Random, staffBonusMult: Double = 1.0
    ): Double {
        // Score base: 60% piloto, 35% coche
        val pilotScore = driver.skill * 0.5 + driver.consistency * 0.3 + driver.aggression * 0.2
        val carScore = (team.car.engine + team.car.aero + team.car.tyres + team.car.reliability) / 4.0
        val downforceMatch = (5 - kotlin.math.abs(circuit.downforceLevel - 3)) * 2.0
        val moraleBonus = (driver.morale - 50) * 0.10

        // === SPECIALTIES DEL PILOTO según el circuito ===
        // Circuitos urbanos (Mónaco, Singapur)
        val streetCircuits = setOf("monaco", "singapore")
        // Circuitos de alta velocidad (Monza, Spa, COTA)
        val highSpeedCircuits = setOf("monza", "spa", "austin", "bahrain")
        val streetBonus = if (circuit.id in streetCircuits) (driver.streetSkill - 60) * 0.15 else 0.0
        val highSpeedBonus = if (circuit.id in highSpeedCircuits) (driver.highSpeedSkill - 60) * 0.15 else 0.0
        val rainBonus = if (weather == "RAIN" || weather == "STORM") (driver.rainSkill - 60) * 0.25 else 0.0
        val tyreBonus = (driver.tyreManagement - 60) * 0.05

        val luck = rng.nextDouble(-10.0, 10.0) * (circuit.difficulty / 5.0)

        return (pilotScore * 0.60 + carScore * 0.35 + downforceMatch + moraleBonus
            + streetBonus + highSpeedBonus + rainBonus + tyreBonus + luck) * staffBonusMult
    }

    private fun endOfSeason(state: GameState, currentDay: Int, rng: Random): GameState {
        val r = state.racing
        // Campeón de pilotos
        val driverChamp = r.drivers.maxByOrNull { it.seasonPoints }
        // Campeón de constructores
        val teamChamp = r.teams.maxByOrNull { it.seasonPoints }
        val notifs = mutableListOf<GameNotification>()
        if (driverChamp != null) {
            notifs += GameNotification(
                id = System.nanoTime(),
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.SUCCESS,
                title = "👑 Campeón temporada ${r.currentSeason}",
                message = "${driverChamp.flag} ${driverChamp.name} con ${driverChamp.seasonPoints} puntos."
            )
        }
        // Premio extra al equipo del jugador si fue campeón
        var company = state.company
        if (teamChamp != null && teamChamp.id == r.ownedTeamId) {
            val bonus = 12_000_000.0
            company = company.copy(cash = company.cash + bonus).addXp(5_000)
            notifs += GameNotification(
                id = System.nanoTime() + 1,
                timestamp = System.currentTimeMillis(),
                kind = NotificationKind.SUCCESS,
                title = "🏆 ¡CAMPEONES DE CONSTRUCTORES!",
                message = "${teamChamp.name} arrasa la temporada. Bonus: ${"%,.0f".format(bonus)} €."
            )
        }

        // === HALL OF FAME ENTRY ===
        val hofEntry = if (driverChamp != null && teamChamp != null) {
            ChampionEntry(
                season = r.currentSeason,
                driverChampionId = driverChamp.id,
                driverChampionPoints = driverChamp.seasonPoints,
                constructorChampionId = teamChamp.id,
                constructorChampionPoints = teamChamp.seasonPoints,
                playerOwnedTeamId = r.ownedTeamId
            )
        } else null

        // Reset de puntos + championship al piloto + nueva temporada
        val resetDrivers = r.drivers.map { d ->
            val wonChamp = if (d.id == driverChamp?.id) 1 else 0
            d.copy(seasonPoints = 0, championships = d.championships + wonChamp)
        }
        val resetTeams = r.teams.map { team ->
            val won = if (team.id == teamChamp?.id) 1 else 0
            team.copy(seasonPoints = 0, championshipsWon = team.championshipsWon + won)
        }
        val newCalendar = generateSeasonCalendar(currentDay + 14, rng)

        return state.copy(
            company = company,
            notifications = (state.notifications + notifs).takeLast(40),
            racing = r.copy(
                currentSeason = r.currentSeason + 1,
                racesThisSeason = 0,
                drivers = resetDrivers,
                teams = resetTeams,
                calendar = newCalendar,
                nextRaceIndex = 0,
                hallOfFame = if (hofEntry != null) r.hallOfFame + hofEntry else r.hallOfFame
            )
        )
    }

    // =====================================================================
    //                      Helpers
    // =====================================================================

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
