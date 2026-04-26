package com.empiretycoon.game.model

/**
 * Pool extendido de logros: 80+ adicionales temáticos. Sumados al catálogo
 * principal mediante [MoreAchievements.merged].
 */
object MoreAchievements {

    val extra: List<Achievement> = listOf(
        // ----- VEHICLES -----
        Achievement("ach_first_car", "Carnet en mano", "Compra tu primer coche", "🚗",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 100, rewardCash = 200.0),
        Achievement("ach_5_cars", "Coleccionista", "Posee 5 coches", "🏆",
            AchievementCategory.WEALTH, threshold = 5, rewardXp = 500, rewardCash = 5_000.0),
        Achievement("ach_10_cars", "Garaje completo", "Posee 10 coches", "🏎️",
            AchievementCategory.WEALTH, threshold = 10, rewardXp = 1_000, rewardCash = 20_000.0),
        Achievement("ach_supercar", "Velocidad pura", "Compra un Saetta", "⚡",
            AchievementCategory.WEALTH, threshold = 1, rewardXp = 800),
        Achievement("ach_classic", "Coleccionista clásico", "Posee un Heritage Classic", "🕰️",
            AchievementCategory.WEALTH, threshold = 1, rewardXp = 400),
        Achievement("ach_electric", "Eco warrior", "Posee un coche eléctrico", "♻️",
            AchievementCategory.CHARACTER, threshold = 1, rewardXp = 300),
        Achievement("ach_limo", "Magnate", "Compra una limusina", "🚙",
            AchievementCategory.WEALTH, threshold = 1, rewardXp = 600),
        Achievement("ach_drive_100km", "Centurión", "Conduce 100 km", "🛣️",
            AchievementCategory.MILESTONE, threshold = 100, rewardXp = 200),
        Achievement("ach_drive_1000km", "Trotamundos", "Conduce 1.000 km", "🌍",
            AchievementCategory.MILESTONE, threshold = 1_000, rewardXp = 1_000),

        // ----- ECONOMY -----
        Achievement("ach_10m", "Diez millones", "Acumula 10M €", "💵",
            AchievementCategory.WEALTH, threshold = 10_000_000, rewardXp = 5_000),
        Achievement("ach_100m", "Cien millones", "Acumula 100M €", "💸",
            AchievementCategory.WEALTH, threshold = 100_000_000, rewardXp = 25_000),
        Achievement("ach_1b", "Mil millones", "Acumula 1.000M €", "🏦",
            AchievementCategory.WEALTH, threshold = 1_000_000_000, rewardXp = 100_000),
        Achievement("ach_first_loan", "Endeudado", "Pide tu primer préstamo", "🏦",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 100),
        Achievement("ach_pay_loan", "Liquidado", "Salda totalmente un préstamo", "✅",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 300),
        Achievement("ach_ipo", "Públicamente cotizado", "Saca tu empresa a bolsa", "📈",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 5_000),

        // ----- PRODUCTION -----
        Achievement("ach_100_recipes", "Productor", "Completa 100 ciclos de producción", "⚙️",
            AchievementCategory.PRODUCTION, threshold = 100, rewardXp = 200),
        Achievement("ach_1000_recipes", "Maestro productor", "Completa 1.000 ciclos", "🏭",
            AchievementCategory.PRODUCTION, threshold = 1_000, rewardXp = 1_500),
        Achievement("ach_smartphone", "Era digital", "Produce tu primer smartphone", "📱",
            AchievementCategory.PRODUCTION, threshold = 1, rewardXp = 400),
        Achievement("ach_car_made", "Línea automotriz", "Produce tu primer coche", "🚗",
            AchievementCategory.PRODUCTION, threshold = 1, rewardXp = 500),
        Achievement("ach_yacht", "Astillero", "Produce tu primer yate", "⛵",
            AchievementCategory.PRODUCTION, threshold = 1, rewardXp = 1_000),
        Achievement("ach_jewelry_master", "Joyero", "Produce 50 joyas", "💎",
            AchievementCategory.PRODUCTION, threshold = 50, rewardXp = 600),

        // ----- CHARACTER -----
        Achievement("ach_lvl_25", "Veterano", "Llega al nivel 25", "🎖️",
            AchievementCategory.CHARACTER, threshold = 25, rewardXp = 500),
        Achievement("ach_lvl_50", "Experto", "Llega al nivel 50", "🥇",
            AchievementCategory.CHARACTER, threshold = 50, rewardXp = 2_000),
        Achievement("ach_lvl_100", "Leyenda", "Llega al nivel 100", "👑",
            AchievementCategory.CHARACTER, threshold = 100, rewardXp = 10_000),
        Achievement("ach_int_100", "Cerebro", "Inteligencia 100", "🧠",
            AchievementCategory.CHARACTER, threshold = 100, rewardXp = 1_500),
        Achievement("ach_str_100", "Hércules", "Fuerza 100", "💪",
            AchievementCategory.CHARACTER, threshold = 100, rewardXp = 1_500),
        Achievement("ach_cha_100", "Carismático", "Carisma 100", "🗣",
            AchievementCategory.CHARACTER, threshold = 100, rewardXp = 1_500),
        Achievement("ach_luc_100", "Afortunado", "Suerte 100", "🍀",
            AchievementCategory.CHARACTER, threshold = 100, rewardXp = 1_500),
        Achievement("ach_dex_100", "Veloz", "Destreza 100", "⚡",
            AchievementCategory.CHARACTER, threshold = 100, rewardXp = 1_500),
        Achievement("ach_polymath", "Polímata", "TODOS los stats a 50", "🌟",
            AchievementCategory.CHARACTER, threshold = 250, rewardXp = 5_000),

        // ----- MARKET -----
        Achievement("ach_500_tx", "Trader", "Realiza 500 transacciones de mercado", "📊",
            AchievementCategory.MARKET, threshold = 500, rewardXp = 800),
        Achievement("ach_arbitrage", "Arbitrajista", "Compra y vende el mismo recurso con beneficio", "💱",
            AchievementCategory.MARKET, threshold = 1, rewardXp = 200),
        Achievement("ach_short_squeeze", "Short Squeeze", "Saca beneficio en una caída", "📉",
            AchievementCategory.MARKET, threshold = 1, rewardXp = 300),

        // ----- SOCIAL -----
        Achievement("ach_50_employees", "Pequeña empresa", "Ten 50 empleados", "👥",
            AchievementCategory.SOCIAL, threshold = 50, rewardXp = 500),
        Achievement("ach_100_employees", "Mediana empresa", "Ten 100 empleados", "👨‍💼",
            AchievementCategory.SOCIAL, threshold = 100, rewardXp = 1_500),
        Achievement("ach_max_loyalty", "El jefe ideal", "Lealtad media >= 95%", "💖",
            AchievementCategory.SOCIAL, threshold = 95, rewardXp = 2_000),
        Achievement("ach_full_csuite", "C-Suite completo", "Asigna CFO, COO, CTO y CMO", "👔",
            AchievementCategory.SOCIAL, threshold = 4, rewardXp = 1_500),

        // ----- REAL ESTATE -----
        Achievement("ach_luxury_villa", "Casa de lujo", "Compra una villa de lujo", "🏰",
            AchievementCategory.REAL_ESTATE, threshold = 1, rewardXp = 1_000),
        Achievement("ach_skyscraper", "Rascacielos", "Compra un rascacielos", "🏙️",
            AchievementCategory.REAL_ESTATE, threshold = 1, rewardXp = 5_000),
        Achievement("ach_re_empire", "Imperio inmobiliario", "20 propiedades", "🏛️",
            AchievementCategory.REAL_ESTATE, threshold = 20, rewardXp = 3_000),

        // ----- KARMA / NPC -----
        Achievement("ach_saint", "Santo", "Karma >= 80", "👼",
            AchievementCategory.SOCIAL, threshold = 80, rewardXp = 2_000),
        Achievement("ach_tyrant", "Tirano", "Karma <= -80", "💀",
            AchievementCategory.SECRET, threshold = -80, rewardXp = 2_000, hidden = true),
        Achievement("ach_meet_all_npcs", "Mariposa social", "Conoce a 10 NPCs", "🦋",
            AchievementCategory.SOCIAL, threshold = 10, rewardXp = 800),
        Achievement("ach_friendship_max", "Mejor amigo", "Friendship 100 con cualquier NPC", "🤝",
            AchievementCategory.SOCIAL, threshold = 100, rewardXp = 1_000),

        // ----- BALWEALTH INDEX -----
        Achievement("ach_harmonic_1d", "Armonía", "1 día completo en estado Armónico", "🌈",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 1_000),
        Achievement("ach_harmonic_30d", "Armonía sostenida", "30 días en Armónico", "🕊️",
            AchievementCategory.MILESTONE, threshold = 30, rewardXp = 10_000),
        Achievement("ach_balwealth_90", "Casi perfecto", "Índice BalWealth >= 90", "💯",
            AchievementCategory.MILESTONE, threshold = 90, rewardXp = 5_000),

        // ----- WORLD EVENTS -----
        Achievement("ach_50_events", "Vida en la calle", "Resuelve 50 eventos del mundo", "🌆",
            AchievementCategory.MILESTONE, threshold = 50, rewardXp = 800),
        Achievement("ach_lottery_winner", "Premiado", "Gana en la lotería del mundo", "🎟️",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 200, hidden = true),
        Achievement("ach_helped_homeless", "Filántropo del barrio", "Ayuda a 10 mendigos", "🤲",
            AchievementCategory.SOCIAL, threshold = 10, rewardXp = 800),

        // ----- CASINO / RIESGO -----
        Achievement("ach_first_bet", "Apostando", "Realiza tu primera apuesta", "🎰",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 50),
        Achievement("ach_casino_winner", "Ganaste a la banca", "Gana 5.000 € en una sola apuesta", "🎲",
            AchievementCategory.WEALTH, threshold = 5_000, rewardXp = 1_000),
        Achievement("ach_casino_addict", "Ludópata", "Apuesta 100 veces", "🃏",
            AchievementCategory.SECRET, threshold = 100, rewardXp = 500, hidden = true),
        Achievement("ach_zero", "El cero gana", "Acierta el verde en la ruleta", "0️⃣",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 1_500, hidden = true),

        // ----- DREAMS -----
        Achievement("ach_dreamed", "Soñador", "Vive tu primer sueño lúcido", "💤",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 100),
        Achievement("ach_nightmare", "Pesadilla", "Sueño en estado Tirano", "👹",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 300, hidden = true),

        // ----- MANAGERS -----
        Achievement("ach_first_manager", "Delegando", "Contrata tu primer gerente", "🤖",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 200),
        Achievement("ach_all_managers", "Equipo directivo completo", "Contrata los 5 tipos de gerente", "🏆",
            AchievementCategory.SOCIAL, threshold = 5, rewardXp = 2_500),
        Achievement("ach_mgr_1000_actions", "Imperio automatizado", "Tus gerentes han ejecutado 1.000 acciones", "⚙️",
            AchievementCategory.PRODUCTION, threshold = 1_000, rewardXp = 1_500),

        // ----- CONTRATOS -----
        Achievement("ach_first_contract", "Primer pedido", "Acepta tu primer contrato B2B", "📋",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 200),
        Achievement("ach_50_contracts", "Vendedor B2B", "Completa 50 contratos", "🤝",
            AchievementCategory.MARKET, threshold = 50, rewardXp = 1_500),

        // ----- SKILLS / PERKS -----
        Achievement("ach_first_skill", "Especialización", "Desbloquea tu primera skill", "✨",
            AchievementCategory.CHARACTER, threshold = 1, rewardXp = 200),
        Achievement("ach_branch_complete", "Maestro de rama", "Completa una rama de skills", "🌳",
            AchievementCategory.CHARACTER, threshold = 1, rewardXp = 1_500),
        Achievement("ach_5_perks", "5 perks", "Posee 5 perks", "🎁",
            AchievementCategory.CHARACTER, threshold = 5, rewardXp = 800),
        Achievement("ach_legendary_perk", "Perk legendario", "Consigue un perk legendario", "🌟",
            AchievementCategory.CHARACTER, threshold = 1, rewardXp = 2_000),

        // ----- RIVALES -----
        Achievement("ach_first_rival", "Primera victoria", "Derrota a tu primer rival", "⚔️",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 500),
        Achievement("ach_all_rivals", "Imbatible", "Derrota a TODOS los rivales", "👑",
            AchievementCategory.MILESTONE, threshold = 8, rewardXp = 10_000),

        // ----- TUTORIAL -----
        Achievement("ach_finished_tutorial", "Aprendiz", "Completa el tutorial", "🎓",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 100),

        // ----- ESPECIALES / OCULTOS -----
        Achievement("ach_5am_play", "Trasnochador", "Juega a las 5 AM", "🌙",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 300, hidden = true),
        Achievement("ach_palindrome_cash", "Palíndromo", "Caja exactamente en palíndromo", "🔁",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 1_000, hidden = true),
        Achievement("ach_zero_cash", "Quebrado total", "Caja en 0 €", "💸",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 100, hidden = true),
        Achievement("ach_speedrun", "Speedrun", "Llega a 100k € en menos de 1 día in-game", "⚡",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 5_000, hidden = true),
        Achievement("ach_pacifist", "Pacifista", "100 días sin despedir a nadie", "🕊️",
            AchievementCategory.SECRET, threshold = 100, rewardXp = 3_000, hidden = true),
        Achievement("ach_hermit", "Ermitaño", "10 días sin hablar con NPCs", "🚪",
            AchievementCategory.SECRET, threshold = 10, rewardXp = 800, hidden = true),
        Achievement("ach_party_animal", "Fiestero", "Asiste a 5 fiestas (eventos)", "🎉",
            AchievementCategory.SOCIAL, threshold = 5, rewardXp = 500),

        // ----- EXPLORACIÓN -----
        Achievement("ach_visit_all_districts", "Geógrafo", "Visita los 7 distritos", "🗺️",
            AchievementCategory.MILESTONE, threshold = 7, rewardXp = 600),
        Achievement("ach_visit_park", "Naturalista", "Visita el parque 10 veces", "🌳",
            AchievementCategory.SOCIAL, threshold = 10, rewardXp = 200),
        Achievement("ach_visit_harbor", "Marinero", "Visita el puerto 10 veces", "⚓",
            AchievementCategory.SOCIAL, threshold = 10, rewardXp = 200),

        // ----- WEATHER -----
        Achievement("ach_rain_lover", "Le encanta la lluvia", "Camina 10 min bajo la lluvia", "🌧",
            AchievementCategory.SECRET, threshold = 600, rewardXp = 200, hidden = true),
        Achievement("ach_snow_walk", "Aurora", "Camina bajo la nieve", "❄️",
            AchievementCategory.SECRET, threshold = 60, rewardXp = 200, hidden = true),
        Achievement("ach_storm", "Tormentoso", "Sobrevive a una tormenta", "⛈",
            AchievementCategory.SECRET, threshold = 1, rewardXp = 100, hidden = true),

        // ----- AVATAR -----
        Achievement("ach_customized", "Estilo propio", "Personaliza tu avatar", "👤",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 100),
        Achievement("ach_top_hat", "Caballero", "Usa la chistera", "🎩",
            AchievementCategory.CHARACTER, threshold = 1, rewardXp = 100),

        // ----- COMUNIDAD -----
        Achievement("ach_donate_50k", "Filántropo", "Dona 50.000 € a causas", "💝",
            AchievementCategory.SOCIAL, threshold = 50_000, rewardXp = 2_000),
        Achievement("ach_donate_500k", "Mecenas", "Dona 500.000 €", "💖",
            AchievementCategory.SOCIAL, threshold = 500_000, rewardXp = 10_000),

        // ----- META -----
        Achievement("ach_play_1h", "Una hora", "1 hora de juego real", "⏱️",
            AchievementCategory.MILESTONE, threshold = 3600, rewardXp = 200),
        Achievement("ach_play_10h", "10 horas", "10 horas de juego real", "⏰",
            AchievementCategory.MILESTONE, threshold = 36000, rewardXp = 2_000),
        Achievement("ach_play_100h", "Adicto sano", "100 horas de juego real", "🎮",
            AchievementCategory.MILESTONE, threshold = 360000, rewardXp = 20_000),

        // ----- PRESTIGIO -----
        Achievement("ach_first_rebirth", "Renacido", "Renace por primera vez", "🦋",
            AchievementCategory.MILESTONE, threshold = 1, rewardXp = 5_000),
        Achievement("ach_5_rebirths", "Reencarnación", "Renace 5 veces", "♾️",
            AchievementCategory.MILESTONE, threshold = 5, rewardXp = 25_000)
    )

    /** Combinado total. */
    val merged: List<Achievement>
        get() = AchievementCatalog.all + extra
}
