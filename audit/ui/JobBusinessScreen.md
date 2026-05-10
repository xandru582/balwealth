# Auditoría QA UX — JobBusinessScreen.kt

## Resumen
Pantalla de empresa de oficio (panadería/taller). Bifurca en `BusinessLockedView` (L64) o `BusinessActiveView` (L98) según `biz == null` (L55).

## Hallazgos

### Apertura (Locked)
- L65-67: Fee de apertura desde `JobBusinessCatalog.openingFee`. Borde Sapphire/InkBorder según affordability — feedback visual correcto.
- L78-84: Mensajes diferenciados (oficio bloqueado vs. caja insuficiente). Bien.
- L87-95: CTA "Abrir empresa" deshabilita correctamente con `enabled = unlockedJob && cash >= fee`.

### Treasury & KPIs (L110-134)
- L114-118: Treasury, Nivel local (x/5), Empleados (n/max) en Row sin `Modifier.weight` — riesgo overflow horizontal en pantallas estrechas.
- L122-125: Revenue/h y Salarios/día. Color condicional Salarios OK.
- L129-132: Lifetime stats a 10sp — legibilidad límite.

### Cobrar (L138-146)
- Botón prominente. `enabled = treasury > 0.01` evita clicks vacíos.

### Upgrade (L150-171)
- L158: Multiplicador hardcoded `1.0 + (level+1)*0.20` — duplica lógica del catálogo, riesgo desincronización.
- L162-168: Botón sin color override (no Gold) — inconsistente con Cobrar/Abrir.

### Empleados/Candidatos (L174-207)
- L243: Borde por skill (≥70 Emerald, ≥50 Sapphire). Buen indicador.
- L264: "Despedir" sin confirmación — destructivo, falta diálogo.
- L193-195: Refrescar candidatos sin coste visible ni cooldown UI.
- L280: `canHire` correcto.

### Cierre (L210-229)
- L218-220: Cálculo neto claro con signo. Sin diálogo de confirmación pese a ser irreversible — riesgo crítico UX.
