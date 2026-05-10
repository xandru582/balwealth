# Auditoría: Production

## Bugs críticos
- [ ] L128 — Race condition en auto-restart: consume inputs sin re-validar tras el ciclo anterior. Si dos ticks consecutivos reducen inventario, puede consumir más de lo existente.
- [ ] L86-95 — Sin tope superior de progreso. Con `skill 2.2 × prodBonus 1.3 × traitProductionMul × happinessFactor` puede dispararse y completar varios ciclos en un tick.

## Bugs de balanceo / lógica
- [ ] L57-58 — `happinessFactor` solo penaliza (<50). No hay bonus por felicidad alta (>50). Asimetría de incentivo.
- [ ] L102 — `freeSpace` calculado con inventario actual; si dos edificios completan en el mismo tick, ambos ven el mismo espacio → overflow potencial.

## UX / feedback
- [ ] L137-139 — Almacén lleno: edificio queda en loop (progress capped en `recipe.seconds`) sin aviso al jugador.
- [ ] L123-125 — Auto-restart desactivado por falta de inputs sin notificación.

## Performance
- [ ] L60-65 — ✅ FIX aplicado (groupBy pre-computado). O(N) total + O(1) lookup.
- [ ] L45 — `TechCatalog.byId()` llamado N veces dentro del bucle (N = research.completed.size) → cacheable.

## Code smell / dead code
- [ ] L153-158 — `averageSkillOfAssigned(company, b)` (versión vieja) ya no se llama desde el bucle principal — eliminar o documentar.

## Localización / texto
- [ ] (sin strings hardcoded en este archivo)

## Recomendación 1-line
**P1** — Validar inputs antes de auto-restart (L128); notificar "Almacén lleno" cuando freeSpace < outputsSum (L137).
