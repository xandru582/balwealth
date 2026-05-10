# Auditoría: QualityEngine + NPCEngine

## Bugs críticos
- [ ] (sin crashes)

## Bugs de balanceo / lógica
- [ ] **QualityEngine L24-41** — `rollQuality()` recibe `rng: Random` parámetro explícito. Determinismo depende del caller. ✅
- [ ] **NPCEngine** — Solo gestiona relationship mechanics (friendship, encounters, gifting). NO incluye pathfinding, movement, stuck detection — eso vive en NpcWorldEngine (separado).
- [ ] **NPCEngine L157-166** — `maybeRandomEncounter()` con 18% probability/tick: con `NPCCatalog.all` grande, `.random()` es costoso.

## UX / feedback
- [ ] **NPCEngine L67/L96/L176** — Notificaciones limitadas a `takeLast(40)`: sin growth.

## Performance
- [ ] **NPCEngine L157** — Reemplazar `.random()` sobre lista por índice random + acceso directo.

## Code smell / dead code
- [ ] No hay cleanup en `state.npcs.known` map para relaciones obsoletas.

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P3** — Quality Engine sólido. NPCEngine: optimizar `maybeRandomEncounter` y añadir cleanup de `known` map cada N días.
