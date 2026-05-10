# Auditoría: PrestigeEngine

## Bugs críticos
- [ ] (sin crashes)

## Bugs de balanceo / lógica
- [ ] L80-82 — Prestige level/points sin cap. En run muy largos, el multiplicador puede crecer infinitamente y romper la economía global.
- [ ] L18 — Fórmula `ln(1 + cash/1e6) * sqrt(buildings + 1) * (1 + level/20)` correcta pero no testeada con valores extremos (cash = 1e15, buildings = 100).
- [ ] L93-103 — Reset borra cash, inventory, buildings, market, research, real estate, stocks, quests. ✅
- [ ] L77-92 — Preserva prestige points/level, achievements (excluyendo internos `__`), traits (vía TraitTreeState que NO se resetea aquí).

## UX / feedback
- [ ] Falta preview claro de "qué pierdes y qué conservas" antes de hacer rebirth.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] L89-90 — Marker `__perks_applied` cleared para reaplicar perks. Lógica idempotente, OK.

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P2** — Cap de prestige level a 50-100 con notificación. Diálogo de confirmación pre-rebirth con preview de cambios.
