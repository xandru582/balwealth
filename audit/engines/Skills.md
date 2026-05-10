# Auditoría: SkillEngine + PerkEngine

## Bugs críticos
- [ ] PerkEngine.kt L49 — No hay duplicate-check. Mismo perkId puede seleccionarse múltiples veces si el flow se manipula.

## Bugs de balanceo / lógica
- [ ] SkillEngine.kt L34-37/L64-80 — Costos validados contra puntos disponibles. ✅
- [ ] SkillEngine.kt L32 — Early check `if (tree.has(skillId)) return state` previene double-unlock. ✅
- [ ] PerkEngine.kt L24-37 — Perks solo en niveles múltiplos de 5; cada selección sobrescribe `pendingChoice`. **Stacking**: si el flujo se repite sin consume, perks pueden acumular.
- [ ] SkillEngine.kt L86-125 — `aggregateEffects()` combina dinámicamente, idempotente. ✅

## UX / feedback
- [ ] (sin hallazgos)

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] PerkEngine duplica `applyOneShotEffects()` con SkillEngine (L66-84 vs L137-155).

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P1** — Añadir `unlockedPerks` set y validar duplicados en `selectPerk()`. **P2** — Extraer `applyOneShotEffects` a helper común.
