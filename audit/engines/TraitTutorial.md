# Auditoría: TraitTreeEngine + TutorialEngine

## Bugs críticos
- [ ] (sin crashes)

## Bugs de balanceo / lógica
- [ ] **TraitTreeEngine L29-30** — Cost en `resilienceXp`. Si DisasterEngine no se ha disparado, `resilienceXp = 0` y NINGÚN trait es comprable. UX: el jugador no entiende por qué.
- [ ] **TraitTreeEngine L24-27** — Prereq lineal por tier respetado. ✅
- [ ] **TutorialEngine L112-125** — Defensa post BUG-08-07 OK.
- [ ] **TutorialEngine L124** — `if (next.tick % 1_440L == 0L) return false`: si el jugador hace MARKET_TX exactamente en tick 1440/2880/etc., el tutorial NO avanza ese tick.

## UX / feedback
- [ ] Sin tip educativo de "haz desastres para ganar Resilience XP" cuando el jugador entra al TraitTree por primera vez.
- [ ] **TutorialEngine L26** — Skip funciona correctamente.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P1** — Tip educativo al entrar TraitTree sin Resilience XP. **P2** — Cambiar boundary check L124 a `prev.tick % 1_440L != next.tick % 1_440L`.
