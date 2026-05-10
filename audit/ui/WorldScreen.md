# Auditoría QA UX — WorldScreen.kt

**Archivo:** `app/src/main/java/com/empiretycoon/game/world/ui/WorldScreen.kt`
**Fecha:** 2026-05-10

## Hallazgos

### 1. Loop de animación — dt clamping (L85-95)
OK. `awaitFrame` + clamp `coerceAtMost(0.1f)` (L89) evita saltos tras pausa/background. Primer frame `dt=0f` correcto.
**Riesgo menor:** no hay clamp inferior; `dt` negativo improbable pero `coerceIn(0f, 0.1f)` sería más defensivo.

### 2. Joystick (L480-487)
**Problema:** No se ve la implementación de `VirtualJoystick` aquí, pero el contrato (`onMove`/`onRelease`) no expone deadzone ni feedback visual desde este screen. Verificar en `VirtualJoystick.kt` que exista deadzone (~0.15) y estado pressed.
**Tamaño 140dp** (L484) razonable para pulgar.

### 3. Banner de distrito (L72-82, L333-363)
**Riesgo de cuelgue:** `LaunchedEffect(state.world.currentDistrict)` con `delay(2_200L)` (L78). Si el distrito cambia dos veces rápido, la corrutina previa se cancela y `bannerDistrict` puede quedar en el valor intermedio sin limpiarse — la nueva ejecución lo sobrescribe pero si el segundo cambio re-entra al mismo valor (`lastSeenDistrict==now` falso solo la 1ª vez), el banner no se reasigna.
**Fix sugerido:** envolver en `try { ... } finally { bannerDistrict = null }`.

### 4. Diálogos (L409-432, L438-477, L571-578)
Dismiss claro vía `onDismiss` callbacks (`vm.dismissFollower`, `vm.dismissWorldEvent`, `dialog=null`). OK. **Pero:** `CompactDialog` del follower (L409) sólo aparece si `dist<2.5f` (L408); si el avatar se aleja mientras está abierto, el diálogo desaparece sin feedback — UX confusa.

### 5. Performance DrawScope (L99-330)
**Caro:** allocaciones por frame: `mutableListOf<RenderObject>` (L129), `sortedBy` (L207), `mutableListOf<PointLight>` (L229), `buildList` en cada botón de evento. A 60fps genera GC churn.
**Doble pasada de luces** (L262, L272) duplica coste — intencional para mezcla, pero costoso.
**Fix:** reusar listas con `remember { ArrayList() }.apply{clear()}`.

### 6. Cull viewport (L132-174)
Bien implementado: bbox check `originTileX±viewW` para props, vehículos, NPCs, pets. **Inconsistencia:** Pet sólo cull en X (L145), falta Y. Follower (L152) **sin cull alguno** — siempre añadido.

## Prioridad
ALTA: banner cuelgue (L78), allocations DrawScope (L129).
MEDIA: cull follower/pet (L144, L152), feedback joystick.
BAJA: clamp dt inferior (L89).
