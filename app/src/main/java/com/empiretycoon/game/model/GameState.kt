package com.empiretycoon.game.model

import kotlinx.serialization.Serializable

/**
 * Estado completo serializable del juego. Un único árbol para toda la
 * aplicación (estilo Redux). El motor produce `GameState -> GameState`.
 *
 * `tick` avanza 1 por cada segundo simulado (1 tick ≈ 1 segundo real).
 * 60 ticks = 1 minuto de juego
 * 3.600 ticks = 1 hora (día in-game = 24 min. = 1.440 ticks)
 */
@Serializable
data class GameState(
    val version: Int = 2,
    val tick: Long = 0,
    val lastRealTimeMs: Long = 0,

    val player: Player = Player(),
    val company: Company = Company(),
    val market: Market = Market.fresh(),
    val research: ResearchState = ResearchState(),
    val realEstate: RealEstatePortfolio = RealEstatePortfolio(),

    val stocks: List<Stock> = StockCatalog.starter(),
    val holdings: StockHoldings = StockHoldings(),

    val candidates: List<Employee> = emptyList(),

    val activeEventId: String? = null,
    val eventsSeenToday: Int = 0,

    val quests: List<Quest> = QuestCatalog.all,

    val notifications: List<GameNotification> = emptyList(),
    val rngSeed: Long = System.currentTimeMillis(),
    val paused: Boolean = false,
    val speedMultiplier: Int = 1,

    val loanPrincipal: Double = 0.0,
    val loanInterestRate: Double = 0.0,

    val daySeconds: Double = 0.0,

    // ===== Sistemas extendidos (10 agentes) =====

    /** Logros + estado de prestigio (Agente 1). */
    val achievements: AchievementsState = AchievementsState(),
    val prestige: PrestigeState = PrestigeState(),

    /** Macroeconomía: ciclos, noticias, contratos B2B (Agente 2). */
    val economy: EconomicState = EconomicState(),
    val news: NewsFeed = NewsFeed(),
    val contracts: ContractsState = ContractsState(),

    /** RPG: árbol de habilidades, perks, rivales (Agente 3). */
    val skillTree: SkillTreeState = SkillTreeState(),
    val perks: PerksState = PerksState(),
    val rivals: RivalsState = RivalRoster.freshState(),

    /** Banca, IPO y opciones (Agente 4). */
    val loans: LoansState = LoansState(),
    val ipo: IPOState = IPOState(),
    val options: OptionsBook = OptionsBook(),

    /** RRHH: roles, formación, ejecutivos (Agente 5). */
    val hrState: HrState = HrState(),

    /** Audio settings (Agente 7). */
    val audio: AudioSettings = AudioSettings(),

    /** Tutorial state (Agente 8). */
    val tutorial: TutorialState = TutorialState(),

    /** Producción avanzada: calidad, líneas, políticas (Agente 9). */
    val qualityInventory: QualityInventory = QualityInventory.Empty,
    val productionLines: ProductionLinesState = ProductionLinesState.Empty,
    val productionPolicies: ProductionPolicies = ProductionPolicies.Empty,

    /** Narrativa: historia, side quests, NPCs (Agente 6). */
    val storyline: StorylineState = StorylineState(),
    val sideQuests: SideQuestsState = SideQuestsState(),
    val npcs: NPCsState = NPCsState(),

    /** Gerentes que automatizan el juego (anti-grind). */
    val managers: ManagersState = ManagersState(),

    /** Mundo 2D explorable (BalWealth). */
    val world: com.empiretycoon.game.world.WorldState =
        com.empiretycoon.game.world.WorldState.fresh(),

    /** NPCs vivos en la ciudad. */
    val npcWorld: com.empiretycoon.game.world.NpcWorldState =
        com.empiretycoon.game.world.NpcWorldState(),

    /** Visual karma echo (saturación, basura, flores). */
    val karmaEcho: com.empiretycoon.game.world.KarmaEchoState =
        com.empiretycoon.game.world.KarmaEchoState(),

    /** Mecánica única: BalWealth Index. */
    val balWealth: BalWealthState = BalWealthState(),

    /** Clima dinámico de la ciudad (sol, lluvia, niebla...). */
    val weather: com.empiretycoon.game.world.WeatherState = com.empiretycoon.game.world.WeatherState(),

    /** Tráfico (coches caminando por la ciudad). */
    val traffic: com.empiretycoon.game.world.TrafficState = com.empiretycoon.game.world.TrafficState(),

    /** Eventos al pisar tile (encuentros estilo Pokemon). */
    val worldEvent: com.empiretycoon.game.world.WorldEventState = com.empiretycoon.game.world.WorldEventState(),

    /** Mobiliario urbano estático (árboles, faroles, bancos...). */
    val cityProps: com.empiretycoon.game.world.CityPropsState = com.empiretycoon.game.world.CityPropsState(),

    /** Garaje del jugador con todos los coches y el actualmente conducido. */
    val garage: GarageState = GarageState(),

    /** Mascotas (perro/gato/...) que pueden seguir al avatar. */
    val pets: com.empiretycoon.game.world.PetsState = com.empiretycoon.game.world.PetsState(),

    /** NPC seguidor con UNA pregunta + timeout 35s (anti-bucle). */
    val follower: com.empiretycoon.game.world.FollowerState = com.empiretycoon.game.world.FollowerState(),

    /** Avistamientos OVNI raros. */
    val ufo: com.empiretycoon.game.world.UfoState = com.empiretycoon.game.world.UfoState(),

    /** Familia (pareja + hijos). */
    val family: com.empiretycoon.game.world.FamilyState = com.empiretycoon.game.world.FamilyState(),

    /** Casa decorable. */
    val house: com.empiretycoon.game.world.HouseState = com.empiretycoon.game.world.HouseState(),

    /** Formula Manager: equipos, pilotos, calendario, championship. */
    val racing: RacingState = RacingState()
) {
    val day: Int get() = (tick / 1_440).toInt() + 1
    val hourOfDay: Int get() = ((tick % 1_440) / 60).toInt()
    fun inventoryOf(resourceId: String): Int = company.inventory[resourceId] ?: 0
}
