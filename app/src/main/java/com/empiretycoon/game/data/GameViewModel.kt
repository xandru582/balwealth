package com.empiretycoon.game.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empiretycoon.game.engine.*
import com.empiretycoon.game.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SaveRepository(app)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var gameLoop: Job? = null
    private var saveLoop: Job? = null

    // FIX BUG-17-02: Mutex serializa todas las escrituras a SaveRepository
    // para evitar guardar estado intermedio si mutate() y saveLoop pisan
    // el mismo write-cycle.
    private val saveMutex = Mutex()

    private suspend fun safeSave(snapshot: GameState) {
        saveMutex.withLock {
            withContext(Dispatchers.IO) {
                repo.save(snapshot)
            }
        }
    }

    init {
        viewModelScope.launch {
            val loaded = repo.load()
            if (loaded != null) {
                val now = System.currentTimeMillis()
                val elapsedSec = ((now - loaded.lastRealTimeMs) / 1000L)
                    .coerceIn(0, 60 * 60 * 8)
                val catched = if (elapsedSec > 0) GameEngine.advanceSeconds(loaded, elapsedSec) else loaded
                _state.value = catched.copy(lastRealTimeMs = now)
            }
            _state.value = GameEngine.refreshCandidates(_state.value)
            _state.value = GameEngine.refreshRealEstate(_state.value)
            // Inicialización de subsistemas extendidos
            _state.value = RivalEngine.ensureInitialized(_state.value)
            _state.value = HrEngine.ensureProfilesForLegacyEmployees(_state.value)
            _state.value = HrEngine.cleanupOrphanProfiles(_state.value)
            _state.value = HrEngine.refreshApplicants(
                _state.value, Random(_state.value.rngSeed xor _state.value.tick)
            )
            _state.value = BankingEngine.refreshOffers(
                _state.value, Random(_state.value.rngSeed xor _state.value.tick)
            )
            _state.value = SideQuestEngine.refreshAvailable(
                _state.value, Random(_state.value.rngSeed xor _state.value.tick)
            )
            _state.value = ManagerEngine.refreshPool(
                _state.value, Random(_state.value.rngSeed xor _state.value.tick)
            )
            // Mobiliario urbano: solo si no se ha generado aún
            if (_state.value.cityProps.props.isEmpty()) {
                _state.value = _state.value.copy(
                    cityProps = com.empiretycoon.game.world.CityPropsGenerator.generate(_state.value.world.grid)
                )
            }
            _loading.value = false
            startLoops()
        }
    }

    private fun startLoops() {
        gameLoop = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val s = _state.value
                if (!s.paused) {
                    _state.value = GameEngine.advanceSeconds(s, s.speedMultiplier.toLong())
                }
            }
        }
        saveLoop = viewModelScope.launch {
            while (true) {
                delay(10_000L)
                safeSave(_state.value.copy(lastRealTimeMs = System.currentTimeMillis()))
            }
        }
    }

    override fun onCleared() {
        // FIX BUG-17-03/04: cancela los loops y espera bloqueante a que el
        // último save persista. Sin esto, el ViewModel se destruye antes de
        // terminar y se pierde el progreso reciente.
        gameLoop?.cancel()
        saveLoop?.cancel()
        runBlocking(NonCancellable) {
            safeSave(_state.value.copy(lastRealTimeMs = System.currentTimeMillis()))
        }
        super.onCleared()
    }

    // ---------- Bridge ----------

    private fun mutate(block: (GameState) -> GameState) {
        val prev = _state.value
        val next = block(prev)
        // Avance de tutorial tras cada acción del jugador
        _state.value = TutorialEngine.checkAdvance(prev, next)
    }

    // ---- Núcleo (básicos) ----
    fun build(type: BuildingType) = mutate { GameEngine.buildNew(it, type) }
    fun upgrade(buildingId: String) = mutate { GameEngine.upgradeBuilding(it, buildingId) }
    fun demolish(buildingId: String) = mutate { GameEngine.demolish(it, buildingId) }
    fun setRecipe(buildingId: String, recipeId: String?) = mutate { GameEngine.setRecipe(it, buildingId, recipeId) }
    fun toggleAutoRestart(buildingId: String) = mutate { GameEngine.toggleAutoRestart(it, buildingId) }
    fun addWorker(buildingId: String) = mutate { GameEngine.assignWorkersDelta(it, buildingId, +1) }
    fun removeWorker(buildingId: String) = mutate { GameEngine.assignWorkersDelta(it, buildingId, -1) }

    fun buy(resId: String, qty: Int) = mutate { GameEngine.marketBuy(it, resId, qty) }
    fun sell(resId: String, qty: Int) = mutate { GameEngine.marketSell(it, resId, qty) }

    fun startResearch(techId: String) = mutate { GameEngine.startResearch(it, techId) }

    fun buyShares(t: String, q: Int) = mutate { GameEngine.buyShares(it, t, q) }
    fun sellShares(t: String, q: Int) = mutate { GameEngine.sellShares(it, t, q) }

    fun refreshCandidates() = mutate { GameEngine.refreshCandidates(it) }
    fun hire(candId: String) = mutate { GameEngine.hire(it, candId) }
    fun fire(empId: String) = mutate { HrEngine.cleanupOrphanProfiles(GameEngine.fire(it, empId)) }

    fun refreshRealEstate() = mutate { GameEngine.refreshRealEstate(it) }
    fun buyProperty(id: String) = mutate { GameEngine.buyProperty(it, id) }
    fun sellProperty(id: String) = mutate { GameEngine.sellProperty(it, id) }

    fun resolveEvent(choice: Int) = mutate { GameEngine.resolveEvent(it, choice) }

    fun train(stat: String) = mutate { GameEngine.train(it, stat) }
    fun rest() = mutate { GameEngine.rest(it) }
    fun work() = mutate { GameEngine.work(it) }
    fun personalToCompany(amount: Double) = mutate { GameEngine.setPersonalToCompany(it, amount) }
    fun companyToPersonal(amount: Double) = mutate { GameEngine.withdrawToPersonal(it, amount) }

    fun rename(player: String?, company: String?) = mutate { GameEngine.rename(it, player, company) }
    fun togglePause() = mutate { GameEngine.togglePause(it) }
    fun setSpeed(s: Int) = mutate { GameEngine.setSpeed(it, s) }
    fun claim(questId: String) = mutate { GameEngine.claimQuest(it, questId) }

    // ---- Logros & Prestigio (Agente 1) ----
    fun claimAchievement(id: String) = mutate {
        val ach = AchievementCatalog.byId(id) ?: return@mutate it
        if (!it.achievements.isUnlocked(id) || it.achievements.isClaimed(id)) return@mutate it
        val claimed = it.achievements.copy(
            claimedAchievements = it.achievements.claimedAchievements + id
        )
        val company = it.company.copy(cash = it.company.cash + ach.rewardCash)
        val player = it.player.addXp(ach.rewardXp)
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.SUCCESS,
            title = "Recompensa cobrada",
            message = "${ach.emoji} ${ach.title}"
        )
        it.copy(
            achievements = claimed,
            company = company,
            player = player,
            notifications = (it.notifications + notif).takeLast(40)
        )
    }
    fun rebirth() = mutate { PrestigeEngine.rebirth(it) }
    fun buyPrestigePerk(id: String) = mutate { PrestigeEngine.buyPerk(it, id) }

    // ---- Macroeconomía & Contratos (Agente 2) ----
    fun acceptContract(id: String) = mutate { ContractsEngine.acceptContract(it, id) }
    fun rejectContract(id: String) = mutate { ContractsEngine.rejectContract(it, id) }
    fun deliverContract(id: String, resId: String, qty: Int) =
        mutate { ContractsEngine.deliverContract(it, id, resId, qty) }
    fun refreshContracts() = mutate {
        ContractsEngine.forceRefresh(it, Random(it.rngSeed xor it.tick))
    }
    fun markNewsRead() = mutate { it.copy(news = it.news.markAllRead()) }

    // ---- Skills, Perks, Rivales (Agente 3) ----
    fun unlockSkill(id: String) = mutate { SkillEngine.unlockSkill(it, id) }
    fun pickPerk(id: String) = mutate { PerkEngine.selectPerk(it, id) }
    fun dismissPerkChoice() = mutate { PerkEngine.discardPending(it) }
    fun requestTrashTalk() = mutate {
        RivalEngine.pushTrashTalk(it, Random(it.rngSeed xor it.tick))
    }
    fun dismissTrashTalk() = mutate { RivalEngine.clearTrashTalk(it) }

    // ---- Banca, IPO, Opciones (Agente 4) ----
    fun takeLoan(offerId: String) = mutate { BankingEngine.takeLoan(it, offerId) }
    fun repayLoan(loanId: String, amount: Double) = mutate { BankingEngine.repayLoan(it, loanId, amount) }
    fun refreshLoanOffers() = mutate {
        BankingEngine.refreshOffers(it, Random(it.rngSeed xor it.tick))
    }
    fun fileProspectus() = mutate { IpoEngine.fileProspectus(it) }
    fun completeRoadshow() = mutate { IpoEngine.completeRoadshow(it) }
    fun listOnExchange() = mutate { IpoEngine.listOnExchange(it) }
    fun sellDownStake(shares: Long) = mutate { IpoEngine.sellDownStake(it, shares) }
    fun buyCallOption(ticker: String, strike: Double, expiryTick: Long) =
        mutate { OptionsEngine.buyCall(it, ticker, strike, expiryTick) }
    fun buyPutOption(ticker: String, strike: Double, expiryTick: Long) =
        mutate { OptionsEngine.buyPut(it, ticker, strike, expiryTick) }
    fun exerciseOption(id: String) = mutate { OptionsEngine.exerciseOption(it, id) }

    // ---- RRHH (Agente 5) ----
    fun refreshApplicants() = mutate {
        HrEngine.refreshApplicants(it, Random(it.rngSeed xor it.tick))
    }
    fun hireApplicant(id: String) = mutate { HrEngine.hireApplicant(it, id) }
    fun startTraining(progId: String, empIds: List<String>) =
        mutate { HrEngine.startTraining(it, progId, empIds) }
    fun promoteEmployee(id: String) = mutate { HrEngine.promote(it, id) }
    fun assignToExec(slot: String, empId: String?) = mutate { HrEngine.assignToExec(it, slot, empId) }

    // ---- Narrativa (Agente 6) ----
    fun resolveStoryChoice(idx: Int) = mutate { StorylineEngine.resolveStoryChoice(it, idx) }
    fun acknowledgeEnding(endingType: String) = mutate {
        it.copy(storyline = it.storyline.copy(achievedEndingType = endingType))
    }
    fun refreshSideQuests() = mutate {
        SideQuestEngine.refreshAvailable(it, Random(it.rngSeed xor it.tick))
    }
    fun acceptSideQuest(id: String) = mutate { SideQuestEngine.acceptSideQuest(it, id) }
    fun abandonSideQuest(id: String) = mutate { SideQuestEngine.abandonSideQuest(it, id) }
    fun claimSideQuestReward(id: String) = mutate { SideQuestEngine.claimReward(it, id) }
    fun chatWithNpc(npcId: String) = mutate { NPCEngine.improveRelationship(it, npcId, +1) }
    fun giftNpc(npcId: String, kind: String, cost: Double) =
        mutate { NPCEngine.gift(it, npcId, kind, cost) }

    // ---- Audio (Agente 7) ----
    fun setSoundEnabled(enabled: Boolean) = mutate { it.copy(audio = it.audio.copy(soundEnabled = enabled)) }
    fun setHapticsEnabled(enabled: Boolean) = mutate { it.copy(audio = it.audio.copy(hapticsEnabled = enabled)) }
    fun setMasterVolume(v: Float) = mutate { it.copy(audio = it.audio.copy(masterVolume = v.coerceIn(0f, 1f))) }
    fun setMusicEnabled(enabled: Boolean) = mutate { it.copy(audio = it.audio.copy(musicEnabled = enabled)) }

    // ---- Tutorial (Agente 8) ----
    fun tutorialAdvance(fromStep: TutorialStep? = null) =
        mutate { TutorialEngine.advanceManually(it, fromStep) }
    fun tutorialSkip() = mutate { TutorialEngine.skip(it) }
    fun tutorialRestart() = mutate { TutorialEngine.restart(it) }
    fun tutorialDismiss() = mutate { TutorialEngine.bumpDismiss(it) }

    // ---- Producción avanzada (Agente 9) ----
    fun createLine(presetId: String, buildingIds: List<String>, name: String? = null,
                   balancingMode: BalancingMode? = null) = mutate {
        val preset = LinePresetCatalog.byId(presetId) ?: return@mutate it
        val withLine = ProductionLinesEngine.createLine(it, preset, buildingIds, name)
        if (balancingMode == null) withLine
        else {
            val newest = withLine.productionLines.lines.lastOrNull() ?: return@mutate withLine
            val updated = newest.copy(balancingModeName = balancingMode.name)
            withLine.copy(productionLines = withLine.productionLines.upsert(updated))
        }
    }
    fun toggleLine(id: String) = mutate { ProductionLinesEngine.toggleLine(it, id) }
    fun deleteLine(id: String) = mutate { ProductionLinesEngine.deleteLine(it, id) }
    fun setProductionPolicy(buildingId: String, policy: ProductionPolicy) = mutate {
        it.copy(productionPolicies = it.productionPolicies.set(buildingId, policy))
    }
    fun sellQuality(resId: String, tier: QualityTier, qty: Int) = mutate { st ->
        if (qty <= 0) return@mutate st
        val (newQInv, taken) = st.qualityInventory.takeTier(resId, tier, qty)
        if (taken <= 0) return@mutate st
        val rawPrice = st.market.sellPriceOf(resId)
        val mktBonus = 1.0 +
            st.research.completed.sumOf { id -> TechCatalog.byId(id)?.marketBonus ?: 0.0 } +
            st.player.stats.charisma.coerceAtMost(100) * 0.003
        val total = rawPrice * mktBonus * tier.mult * taken
        val company = st.company.copy(cash = st.company.cash + total).addXp((total / 18).toLong())
        st.copy(
            qualityInventory = newQInv,
            company = company,
            market = Economy.applySale(st.market, resId, taken)
        )
    }
    fun setRecipeAdvanced(buildingId: String, recipeId: String?) = mutate { st ->
        if (recipeId != null && AdvancedRecipeCatalog.byId(recipeId) == null) return@mutate st
        val bs = st.company.buildings.map {
            if (it.id == buildingId) it.copy(currentRecipeId = recipeId, progressSeconds = 0.0) else it
        }
        st.copy(company = st.company.copy(buildings = bs))
    }

    // ---- Gerentes (automatización) ----
    fun refreshManagerPool() = mutate {
        ManagerEngine.refreshPool(it, Random(it.rngSeed xor it.tick))
    }
    fun hireManager(id: String) = mutate { ManagerEngine.hire(it, id) }
    fun fireManager(id: String) = mutate { ManagerEngine.fire(it, id) }
    fun toggleManagerEnabled(id: String) = mutate { ManagerEngine.toggleEnabled(it, id) }
    fun upgradeManager(id: String) = mutate { ManagerEngine.upgradeManager(it, id) }
    fun updateManagerConfig(id: String, config: ManagerConfig) =
        mutate { ManagerEngine.updateConfig(it, id, config) }

    // ---- Mundo 2D / BalWealth ----
    fun applyWorldMove(dx: Float, dy: Float, deltaSec: Float) {
        val s = _state.value
        // Si conduce, multiplica la velocidad por el topSpeed del coche
        val speed = 4.5f * com.empiretycoon.game.engine.DrivingEngine.speedMultiplier(s)
        val w = com.empiretycoon.game.world.MovementEngine.applyMovement(
            s.world, com.empiretycoon.game.world.MoveInput(dx, dy), deltaSec, speed
        )
        if (w !== s.world) _state.value = s.copy(world = w)
    }
    fun updateAvatarLook(look: com.empiretycoon.game.world.AvatarLook) = mutate {
        it.copy(world = it.world.copy(avatar = it.world.avatar.copy(look = look)))
    }
    fun ensureWorldPopulated() = mutate { st ->
        val populated = com.empiretycoon.game.world.NpcWorldEngine.ensurePopulated(
            st.npcWorld, st.world.grid, target = 18
        )
        st.copy(npcWorld = populated)
    }

    /** Resuelve la elección del jugador en un evento del mundo (Pokemon-style). */
    fun resolveWorldEvent(choiceIndex: Int) = mutate { st ->
        val evId = st.worldEvent.activeEventId ?: return@mutate st
        val ev = com.empiretycoon.game.world.MoreEventsCatalog.merged.find { it.id == evId } ?: return@mutate st
        val choice = ev.choices.getOrNull(choiceIndex) ?: return@mutate st
        // Aplicar efectos
        var company = st.company.copy(
            cash = st.company.cash + choice.cashDelta,
            reputation = (st.company.reputation + choice.reputationDelta).coerceIn(0, 100)
        )
        var player = st.player.addXp(choice.xpDelta)
            .withEnergy(choice.energyDelta)
            .withHappiness(choice.happinessDelta)
        val storyline = st.storyline.copy(karma = (st.storyline.karma + choice.karmaDelta).coerceIn(-100, 100))
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.EVENT,
            title = "${ev.emoji} ${ev.title}",
            message = choice.resultMessage.ifBlank { choice.label }
        )
        st.copy(
            company = company,
            player = player,
            storyline = storyline,
            worldEvent = st.worldEvent.copy(
                activeEventId = null,
                seenIds = st.worldEvent.seenIds + ev.id
            ),
            notifications = (st.notifications + notif).takeLast(40)
        )
    }

    fun dismissWorldEvent() = mutate {
        it.copy(worldEvent = it.worldEvent.copy(activeEventId = null))
    }

    // ---- Mascotas ----
    fun buyPet(species: com.empiretycoon.game.world.PetSpecies, name: String) = mutate { st ->
        if (st.company.cash < species.cost) return@mutate st
        val pet = com.empiretycoon.game.world.Pet(
            id = "pet_${System.nanoTime()}",
            species = species.name,
            name = name.ifBlank { species.displayName },
            x = st.world.avatar.x,
            y = st.world.avatar.y + 0.5f
        )
        st.copy(
            company = st.company.copy(cash = st.company.cash - species.cost),
            pets = st.pets.copy(
                owned = st.pets.owned + pet,
                activePetId = pet.id
            ),
            player = st.player.withHappiness(species.happinessPerDay * 3)
        )
    }
    fun setActivePet(petId: String?) = mutate {
        it.copy(pets = it.pets.copy(activePetId = petId))
    }
    fun feedActivePet() = mutate { st ->
        if (st.company.cash < 5) return@mutate st
        val updatedPets = com.empiretycoon.game.world.PetEngine.feedActive(st.pets, st.tick)
        st.copy(pets = updatedPets, company = st.company.copy(cash = st.company.cash - 5))
    }

    // ---- Follower NPC ----
    fun resolveFollower(choice: Int) = mutate { st ->
        val (newFollower, cashDelta, hkn) = com.empiretycoon.game.world.FollowerEngine.resolve(
            st.follower, choice, st.tick
        )
        val (happy, karma, name) = hkn
        val notif = GameNotification(
            id = System.nanoTime(),
            timestamp = System.currentTimeMillis(),
            kind = NotificationKind.EVENT,
            title = "👥 $name",
            message = "Conversación resuelta."
        )
        st.copy(
            follower = newFollower,
            company = st.company.copy(cash = st.company.cash + cashDelta),
            player = st.player.withHappiness(happy),
            storyline = st.storyline.copy(karma = (st.storyline.karma + karma).coerceIn(-100, 100)),
            notifications = (st.notifications + notif).takeLast(40)
        )
    }
    fun dismissFollower() = mutate {
        it.copy(follower = it.follower.copy(current = null, cooldownUntilTick = it.tick + 240L))
    }

    // ---- Familia / Casa ----
    fun proposeMarriage(npcName: String, portraitSeed: Long = System.nanoTime()) = mutate { st ->
        if (st.family.spouse != null) return@mutate st
        if (st.company.cash < 5_000.0) return@mutate st
        val sp = com.empiretycoon.game.world.Spouse(
            name = npcName, portraitSeed = portraitSeed, daysWith = 0
        )
        st.copy(
            family = st.family.copy(spouse = sp, proposedAt = st.tick),
            company = st.company.copy(cash = st.company.cash - 5_000.0),
            player = st.player.withHappiness(20)
        )
    }
    fun haveChild(name: String) = mutate { st ->
        if (st.family.spouse == null) return@mutate st
        if (st.family.children.size >= 4) return@mutate st
        val child = com.empiretycoon.game.world.Child(
            id = "child_${System.nanoTime()}", name = name, portraitSeed = System.nanoTime()
        )
        st.copy(
            family = st.family.copy(children = st.family.children + child),
            player = st.player.withHappiness(15)
        )
    }
    fun placeFurniture(kind: com.empiretycoon.game.world.FurnitureKind, x: Int, y: Int) = mutate { st ->
        if (st.company.cash < kind.price) return@mutate st
        // FIX BUG-03-HOU-01/02: chequea TODA el área, no solo el origen
        if (!st.house.areaFree(kind, x, y)) return@mutate st
        val placed = com.empiretycoon.game.world.PlacedFurniture(
            id = "f_${System.nanoTime()}", kind = kind.name, x = x, y = y
        )
        st.copy(
            company = st.company.copy(cash = st.company.cash - kind.price),
            house = st.house.copy(furniture = st.house.furniture + placed)
        )
    }
    fun removeFurniture(id: String) = mutate { st ->
        val item = st.house.furniture.find { it.id == id } ?: return@mutate st
        val refund = item.spec().price * 0.5
        st.copy(
            company = st.company.copy(cash = st.company.cash + refund),
            house = st.house.copy(furniture = st.house.furniture - item)
        )
    }

    // ---- Coches / Conducción ----
    fun buyCar(modelId: String, customColor: Long? = null) =
        mutate { DrivingEngine.buyCar(it, modelId, customColor) }
    fun sellCar(instanceId: String) = mutate { DrivingEngine.sellCar(it, instanceId) }
    fun toggleDriving(instanceId: String? = null) =
        mutate { DrivingEngine.toggleDriving(it, instanceId) }
    fun expandGarage() = mutate { DrivingEngine.expandGarage(it) }
    fun repaintCar(instanceId: String, color: Long) =
        mutate { DrivingEngine.repaintCar(it, instanceId, color) }

    /** Casino: aplica el resultado de la apuesta (puede ser positivo o negativo).
     *  Si se gana mucho, el karma baja un poco (la fortuna fácil tiene precio). */
    fun casinoWin(delta: Double) = mutate { st ->
        val company = st.company.copy(cash = st.company.cash + delta)
        val karmaDrop = if (delta > 1000) -2 else 0
        st.copy(
            company = company,
            storyline = st.storyline.copy(karma = (st.storyline.karma + karmaDrop).coerceIn(-100, 100))
        )
    }

    // ---- Formula Manager ----
    /** Inicializa o desbloquea el sistema de carreras (idempotente). */
    fun racingInit() = mutate { st ->
        RacingEngine.ensureInitialized(st, st.day)
    }

    fun racingBuyTeam(teamId: String) = mutate { RacingEngine.buyTeam(it, teamId) }
    fun racingSellTeam() = mutate { RacingEngine.sellTeam(it) }
    fun racingUpgrade(part: RacingEngine.CarPart) = mutate { RacingEngine.upgradeCarPart(it, part) }
    fun racingSignDriver(driverId: String, slot: Int) =
        mutate { RacingEngine.signDriver(it, driverId, slot) }
    fun racingFireDriver(slot: Int) = mutate { RacingEngine.fireDriver(it, slot) }
    fun racingSignSponsor(sponsorId: String) = mutate { RacingEngine.signSponsor(it, sponsorId) }
    fun racingCancelSponsor(sponsorId: String) = mutate { RacingEngine.cancelSponsor(it, sponsorId) }
    fun racingHireStaff(staffId: String) = mutate { RacingEngine.hireStaff(it, staffId) }
    fun racingFireStaff(staffId: String) = mutate { RacingEngine.fireStaff(it, staffId) }

    fun resetAll() = viewModelScope.launch {
        repo.clear()
        _state.value = GameState()
        _state.value = GameEngine.refreshCandidates(_state.value)
        _state.value = GameEngine.refreshRealEstate(_state.value)
        _state.value = RivalEngine.ensureInitialized(_state.value)
    }

    // ===================== v17 — Cripto =====================
    fun cryptoUnlock() = mutate { CryptoEngine.unlock(it) }
    fun cryptoBuy(symbol: String, qty: Double) = mutate { CryptoEngine.buy(it, symbol, qty) }
    fun cryptoSell(symbol: String, qty: Double) = mutate { CryptoEngine.sell(it, symbol, qty) }
    fun cryptoStake(symbol: String, qty: Double, days: Int) =
        mutate { CryptoEngine.stake(it, symbol, qty, days) }
    fun cryptoUnstake(symbol: String) = mutate { CryptoEngine.unstake(it, symbol) }
    fun cryptoAssignMiners(symbol: String, delta: Int) =
        mutate { CryptoEngine.assignMiners(it, symbol, delta) }
    fun cryptoClaimMining(symbol: String) = mutate { CryptoEngine.claimMining(it, symbol) }

    // ===================== v17 — Desastres =====================
    fun disasterToggleInsurance(on: Boolean) =
        mutate { DisasterEngine.toggleInsurance(it, on) }
    fun disasterMitigate(disasterId: String, strategy: MitigationStrategy) =
        mutate { DisasterEngine.mitigate(it, disasterId, strategy) }

    // ===================== v17 — Retos diarios =====================
    fun claimChallenge(challengeId: String) =
        mutate { DailyChallengeEngine.claim(it, challengeId) }
    fun markCasinoVisited() = mutate { DailyChallengeEngine.markCasinoVisited(it) }

    // ===================== v17 — Heists =====================
    fun heistsUnlock() = mutate { HeistEngine.unlock(it) }
    fun heistRecruit(crewId: String) = mutate { HeistEngine.recruit(it, crewId) }
    fun heistFireCrew(crewId: String) = mutate { HeistEngine.fireCrew(it, crewId) }
    fun heistPlan(
        heistInstanceId: String,
        crewIds: List<String>,
        approach: HeistApproach,
        gearSpend: Double
    ) = mutate {
        HeistEngine.planHeist(it, heistInstanceId, crewIds, approach, gearSpend)
    }
    fun heistExecute(heistInstanceId: String) =
        mutate { HeistEngine.execute(it, heistInstanceId) }
}
