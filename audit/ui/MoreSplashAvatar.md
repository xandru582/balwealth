# Auditoría QA UX — MoreScreen / SplashScreen / AvatarScreen

## MoreScreen.kt

**Navegación y agrupación (L23–L35):** Tabs `Misiones / Inventario / Ajustes` con `rememberSaveable` — sobrevive rotación. Agrupación lógica correcta; sin embargo, `Inventario` no es una "sub-pantalla" navegable, solo un tab — no hay deep-link ni back stack propio.

**QuestsTab (L45–L49):** Orden por estado correcto (completadas primero, reclamadas al fondo). Botón "Cobrar" (L78) destacado en Emerald — buena affordance.

**InventoryTab (L92–L126):** Filtra `it.value > 0` (L93), evita ítems vacíos. Falta agrupación por `category` — todo plano. Posible mejora: secciones colapsables.

**SettingsTab (L129–L226):** Identidad, Velocidad, Datos agrupados en `EmpireCard`. Diálogo de reset (L208–L225) tiene confirmación destructiva en Ruby, correcto. **Issue:** botones de velocidad (L168–L180) en `Row` sin `horizontalScroll` ni weight uniforme — riesgo de overflow en pantallas estrechas.

## SplashScreen.kt

**Tiempo (L54):** `autoMs = 1_500L` — dentro del límite de 2s, OK.
**Tap-to-skip (L87–L95):** Implementado con flag `dismissing` (L59) que previene doble-onFinish. Correcto.
**Animaciones (L61–L75):** Logo + subtítulo paralelos, 550–700ms — fluido. `delay(autoMs)` tras animaciones podría exceder 1.5s reales (~2.2s acumulado). **Issue menor:** revisar suma.

## AvatarScreen.kt

**Wrapper delgado (L9–L18):** Delega 100% en `AvatarCustomizerScreen` (módulo `world.ui`). Persistencia vía `vm.updateAvatarLook(newLook)` (L13) — correcto patrón Save→onBack.
**Issue:** No se ve `equipment` aquí — solo `look`. La customización de equipo debe auditarse en `AvatarCustomizerScreen.kt`. No hay preview del estado actual antes de guardar visible en este wrapper.

## Prioridades
1. SettingsTab: weight en botones de velocidad.
2. Splash: medir tiempo total real ≤ 2s.
3. Avatar: confirmar persistencia de equipment en archivo delegado.
