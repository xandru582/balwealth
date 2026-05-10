# Auditoría Cross-cut: Navegación (Root.kt)

Fuente: `app/src/main/java/com/empiretycoon/game/ui/Root.kt`

## 1. Estructura de navegación

- **Bottom-nav** con 5 tabs: Home, City, Empire, Market, More (L47-57, L240-275).
- **Drawer**: NO existe drawer real; "Más" abre `MoreMenuScreen` (LazyColumn) que lista 39 sub-pantallas (L67-107, L379-431).
- El comentario L40-46 promete un "Drawer" pero implementación es una lista scrollable -> documentación inconsistente.

## 2. Pantallas alcanzables

- **Tabs raíz**: Home, City (`WorldScreen`), Empire, Market, More (L297-317).
- **39 SubScreens** enrutadas en `SubScreenHost` (L455-498) por `id` String.
- Riesgo: `when(id)` SIN `else` (L455) -> id desconocido => pantalla en blanco silenciosa. Falta fallback.
- `WorldScreen` recibe lambda que distingue MainTab vs SubScreen (L299-311); buen patrón de cross-linking.

## 3. Deep links

- **Ausentes**: ningún `NavController`, intent-filter ni mapeo URI. Navegación 100% en estado Compose. Bloquea notificaciones, share-to-screen y testing E2E por URI.

## 4. Back button handling

- `BackHandler` global (L186-192): si `subScreen != null` -> cierra sub; si tab != Home -> vuelve Home; si Home raíz -> sale app.
- Correcto y minimalista, pero **pierde historial**: ir Home->Market->sub->back NO retorna a Market sino a Home (no hay back-stack).
- `OnboardingDialog` y `EventDialog` con `dismissOnBackPress=false` (L530, L574) -> back queda capturado por diálogos modales antes que por `BackHandler`. OK.

## 5. Agrupación lógica

- Bottom-nav mezcla geografía (City), gestión (Empire) y comercio (Market) razonablemente, pero "More" agrupa 39 ítems heterogéneos sin secciones (Patrimonio, RRHH, Casino, Logros, Ajustes...). Falta categorización visual / collapsing headers en L402.
- "Patrimonio" prometido en doc-comment (L42) está dentro de More (`wealth`, L73), no como tab raíz -> friction alta.

## 6. rememberSaveable de tabs

- `currentTab` con `rememberSaveable` (L181) — sobrevive rotación/proceso. OK.
- `subScreen` con `rememberSaveable` (L182) — OK.
- `splashShown` (L121) y `lastMilestoneIdx` (L168) también persisten — correcto.
- **NO se persiste scroll position** del `LazyColumn` de `MoreMenuScreen` (L380) ni de sub-pantallas; cada visita reinicia scroll.

## 7. Hallazgos prioritarios

1. Sin back-stack real (UX P1).
2. Sin deep links (P2).
3. `when` sin `else` en `SubScreenHost` (P2 robustez).
4. 39 ítems sin agrupar en More (P2 UX).
5. Doc-comment desincronizado: "Drawer" inexistente (P3).
