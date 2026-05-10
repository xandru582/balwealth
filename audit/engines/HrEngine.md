# Auditoría: HrEngine

## Bugs críticos
- [ ] L421-433 — `refreshApplicants()` reemplaza la pool COMPLETA con `ApplicantGenerator.pool()`. No hay rotación ni expiración → comportamiento poco coherente.

## Bugs de balanceo / lógica
- [ ] L210-215 — `salaryAtLevel(profile.level)` usa cast `toInt().toDouble()`: precision loss aceptable, sin overflow.
- [ ] L77-122 — Training: ✅ correctamente bloquea double-train (L81-84 con `activeIds`).

## UX / feedback
- [ ] (sin hallazgos)

## Performance
- [ ] L464-473 — `cleanupOrphanProfiles` recorre todos los perfiles + employees → O(N). Aceptable.

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] Strings hardcoded español en notify().

## Recomendación 1-line
**P2** — En refresh applicants, rotar 50% en vez de reemplazar todo (L421-433) para que el jugador no pierda candidatos buenos.
