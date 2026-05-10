# Auditoría: ManagerEngine

## Bugs críticos
- [ ] L282 — Auto-fire `loyalty <= autoFireBelowLoyalty` filtra cualquier empleado sin validar si es clave. Riesgo de despedir a un C-level que sea irreemplazable.

## Bugs de balanceo / lógica
- [ ] L292-298 — Auto-asignar prioriza por `level desc` y filtra WAREHOUSE: ✅. Pero solo asigna 1 trabajador por tick → en escenarios con 50 plazas vacías, tarda 50 ticks (~50s reales) en llenar todo.
- [ ] L121 — `efficiency` cap 2.0; sin decay → estable, no bloquea. ✅

## UX / feedback
- [ ] No hay logs visibles de qué hicieron los managers automáticamente. El jugador puede pensar "¿quién me despidió a Juan?".

## Performance
- [ ] L286 — Filter sobre `company.employees` (puede ser N=1000+) por cada manager activo cada tick → con varios managers, O(K×N).

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (sin strings hardcoded relevantes)

## Recomendación 1-line
**P1** — Auto-asignar más de 1 trabajador por tick (loop hasta agotar empleados libres). **P2** — Log "Manager Mary fired Juan (loyalty 0.15)" en feed.
