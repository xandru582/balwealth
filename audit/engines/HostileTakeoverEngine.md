# Auditoría: HostileTakeoverEngine

## Bugs críticos
- [ ] (sin bugs críticos)

## Bugs de balanceo / lógica
- [ ] L99-101 — GOLDEN_PARACHUTE: el rival "escapa con 30% del coste" pero solo `costPaid` se resta del cash. La lógica nominal es correcta pero el flavor text dice que el rival "se lleva un % adicional" — verificar coherencia narrativa con el coste real.
- [ ] L65-71 — RNG distribución: 30/25/25/20% — ✅ correcto.
- [ ] L29-43 — `canLaunch` valida cooldown, reputación, cash. ✅

## UX / feedback
- [ ] L39-40 — Cuando el cash es insuficiente para POISON_PILL (encarece 25%), la operación cambia a WHITE_KNIGHT con refund 80% — debería avisarse al jugador antes, no como sorpresa.

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P2** — Pre-validar cash incluyendo overhead de POISON_PILL (cost × 1.25) en `canLaunch`. Mostrar coste mín/máx en UI.
