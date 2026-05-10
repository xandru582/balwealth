# Auditoría: RacingEngine

## Bugs críticos
- [ ] L799 (endOfSeason) — Championship bonus de constructor usa `company.cash` en vez de `team.budget`. Inconsistente con el refactor budget.
- [ ] L723-735 — Bonus de patrocinadores se calcula en `newActiveSponsorships` y luego se aplica DE NUEVO a `finalTeams`. Posible doble cobro al budget del equipo del jugador.
- [ ] L446 — Sponsorships expirados: el income se cobra antes de removerlos → 1 día extra gratis.

## Bugs de balanceo / lógica
- [ ] L510 — Probabilidad DNF `(100-reliability) * 0.001 * difficulty` cap 10%. Con reliability bajo + circuito difícil, DNF en cascada y todo el grid se rompe.
- [ ] L479 — Auto-tick offline: si `currentDay - lastSimulatedDay` salta varios días, ingresos/costes intermedios se silencian.

## UX / feedback
- [ ] L479 — No hay notificación de "se procesaron N días offline" — el jugador no entiende los cambios al volver.

## Performance
- [ ] L544 — Driver list (DriverPool.all) re-escaneada en cada simulación de carrera; cacheable.

## Code smell / dead code
- [ ] (sin hallazgos críticos)

## Localización / texto
- [ ] (español-only; muchas strings en notify())

## Recomendación 1-line
**P0** — Fix L799 (championship bonus al budget) y L723 (doble cobro de sponsor bonus). **P1** — DNF cap simétrico y notificación de offline jump.
