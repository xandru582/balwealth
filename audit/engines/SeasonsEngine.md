# Auditoría: SeasonsEngine

## Bugs críticos
- [ ] L62 — `player.addXp(template.xpBonus)` aplica XP sin validar duplicación con TraitTree multiplier. Posible doble XP si la temporada lo añade y TraitTree multiplica.

## Bugs de balanceo / lógica
- [ ] L24-25 — Detección de temporada por `cycleDay` modular: lógica correcta.
- [ ] No hay lógica de previene solapamientos entre 2 temporadas si `seasonForDay()` retorna inconsistencia.
- [ ] Buffs de temporada (`activeModifiers`) declarados pero NO aplicados en este archivo. Asume que otros engines los leen — verificar.

## UX / feedback
- [ ] L71 — Cap 20 rewards históricos. ✅

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] activeModifiers declarado pero su uso real no se ve.

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P2** — Auditar si TraitTree PLAYER_XP_MUL se aplica también al `xpBonus` de seasons (posible duplicación). Verificar que `activeModifiers` tienen consumidores.
