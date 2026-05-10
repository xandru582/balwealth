# Audit: World 2D Render Performance

**Scope:** `world/ui/WorldScreen.kt`, `world/render/*.kt`, `world/sprites/*.kt`
**Severity legend:** P0 frame killer · P1 measurable · P2 polish

## P0 — Per-frame allocations en draw loop

### 1. `mutableListOf` por frame para depth sort
- `WorldScreen.kt:129` `val objects = mutableListOf<RenderObject>()` — nuevo ArrayList cada frame.
- `WorldScreen.kt:207` `val sorted = objects.sortedBy { it.worldY + it.zBoost }` — segundo ArrayList + lambda boxing + comparator alloc.
- `WorldScreen.kt:229` `val lights = mutableListOf<PointLight>()` — tercera lista por frame en horas oscuras.
- A 60fps con N≈props+npcs+vehicles (≈80) eso son ~3 ArrayLists * 60 = 180 allocs/s + GC. **Fix:** mover a `remember { ArrayList() }` con `clear()` + sort in-place vía `Collections.sort` con Comparator cacheado.

### 2. Doble pass de luces (capa duplicada literal)
- `WorldScreen.kt:261-263` y `WorldScreen.kt:271-273`: `LightingEngine.drawLights(lights, ambient, size)` se invoca **dos veces** por frame en horas con `ambient < 0.7f`. Cada call hace 3 `drawCircle` por luz, uno con `Brush.radialGradient` recién alocado (`RenderEngine.kt:505`). Con 50 luces × 2 pases = **100 radialGradients/frame**. **Fix:** dibujar UNA vez con BlendMode.Plus después del velo, eliminar el primer pase.

### 3. Velo nocturno + pre-pase de luces redundantes
- El comentario `WorldScreen.kt:264` reconoce el truco "se mezcla", pero el primer pase pre-velo aporta 0 al resultado visible (Multiply del velo borra Screen). Pure waste.

## P0 — Bloom/shadow/gradient allocation por objeto/frame

### 4. `Brush.radialGradient` en hot path
- `RenderEngine.kt:505-512` un radialGradient nuevo por luz por frame (descrito como "optimizado" pero sigue alocando).
- `RenderEngine.kt:551` vignette gradient cada frame.
- `RenderEngine.kt:616` fog vertical gradient cada frame.
- TileRenderer/CarSprites/PropSprites: ~15 `Brush.verticalGradient(listOf(...))` por sprite (e.g. `TileRenderer.kt:66, 178, 255, 311, 382, 456, 658, 765` y `CarSprites.kt:151..325`). Con viewport ≈ 18×18 tiles + N coches/props son **cientos de Brush + listOf por frame**. **Fix:** cachear Brush por (color,size) en un map estático o usar SolidColor donde el degradé sea sutil.

### 5. Sombras con 3-tap PCF por objeto
- `RenderEngine.kt:452-470` 3 `drawArc` por objeto sombreado. Con todos los sorted (avatar+npcs+vehicles+props) ≈ 60 objs × 3 = **180 drawArc/frame** solo en sombras. **Fix:** 1 sola elipse para objetos lejos del avatar (LOD).

## P1 — Particle bloat y SkyEngine

### 6. SkyEngine repinta TODO cada frame
- `RenderEngine.kt:233-312` redibuja gradient + sol(5 circles) + 6 nubes(4 ovals c/u = 24 arcs) + 80 estrellas + luna(5) + 5 god rays. Cielo es estático salvo `animPhase`. **Fix:** cachear sky en offscreen layer, refrescar cada N frames o solo cuando `hour` cambie.
- `drawClouds` `RenderEngine.kt:322-349` 6×4 = 24 `drawArc` por frame.
- `drawAmbientParticles` `RenderEngine.kt:582-598` ya optimizado a 16, ok.

### 7. WeatherOverlay + WeatherFx solapan
- `WorldScreen.kt:302` + `WorldScreen.kt:303-305` dos pases de clima consecutivos.
- `LiveSprites.kt:82` `Random(seed)` reinstanciado cada frame para gotas — alloc + reset costosa.

## P1 — MapLabels Paint allocation

### 8. `Paint()` x3 cada frame
- `MapLabels.kt:54, 67, 80` tres `android.graphics.Paint()` con `setShadowLayer` por frame. Loop `CityBlueprint.places` (todas, sin pre-cull antes del Paint setup). **Fix:** `remember`/static singletons + densityScale param.

## Resumen de fix prioritario
1. Cachear `objects/lights` listas (P0, ~3 ArrayList/frame).
2. Eliminar doble drawLights (P0, -50% allocs nocturnas).
3. Sky offscreen cache (P1, -100 ops/frame).
4. Paint() static en MapLabels (P1).
