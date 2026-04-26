package com.empiretycoon.game.world.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.empiretycoon.game.data.GameViewModel
import com.empiretycoon.game.model.GameState
import com.empiretycoon.game.world.Avatar
import com.empiretycoon.game.world.Camera
import com.empiretycoon.game.world.Facing
import com.empiretycoon.game.world.MoveInput
import com.empiretycoon.game.world.MovementEngine
import com.empiretycoon.game.world.NpcWorldEngine
import com.empiretycoon.game.world.WorldGrid
import com.empiretycoon.game.world.WorldState
import com.empiretycoon.game.world.WorldEventCatalog
import com.empiretycoon.game.world.sprites.drawAvatar
import com.empiretycoon.game.world.sprites.drawNpc
import com.empiretycoon.game.world.sprites.drawPlayerCar
import com.empiretycoon.game.world.sprites.drawFollowerHalo
import com.empiretycoon.game.world.sprites.drawPet
import com.empiretycoon.game.world.sprites.drawProp
import com.empiretycoon.game.world.sprites.drawTile
import com.empiretycoon.game.world.sprites.drawUfo
import com.empiretycoon.game.world.sprites.drawVehicle
import com.empiretycoon.game.world.sprites.drawWeatherOverlay
import com.empiretycoon.game.world.sprites.drawBird
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.ceil

/**
 * Pantalla 2D explorable: dibuja el mundo, mueve al jugador con joystick
 * y permite interactuar con tiles delante. Diseñada para correr a 60fps
 * usando un loop con `withFrameNanos`.
 */
@Composable
fun WorldScreen(state: GameState, vm: GameViewModel, onNavigateTo: (String) -> Unit = {}) {
    val density = LocalDensity.current
    var input by remember { mutableStateOf(MoveInput.ZERO) }
    var camera by remember { mutableStateOf(Camera(state.world.avatar.x, state.world.avatar.y, zoom = 2.4f)) }
    var animPhase by remember { mutableStateOf(0f) }
    var dialog by remember { mutableStateOf<DialogPayload?>(null) }

    // Loop de animación: avanza movimiento + cámara + animPhase a cada frame
    LaunchedEffect(Unit) {
        var lastTime = 0L
        while (true) {
            val time = awaitFrame()
            val dt = if (lastTime == 0L) 0f else ((time - lastTime) / 1_000_000_000f).coerceAtMost(0.1f)
            lastTime = time
            animPhase = (animPhase + dt * 0.6f) % 1f
            vm.applyWorldMove(input.dx, input.dy, dt)
            camera = camera.follow(vm.state.value.world.avatar)
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F1724))) {
        // === RENDER PIPELINE: cielo → tiles → sombras → objetos sorted → luces → atmósfera → vignette → clima ===
        Canvas(Modifier.fillMaxSize()) {
            if (size.width <= 1f || size.height <= 1f) return@Canvas
            val tileSize = ((size.width / 18f) * camera.zoom * 0.5f).coerceAtLeast(24f)
            val viewW = ceil(size.width / tileSize).toInt() + 2
            val viewH = ceil(size.height / tileSize).toInt() + 2
            val originTileX = camera.centerX - viewW / 2f
            val originTileY = camera.centerY - viewH / 2f
            val hour = state.hourOfDay
            val ambient = com.empiretycoon.game.world.render.SkyEngine.ambientFor(hour)
            val sunAngle = com.empiretycoon.game.world.render.SkyEngine.sunAngle(hour)

            // === CAPA 0: SKY (gradient + estrellas/luna/sol) ===
            with(com.empiretycoon.game.world.render.SkyEngine) { drawSky(hour, animPhase) }

            // === CAPA 1: TILES del suelo ===
            val grid = state.world.grid
            for (dy in 0..viewH) {
                val ty = (originTileY + dy).toInt()
                for (dx in 0..viewW) {
                    val tx = (originTileX + dx).toInt()
                    if (!grid.inBounds(tx, ty)) continue
                    val type = grid.tileAt(tx, ty)
                    val deco = grid.decoAt(tx, ty)
                    val sx = (tx - originTileX) * tileSize
                    val sy = (ty - originTileY) * tileSize
                    drawTile(type, sx, sy, tileSize, deco, animPhase)
                }
            }

            // === CAPA 2: OBJETOS DINÁMICOS con depth sort por Y ===
            val objects = mutableListOf<com.empiretycoon.game.world.render.RenderObject>()

            // Props
            for (prop in state.cityProps.props) {
                if (prop.x < originTileX - 1 || prop.x > originTileX + viewW + 1) continue
                if (prop.y < originTileY - 1 || prop.y > originTileY + viewH + 1) continue
                objects += com.empiretycoon.game.world.render.PropObject(prop.propKind(), prop.x, prop.y)
            }
            // Vehículos
            for (veh in state.traffic.vehicles) {
                if (veh.x < originTileX - 2 || veh.x > originTileX + viewW + 2) continue
                if (veh.y < originTileY - 2 || veh.y > originTileY + viewH + 2) continue
                objects += com.empiretycoon.game.world.render.VehicleObject(veh)
            }
            // Pet activa
            state.pets.active()?.let { pet ->
                if (pet.x >= originTileX - 2 && pet.x <= originTileX + viewW + 2) {
                    objects += com.empiretycoon.game.world.render.PetObject(
                        pet.spec(), pet.x, pet.y, pet.walkPhase
                    )
                }
            }
            // Follower
            state.follower.current?.let { f ->
                objects += com.empiretycoon.game.world.render.FollowerObject(f, animPhaseRef = { animPhase })
            }
            // NPCs
            for (npc in state.npcWorld.walkers) {
                if (npc.x < originTileX - 2 || npc.x > originTileX + viewW + 2) continue
                if (npc.y < originTileY - 2 || npc.y > originTileY + viewH + 2) continue
                objects += com.empiretycoon.game.world.render.NpcObject(
                    worldX = npc.x, worldY = npc.y,
                    drawer = { scope, sx, sy, ts ->
                        with(scope) {
                            drawNpc(
                                seed = npc.seedColor,
                                x = sx - ts / 2f,
                                y = sy - ts * 0.6f,
                                scale = ts / 16f,
                                walkPhase = npc.walkPhase,
                                facing = npc.facing
                            )
                        }
                    }
                )
            }
            // Avatar / coche
            val avatar = state.world.avatar
            val driving = state.garage.current()
            if (driving != null) {
                objects += com.empiretycoon.game.world.render.AvatarObject(
                    worldX = avatar.x, worldY = avatar.y,
                    drawer = { scope, sx, sy, ts, anim ->
                        with(scope) {
                            drawPlayerCar(owned = driving, facing = avatar.facing,
                                x = sx, y = sy, tileSize = ts, animPhase = anim)
                        }
                    }
                )
            } else {
                objects += com.empiretycoon.game.world.render.AvatarObject(
                    worldX = avatar.x, worldY = avatar.y,
                    drawer = { scope, sx, sy, ts, _ ->
                        with(scope) {
                            drawAvatar(
                                look = avatar.look, facing = avatar.facing,
                                walkPhase = avatar.walkPhase,
                                x = sx - ts / 2f, y = sy - ts * 0.6f,
                                scale = ts / 16f
                            )
                        }
                    }
                )
            }
            // UFO (z-boost altísimo, siempre encima)
            state.ufo.active?.let { u -> objects += com.empiretycoon.game.world.render.UfoObject(u) }

            // Sort por (worldY + zBoost)
            val sorted = objects.sortedBy { it.worldY + it.zBoost }

            // SHADOWS antes de los objetos (por debajo)
            if (ambient > 0.4f) {
                with(com.empiretycoon.game.world.render.ShadowEngine) {
                    for (obj in sorted) {
                        if (!obj.castsShadow) continue
                        val (sx, sy) = ((obj.worldX - originTileX) * tileSize) to ((obj.worldY - originTileY) * tileSize)
                        drawObjectShadow(sx, sy, obj.shadowRadius * tileSize, sunAngle, ambient)
                    }
                }
            }

            // OBJETOS en orden
            for (obj in sorted) {
                val sx = (obj.worldX - originTileX) * tileSize
                val sy = (obj.worldY - originTileY) * tileSize
                obj.draw(this, sx, sy, tileSize, animPhase)
            }

            // === CAPA 3: ILUMINACIÓN nocturna (faroles, ventanas, faros) ===
            if (ambient < 0.7f) {
                val lights = mutableListOf<com.empiretycoon.game.world.render.PointLight>()
                // Faroles
                for (prop in state.cityProps.props) {
                    if (prop.propKind() == com.empiretycoon.game.world.PropKind.LAMP_POST) {
                        if (prop.x < originTileX - 2 || prop.x > originTileX + viewW + 2) continue
                        if (prop.y < originTileY - 2 || prop.y > originTileY + viewH + 2) continue
                        val (sx, sy) = ((prop.x - originTileX) * tileSize) to ((prop.y - originTileY) * tileSize)
                        lights += com.empiretycoon.game.world.render.PointLight(
                            x = sx, y = sy - tileSize * 0.25f,
                            radius = tileSize * 2.2f,
                            color = Color(0xFFFFEB80),
                            intensity = 0.8f
                        )
                    }
                }
                // Faros del coche del jugador (cono frontal aprox)
                if (driving != null) {
                    val (cx, cy) = ((avatar.x - originTileX) * tileSize) to ((avatar.y - originTileY) * tileSize)
                    val (dx, dy) = when (avatar.facing) {
                        com.empiretycoon.game.world.Facing.UP -> 0f to -tileSize * 1.5f
                        com.empiretycoon.game.world.Facing.DOWN -> 0f to tileSize * 1.5f
                        com.empiretycoon.game.world.Facing.LEFT -> -tileSize * 1.5f to 0f
                        com.empiretycoon.game.world.Facing.RIGHT -> tileSize * 1.5f to 0f
                    }
                    lights += com.empiretycoon.game.world.render.PointLight(
                        x = cx + dx, y = cy + dy,
                        radius = tileSize * 3.5f,
                        color = Color(0xFFFFFFFF),
                        intensity = 0.85f
                    )
                }
                // Ventanas iluminadas (sutiles, en cualquier WALL adyacente)
                with(com.empiretycoon.game.world.render.LightingEngine) {
                    drawLights(lights, ambient, size)
                }
                // Velo nocturno general (encima de tiles pero debajo de luces NO — por eso se mezcla)
                drawRect(
                    color = Color(0xFF0A0E27).copy(alpha = (1f - ambient) * 0.55f),
                    topLeft = Offset.Zero, size = size,
                    blendMode = BlendMode.Multiply
                )
                // Re-aplicar luces ENCIMA del velo para que brillen sobre la oscuridad
                with(com.empiretycoon.game.world.render.LightingEngine) {
                    drawLights(lights, ambient, size)
                }
            }

            // === CAPA 4: ATMÓSFERA tintada (hora dorada, crepúsculo) ===
            with(com.empiretycoon.game.world.render.PostFx) { drawAtmosphereTint(hour) }

            // === CAPA 4b: NIEBLA atmosférica (clima/hora) ===
            with(com.empiretycoon.game.world.render.PostFx) {
                drawAtmosphericFog(state.weather.currentWeather().name, hour)
            }

            // === CAPA 4c: PARTÍCULAS ambientales (polen/polvo de día) ===
            with(com.empiretycoon.game.world.render.PostFx) { drawAmbientParticles(animPhase, hour) }

            // === CAPA 5: KARMA echo (filtros visuales según karma) ===
            val echo = state.karmaEcho
            if (echo.saturation < 0.95f) {
                drawRect(
                    color = Color(0xFF424242).copy(alpha = (1f - echo.saturation).coerceIn(0f, 0.4f)),
                    topLeft = Offset.Zero, size = size, blendMode = BlendMode.Saturation
                )
            } else if (echo.saturation > 1.05f) {
                drawRect(
                    color = Color(0xFFFFEB3B).copy(alpha = (echo.saturation - 1f).coerceIn(0f, 0.2f)),
                    topLeft = Offset.Zero, size = size, blendMode = BlendMode.Overlay
                )
            }

            // === CAPA 6: CLIMA partículas (lluvia, nieve, niebla) ===
            drawWeatherOverlay(state.weather.currentWeather(), animPhase, size)
            with(com.empiretycoon.game.world.render.PostFx) {
                drawWeatherFx(state.weather.currentWeather().name, animPhase)
            }

            // === CAPA 7: PÁJAROS si soleado ===
            if (state.weather.currentWeather() == com.empiretycoon.game.world.Weather.SUNNY && hour in 7..19) {
                for (i in 0 until 3) {
                    val bx = (animPhase * size.width * 1.2f + i * 200f) % (size.width + 100f) - 50f
                    val by = size.height * 0.15f + i * 30f
                    drawBird(bx, by, (animPhase * 4 + i * 0.3f) % 1f)
                }
            }

            // === CAPA 8: VIGNETTE cinemático ===
            with(com.empiretycoon.game.world.render.PostFx) { drawVignette(strength = 0.50f) }
        }

        // HUD: distrito + hora arriba
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 8.dp)
        ) {
            Text(
                "📍 ${state.world.currentDistrict.uppercase()}",
                color = Color(0xFFFFD166),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(Color(0xCC0F1724))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "🕐 ${"%02d".format(state.hourOfDay)}:00 · Día ${state.day}",
                color = Color(0xFFCFD8DC),
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color(0xCC0F1724))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.height(4.dp))
            val w = state.weather.currentWeather()
            Text(
                "${w.emoji} ${w.displayName}",
                color = Color(0xFFCFD8DC),
                fontSize = 11.sp,
                modifier = Modifier
                    .background(Color(0xCC0F1724))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        // Diálogo del follower NPC (con timeout visible)
        state.follower.current?.let { f ->
            val timeLeft = (com.empiretycoon.game.world.FollowerNpc.TIMEOUT_TICKS - (state.tick - f.spawnedAtTick)).coerceAtLeast(0)
            // Solo mostrar diálogo cuando ya está cerca
            val dx = state.world.avatar.x - f.x
            val dy = state.world.avatar.y - f.y
            val dist = kotlin.math.hypot(dx, dy)
            if (dist < 2.5f) {
                com.empiretycoon.game.ui.components.CompactDialog(
                    title = "${f.name} se acerca…",
                    icon = "💬",
                    onDismiss = { vm.dismissFollower() }
                ) {
                    Text(f.question, color = Color(0xFFCFD8DC), fontSize = 12.sp)
                    Text("Tiempo restante: ${timeLeft}s",
                        color = Color(0xFFFFEB3B), fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { vm.resolveFollower(0) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243042), contentColor = Color.White)
                    ) { Text(f.choiceA, fontSize = 12.sp) }
                    Button(
                        onClick = { vm.resolveFollower(1) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243042), contentColor = Color.White)
                    ) { Text(f.choiceB, fontSize = 12.sp) }
                }
            }
        }

        // Diálogo de evento del mundo si hay uno activo (compacto)
        state.worldEvent.activeEventId?.let { eid ->
            val ev = com.empiretycoon.game.world.MoreEventsCatalog.merged.find { it.id == eid }
            if (ev != null) {
                com.empiretycoon.game.ui.components.CompactDialog(
                    title = ev.title,
                    icon = ev.emoji,
                    onDismiss = { vm.dismissWorldEvent() }
                ) {
                    Text(ev.body, color = Color(0xFFCFD8DC), fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                    ev.choices.forEachIndexed { idx, c ->
                        Button(
                            onClick = { vm.resolveWorldEvent(idx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF243042),
                                contentColor = Color(0xFFFFFFFF)
                            )
                        ) {
                            androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth()) {
                                Text(c.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val tags = buildList {
                                    if (c.cashDelta > 0) add("+${c.cashDelta.toInt()}€")
                                    if (c.cashDelta < 0) add("${c.cashDelta.toInt()}€")
                                    if (c.karmaDelta > 0) add("+${c.karmaDelta} karma")
                                    if (c.karmaDelta < 0) add("${c.karmaDelta} karma")
                                    if (c.energyDelta != 0) add("${c.energyDelta} ⚡")
                                    if (c.happinessDelta != 0) add("${c.happinessDelta} 😊")
                                }
                                if (tags.isNotEmpty()) {
                                    Text(tags.joinToString(" · "),
                                        fontSize = 9.sp,
                                        color = Color(0xFF8899AA))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Joystick abajo izquierda
        VirtualJoystick(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            sizeDp = 140,
            onMove = { dx, dy -> input = MoveInput(dx, dy) },
            onRelease = { input = MoveInput.ZERO }
        )

        // Botón coche/peatón (encima del A)
        if (state.garage.cars.isNotEmpty()) {
            ActionButton(
                label = if (state.garage.isDriving) "🚶" else "🚗",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 130.dp),
                onClick = { vm.toggleDriving() }
            )
        }

        // Botón A abajo derecha
        ActionButton(
            label = "A",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 40.dp),
            onClick = {
                val req = MovementEngine.interact(state.world)
                val nearest = NpcWorldEngine.nearest(state.npcWorld, state.world.avatar.x, state.world.avatar.y, 1.8f)
                when {
                    req != null -> {
                        // Resolver según place_id si lo tenemos
                        val placeId = req.payload
                        val place = com.empiretycoon.game.world.CityBlueprint.places.find { it.id == placeId }
                        val target = when (place?.kind) {
                            com.empiretycoon.game.world.PlaceKind.BANK -> "banking"
                            com.empiretycoon.game.world.PlaceKind.STOCK_EXCHANGE -> "wealth"
                            com.empiretycoon.game.world.PlaceKind.CASINO -> "casino"
                            com.empiretycoon.game.world.PlaceKind.NIGHTCLUB -> "casino"
                            com.empiretycoon.game.world.PlaceKind.MARKET -> "market"
                            com.empiretycoon.game.world.PlaceKind.NEWSPAPER -> "news"
                            com.empiretycoon.game.world.PlaceKind.UNIVERSITY -> "research"
                            com.empiretycoon.game.world.PlaceKind.HOSPITAL -> "dream"
                            com.empiretycoon.game.world.PlaceKind.GYM -> "player"
                            com.empiretycoon.game.world.PlaceKind.MUSEUM -> "achievements"
                            com.empiretycoon.game.world.PlaceKind.CITY_HALL -> "story"
                            com.empiretycoon.game.world.PlaceKind.COURT -> "rivals"
                            com.empiretycoon.game.world.PlaceKind.POLICE -> "rivals"
                            com.empiretycoon.game.world.PlaceKind.APARTMENT, com.empiretycoon.game.world.PlaceKind.MANSION -> "avatar"
                            com.empiretycoon.game.world.PlaceKind.TAVERN -> "npcs"
                            com.empiretycoon.game.world.PlaceKind.BLACK_MARKET_DOOR -> "managers"
                            com.empiretycoon.game.world.PlaceKind.FACTORY_SLOT,
                            com.empiretycoon.game.world.PlaceKind.FARM_SLOT,
                            com.empiretycoon.game.world.PlaceKind.MINE_SLOT -> "empire"
                            else -> null
                        }
                        if (target != null) {
                            onNavigateTo(target)
                        } else {
                            dialog = DialogPayload(
                                title = place?.name ?: "Lugar",
                                body = "Lugar de la ciudad. ${place?.kind?.name ?: "Sin información"}"
                            )
                        }
                    }
                    nearest != null -> dialog = DialogPayload(
                        title = "Conversación",
                        body = "Hablas con un transeúnte de la ciudad. Te dice: \"${randomLine(nearest.id.hashCode())}\""
                    )
                    else -> dialog = DialogPayload(
                        title = "Nada por aquí",
                        body = "No hay nada con lo que interactuar. Acércate a una puerta o a un personaje."
                    )
                }
            }
        )

        // Tip arriba si está cerca de algo interactuable
        val front = MovementEngine.interact(state.world)
        if (front != null) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(Color(0xCCFFD166))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Pulsa A para ${front.kind.actionLabel()}", color = Color(0xFF0F1724), fontWeight = FontWeight.Bold)
            }
        }

        dialog?.let {
            com.empiretycoon.game.ui.components.MiniInfoDialog(
                title = it.title,
                body = it.body,
                onDismiss = { dialog = null },
                actionLabel = "Cerrar"
            )
        }
    }
}

private data class DialogPayload(val title: String, val body: String)

private fun com.empiretycoon.game.world.TileInteractKind.actionLabel(): String = when (this) {
    com.empiretycoon.game.world.TileInteractKind.ENTER_BUILDING -> "entrar"
    com.empiretycoon.game.world.TileInteractKind.READ_SIGN -> "leer"
    com.empiretycoon.game.world.TileInteractKind.TALK_NPC -> "hablar"
    com.empiretycoon.game.world.TileInteractKind.BUS_STOP -> "tomar el bus"
    com.empiretycoon.game.world.TileInteractKind.FAST_TRAVEL -> "viajar rápido"
    com.empiretycoon.game.world.TileInteractKind.ATM -> "operar en el cajero"
    com.empiretycoon.game.world.TileInteractKind.SHOP -> "comprar"
    com.empiretycoon.game.world.TileInteractKind.BENCH -> "descansar"
}

private fun randomLine(seed: Int): String {
    val lines = listOf(
        "He oído que la bolsa se va a desplomar mañana.",
        "Tu empresa no es lo que era. La gente comenta.",
        "Hace un día genial para construir algo grande.",
        "Cuidado con los del Polígono. Andan haciendo movidas raras.",
        "Si vas al puerto, lleva ropa caliente.",
        "El alcalde habla bien de ti últimamente.",
        "Sigue así y la ciudad será tuya.",
        "Mi primo busca trabajo. ¿Tienes algo?",
        "Dicen que en el bar de la esquina pasan cosas turbias.",
        "Hay una nueva tecnología que tienes que ver."
    )
    return lines[(seed and 0x7FFFFFFF) % lines.size]
}
