# 🏢 BalWealth Studio — Estructura de 30 personas

> Compañía interna que desarrolla **BalWealth**, un juego tycoon de simulación
> económica con mundo 2D explorable estilo Pokemon. Objetivo: que sea
> innovador y visualmente espectacular.

---

## ▍Dirección (3)

| # | Rol | Responsabilidad |
|---|---|---|
| 01 | **Game Director** | Visión global, prioridades, vetos |
| 02 | **Creative Director** | Estilo visual y tono narrativo |
| 03 | **Technical Director** | Arquitectura, performance, deuda técnica |

## ▍Producción (3)

| # | Rol | Responsabilidad |
|---|---|---|
| 04 | **Lead Producer** | Roadmap, sprints, dependencias |
| 05 | **QA Lead** | Plan de testing, regresiones |
| 06 | **Localization Lead** | i18n (ES base, EN/PT/FR a futuro) |

## ▍Game Design (5)

| # | Rol | Responsabilidad |
|---|---|---|
| 07 | **Lead Game Designer** | Loop principal, equilibrio BalWealth Index |
| 08 | **Economy Designer** | Recetas, mercados, factores, contratos B2B |
| 09 | **Systems Designer** | Gerentes, RRHH, prestigio, banca |
| 10 | **Narrative Designer** | Storyline, NPCs, eventos, finales |
| 11 | **Level Designer** | Mapa de la ciudad, distritos, lugares |

## ▍Programación (8)

| # | Rol | Responsabilidad |
|---|---|---|
| 12 | **Engine Programmer** | `GameEngine`, ticks, persistencia |
| 13 | **Gameplay Programmer** | Comandos, mecánicas, hooks |
| 14 | **World Programmer** | Mundo 2D, movimiento, cámara, NPCs vivos |
| 15 | **UI Programmer** | Compose, navegación, pestañas |
| 16 | **Tools Programmer** | Editor visual de gerentes, customizer |
| 17 | **Render Programmer** | TileRenderer, sprites, animaciones, partículas |
| 18 | **Audio Programmer** | SoundEngine sintético + háptica |
| 19 | **Net/Cloud Programmer** | Save backups, futura sincronización cloud |

## ▍Arte (6)

| # | Rol | Responsabilidad |
|---|---|---|
| 20 | **Art Director** | Paleta, dirección visual unificada |
| 21 | **Pixel Artist – Personajes** | Avatar, NPCs, animaciones de andar |
| 22 | **Pixel Artist – Edificios** | Sprites por tipo de edificio + variantes nivel |
| 23 | **Pixel Artist – Mundo** | Tiles (grass/road/water/etc.), props, mobiliario |
| 24 | **Concept Artist** | Bocetos, ambientación, key frames |
| 25 | **UI Artist** | HUD, iconos, gradientes, marca |

## ▍Audio (2)

| # | Rol | Responsabilidad |
|---|---|---|
| 26 | **Sound Designer** | SFX sintetizados (cash register, level-up...) |
| 27 | **Composer** | Ambient lo-fi del menú/ciudad/sueños |

## ▍QA & UX (3)

| # | Rol | Responsabilidad |
|---|---|---|
| 28 | **QA Engineer – Functional** | Smoke + regression, bug triage |
| 29 | **QA Engineer – Performance** | FPS, jank, memoria, benchmarks |
| 30 | **UX Researcher** | Onboarding, tutorial, feedback de testers |

---

## ▍Tableros de trabajo activos

- `BUG-NN`: bugs reportados (estilo Jira)
- `FEAT-NN`: features pedidas
- `TEST-NN`: planes de QA por release

## ▍Última iteración

- **#23 Pixel Artist – Mundo**: re-hizo `TileRenderer` con texturas detalladas y creó `PropSprites` con 22 tipos de mobiliario (árboles 5 variantes, faroles, bancos, fuentes, papeleras, kioscos, parados, hidrantes, mesas de café, paradas de autobús, postes, puestos de mercado, humo, arbustos, parterres).
- **#11 Level Designer**: pidió generador de props que distribuye decoración por banda de distrito (parque denso de árboles, downtown con kioscos+coches, comercial con puestos de mercado, etc.).
- **#17 Render Programmer**: integró el render de props en `WorldScreen` entre tiles y NPCs.
- **#25 UI Artist**: rediseñó el logo (`ic_launcher_foreground.xml`) — balanza dorada con monedas a un lado y corazón al otro, símbolo del **Bal**ance + **Wealth**.
- **#28 QA Engineer**: verificó que los pop-ups dejan el juego visible (cap 92% ancho, 78% alto).
