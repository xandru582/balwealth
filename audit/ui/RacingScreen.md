# Auditoría QA UX — RacingScreen.kt

## Resumen
Pantalla `Formula Manager` con 8 tabs scrollables (L28-37, L64-82). Estructura sólida, con varios puntos de fricción.

## Hallazgos por área

### Tabs scrollables (alcanzables)
- L64-82: `ScrollableTabRow` con `edgePadding=0.dp` y `fontSize=11.sp` — los 8 tabs son alcanzables por scroll horizontal. OK.
- L99-100: `rememberSaveableTab` NO usa `rememberSaveable` real, solo `remember`. Se pierde el tab activo en rotación/recreación. **Bug**.

### Botones upgrade (+1 / +10)
- L322-346: Estados `enabled = canAfford && !maxed` correctos.
- L290-293: `cost` validado contra `team.budget`, no `state.company.cash`. Coherente con el modelo nuevo. OK.
- **Falta feedback +10**: el botón "+10" no comprueba si hay budget para los 10 niveles, solo para el primero (L336, `canAfford` mira `cost` actual). Usuario puede pulsar y quedarse a medias sin aviso.
- L317: Texto solo muestra coste de `+1`. Sería útil indicar coste estimado de `+10`.

### Driver / Sponsor / Staff cards
- L361-407: `DriverSlot` denso pero claro; chips `SK/CO/AG/MO` (L379-382) sin tooltip — abreviaturas opacas para nuevo usuario.
- L693-717: Sponsors activos muestran días restantes, ganado, wins/podios. Bien.
- L808-833: Staff card clara con rating + bonus %. OK.

### Race calendar — countdown
- L450-459: `nextRace()` muestra "faltan X días". OK.
- L462-495: Items diferencian past/next/future por borde y badge "PRÓXIMA". OK.

### Standings constructor & driver
- L509-547: Dos secciones separadas (Pilotos top 10 + Constructores top 10), borde `Gold` resalta equipo propio. Claro.

### Tesorería empresa↔equipo
- L218-274: Card excelente con presets `100k/1M/10M/Cash/Budget`. Botones bidireccionales con `enabled` correcto (L251, L264). OK.
- L229: Filtro `isDigit()` impide negativos/decimales — OK.

## Acciones recomendadas
1. Reemplazar `remember` por `rememberSaveable` en L100.
2. Validar coste agregado de `+10` y mostrarlo en L317.
3. Tooltip/leyenda para chips `SK/CO/AG/MO` en L379-382.
