# Auditoría: AICompanionEngine

## Bugs críticos
- [ ] (sin bugs)

## Bugs de balanceo / lógica
- [ ] L132/L142/L152/L168/L182/L195/L211/L223/L233/L243/L254 — 10 heurísticas cubren situaciones críticas (nóminas vencidas, reputación baja, deuda, energía baja, etc.). ✅ Aporta valor real.
- [ ] L83 — `tick()` analiza solo cada 300 ticks (5 min in-game). Frecuencia razonable, no spam.
- [ ] L31 — Cap de 6 tips simultáneos.
- [ ] L34 — TTL 2 días para que no se acumulen tips viejos.

## UX / feedback
- [ ] L324-339 — Personalidad textual con firma contextual: añade variedad sin caer en plantilla.
- [ ] L349 — Heurística de nóminas con offset 800 ticks hardcoded. Debería ser configurable.

## Performance
- [ ] L276-278 — Filter + sort por prioridad, O(K) con K=10. Aceptable.

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] Tips en español-only.

## Recomendación 1-line
**P3** — Sistema sólido y útil. Solo nice-to-have: extraer constantes hardcoded a top del archivo.
