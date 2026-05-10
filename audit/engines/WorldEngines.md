# Auditoría: World Engines

Fecha: 2026-05-10
Alcance: motores de mundo vivo (NPCs peatonales, tráfico, clima, eventos).

## Archivos auditados

- `app/src/main/java/com/empiretycoon/game/world/NpcWorld.kt` (contiene `NpcWorldEngine`)
- `app/src/main/java/com/empiretycoon/game/world/LiveWorld.kt` (contiene `WeatherEngine`, `TrafficEngine`, `WorldEventCatalog`)
- `app/src/main/java/com/empiretycoon/game/world/MoreWorldEvents.kt` (catálogo extendido)
- Nota: no existen `NpcWorldEngine.kt`, `TrafficEngine.kt`, `WeatherEngine.kt` ni `WorldEventEngine.kt` como archivos separados — todo agrupado en `LiveWorld.kt` + `NpcWorld.kt`.

## Hallazgos

### 1. Pathfinding stuck (NpcWorldEngine)
- `NpcWorld.kt:64-87` — `tick()` calcula `nx/ny` por órbita coseno/seno; si el tile órbita está bloqueado, `targetX = w.x` (línea 77) y `targetY = w.y` (línea 78), pero `walkPhase` sigue avanzando. El walker queda **clavado en la home** sin recalcular `homeX/homeY` ni intentar fallback. Severidad media: 12 NPCs por defecto, no rompe juego pero rompe inmersión.
- No hay detección de "stuck N ticks" ni reinicio de home.

### 2. Pathfinding stuck (TrafficEngine)
- `LiveWorld.kt:120-173` — al no encontrar perpendicular válida hace U-turn (línea 167). Bien.
- Riesgo: si el coche está en una **carretera de 1 tile entre obstáculos**, U-turn solo invierte dirección y al siguiente tick puede repetir el ciclo. No hay contador anti-loop.

### 3. Populate cap
- `NpcWorld.kt:39-61` — `target=12` walkers, `attempts < target*20` (240 tries). Razonable.
- `LiveWorld.kt:86-118` — `target=18` vehículos, `attempts < 800`. Razonable.
- Falta: ambos no escalan con tamaño de grid; mapas grandes quedarán vacíos.

### 4. Despawn / cleanup
- `LiveWorld.kt:134-138` — `mapNotNull` despawnea coches off-road. Correcto. Repobla en siguiente `ensurePopulated`.
- `NpcWorld.kt` — **NO hay despawn** de walkers fuera de tiles caminables (línea 77-78 solo bloquea movimiento). Walkers atrapados nunca se reciclan.
- `WorldEventState`: `EXPIRE_TICKS=600` (`LiveWorld.kt:215`). No veo aquí la lógica que aplique la expiración — probablemente vive en el reducer/ViewModel; verificar fuera.

### 5. Performance per tick
- `NpcWorld.kt:65` — `state.walkers.map { ... }` crea lista nueva cada tick. 12 walkers OK; con 100+ asignaría GC presión.
- `LiveWorld.kt:134` — `mapNotNull` igual; `nextTile` consulta `grid.tileAt` 3 veces por vehículo (líneas 151, 162, 163). Aceptable a 18 coches.
- `NpcWorld.kt:91-95` — `nearest()` hace `map` + `filter` + `minByOrNull` (3 pasadas, allocs). Mejor `minByOrNull` directo con filtro de radio.
- `WeatherEngine.tick` (`LiveWorld.kt:34-52`) — O(1), sin coste relevante.

## Recomendaciones (priorizadas)

1. **NpcWorldEngine**: añadir `stuckTicks` por walker; tras umbral, re-rollear `homeX/homeY` sobre tile caminable o despawnear (`NpcWorld.kt:64-87`).
2. **TrafficEngine**: contador anti-bucle U-turn (`LiveWorld.kt:164-168`); tras 2 U-turns consecutivos, despawnear.
3. **Populate**: escalar `target` con `grid.width*grid.height` o densidad configurable.
4. **`nearest`**: simplificar a un solo `minByOrNull` con guard de radio (`NpcWorld.kt:90-96`).
5. Verificar que `WorldEventState.EXPIRE_TICKS` (`LiveWorld.kt:215`) se aplica realmente en el reducer.
