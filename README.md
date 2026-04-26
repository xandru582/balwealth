<div align="center">

# 🏆 BalWealth

### El tycoon que no sabías que necesitabas

**SimCompanies × GTA × Pokémon × Sims × Stardew Valley × F1 Manager**
**·**
**Single-player · Android · 100% Kotlin + Jetpack Compose**

![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-BOM_2024.10.01-4285F4?style=flat-square&logo=jetpackcompose)
![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?style=flat-square&logo=android)
![Gradle](https://img.shields.io/badge/Gradle-8.10.2-02303A?style=flat-square&logo=gradle)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)
![APK](https://img.shields.io/badge/APK-203_MB-orange?style=flat-square)
![Status](https://img.shields.io/badge/status-jugable-success?style=flat-square)

</div>

---

## ✨ Resumen

**BalWealth** es un juego de simulación de empresas con alma de RPG, mundo abierto 2D explorable estilo Pokémon, y mini-juegos profundos. **No es una clone** de nada concreto: es un cóctel ambicioso que te deja gestionar una empresa, vivir tu vida, conducir tu coche por la ciudad, montar una familia, comprar un equipo de Fórmula y mucho más — todo en una sola app, sin DLC, sin login, sin ads.

> **¿Por qué se llama BalWealth?** Por el **BalWealth Index**: un score único 0-100 que mide el equilibrio entre tu **riqueza, empleados, comunidad y mente**. Si te haces millonario pero descuidas todo lo demás, el juego te castiga con eventos de "revolución" y burnout. La verdadera victoria es el equilibrio.

---

## 🎮 Sistemas que ya están en el juego

### 🧑‍💼 Personaje (RPG — estilo Torn City)
- **5 atributos**: Inteligencia, Fuerza, Carisma, Suerte, Destreza.
- Energía, felicidad, nivel, XP, **caja personal** separada de la corporativa.
- **Entrenamiento** (10 ⚡ por punto), trabajo, descanso.
- Avatar **personalizable** (pelo, ropa, accesorios) con render pixel-art propio.

### 🏢 Empresa (estilo SimCompanies / Virtonomics)
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

---

## 🌍 Mundo 2D explorable (estilo Pokémon)

Render engine **propio** sobre Compose Canvas — sin librerías de juegos.

- **Avatar** con animación de caminado en 4 direcciones, sprite pixel-art.
- **Ciudad** con 5 distritos: Centro, Industrial, Parque, Puerto, Residencial, Comercial, Afueras.
- **12 tipos de tiles**: hierba, asfalto con líneas dashed, acera con bevel, plaza ornamental, agua con cáusticos animados, arena con conchas y huellas, muros de ladrillo, parqué, puerta, suelo de bosque con hongos, raíles, puente.
- **22 props únicos**: 5 árboles (roble, pino, palmera, abedul, otoño), faroles con cono de luz nocturno, fuente con water spray, banco, papelera, kiosko, buzón, hidrante, coches aparcados, mesa de café con sombrilla, parada de bus, cartel, puesto de mercado, humo de chimenea, arbusto, parterre.
- **Día/noche cinemático**: amanecer, mediodía, hora dorada, atardecer, crepúsculo, noche con estrellas + luna con cráteres + estrellas fugaces.
- **Nubes procedurales** que se mueven por el cielo + **bandadas de pájaros en V** + **god rays** durante amanecer/atardecer.
- **Iluminación nocturna real** (radial gradient halos + cono de luz proyectado en suelo).
- **Sombras direccionales** que siguen al sol.
- **Atmósfera**: niebla volumétrica, bruma matinal, **partículas ambientales** (polen flotando), vignette cinemático, tinte de hora dorada.
- **Clima dinámico**: soleado, nublado, lluvia, niebla, tormenta — afecta render y simulación.
- **Tráfico** (vehículos en carreteras), **NPCs** caminando, **mascotas** que te siguen.

### 🚗 Coches y conducción
- **Concesionario con 25 modelos** en 9 marcas reales-fictícias.
- 10 tipos de carrocería: hatchback, sedan, SUV, coupé, descapotable, supercoche, limusina, pod eléctrico, clásico, camión.
- Repintar (10 colores), vender (refund 70%), **conducir** por la ciudad (10x velocidad), garaje ampliable.
- **Faros direccionales reales**, llamas de escape en supercoches, spoiler animado.

### 🐶 Vida y familia
- **7 mascotas** (perro, gato, loro, hámster, conejo, zorro, dragoncito) con hambre/felicidad, te siguen respetando paredes.
- **Casarte** con NPC (proposeMarriage) → felicidad +. Spouse con happiness, daysWith, likes.
- **Tener hijos** (con nombre y ageDays).
- **Casa decorable**: 17 tipos de mueble (cama, sofá, mesa, TV, piano, acuario, etc.) con sistema de placement por área (no overlap).

### 👽 Eventos y encuentros
- **50 eventos aleatorios** al pisar tile (estilo Pokémon): cartera en el suelo, mendigo, músico callejero, trato con dealer, ovni…
- **Avistamientos OVNI** raros (1 cada ~50 min in-game).
- **NPC follower**: alguien se te acerca con UNA pregunta, tienes 35s para decidir.

### 🎰 Mini-juegos
- **Casino**: ruleta con tu caja corporativa (la fortuna fácil baja karma).
- **Sueño lúcido**: descansar para regenerar energía + escuchar a tu subconsciente.

---

## 🏁 Formula Manager (mini-juego AAA dentro del juego)

Sistema **completo** para gestionar un equipo de Fórmula. Casi un juego dentro del juego.

### Lo que tienes
- **16 circuitos icónicos**: Mónaco, Monza, Spa, Silverstone, Suzuka, Interlagos, Nürburgring, Catalunya, Imola, Bahréin, Marina Bay, Zandvoort, COTA, Hermanos Rodríguez, Yas Marina, Hungaroring. Cada uno con longitud, vueltas, curvas, dificultad, downforce y premio base 580k–850k €.
- **10 equipos comprables** desde **9.5M €** (Phoenix Garage 🇳🇱) hasta **320M €** (Apex Racing 🇮🇹, 7× campeón).
- **32 pilotos** con stats únicos: skill, aggression, consistency + **6 specialties** (rain, street, high-speed, qualifying, tyre management, overtaking).
- **24 patrocinadores** en 5 tiers (🥉 Bronce → 🥈 Plata → 🥇 Oro → 💎 Platino → 👑 Titanio). Pagos diarios 20k–240k € + bonus por victoria, podio, y por punto. Brand value mínimo requerido (20-92).
- **21 técnicos** en 5 roles (Ingeniero de Pista, Estratega, Aerodinámico, Jefe de Motor, Especialista de Neumáticos), cada uno con rating 50-96. Bonus al rendimiento del equipo cap +20%.

### Lo que puedes hacer
- 💰 **Comprar/vender equipo** (refund 80%).
- 🔧 **Mejorar coche** en 4 partes (motor, aero, fiabilidad, neumáticos), coste escala con nivel.
- ✍️ **Fichar/despedir pilotos** con prima 30 días salario / indemnización 60.
- 📜 **Firmar/cancelar sponsorships** con primas 150k–5.5M € y duraciones 90-365 días.
- 👨‍🔧 **Contratar/despedir personal técnico** (1 por rol).
- 🏁 **Ver simulación de carrera** (1 por semana in-game): fórmula 60% piloto + 35% coche + 5% suerte + matches por specialty/circuito + bonus de staff.
- 🏆 **Disputar championship**: 16 carreras por temporada → campeón de pilotos + constructores. Si tu equipo gana: bonus 12M €.

### Datos y tablas
- **Mundial de Pilotos** + **Mundial de Constructores** (top 10 por puntos).
- **Calendario** completo con próxima carrera destacada y resultados pasados.
- **Récords por circuito**: lap record holder + tiempo + temporada, último ganador, dominador (más wins).
- **Stats detalladas all-time** por piloto: starts, wins, podios, poles, fastest laps, DNFs, current+longest win streak, win rate %, podium rate %, average finish, best season points, championships.
- **Tabla de naciones** (top países por victorias acumuladas).
- **Hall of Fame** con palmarés histórico: campeones por temporada (driver + constructor), equipos más laureados, pilotos con más mundiales (Schumacher/Hamilton style).

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
│  └─ RacingEngine      Formula Manager
├─ model/               Data classes inmutables (@Serializable)
├─ world/               Render engine + grid + tiles + props
│  ├─ render/           SkyEngine, ShadowEngine, LightingEngine, PostFx
│  └─ sprites/          AvatarSprites, CarSprites, BuildingSprites, …
├─ data/                GameViewModel + SaveRepository
└─ audio/               AssetMusicPlayer + AmbientPlayer (procedural)
```

### Render pipeline (cada frame)
```
Sky (clouds + sun + birds + god rays + stars + moon)
  └→ Tiles (12 tipos con noise variants)
      └→ Shadows (direccionales por sun angle)
          └→ Objects depth-sorted by Y (avatar + npcs + cars + props)
              └→ Lights (lampposts halos + window glows nocturnos)
                  └→ Atmosphere tint (golden hour, dusk, dawn)
                      └→ Fog (clima + bruma matinal)
                          └→ Ambient particles (polen / motas de polvo)
                              └→ Karma echo (saturation filter)
                                  └→ Weather (rain/snow/storm overlay)
                                      └→ Vignette cinemático
```

### Game loop
- **1 tick = 1 segundo real**.
- `advanceSeconds(N)` para offline progress (capped a 8h).
- **Mutex** sobre `SaveRepository.save()` para evitar corrupción.
- **Cancel + join** en `onCleared()` para no perder progreso al cerrar.

---

## 🚀 Compilar y ejecutar

```bash
# Requiere Java 21 (Homebrew):
brew install openjdk@21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

# Configura el SDK de Android:
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Build debug APK (~203 MB con assets de audio, ~50 MB sin ellos):
./gradlew :app:assembleDebug

# Instalar en dispositivo conectado:
./gradlew :app:installDebug
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

> ⚠️ Los archivos `*.wav` de música procedural (153 MB) **no están versionados** para mantener el repo ligero. La app compila sin ellos pero la música no se reproducirá. Genera tus propios assets en `app/src/main/assets/audio/music/` o pídelos al autor.

---

## 🐛 QA

El proyecto pasó por **18 oleadas de QA automatizado** con agentes especializados que encontraron y arreglaron **200+ bugs**. Algunos destacados:

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

---

## 📜 Versiones (highlights)

| Versión | Cambios principales |
|---------|---------------------|
| **v16-formula-pro** | Formula Manager Pro: 24 sponsors, 21 tech staff, records por circuito, hall of fame, tabla de naciones, specialties por piloto |
| **v15-formula** | Formula Manager: 16 circuitos, 10 equipos, 32 pilotos, championship con 16 carreras |
| **v14-world** | 2ª ola visual: lampposts con cono de luz, plaza ornamental, fountain con water spray, niebla atmosférica, partículas de polen |
| **v13-world** | 1ª ola visual: cielo con sol/nubes/pájaros/god rays, árboles con clusters de hojas, edificios con ventanas iluminadas |
| **v12-fixes** | Música determinista (no se resetea al cambiar tab), pause en background con LifecycleObserver, dividendos generosos (yields 6-22% APR + frecuencia /52) |
| **v11-fixes** | Round 2 QA: contract penalty no deja cash negativo, save races con Mutex, onCleared sincrónico, worldEvent timeout |
| **v10-fixes** | Round 1 QA: 200+ bugs arreglados (exploits, crashes, tutorial misfires, walkable checks) |

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
