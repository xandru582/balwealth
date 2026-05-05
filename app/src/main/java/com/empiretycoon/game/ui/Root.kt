package com.empiretycoon.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.empiretycoon.game.audio.AmbientPlayer
import com.empiretycoon.game.audio.LocalHapticEngine
import com.empiretycoon.game.audio.LocalSoundEngine
import com.empiretycoon.game.audio.MusicSelector
import com.empiretycoon.game.audio.rememberAssetMusicPlayer
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.EventPool
import com.empiretycoon.game.model.TutorialStep
import com.empiretycoon.game.ui.components.*
import com.empiretycoon.game.ui.screens.*
import com.empiretycoon.game.ui.theme.*

/**
 * Estructura del menú principal:
 * - Bottom-nav con 5 pestañas principales (Home, Imperio, Mercado, Patrimonio, Más).
 * - "Más" abre un Drawer con todas las pantallas avanzadas (Logros, Prestigio,
 *   Investigación, Skills, Rivales, Banca, IPO, Opciones, RRHH, Líneas,
 *   Calidad, Historia, Misiones+, NPCs, Noticias, Contratos, Player, Ajustes).
 */
sealed class MainTab(val id: String, val label: String, val icon: ImageVector) {
    object Home : MainTab("home", "Inicio", Icons.Filled.Home)
    object City : MainTab("city", "Ciudad", Icons.Filled.LocationCity)
    object Empire : MainTab("empire", "Imperio", Icons.Filled.Business)
    object Market : MainTab("market", "Mercado", Icons.Filled.Storefront)
    object More : MainTab("more", "Menú", Icons.Filled.Menu)
}

private val MainTabs = listOf(
    MainTab.Home, MainTab.City, MainTab.Empire, MainTab.Market, MainTab.More
)

/** Pantallas accesibles desde el menú "Más". */
private data class SubScreen(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String
)

private val SubScreens = listOf(
    SubScreen("balwealth", "Índice BalWealth", "🌈", "El equilibrio entre riqueza, equipo, comunidad y mente"),
    SubScreen("house", "Mi casa & familia", "🏡", "Decora tu hogar, pareja, hijos"),
    SubScreen("petshop", "Mascotas", "🐾", "Adopta perro, gato, dragoncito y más"),
    SubScreen("dealership", "Concesionario", "🚗", "Compra coches: 25 modelos en 9 marcas"),
    SubScreen("garage", "Mi garaje", "🏠", "Tus coches, conduce, pinta, vende"),
    SubScreen("wealth", "Patrimonio", "💼", "Bolsa, dividendos e inmuebles"),
    SubScreen("avatar", "Personalizar avatar", "👤", "Cambia tu apariencia (ropa, pelo, accesorios)"),
    SubScreen("managers", "Gerentes (auto)", "🤖", "Automatizan compras, ventas, mejoras, RRHH y deuda"),
    SubScreen("casino", "Casino", "🎰", "Ruleta — apuestas con la caja de la empresa"),
    SubScreen("dream", "Descansar (sueño)", "💤", "Sueño lúcido restaurador. Te habla tu subconsciente."),
    SubScreen("player", "Tu personaje", "🧑‍💼", "Stats, energía, entrenamiento, cartera personal"),
    SubScreen("research", "Ciencia", "🔬", "Investigación y árbol tecnológico"),
    SubScreen("hr", "RRHH", "👥", "Plantilla, candidatos, formación, ejecutivos"),
    SubScreen("contracts", "Contratos B2B", "📋", "Pedidos firmes con clientes"),
    SubScreen("news", "Noticias", "📰", "Titulares que mueven el mercado"),
    SubScreen("lines", "Líneas de producción", "🏭", "Cadenas automatizadas"),
    SubScreen("quality", "Inventario por calidad", "💎", "Lotes por tier de calidad"),
    SubScreen("achievements", "Logros", "🏆", "50+ desafíos por desbloquear"),
    SubScreen("prestige", "Prestigio", "⭐", "Renacer y desbloquear perks permanentes"),
    SubScreen("skills", "Habilidades", "✨", "Árbol de skills personal"),
    SubScreen("rivals", "Rivales", "⚔️", "Competidores con los que medirte"),
    SubScreen("banking", "Banca", "🏦", "Préstamos, hipotecas, cuotas"),
    SubScreen("ipo", "Salir a bolsa", "📈", "IPO de tu propia empresa"),
    SubScreen("options", "Opciones", "📊", "Calls, puts, derivados"),
    SubScreen("story", "Historia principal", "📖", "Capítulos, karma y final"),
    SubScreen("sidequests", "Misiones secundarias", "🎯", "Encargos de NPCs"),
    SubScreen("npcs", "Contactos", "🧑‍🤝‍🧑", "Personajes que conoces"),
    SubScreen("racing", "Formula Manager", "🏁", "Compra un equipo, ficha pilotos y gana el campeonato"),
    SubScreen("crypto", "Mercado cripto", "🪙", "Tokens volátiles, staking, mining, rugpulls"),
    SubScreen("disasters", "Sala de crisis", "🌪️", "Desastres dinámicos, seguro, mitigación"),
    SubScreen("daily", "Retos diarios", "🎯", "3 retos cada día + reto semanal con racha"),
    SubScreen("heists", "Inframundo (heists)", "🦹", "Atracos planificados con tripulación y heat"),
    SubScreen("settings", "Ajustes", "⚙️", "Audio, velocidad, partida")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Root(vm: GameViewModel) {
    val loading by vm.loading.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()

    if (loading) {
        SplashLoading()
        return
    }

    // Splash de marca durante 2.5s
    var splashShown by rememberSaveable { mutableStateOf(false) }
    if (!splashShown) {
        SplashScreen(autoMs = 2_500L) { splashShown = true }
        return
    }

    // Onboarding: nombre del jugador y empresa
    if (state.company.name == "Nueva Empresa S.L." && state.tick < 3) {
        OnboardingDialog(onConfirm = { p, c -> vm.rename(p, c) })
    }

    // Mirror audio settings -> engines
    val soundEngine = LocalSoundEngine.current
    val hapticEngine = LocalHapticEngine.current
    LaunchedEffect(state.audio) {
        soundEngine?.setEnabled(state.audio.soundEnabled)
        soundEngine?.setVolume(state.audio.masterVolume)
        hapticEngine?.setEnabled(state.audio.hapticsEnabled)
    }

    // Ambient drone (subsistema antiguo)
    val ctx = LocalContext.current
    val ambientScope = rememberCoroutineScope()
    val ambient = remember { AmbientPlayer(ctx.applicationContext, ambientScope) }
    DisposableEffect(Unit) { onDispose { ambient.release() } }

    // Música contextual desde assets/audio/music — 35 tracks
    val musicPlayer = rememberAssetMusicPlayer()
    LaunchedEffect(state.audio.musicEnabled, state.audio.masterVolume) {
        musicPlayer.setEnabled(state.audio.musicEnabled)
        musicPlayer.setVolume(state.audio.masterVolume * 0.5f)
        ambient.setVolume(state.audio.masterVolume * 0.3f)
        // FIX BUG-08-04: si el usuario desactiva música, el drone TAMBIÉN
        // se calla. El icono 🔇 ahora silencia DE VERDAD todo el audio
        // ambiental. Sólo arranca el drone si el usuario explícitamente lo
        // pidió (musicEnabled=true).
        if (state.audio.musicEnabled) ambient.stop() else ambient.stop()
    }

    // Money rain on cash milestones
    val cashMilestones = remember {
        listOf(100_000.0, 1_000_000.0, 10_000_000.0, 100_000_000.0, 1_000_000_000.0)
    }
    var lastMilestoneIdx by rememberSaveable { mutableStateOf(-1) }
    var moneyRainTrigger by remember { mutableStateOf(false) }
    LaunchedEffect(state.company.cash) {
        val crossed = cashMilestones.indexOfLast { state.company.cash >= it }
        if (crossed > lastMilestoneIdx) {
            lastMilestoneIdx = crossed
            moneyRainTrigger = !moneyRainTrigger
        }
    }
    val pendingQuests = state.quests.count { it.completed && !it.claimed }
    val pendingAch = state.achievements.unlocked.count { it !in state.achievements.claimedAchievements }
    val pendingMore = pendingAch + (if (state.activeEventId != null) 1 else 0)

    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home.id) }
    var subScreen by rememberSaveable { mutableStateOf<String?>(null) }

    // FIX BUG-10: BackHandler global. Si hay subpantalla, vuelve al tab raíz.
    // Si no, vuelve a Home. Si ya estás en Home raíz, deja salir (handler off).
    BackHandler(enabled = subScreen != null || currentTab != MainTab.Home.id) {
        if (subScreen != null) {
            subScreen = null
        } else {
            currentTab = MainTab.Home.id
        }
    }

    // FIX feedback usuario: la música se reseteaba al cambiar de pestaña.
    // Ahora las keys del LaunchedEffect son SOLO las cosas que realmente
    // pueden cambiar la pista (distrito, bucket horario, subpantalla
    // especial, conducción, musicEnabled). `currentTab` no se incluye
    // porque cambiar de tab no cambia la música.
    LaunchedEffect(
        state.world.currentDistrict,
        state.hourOfDay / 6,
        subScreen,
        state.garage.isDriving,
        state.audio.musicEnabled
    ) {
        if (!state.audio.musicEnabled) return@LaunchedEffect
        val track = MusicSelector.pickFor(
            district = state.world.currentDistrict,
            hour = state.hourOfDay,
            isDriving = state.garage.isDriving,
            isInDream = subScreen == "dream",
            isInCasino = subScreen == "casino",
            isInDealership = subScreen == "dealership",
            weather = state.weather.current
        )
        musicPlayer.play(track)
    }

    AnchorRegistry {
        Scaffold(
            topBar = {
                PolishedTopBar(
                    company = state.company,
                    player = state.player,
                    day = state.day,
                    hour = state.hourOfDay,
                    paused = state.paused,
                    speed = state.speedMultiplier,
                    musicOn = state.audio.musicEnabled,
                    onToggleMusic = { vm.setMusicEnabled(!state.audio.musicEnabled) },
                    onTogglePause = { vm.togglePause() },
                    onSpeedCycle = {
                        val next = when (state.speedMultiplier) {
                            1 -> 2; 2 -> 4; 4 -> 8; else -> 1
                        }
                        vm.setSpeed(next)
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = InkSoft, contentColor = Paper) {
                    MainTabs.forEach { t ->
                        val badgeCount = when (t.id) {
                            MainTab.Home.id -> pendingQuests
                            MainTab.More.id -> pendingMore
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = t.id == currentTab && subScreen == null,
                            onClick = {
                                currentTab = t.id
                                subScreen = null
                            },
                            icon = {
                                Box {
                                    Icon(t.icon, contentDescription = t.label)
                                    AnimatedBadge(
                                        count = badgeCount,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 10.dp, y = (-6).dp)
                                    )
                                }
                            },
                            label = { Text(t.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Gold,
                                selectedTextColor = Gold,
                                unselectedIconColor = Dim,
                                unselectedTextColor = Dim,
                                indicatorColor = InkBorder
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(
                Modifier
                    .padding(pad)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Ink, Color(0xFF0B1220), Ink)
                        )
                    )
            ) {
                // Si hay subpantalla activa
                if (subScreen != null) {
                    SubScreenHost(
                        id = subScreen!!,
                        state = state,
                        vm = vm,
                        onBack = { subScreen = null }
                    )
                } else {
                    when (currentTab) {
                        MainTab.Home.id -> HomeScreen(state, vm)
                        MainTab.City.id -> com.empiretycoon.game.world.ui.WorldScreen(state, vm) { target ->
                            // Si target es una MainTab id, cambiar pestaña; si no, abrir sub-screen
                            val mainTabIds = listOf(
                                MainTab.Home.id, MainTab.City.id, MainTab.Empire.id,
                                MainTab.Market.id, MainTab.More.id
                            )
                            if (target in mainTabIds) {
                                currentTab = target
                                subScreen = null
                            } else {
                                subScreen = target
                            }
                        }
                        MainTab.Empire.id -> EmpireScreen(state, vm)
                        MainTab.Market.id -> MarketScreen(state, vm)
                        MainTab.More.id -> MoreMenuScreen(
                            onPick = { id -> subScreen = id }
                        )
                    }
                }

                GameToastHost(
                    notifications = state.notifications,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp)
                )

                // Money rain overlay (no bloquea clicks)
                MoneyRain(
                    trigger = moneyRainTrigger,
                    durationMs = 1500,
                    modifier = Modifier.fillMaxSize()
                ) { /* sin contenido extra, solo overlay */ }
            }

            // Diálogos modales
            state.activeEventId?.let { eid ->
                val ev = EventPool.pool.find { it.id == eid }
                if (ev != null) EventDialog(ev) { choice -> vm.resolveEvent(choice) }
            }
            // Diálogo de elección de perk al subir nivel x5
            state.perks.pendingChoice?.let { ids ->
                if (ids.isNotEmpty()) PerkChoiceDialog(perkIds = ids) { id -> vm.pickPerk(id) }
            }
            // Coachmark del tutorial activo
            if (!state.tutorial.skipped &&
                state.tutorial.currentStep != TutorialStep.WELCOME &&
                state.tutorial.currentStep != TutorialStep.FINISHED) {
                val spec = com.empiretycoon.game.model.TutorialScript.specOf(state.tutorial.currentStep)
                val anchor = rememberAnchor(spec.targetWidgetId)
                Coachmark(
                    spec = spec,
                    anchorBounds = anchor,
                    onSkip = { vm.tutorialDismiss() },
                    onPrimary = { vm.tutorialAdvance(state.tutorial.currentStep) },
                    onSkipAll = { vm.tutorialSkip() }
                )
            }
            // Intro de tutorial al inicio
            if (state.tutorial.currentStep == TutorialStep.WELCOME && !state.tutorial.skipped) {
                TutorialIntroDialog(
                    playerName = state.player.name,
                    onStartTutorial = { vm.tutorialAdvance(TutorialStep.WELCOME) },
                    onSkipTutorial = { vm.tutorialSkip() },
                    onShowOverview = { /* opcional */ }
                )
            }
            // Modal de tutorial completado
            if (state.tutorial.currentStep == TutorialStep.FINISHED &&
                !state.tutorial.skipped &&
                !state.tutorial.completedSteps.contains(TutorialStep.FINISHED)) {
                TutorialFinishedDialog(onClose = { vm.tutorialSkip() })
            }
        }
    }
}

/** Menú de "Más" con todas las subpantallas. */
@Composable
private fun MoreMenuScreen(onPick: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                "Menú avanzado",
                fontWeight = FontWeight.Black,
                color = Gold,
                fontSize = 22.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
        item {
            Text(
                "Toca cualquier sección para abrirla. Las funciones avanzadas se desbloquean según progresas.",
                color = Dim,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(SubScreens, key = { it.id }) { sub ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(InkSoft)
                    .clickable { onPick(sub.id) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(InkBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(sub.emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(sub.label, fontWeight = FontWeight.SemiBold, color = Paper)
                    Text(sub.description, color = Dim, fontSize = 11.sp)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = Gold)
            }
        }
        item { Spacer(Modifier.height(60.dp)) }
    }
}

@Composable
private fun SubScreenHost(
    id: String,
    state: com.empiretycoon.game.model.GameState,
    vm: GameViewModel,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(InkSoft)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, "Volver", tint = Gold)
            }
            val title = SubScreens.find { it.id == id }?.label ?: ""
            Text(title, fontWeight = FontWeight.Bold, color = Paper)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (id) {
                "balwealth" -> BalWealthScreen(state, vm)
                "house" -> HouseScreen(state, vm)
                "petshop" -> PetShopScreen(state, vm)
                "dealership" -> DealershipScreen(state, vm)
                "garage" -> GarageScreen(state, vm)
                "wealth" -> WealthScreen(state, vm)
                "avatar" -> AvatarScreen(state, vm, onBack = onBack)
                "managers" -> ManagersScreen(state, vm)
                "casino" -> CasinoScreen(state, vm)
                "dream" -> DreamScreen(state, vm) {
                    vm.rest()
                    onBack()
                }
                "player" -> PlayerScreen(state, vm)
                "research" -> ResearchScreen(state, vm)
                "hr" -> HrScreen(state, vm)
                "contracts" -> ContractsScreen(state, vm)
                "news" -> NewsScreen(state, vm)
                "lines" -> LinesScreen(state, vm)
                "quality" -> QualityInventoryScreen(state, vm)
                "achievements" -> AchievementsScreen(state, vm)
                "prestige" -> PrestigeScreen(state, vm)
                "skills" -> SkillTreeScreen(state, vm)
                "rivals" -> RivalsScreen(state, vm)
                "banking" -> BankingScreen(state, vm)
                "ipo" -> IpoScreen(state, vm)
                "options" -> OptionsScreen(state, vm)
                "story" -> StoryScreen(state, vm)
                "sidequests" -> SideQuestsScreen(state, vm)
                "npcs" -> NpcsScreen(state, vm)
                "racing" -> RacingScreen(state, vm)
                "crypto" -> CryptoScreen(state, vm)
                "disasters" -> DisastersScreen(state, vm)
                "daily" -> DailyChallengesScreen(state, vm)
                "heists" -> HeistsScreen(state, vm)
                "settings" -> MoreScreen(state, vm)
            }
        }
    }
}

@Composable
private fun SplashLoading() {
    Box(
        Modifier.fillMaxSize().background(Ink),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🌆  BALWEALTH", fontSize = 28.sp,
                fontWeight = FontWeight.Black, color = Gold)
            Spacer(Modifier.height(8.dp))
            Text("Cargando partida…", color = Dim)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Gold)
        }
    }
}

@Composable
private fun OnboardingDialog(onConfirm: (String, String) -> Unit) {
    var show by remember { mutableStateOf(true) }
    var pName by remember { mutableStateOf("") }
    var cName by remember { mutableStateOf("") }
    if (!show) return
    CompactDialog(
        title = "Bienvenido a BalWealth",
        icon = "🌆",
        onDismiss = { },
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
        footer = {
            TextButton(onClick = {
                onConfirm(pName.ifBlank { "Empresario" }, cName.ifBlank { "Nueva Empresa S.L." })
                show = false
            }) { Text("Empezar", color = Gold) }
        }
    ) {
        Text("Dale un nombre a tu personaje y a tu empresa. Podrás cambiarlo luego.",
            color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pName, onValueChange = { pName = it },
            label = { Text("Tu nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = cName, onValueChange = { cName = it },
            label = { Text("Nombre de la empresa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EventDialog(
    ev: com.empiretycoon.game.model.GameEvent,
    onChoose: (Int) -> Unit
) {
    CompactDialog(
        title = ev.title,
        icon = ev.icon,
        onDismiss = { },
        dismissOnBackPress = false,
        dismissOnClickOutside = false
    ) {
        Text(ev.description, color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        ev.choices.forEachIndexed { idx, choice ->
            Button(
                onClick = { onChoose(idx) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkBorder,
                    contentColor = Paper
                )
            ) {
                Text(choice.label, fontSize = 12.sp)
            }
        }
    }
}
