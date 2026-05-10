# Auditoría: RivalEngine

## Bugs críticos
- [ ] L46-52 — Defeat rewards: aplica cash + XP + reputación cuando `playerCash >= rival.cash`. **POTENCIAL DUPLICACIÓN** con `HostileTakeoverEngine`: si el jugador absorbe al rival mediante takeover, ambos engines pueden marcarlo como derrotado y dar rewards 2x.

## Bugs de balanceo / lógica
- [ ] L29-38 — `checkChallenges` detecta correctamente cuando cash supera al rival. ✅
- [ ] L102-108 — `pickNextChallenge`: filtra no derrotados, ordena por cash asc, retorna el más barato. ✅
- [ ] L18-22 — `ensureInitialized` con check de roster vacío. ✅

## UX / feedback
- [ ] L77-89 — Trash talk con cooldown via `lastTrashTalk`. Sin spam.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P0** — `checkChallenges` debe verificar si el rival ya está en `defeated` (puesto por HostileTakeoverEngine) antes de aplicar rewards (L46).
