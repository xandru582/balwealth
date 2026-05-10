# Auditoría: Performance (cross-cut hot path)

Archivos revisados:
- `engine/GameEngine.kt` (foco: `advanceOneSecond` L24-408)
- `engine/Production.kt`
- `world/render/RenderEngine.kt` (no existe `world/WorldEngine.kt`; el motor de render es el equivalente "draws scope" del mundo)

## Bugs críticos
- [ ] Ninguno bloqueante. Las degradaciones aparecen al escalar (offline progress, > 30 edificios, > 40 NPCs).

## Bugs de balanceo / lógica
- [ ] N/A en este corte (solo performance).

## UX / feedback
- [ ] El offline progress alto puede colgar UI varios segundos por culpa de los puntos abajo.

## Performance

### GameEngine.kt — `advanceOneSecond` (hot path 1Hz × speedMultiplier)
- [ ] **Cascada de `state.copy(...)`** L128-405: 30+ reasignaciones de `s2 = s2.copy(...)`. Cada una clona un `data class` enorme. Allocations × tick. Agrupar en un solo `copy` al final o mutar builder.
- [ ] **`notifications + ...takeLast(40)` repetido** L87, L92, L125, L155, L197, L257, L712, L796, L808: cada notificación crea una lista nueva y luego subconjunto. Reemplazar por `ArrayDeque<GameNotification>` con cap.
- [ ] **`state.research.completed.sumOf { TechCatalog.byId(id)?... }`** L527 (marketSell), también en `Production` L45 y `qualityResearchBonus` L54: lookup + `sumOf` cada venta y cada tick. Cachear `prodBonus`/`marketBonus` en `ResearchState` cuando se completa una tech.
- [ ] **`EventPool.pool.find { it.id == ... }`** L687 y `s2.stocks.find { it.ticker == ticker }` L564, L586: scan O(N) por acción. Construir `Map<String, Stock>` y `Map<String, Event>` en init.
- [ ] **`for (stock in s2.stocks) { holdings.shares[stock.ticker] }`** L242-247: O(stocks) cada cambio de día aunque el jugador no posea casi nada. Iterar `holdings.shares` (típicamente más pequeño) en lugar del catálogo.
- [ ] **Hooks que se ejecutan TODOS los ticks** sin guarda: `AICompanionEngine.tick` L378, `WeatherEngine.tick` L283, `TrafficEngine.tick` L294, `PetEngine.tick` L303, `FollowerEngine.tick` L308, `UfoEngine.tick` L318, `KarmaEchoEngine.tick` (cada 3). Confirmar que cada uno tiene early-exit; si no, mover detrás de un módulo `% k == 0`.
- [ ] **`buildings.find/map/filterNot`** en `buildNew/upgrade/demolish/setRecipe` L427-462: lineales pero aceptables. Solo crítico si hay > 100 edificios — `Map<String,Building>` recomendado a futuro.

### Production.kt — `advance` (1Hz)
- [ ] **`HashMap(company.inventory)`** L37: allocation completo del inventario cada tick. Si nada cambia (frecuente si no hay recetas activas), se desperdicia. Hacer copia perezosa solo al primer `inv[id] = ...`.
- [ ] **`company.employees.groupBy { ... }`** L64: ya optimizado vs. el filter previo, pero sigue siendo allocation/tick. Cachear en `Company` cuando cambian asignaciones (eventos discretos).
- [ ] **`inv.values.sum()`** L100 dentro del loop por edificio: O(M) por edificio → O(B × M). Calcular `usedCapacity` UNA vez fuera y mantener delta al consumir/producir.
- [ ] **`research.completed.sumOf { TechCatalog.byId(id)?.productionBonus }`** L45: idéntico al smell de GameEngine — cachear al completar tech.
- [ ] `recipe.outputs.values.sum()` L102 ok (mapas pequeños).

### RenderEngine.kt — `DrawScope` por frame (60Hz)
- [ ] **`Brush.radialGradient(...)` por luz, por frame** L505 dentro de `for (light in lights)`: 50 luces ⇒ 50 allocations/frame + 50 drawCalls. Comentado pero no resuelto. Cachear el `Brush` o usar atlas/baked.
- [ ] **Estrellas: `for (i in 0 until 80)` con `sin(...)`** L275-284: 80 trig + drawCircle/frame en horario nocturno. Pre-computar posiciones en lista estática y solo recalcular twinkle en GPU shader o reducir a 32 estrellas con LUT.
- [ ] **`drawClouds` / `drawBirds` / `drawGodRays`** L322-405: cada uno hace `sin/cos` × N en el path de dibujo. Aceptable, pero contribuye cuando se acumula con el depth-sort.
- [ ] **`PostFx.drawAmbientParticles`** L582-598: ya optimizado a 16 partículas — ok.
- [ ] **Depth sort implícito**: `sealed class RenderObject` con varias subclases — verificar que la lista renderable se ordena UNA vez con `.sortedBy { it.worldY + it.zBoost }` y NO en cada draw. Si es `List<RenderObject>` recreada por frame con `.map` desde estado mundo, allocation 60Hz.
- [ ] **`Vignette`/`AtmosphericFog` Brush.gradient** L551, L616, L549: allocations de Brush por frame en post-FX. Cachear Brushes constantes (no dependen de animPhase).

## Code smell / dead code
- [ ] `Production.averageSkillOfAssigned(company, b)` L153 no se invoca tras la migración a `FromMap` — eliminar.

## Localización / texto
- [ ] N/A en este corte.

## Recomendación 1-line
**P1**: el `state.copy` en cadena de `advanceOneSecond` y el `HashMap(inventory)` por tick + scan lineal en `stocks.find`/`EventPool.find` dominan el coste de offline-progress y de tick activo cuando hay > 30 edificios; cachear bonos derivados de research y mover notifications a `ArrayDeque` rinde la mejor relación esfuerzo/ganancia.
