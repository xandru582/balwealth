# Auditoría: MultiCityEngine

## Bugs críticos
- [ ] L89-116 / L212-214 — Exploit arbitraje cíclico: sin validación de margen mínimo. Si NY vende alto y Tokyo compra bajo, se puede ciclar shipments y multiplicar dinero. Sin cap de envíos simultáneos por ruta.

## Bugs de balanceo / lógica
- [ ] L30-46 — Unlock condition: 1M€ + reputación ≥50. Razonable.
- [ ] L91-93 — Precios: `effectiveSellPrice()` por ciudad / `buyPriceOf()` global. Sin cálculo dinámico de spread → arbitraje plano permitido.
- [ ] L57 — Tick diario excluye HOME del drift de demandas. Comportamiento razonable.

## UX / feedback
- [ ] Sin warning visual de "esta ruta está saturada" o "el spread se ha cerrado".

## Performance
- [ ] (sin hallazgos)

## Code smell / dead code
- [ ] (sin hallazgos)

## Localización / texto
- [ ] (español-only)

## Recomendación 1-line
**P1** — Limitar shipments concurrentes por ruta (max 3). Aplicar comisión fija al envío (~2% del valor) para evitar arbitraje rentable trivial.
