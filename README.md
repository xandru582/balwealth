<div align="center">

# 🏆 BalWealth

### El tycoon que no sabías que necesitabas

**Simulación empresarial · RPG · Mundo abierto 2D · Mini-juegos profundos · Comercio internacional · Oficios jugables**
**·**
**Single-player · Android · 100% Kotlin + Jetpack Compose**

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-BOM_2024.10.01-4285F4?style=flat-square&logo=jetpackcompose)
![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?style=flat-square&logo=android)
![Gradle](https://img.shields.io/badge/Gradle-8.10.2-02303A?style=flat-square&logo=gradle)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)
![APK](https://img.shields.io/badge/APK-203_MB-orange?style=flat-square)
![Status](https://img.shields.io/badge/status-jugable-success?style=flat-square)
![Version](https://img.shields.io/badge/version-v17--infinite-purple?style=flat-square)

</div>

---

## ✨ Resumen

**BalWealth** es un juego original de simulación con alma de RPG, mundo abierto 2D explorable y mini-juegos profundos. Es un cóctel ambicioso que te deja gestionar tu empresa, vivir tu vida personal, conducir tu coche por la ciudad, montar familia, gestionar un equipo de monoplazas, especular con cripto y desastres, hacer atracos, comerciar entre 5 ciudades extranjeras y mucho más — todo en una sola app, sin DLC, sin login, sin ads.

> **¿Por qué se llama BalWealth?** Por el **BalWealth Index**: un score único 0-100 que mide el equilibrio entre tu **riqueza, empleados, comunidad y mente**. Si te haces millonario pero descuidas todo lo demás, el juego te castiga con eventos de "revolución" y burnout. La verdadera victoria es el equilibrio.

---

## 🎮 Sistemas que ya están en el juego (v1-v16)

### 🧑‍💼 Personaje (RPG stat-based)
- **5 atributos**: Inteligencia, Fuerza, Carisma, Suerte, Destreza.
- Energía, felicidad, nivel, XP, **caja personal** separada de la corporativa.
- **Entrenamiento** (10 ⚡ por punto), trabajo, descanso.
- Avatar **personalizable** (pelo, ropa, accesorios) con render pixel-art propio.

### 🏢 Empresa (cadenas de producción reales)
- **11 tipos de edificios**: Granja, Aserradero, Mina, Panadería, Fundición, Refinería, Fábrica, Oficina, Joyería, Astillero, Almacén.
- **Cadenas de producción reales**: `hierro + carbón → lingote → acero → engranaje → motor → coche`.
- **30+ recetas base** + **30 recetas avanzadas** con sistema de **calidad** (Pobre → Estándar → Buena → Premium → Ultra → Obra Maestra), cada tier multiplica el precio de venta.
- **Empleados** con skill, lealtad y nóminas diarias. Si no hay liquidez, dimiten.
- **Capacidad de almacén** ampliable.

### 📈 Mercado dinámico
- **29 recursos** en 7 categorías.
- Precios oscilan por **oferta/demanda** (random walk con reversión a la media).
- **Comprar sube precio, vender lo baja**. Spread compra/venta del 25%.
- **Macroeconomía**: ciclos boom/recesión, **fase de mercado** (alcista/bajista) y **noticias** que mueven precios sectoriales.

### 🔬 Investigación
- **14 tecnologías** con prerrequisitos (árbol).
- Desbloquean recetas avanzadas, % producción y % precio de venta.

### 💼 Patrimonio (Bolsa, Inmuebles, IPO, Opciones, Banca)
- **Bolsa**: 8 acciones con volatilidad propia, sparkline histórico, **dividendos diarios** (yields 6-22% APR).
- **Inmuebles**: 6 tipos (piso, apartamento, casa, villa, local, rascacielos) con renta pasiva.
- **Banca**: préstamos, hipotecas, cuotas diarias, default, recuperación con reputación.
- **IPO**: saca **tu propia** empresa a bolsa.
- **Opciones financieras** (calls/puts/derivados con Black-Scholes simplificado).

### 👥 RRHH avanzado
- Plantilla con **roles**, **formación** y **ejecutivos** que dan bonuses de empresa.
- Pool de candidatos generado proceduralmente según reputación.
- **Whitelist de venta**, **protección de inputs activos**, **políticas de producción**.

### 🤖 Gerentes (anti-grind)
- 4 gerentes IA que **automatizan** compras, ventas, mejoras y RRHH.
- Cada uno con personalidad, skill y coste mensual.

### 📋 Contratos B2B
- Pedidos firmes con clientes a precio fijo, deadline y multa por incumplimiento.
- Auto-deliver desde inventario.

### 🏆 Logros + Misiones
- **50+ logros** desbloqueables.
- **Misiones principales** + **misiones secundarias diarias** con karma/dinero/XP.
- **Sistema de prestigio**: renacer y desbloquear perks permanentes.

### 📖 Historia con karma
- Capítulos narrativos con **decisiones morales** que afectan tu karma.
- **NPCs con relaciones** (amistad, romance, rivalidad).
- **20 rivales** con los que competir.
- **Final emergente** según tu BalWealth Index al final.

### 🌈 BalWealth Index (mecánica única)
- 4 ejes (riqueza, empleados, comunidad, mente).
- 6 tiers: Burnout → Struggling → Growing → Harmonic → Tyrant Bubble → Martyr.
- Si pasas 14 días en **Tyrant Bubble** → ¡revolución y pierdes parte de tu imperio!

### 🏁 Formula Manager (mini-juego AAA dentro del juego)
**16 circuitos ficticios · 10 equipos · 32 pilotos · 24 patrocinadores · 21 técnicos**

Sistema completo para gestionar un equipo de monoplazas. Comprar/vender equipo, mejorar coche en 4 partes, fichar pilotos, firmar sponsorships, contratar staff, ver simulación de carrera, disputar campeonato de 16 carreras, mundial de pilotos + constructores, hall of fame con palmarés histórico.

---

## 🆕 v17-infinite — Lo nuevo

8 sistemas extra que llenan los huecos del juego (riesgo, drama, meta-progresión, comercio global, mini-juegos arcade) — diseñados para sostener cientos de horas de juego.

### 🪙 Mercado cripto (CryptoEngine)
- **6 tokens ficticios** con perfiles distintos (BLU bluechip, GLD stable, MOON growth, PUMP meme, SAFE stable de yield, RUG candidato a rugpull).
- Random walk geométrico con drift+vol diarios + saltos discretos por noticias.
- **Rugpulls reales**: cada token tiene `rugChancePerDay`. Si pulsa → -90% en 1 tick.
- **Mining**: empleados a minar → genera token/día.
- **Staking**: bloqueas tokens X días → APY 8%-200%.
- **Whale moves**: noticias en feed cuando una ballena mueve fuerte.
- **Karma**: salir limpio antes de un rug → reputación. Hacer dump después de un pump → karma cae.

### 🌪️ Desastres dinámicos (DisasterEngine)
- **12 tipos**: terremoto, inundación, incendio, pandemia, apagón, huracán, hack ransomware, flash crash, hiperinflación, huelga logística, boicot, robo a mano armada.
- **Severidad 1-5** con daños inmediatos + modificadores temporales.
- **Ventana de respuesta de 24h** in-game para mitigar (5 estrategias: emergencia, PR, donaciones, seguridad, técnico).
- **Insurance system**: paga prima diaria → cobertura.
- **Resilience XP**: superar desastres da puntos para futuros perks.

### 🎯 Retos diarios y semanales (DailyChallengeEngine)
- **3 retos cada 24h** + **1 reto semanal** rotatorios.
- Categorías: ganar X cash, construir, comprar acciones, ganar carreras F1, mantener felicidad sin casino, conducir distancia…
- **Rachas**: completar X días seguidos da multiplicador acumulativo (cap ×5).
- **Reto del finde**: super-reto con recompensa permanente.

### 🦹 Heists / atracos (HeistEngine)
Mini-juego **roguelike** dentro del tycoon.
- **8 tipos de golpe** desde robo a tienda (50k €) hasta The Big One (50M €).
- **Tripulación**: 4 roles (líder, hacker, conductor, francotirador).
- **Planificación**: enfoque (ruidoso/sigiloso/negociación) + gear spend.
- **Resolución**: tirada modificada por skills + plan + suerte → 4 outcomes (PERFECT/SUCCESS/ESCAPE/DISASTER).
- **Heat policial**: cada golpe sube el heat → eventos de policía, embargo, miembros caen.
- **Karma**: cada heist baja karma fuerte. Path "héroe" puede saltarse heists. Path "villano" desbloquea misiones extra.

### 🤖 Asistente IA (AICompanionEngine)
**Local-only, sin red.** Heurísticas que leen tu estado cada 5 minutos y emiten tips priorizados.
- **5 personalidades** (Profesora, Tiburón, Maestro Zen, El Apostador, Mentor) que solo cambian el tono — las recomendaciones son las mismas.
- **11 heurísticas activas**: nóminas próximas con caja justa, reputación caída, cripto en sentimiento muy negativo (rug risk), reto al 80%, caja parada con mucho cash sin deuda, stake casi liberado, desastre sin mitigar, heat alto en heists, energía baja, deuda > 2× cash, edificios sin upgrade.
- **Mood meter** 0-100 según el ratio ack vs dismiss.
- **Sliding-window** que evita repetir el mismo aviso cada tick.
- Cap a 6 tips activos. Gating: nivel ≥ 3.

### 🌐 Imperio global (MultiCityEngine)
Comercio internacional con 4 ciudades extranjeras + tu HOME.
- **5 mercados** (HOME + Neo Tokio + Dubai City + Lagos Bay + New Coast + Berlín Nuevo) con multiplicadores de precio por recurso, demanda variable, tipo de cambio (FX), sentimiento boom/bust con reversión a la media.
- **Aranceles**: import + export distintos por ciudad. Dubai = paraíso fiscal (5% local tax); Lagos = altos aranceles (18%) pero compras baratas.
- **Rutas logísticas**: pago de apertura (30 días de mantenimiento) + coste diario.
- **Envíos en tránsito**: tras N días llegan y se liquidan automáticamente al precio remoto efectivo.
- **Volatilidad por ciudad**: Tokio 4%, Dubai 6%, Lagos 10%, NYC 5%, Berlín 3%.
- Gating: 1.000.000 € + reputación ≥ 50.

### 🎮 Arcade (ArcadeEngine)
Mini-juegos clásicos jugables con apuestas en cash de empresa.
- **🐍 Serpiente** — JUGABLE. 12×16 grid, control direccional, 220ms tick, recompensas escaladas (4-9 piezas = recuperación proporcional, 10 = ×1.5, 25 = ×5, 40+ = ×10). Pause + plantarse + game over.
- **🔢 2048**, **🧱 Breakout**, **🏓 Pong**, **🟦 Tetrix** — preparados, próximamente.
- **High scores** + estadísticas por juego + **historial de últimas 20 partidas**.
- Gating: nivel ≥ 2.

### 🎭 Temporadas / Festivales (SeasonsEngine)
Ciclo cíclico de 30 días con 4 temporadas + temporada baja.
- **🎃 Halloween** (días 1-7) — eventos al pisar tile multiplicados ×1.5.
- **🎄 Navidad** (días 8-14) — bonificación ventas +12%, nevada visual.
- **🎆 Año Nuevo** (días 15-21) — bonus XP del jugador +25%, fuegos artificiales.
- **☀️ Verano** (días 22-28) — renta de inmuebles +18%.
- **🌫️ Temporada baja** (días 29-30) — descanso.
- **Recompensas únicas** la primera vez que vives una temporada entera (+ cash + XP).

---

## 🌍 Mundo 2D explorable

Render engine **propio** sobre Compose Canvas — sin librerías de juegos.

- **Avatar** con animación de caminado en 4 direcciones, sprite pixel-art.
- **Ciudad** con 5 distritos: Centro, Industrial, Parque, Puerto, Residencial, Comercial, Afueras.
- **12 tipos de tiles**: hierba, asfalto con líneas dashed, acera con bevel, plaza ornamental, agua con cáusticos animados, arena con conchas y huellas, muros de ladrillo, parqué, puerta, suelo de bosque con hongos, raíles, puente.
- **22 props únicos**: 5 árboles (roble, pino, palmera, abedul, otoño), faroles con cono de luz nocturno, fuente con water spray, banco, papelera, kiosko, buzón, hidrante, coches aparcados, mesa de café con sombrilla, parada de bus, cartel, puesto de mercado, humo de chimenea, arbusto, parterre.
- **Día/noche cinemático**: amanecer, mediodía, hora dorada, atardecer, crepúsculo, noche con estrellas + luna con cráteres + estrellas fugaces.
- **Nubes procedurales** que se mueven por el cielo + **bandadas de pájaros en V** + **god rays** durante amanecer/atardecer.
- **Iluminación nocturna real** con **bloom PCF** (faroles que respiran de verdad).
- **Sombras suaves** que siguen al sol (PCF de 3 muestras, bordes graduados).
- **Atmósfera**: niebla volumétrica, bruma matinal, **partículas ambientales** (polen flotando), vignette cinemático, tinte de hora dorada.
- **Agua animada con olas sinusoidales** — el gradient respira como una marea.
- **Clima dinámico**: soleado, nublado, lluvia, niebla, tormenta — afecta render y simulación.
- **Tráfico** (vehículos en carreteras), **NPCs** caminando, **mascotas** que te siguen.

### 🚗 Coches y conducción
- **Concesionario con 25 modelos** ficticios en 9 marcas inventadas.
- 10 tipos de carrocería: hatchback, sedán, SUV, coupé, descapotable, deportivo, limusina, pod eléctrico, clásico, camión.
- Repintar (10 colores), vender (refund 70%), **conducir** por la ciudad (10x velocidad), garaje ampliable.
- **Faros direccionales reales**, llamas de escape en deportivos, spoiler animado.

### 🐶 Vida y familia
- **7 mascotas** (perro, gato, loro, hámster, conejo, zorro, dragoncito) con hambre/felicidad, te siguen respetando paredes.
- **Casarte** con NPC → felicidad +. Spouse con happiness, daysWith, likes.
- **Tener hijos** (con nombre y ageDays).
- **Casa decorable**: 17 tipos de mueble (cama, sofá, mesa, TV, piano, acuario, etc.) con sistema de placement por área (no overlap).

### 👽 Eventos y encuentros
- **50 eventos aleatorios** al pisar tile: cartera en el suelo, mendigo, músico callejero, trato con dealer, ovni…
- **Avistamientos OVNI** raros (1 cada ~50 min in-game).
- **NPC follower**: alguien se te acerca con UNA pregunta, tienes 35s para decidir.

### 🎰 Mini-juegos del mundo
- **Casino**: ruleta con tu caja corporativa (la fortuna fácil baja karma).
- **Sueño lúcido**: descansar para regenerar energía + escuchar a tu subconsciente.

---

## 🚧 En desarrollo (siguiente gran tanda)

> Lo que viene después de v17-infinite. **Implementación incremental**: cada oficio se añade en commits separados con su mini-juego. El framework es lo que se monta primero.

### 💼 Sistema de Oficios — 40 trabajos jugables

Trabajos donde controlas **tú directamente** al personaje haciendo el oficio, no la empresa. Cada uno es un **mini-juego propio** con apuestas/sueldo. Tu nivel + carisma + suerte modulan recompensas.

Cada oficio tendrá **además** la opción de **montar tu empresa de ese oficio** y **contratar empleados** especializados (ej. tu propia panadería con 5 panaderos asignados que producen mientras estás offline).

#### 🚨 Servicios de Emergencia (4)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 01 | 🚓 Policía | Tap-reaction sobre sospechosos en vista top-down |
| 02 | 🚒 Bombero | Drag&drop de mangueras con timer de propagación |
| 03 | 🚑 Paramédico | Arrastrar al paciente + secuencia QTE de RCP |
| 04 | 🦮 K-9 Officer | Rhythm game para órdenes al perro policía |

#### 🏥 Sanidad (3)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 05 | 👨‍⚕️ Médico (cirujano) | Precision-tap sobre puntos del paciente con tiempo límite |
| 06 | 🦷 Dentista | Timing-based tap (limpieza/empastes) |
| 07 | 💊 Farmacéutico | Combinar viales en orden correcto según receta |

#### 🏫 Educación / Arte (4)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 08 | 👨‍🏫 Profesor | Trivia con tiempo límite |
| 09 | 🎨 Pintor | Trazos en orden (memory + drag) |
| 10 | 📚 Bibliotecario | Sort puzzle por categorías |
| 11 | 🎭 Actor de teatro | Secuencia de gestos sincronizados con guion |

#### 🍞 Restauración (5)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 12 | 🥖 Panadero | Timing del horno + balance de ingredientes |
| 13 | 👨‍🍳 Chef de restaurante | Cola de tickets con prioridad y combos |
| 14 | 🍕 Pizzero | Estirar masa + colocar toppings + horno |
| 15 | ☕ Barista | Drag patterns para latte art |
| 16 | 🍦 Heladero | Stack + balance de bolas en cucurucho |

#### 🔨 Construcción / Reparación (5)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 17 | 🔨 Albañil | Tetris-like de ladrillos en pared |
| 18 | 🪚 Carpintero | Cortes precisos con swipe |
| 19 | 🔧 Mecánico de coches | Identificar pieza rota + reemplazo |
| 20 | ⚡ Electricista | Puzzle de cables sin cortocircuitar |
| 21 | 🚿 Fontanero | Pipe-puzzle (Pipe Mania-like) |

#### 🚗 Transporte (5)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 22 | 🚕 Taxista | Pickup + dropoff con ruta más corta |
| 23 | 🚚 Camionero | Parking inverso de camión |
| 24 | ✈️ Piloto de avión | Despegues/aterrizajes con balance de palanca |
| 25 | 🚂 Maquinista de tren | Timing exacto en paradas + frenado |
| 26 | 🏎️ Piloto de carreras | Drift + timing en circuito (link a Formula Manager) |

#### 📮 Servicios Públicos (3)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 27 | 📮 Cartero | Encontrar buzones en mapa con ruta limitada |
| 28 | 🚛 Recolector de basura | Tap timing en contenedores antes de derrame |
| 29 | 🌳 Jardinero municipal | Trim + water sequence con timing |

#### 🐾 Naturaleza / Animales (3)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 30 | 🐶 Veterinario | Examina + diagnostica + receta |
| 31 | 🌾 Granjero | Tap rhythm para sembrar + regar + cosechar |
| 32 | 🌊 Pescador | Tug-of-war con peces + timing del anzuelo |

#### 💻 Tecnología (3)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 33 | 💻 Programador | Encontrar el bug en código simple |
| 34 | 📱 Diseñador UI/UX | Snap-to-grid de elementos en wireframe |
| 35 | 🎮 Streamer / Gamer Pro | Speed-tap react contra IA |

#### 🎬 Espectáculo / Deporte (3)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 36 | ⚽ Futbolista | Tap+drag para chutar penaltis con dirección |
| 37 | 🥊 Boxeador | Dodge + counter en rhythm vs IA |
| 38 | 🎬 Director de cine | Gestiona crew + timing de tomas |

#### 🕵️ Curiosos / Especiales (2)
| # | Oficio | Mini-juego planeado |
|---|---|---|
| 39 | 🦸 Detective privado | Encuentra pistas en escena (find-the-clue) |
| 40 | 🧙 Ilusionista | Secuencia de cartas en orden bajo presión |

**= 40 oficios totales**

### 🏗️ Otros sistemas planeados después de Jobs

- **🧬 TraitTreeEngine** — 60 talents permanentes en 5 ramas (Magnate, Visionario, Político, Artista, Outlaw). Se desbloquean con XP de Resilience + acciones específicas. Permanentes a través del prestigio.
- **⚔️ HostileTakeoverEngine** — adquisiciones agresivas (comprar 51% de empresa rival). Defensa: poison pill, white knight, golden parachute.
- **🏪 Empresas de oficios** — montar tu propia panadería / taller / consultorio / etc. con empleados especializados que producen offline. Integración con HrEngine existente.

---

## 🛠️ Stack técnico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.10.01) + Material 3 |
| Render | Custom 2D engine sobre Compose Canvas con depth sorting |
| Audio | MediaPlayer + AudioTrack (lifecycle-aware) |
| Persistencia | kotlinx.serialization JSON + autosave cada 10s con `Mutex` |
| Build | Gradle 8.10.2 + AGP 8.8.0 |
| JDK | Java 21 |
| Min/Target SDK | 26 / 36 |

### Arquitectura

```
┌─ ui/                  Jetpack Compose screens (40+)
├─ engine/              Lógica pura GameState → GameState
│  ├─ GameEngine        orquestador del tick
│  ├─ Production        cadenas de producción + quality
│  ├─ Economy           market, stocks, news
│  ├─ BankingEngine     préstamos, hipotecas
│  ├─ HrEngine          plantilla, formación
│  ├─ ContractsEngine   B2B
│  ├─ ManagerEngine     automatización
│  ├─ ResearchEngine    tech tree
│  ├─ AchievementEngine logros
│  ├─ QuestEngine       misiones
│  ├─ StorylineEngine   narrativa
│  ├─ SideQuestEngine   misiones secundarias
│  ├─ RivalEngine       competidores
│  ├─ EconomicEngine    macroeconomía
│  ├─ IpoEngine         IPOs
│  ├─ OptionsEngine     derivados
│  ├─ PrestigeEngine    renacer
│  ├─ TutorialEngine    onboarding
│  ├─ DrivingEngine     coches y movimiento
│  ├─ QualityEngine     calidad de productos
│  ├─ RacingEngine      Formula Manager
│  ├─ CryptoEngine      v17 — mercado cripto + rugpulls + staking
│  ├─ DisasterEngine    v17 — desastres dinámicos + insurance
│  ├─ DailyChallengeEngine  v17 — retos rotatorios
│  ├─ HeistEngine       v17 — atracos roguelike
│  ├─ AICompanionEngine v17 — asistente IA con heurísticas locales
│  ├─ MultiCityEngine   v17 — comercio internacional
│  ├─ ArcadeEngine      v17 — mini-juegos con apuestas
│  └─ SeasonsEngine     v17 — temporadas y festivales
├─ model/               Data classes inmutables (@Serializable)
├─ world/               Render engine + grid + tiles + props
│  ├─ render/           SkyEngine, ShadowEngine (PCF), LightingEngine (bloom), PostFx
│  └─ sprites/          AvatarSprites, CarSprites, BuildingSprites, TileRenderer (water animado), …
├─ data/                GameViewModel + SaveRepository
└─ audio/               AssetMusicPlayer + AmbientPlayer (procedural)
```

### Render pipeline (cada frame)

```
Sky (clouds + sun + birds + god rays + stars + moon)
  └→ Tiles (12 tipos, water con waves animadas)
      └→ Shadows (PCF 3-tap, bordes graduados)
          └→ Objects depth-sorted by Y (avatar + npcs + cars + props)
              └→ Lights (bloom: halo + core + hot-spot, BlendMode.Plus)
                  └→ Atmosphere tint (golden hour, dusk, dawn)
                      └→ Fog (clima + bruma matinal)
                          └→ Ambient particles (16 motas, optimizado)
                              └→ Karma echo (saturation filter)
                                  └→ Weather (rain/snow/storm overlay)
                                      └→ Vignette cinemático
```

### Game loop

- **1 tick = 1 segundo real**.
- `advanceSeconds(N)` para offline progress (capped a 8h).
- **Mutex** sobre `SaveRepository.save()` para evitar corrupción.
- **Cancel + join** en `onCleared()` para no perder progreso al cerrar.
- **30 hooks** de subsistemas en `advanceOneSecond` (uno por engine, con su periodicidad propia).

---

## 🚀 Compilar y ejecutar

```bash
# Requiere Java 21 (Homebrew):
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Configura el SDK de Android:
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Build debug APK:
./gradlew :app:assembleDebug

# Instalar en dispositivo conectado:
./gradlew :app:installDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

> ⚠️ Los archivos `*.wav` de música procedural (153 MB) **no están versionados** para mantener el repo ligero. La app compila sin ellos pero la música no se reproducirá. Genera tus propios assets en `app/src/main/assets/audio/music/` o pídelos al autor.

### En Windows (sin Homebrew)

Si usas Android Studio en Windows, el JBR de Studio ya trae Java 21:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# local.properties:
echo "sdk.dir=$env:ANDROID_HOME" > local.properties

# Build:
.\gradlew.bat :app:assembleDebug
```

---

## 🐛 QA

El proyecto pasó por **18 oleadas de QA automatizado** en v10-v15 que encontraron y arreglaron **200+ bugs**, más una **ronda extra v17** con 12 fixes adicionales:

**v10-v15 — 200+ bugs (los más destacados):**
- ✅ Exploit de free money por compra/venta de acciones con qty negativa.
- ✅ Crash en `BankingEngine.repayLoan` con `coerceIn(0, negativo)`.
- ✅ Doble-aplicación de `productionBonus` en cadenas con tech avanzada.
- ✅ Race condition en `SaveRepository.save()` que podía corromper el save.
- ✅ Música que se reseteaba al cambiar de pestaña (selector aleatorio sin seed).
- ✅ Música que seguía sonando con la app en background (sin lifecycle observer).
- ✅ Tutorial bloqueado en pasos sin anchor (no había escape).
- ✅ Mascotas y NPCs atravesaban paredes (sin walkable check).
- ✅ Coachmark eats clicks del widget enfocado.
- ✅ World event activeEventId se quedaba colgado para siempre.
- … y 190+ más.

**v17 — 12 fixes adicionales (review estática + ronda 3):**
- ✅ `DRIVE_DISTANCE` autocancelado: la fórmula sumaba y restaba el snapshot, el reto nunca progresaba.
- ✅ `CryptoEngine.sell` silencioso con `qty <= 0` (potencial exploit).
- ✅ `CryptoEngine.stake` sin validar `days <= 0` → unlock instantáneo = farm gratuito.
- ✅ `DisasterEngine` crash con `buildings` vacío (`coerceIn(1, 0)` → IllegalArgumentException).
- ✅ `wasEverBought` flag en `CryptoHolding` para detectar correctamente rugpull survivors tras venta total.
- ✅ `CryptoEngine.assignMiners` permite ahora decrementar mineros incluso si capacity = 0.
- ✅ `DisasterEngine.cooldownDays` con `coerceAtLeast(1)` defensivo.
- ✅ `HeistEngine.planHeist` rechaza `crewIds.isEmpty()` upfront.
- ✅ `HeistEngine.execute` extrae `plan` a `val` local (smart-cast explícito).
- ✅ Cap simétrico (0.5..2.0) en `buyPriceMul` de mitigación.
- ✅ Optimizaciones de render (perf): bloom outer-layer eliminado, sombras de 5-tap a 3-tap, water bands de 3 a 1, partículas ambient de 32 a 16.

---

## 📜 Versiones (highlights)

| Versión | Cambios principales |
|---------|---------------------|
| **v17-infinite** | 8 sistemas nuevos: cripto, desastres, retos diarios, heists, **asistente IA**, **comercio internacional (5 ciudades)**, **Arcade con Snake jugable**, **temporadas (Halloween/Navidad/Año Nuevo/Verano)**. Render: bloom PCF, agua animada, sombras suaves. 12 bugs adicionales arreglados. Optimizaciones de perf. |
| **v16-formula-pro** | Formula Manager Pro: 24 sponsors, 21 tech staff, records por circuito, hall of fame, tabla de naciones, specialties por piloto. |
| **v15-formula** | Formula Manager: 16 circuitos, 10 equipos, 32 pilotos, campeonato con 16 carreras. |
| **v14-world** | 2ª ola visual: lampposts con cono de luz, plaza ornamental, fountain con water spray, niebla atmosférica, partículas de polen. |
| **v13-world** | 1ª ola visual: cielo con sol/nubes/pájaros/god rays, árboles con clusters de hojas, edificios con ventanas iluminadas. |
| **v12-fixes** | Música determinista (no se resetea al cambiar tab), pause en background con LifecycleObserver, dividendos generosos (yields 6-22% APR + frecuencia /52). |
| **v11-fixes** | Round 2 QA: contract penalty no deja cash negativo, save races con Mutex, onCleared sincrónico, worldEvent timeout. |
| **v10-fixes** | Round 1 QA: 200+ bugs arreglados (exploits, crashes, tutorial misfires, walkable checks). |

---

## ⚖️ Aviso legal

Este proyecto es una **obra original** desarrollada de forma independiente. Todos los nombres de equipos, pilotos, marcas, circuitos, productos, edificios, recetas, ciudades, tokens cripto y NPCs **son ficticios e inventados** por el autor. Cualquier parecido con marcas, personajes, eventos o nombres reales es **pura coincidencia** y **no implica relación alguna**. No está afiliado, patrocinado ni respaldado por ninguna entidad real. La música es **procedural y generada matemáticamente**. El código es 100% propio.

---

## 🤝 Contribuir

Este es un proyecto personal hecho a martillazos. Si quieres aportar:
- **Bug reports**: abre un issue con stack trace + pasos para reproducir.
- **Pull requests**: bienvenidas pero discútelo primero en un issue.
- **Ideas**: cuéntame en issues o por DM.

---

## 📄 Licencia

MIT — haz lo que quieras pero atribuye. Ver [LICENSE](LICENSE).

---

<div align="center">

**Hecho con 🩷 en Kotlin por [@xandru582](https://github.com/xandru582)**

*"La verdadera victoria es el equilibrio."*

</div>
