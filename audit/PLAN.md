# Plan de auditoría — 100 agentes

Cada agente cubre UN área del proyecto en read-only y deja su informe en
`audit/<categoria>/<area>.md`. Al final agregaremos todos los informes en
`audit/RESUMEN.md` con bugs/críticos/balance/UX agrupados.

## Estructura del informe (todos siguen este formato)

Cada `<area>.md` debe contener exactamente estas secciones:

```markdown
# Auditoría: <área>

## Bugs críticos
- [ ] Lista de bugs que rompen funcionalidad o pueden causar crash/ANR.

## Bugs de balanceo / lógica
- [ ] Números fuera de rango, exploits, paths inalcanzables, etc.

## UX / feedback
- [ ] Botones sin feedback, mensajes confusos, navegación atrapada, etc.

## Performance
- [ ] Hot loops O(N²), allocations en cada tick, save bloat, etc.

## Code smell / dead code
- [ ] Funciones nunca llamadas, ramas inalcanzables, TODOs.

## Localización / texto
- [ ] Strings hardcoded, español-only, typos.

## Recomendación 1-line
Una frase con la prioridad: P0 (bloquea release), P1 (rompe experiencia),
P2 (pulido), P3 (nice to have).
```

## Oleadas

- **Wave 1 (engines, ~30 agentes)**: revisar cada `engine/*.kt` por bugs,
  performance y balance.
- **Wave 2 (UI screens, ~35 agentes)**: revisar cada `screens/*.kt` por
  UX, feedback, navegación.
- **Wave 3 (models, ~20 agentes)**: revisar `model/*.kt` por
  serialización, defaults, migraciones.
- **Wave 4 (cross-cutting, ~15 agentes)**: balanceo global, performance
  end-to-end, tutorial, onboarding, save/load, localización.

Total: ~100 informes.
