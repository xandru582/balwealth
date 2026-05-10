# 🚀 BalWealth — Plan de mejoras "infinite"

> Versión actual: **v17-infinite** · Objetivo: convertir BalWealth en un *idle-tycoon-RPG-life-sim* que se pueda jugar **cientos de horas** sin tocar fondo.

---

## 🧭 Filosofía del rediseño

El juego ya era enorme antes de v17 (24 motores, 33 pantallas, 169 archivos Kotlin). El problema **no** era la falta de contenido — era la falta de **bucles** y de **rol del jugador**:

| Loop | Duración típica | Estado |
|------|-----------------|--------|
| Producción → vender → reinvertir | minutos | ✅ sólido |
| Investigación → desbloquear recetas | horas | ✅ sólido |
| Campeonato F1 (16 carreras) | 1 semana in-game | ✅ sólido |
| Riesgo / drama / crisis | — | ✅ DisasterEngine |
| Meta-progresión NG+ | renacer | ✅ Prestige + DisasterEngine resilience XP |
| Volatilidad financiera | mercado/news | ✅ CryptoEngine |
| Retos limitados-temporales | — | ✅ DailyChallengeEngine |
| Heists / atracos / golpes | — | ✅ HeistEngine |
| Asistente que guía | — | ✅ AICompanionEngine |
| Comercio global | — | ✅ MultiCityEngine |
| Mini-juegos arcade | — | ✅ ArcadeEngine (Snake jugable) |
| Temporadas / festivales | — | ✅ SeasonsEngine |
| **Oficios jugables (rol del jugador como persona)** | — | 🚧 PRÓXIMA TANDA |
| Talents permanentes | — | 🟡 planeado (TraitTreeEngine) |
| Adquisiciones agresivas | — | 🟡 planeado (HostileTakeoverEngine) |
| Empresas de oficios + contratación especializada | — | 🟡 planeado |

---

## ✅ Sistemas implementados en v17-infinite

### Tanda 1 (commit `cf04ca9`) — 4 sistemas base
- ✅ **CryptoEngine** — 6 tokens, rugpulls, staking, mining (373 LOC)
- ✅ **DisasterEngine** — 12 desastres, insurance, mitigación 5-strategy (263 LOC)
- ✅ **DailyChallengeEngine** — 3 retos diarios + 1 semanal con racha ×5 (322 LOC)
- ✅ **HeistEngine** — 8 heists, tripulación, heat, 4 outcomes (280 LOC)

### Tanda 2 (commits `a22ef41` + `d820a8f`) — QA Round 3
- ✅ 4 bugs críticos arreglados (DRIVE_DISTANCE, sell silencioso, stake validation, coerceIn crash)
- ✅ 8 bugs importantes arreglados (wasEverBought, assignMiners, cooldown defensive, plan extract, crew validation, etc.)

### Tanda 3 (commit `a1aa04c`) — AICompanion
- ✅ **AICompanionEngine** — 5 personalidades, 11 heurísticas, mood meter, sliding-window dismiss (819 LOC totales)

### Tanda 4 (commit `9bc48c9`) — Comercio internacional
- ✅ **MultiCityEngine** — 5 ciudades, FX, aranceles, rutas, envíos en tránsito (1057 LOC totales)

### Tanda 5 (commits `29028ee` + `5a1ca21`) — Render improvements + perf
- ✅ Bloom PCF en luces nocturnas (LightingEngine)
- ✅ Agua con olas sinusoidales animadas (TileRenderer.drawWater)
- ✅ Sombras suaves PCF 3-tap (ShadowEngine)
- ✅ Optimizaciones perf: bloom outer-layer eliminado, partículas ambient 32→16, water gradient bands 3→1

### Tanda 6 (commit `129216e`) — Arcade + Seasons
- ✅ **ArcadeEngine** — modelo + engine + UI hub + 🐍 Snake completamente jugable. 4 más como stub "próximamente" (1315 LOC totales)
- ✅ **SeasonsEngine** — ciclo 30 días con 4 temporadas + temporada baja, modificadores globales, recompensas únicas

### Tanda 7 (commits `aa009c4`, `932d1f0`) — Jobs framework + map polish
- ✅ **Jobs framework**: 40 oficios catalogados, JobsState, JobsEngine (workShift instantáneo + workShiftWithScore para mini-juegos), JobsScreen con hub colapsable por categoría.
- ✅ **Map fixes**: tráfico restringido a tiles ROAD + despawn de off-road, prop placement con allow-list explícita por PropKind, 5 easter eggs nuevos (estatua que susurra, helado cósmico, OVNI de día, mensaje en botella, reloj a las 3:14).

### Tanda 8 (commits `d724382`–`3dc4153`, `ecca259`–`17f2d6a`) — Mini-juegos + TraitTree
- ✅ **TraitTreeEngine**: 60 talentos en 5 ramas, prereq lineal, coste en Resilience XP, UI con tabs por rama. Otros engines pueden leer multiplicadores via `state.traitTree.multiplierFor(type)`.
- ✅ **14 mini-juegos jugables** (de 40 totales):
    - 🚓 Policía (tap-reaction sospechosos)
    - 🚒 Bombero (apagar fuegos en grid 4×4)
    - 🥖 Panadero (timing del horno con barra de tueste)
    - 👨‍🍳 Chef (cola de tickets con receta)
    - 🚕 Taxista (pickup + dropoff en grid 6×4)
    - 🔧 Mecánico (identifica pieza averiada)
    - 💻 Programador (encuentra el bug)
    - 🦸 Detective (find-the-clue en grid 5×5)
    - 🥊 Boxeador (esquiva direccional rítmica)
    - 🌊 Pescador (agotando peces)
    - ⚽ Futbolista (penaltis vs portero)
    - 🎮 Streamer (secuencia de colores)
    - 🎨 Pintor (traza puntos en orden)
    - 💊 Farmacéutico (combina viales sin orden)
- 🚧 **26 mini-juegos pendientes** (siguientes tandas): Paramédico, K-9, Médico, Dentista, Profesor, Bibliotecario, Actor, Pizzero, Barista, Heladero, Albañil, Carpintero, Electricista, Fontanero, Camionero, Piloto avión, Maquinista, Piloto carreras, Cartero, Basurero, Jardinero, Veterinario, Granjero, Diseñador UI, Director cine, Ilusionista.

---

## 🚧 PRÓXIMA TANDA: completar oficios + empresas

**Objetivo grande**: 40 oficios jugables donde el jugador controla a su personaje haciendo el trabajo (no la empresa). Cada uno con su mini-juego + posibilidad de montar empresa de ese oficio.

**Estado actual: 14/40 implementados** (ver Tanda 8 arriba). Para los 26 restantes bastará con:
- Crear su archivo `ui/screens/jobs/{Job}JobScreen.kt` siguiendo el patrón de los ya hechos.
- Activar el `JobId` en `Jobs.miniGameImplemented` + `JobsScreen.when`.
- Idealmente reciclar mecánicas existentes (rhythm, tap-secuencia, find-the-thing, timing-bar) con datos distintos para acelerar el ritmo.

### 📋 Plan de implementación incremental

Cada bloque es un commit independiente. **Empezamos por el framework**, luego oficios uno a uno.

#### Fase A — Framework (1 commit)
- `model/Jobs.kt`: JobId enum, JobShift, JobSkillLevel, JobStats
- `engine/JobsEngine.kt`: clockIn, clockOut, finishShift, levelUp
- `ui/screens/JobsScreen.kt`: hub con todos los oficios (la mayoría locked al principio)
- Wiring en GameState/GameEngine/VM/Root
- **Sin** mini-juegos jugables todavía — solo el sistema de "trabajar X horas y cobrar Y".

#### Fase B — Oficios MVP (1 commit por oficio recomendado o agrupados de 2-3)

| # | Oficio | Categoría | Mini-juego | Prioridad |
|---|---|---|---|---|
| 01 | 🚓 Policía | Emergencias | Tap-reaction sospechosos | ⭐⭐⭐ |
| 02 | 🚒 Bombero | Emergencias | Drag&drop mangueras + timer | ⭐⭐⭐ |
| 03 | 🚑 Paramédico | Emergencias | QTE de RCP | ⭐⭐ |
| 04 | 🦮 K-9 Officer | Emergencias | Rhythm órdenes al perro | ⭐ |
| 05 | 👨‍⚕️ Médico | Sanidad | Precision-tap puntos del paciente | ⭐⭐⭐ |
| 06 | 🦷 Dentista | Sanidad | Timing-based tap | ⭐⭐ |
| 07 | 💊 Farmacéutico | Sanidad | Combinar viales en orden | ⭐⭐ |
| 08 | 👨‍🏫 Profesor | Educación | Trivia con tiempo | ⭐⭐ |
| 09 | 🎨 Pintor | Educación | Trazos en orden (memory) | ⭐⭐ |
| 10 | 📚 Bibliotecario | Educación | Sort puzzle por categorías | ⭐ |
| 11 | 🎭 Actor | Educación | Secuencia de gestos | ⭐ |
| 12 | 🥖 Panadero | Restauración | Timing horno + balance | ⭐⭐⭐ |
| 13 | 👨‍🍳 Chef | Restauración | Cola tickets prioridad | ⭐⭐⭐ |
| 14 | 🍕 Pizzero | Restauración | Estirar masa + toppings | ⭐⭐ |
| 15 | ☕ Barista | Restauración | Drag patterns latte art | ⭐⭐ |
| 16 | 🍦 Heladero | Restauración | Stack + balance bolas | ⭐ |
| 17 | 🔨 Albañil | Construcción | Tetris de ladrillos | ⭐⭐ |
| 18 | 🪚 Carpintero | Construcción | Cortes precisos swipe | ⭐⭐ |
| 19 | 🔧 Mecánico | Construcción | Identificar pieza rota | ⭐⭐⭐ |
| 20 | ⚡ Electricista | Construcción | Puzzle cables sin corto | ⭐⭐ |
| 21 | 🚿 Fontanero | Construcción | Pipe-puzzle | ⭐⭐ |
| 22 | 🚕 Taxista | Transporte | Pickup + dropoff ruta corta | ⭐⭐⭐ |
| 23 | 🚚 Camionero | Transporte | Parking inverso | ⭐⭐ |
| 24 | ✈️ Piloto avión | Transporte | Balance palanca despegue | ⭐⭐ |
| 25 | 🚂 Maquinista | Transporte | Timing paradas | ⭐ |
| 26 | 🏎️ Piloto carreras | Transporte | Drift + timing (link a Formula Manager) | ⭐⭐⭐ |
| 27 | 📮 Cartero | Públicos | Encontrar buzones en mapa | ⭐⭐ |
| 28 | 🚛 Basurero | Públicos | Tap timing contenedores | ⭐ |
| 29 | 🌳 Jardinero | Públicos | Trim + water sequence | ⭐ |
| 30 | 🐶 Veterinario | Animales | Examina + diagnostica | ⭐⭐ |
| 31 | 🌾 Granjero | Animales | Tap rhythm sembrar+regar+cosechar | ⭐⭐ |
| 32 | 🌊 Pescador | Animales | Tug-of-war con peces | ⭐⭐⭐ |
| 33 | 💻 Programador | Tecnología | Encontrar bug en código | ⭐⭐⭐ |
| 34 | 📱 Diseñador UI | Tecnología | Snap-to-grid wireframe | ⭐⭐ |
| 35 | 🎮 Streamer | Tecnología | Speed-tap react vs IA | ⭐⭐ |
| 36 | ⚽ Futbolista | Deporte | Tap+drag chutar penaltis | ⭐⭐⭐ |
| 37 | 🥊 Boxeador | Deporte | Dodge + counter rhythm | ⭐⭐ |
| 38 | 🎬 Director cine | Deporte | Gestiona crew + timing tomas | ⭐ |
| 39 | 🦸 Detective | Especiales | Find-the-clue en escena | ⭐⭐⭐ |
| 40 | 🧙 Ilusionista | Especiales | Secuencia cartas bajo presión | ⭐⭐ |

**Sugerencia de orden de implementación** (priorizando alto-impacto + variedad de mecánicas):
1. Framework (Fase A)
2. 🚓 Policía + 🚒 Bombero (mecánicas reactivas distintas)
3. 🥖 Panadero + 👨‍🍳 Chef (restauración timing)
4. 🚕 Taxista + 🔧 Mecánico (transporte + reparación)
5. 💻 Programador + 🦸 Detective (puzzle / find-the-clue)
6. 🌊 Pescador + ⚽ Futbolista (mini-juegos físicos)
7. … resto

#### Fase C — Empresas de Oficios (1 commit)
- `model/JobBusiness.kt`: enum por oficio, empleados especializados, producción offline.
- `engine/JobBusinessEngine.kt`: comprar local, contratar, asignar empleado a turno, cobrar al cierre.
- Integración con `HrEngine`: candidatos especializados por oficio (panadero con skill `baking`, detective con skill `investigation`…).
- UI: dentro del JobsScreen, botón "Montar tu propia [oficio]" cuando alcanzas nivel X en el oficio.

---

## 🟡 Sistemas planeados después de Jobs

### 🧬 TraitTreeEngine — Talentos permanentes
- 60 traits en 5 ramas (Magnate, Visionario, Político, Artista, Outlaw).
- Se desbloquean con XP de Resilience (DisasterEngine ya emite eso) + acciones específicas.
- Permanentes a través del prestigio.

### ⚔️ HostileTakeoverEngine — Adquisiciones agresivas
- Comprar acciones de empresa rival hasta 51% → la absorbes.
- Defensa: poison pill, white knight, golden parachute.
- Integración con `RivalEngine` existente.

### 🗺️ Mapa "with sense" (post commits Jobs)
Quejas reportadas por usuario al jugar v17:
- Edificios sin etiqueta (no se sabe qué son).
- Coches por agua / acera (tráfico no respeta tiles ROAD).
- Props colocados sin lógica de zona.
- Falta de easter eggs / curiosidades.

Plan:
- Etiquetas flotantes sobre cada edificio (drawText con nombre + nivel).
- `TrafficEngine.tick` con check `tile.kind == ROAD` antes de mover.
- `CityPropsGenerator` con reglas estrictas por banda de distrito.
- 5-10 easter eggs (estatuas con texto al pisar, OVNI raro de día, NPC vendedor de helados…).

---

## 🎯 KPIs de éxito (revisados)

Antes de llamarlo "infinite":

- [x] Una sesión típica dura **>2 horas** sin sentirse repetitiva (cumplido con AICompanion + Crypto + Disasters).
- [x] Cada **20 min** ocurre algo *no rutinario* (DisasterEngine + DailyChallengeEngine + Heists).
- [x] La curva de poder permite **NG+ × 10** sin romper la economía.
- [x] El jugador tiene **3+ paths** distintos (limpio, gris, criminal) — Heists + Crypto rugs + Karma echo.
- [x] Un día in-game (24 min reales) genera **>5 decisiones interesantes**.
- [ ] Existe rol del jugador "como persona": oficios jugables → 🚧 próxima tanda.
- [ ] Mapa pulido sin glitches (coches en agua, props sin sentido) → 🚧 próxima tanda.

---

## 🛠️ Decisiones técnicas (sin cambios)

- **No** romper el contrato `GameState -> GameState` — engines puros.
- **No** mover archivos existentes — solo añadir.
- Todo `@Serializable` con defaults para que **saves antiguos sigan cargando**.
- Cada engine tickea con un período propio (crypto: 10s, disaster: 1440 ticks, daily: 1440, heist: on-demand, multicity: 1440, seasons: 1440).
- UI: una pantalla nueva por sistema, integrada en el menú "Más".
- Render: solo primitivas Compose Canvas, sin shaders GLSL, sin assets externos.

---

*Última actualización: 2026-05-10. v17-infinite con 8 sistemas implementados + render improvements + perf. Próximo objetivo: framework de Jobs + 40 oficios + empresas.*
