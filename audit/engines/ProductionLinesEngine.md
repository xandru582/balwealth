# Auditoría: ProductionLinesEngine

## Bugs críticos
- [ ] L71-72 — `toggle()` solo cambia el flag `enabled`. NO libera empleados ni resetea `progressSeconds`. Empleados quedan asignados como "fantasmas" bloqueando otras recetas.
- [ ] L138-168 — Flujo de input solo autocompra para el PRIMER edificio. Etapas intermedias dependen de outputs anteriores; si se llena el inventario en una etapa, todo se bloquea sin notificación.

## Bugs de balanceo / lógica
- [ ] L126-127 — `adjustBalancing()` fuerza `autoRestart = true`, anulando configuración manual del jugador.
- [ ] L108 — Cambio de receta resetea `progressSeconds = 0.0`: se pierde trabajo en curso.

## UX / feedback
- [ ] Sin feedback de "etapa N bloqueada por inventario lleno".
- [ ] L117-151 — Multiplicadores de balancing (1×/4×/2×) son cosmética: no afectan logística real.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P0** — Toggle off debe liberar empleados (L71). **P1** — Notificar etapas bloqueadas. **P2** — Quitar override de autoRestart.
