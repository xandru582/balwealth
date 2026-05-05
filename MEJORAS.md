# 🚀 BalWealth — Plan de mejoras "1000×"

> Versión propuesta: **v17-infinite** · Objetivo: convertir BalWealth en un *idle-tycoon-RPG* que se pueda jugar **cientos de horas** sin tocar fondo, añadiendo bucles de meta-progresión, eventos volátiles y mini-juegos high-stakes.

---

## 🧭 Filosofía del rediseño

El juego actual ya es enorme (24 motores, 33 pantallas, 169 archivos Kotlin). El problema **no** es la falta de contenido — es la falta de **bucles**:

| Loop | Duración típica | Estado actual |
|------|-----------------|---------------|
| Producción → vender → reinvertir | minutos | ✅ sólido |
| Investigación → desbloquear recetas | horas | ✅ sólido |
| Campeonato F1 (16 carreras) | 1 semana in-game | ✅ sólido |
| **Riesgo / drama / crisis** | — | ⚠️ casi inexistente |
| **Meta-progresión NG+** | renacer 1 vez | ⚠️ poca profundidad |
| **Volatilidad financiera** | mercado/news | ⚠️ predecible |
| **Retos limitados-temporales** | — | ❌ falta |
| **Heists / atracos / golpes** | — | ❌ falta |

→ Las **4 piezas nuevas** llenan exactamente esos huecos.

---

## 🆕 Sistemas nuevos (esta tanda)

### 1. 🪙 CryptoEngine — Volatilidad pura
- **6 tokens ficticios** con perfiles distintos (BLU, GLD, MOON, PUMP, SAFE, RUG).
- Movimiento de precio con **garchear (drift+vol)** + saltos discretos por noticias.
- **Rugpulls reales**: cada token tiene `rugChancePerDay`. Si pulsa → -90% del valor en 1 tick. Tu posición se queda con el 10%.
- **Mining**: dedica empleados a "minar" → genera token/día.
- **Staking**: bloquea tokens X días → APY 30-200%.
- **Whale moves**: noticias en feed cuando una ballena mueve 10M €.
- **Karma**: salir limpio antes de un rug → +1 reputación. Hacer dump después de un pump → -2 karma.

### 2. 🌪️ DisasterEngine — Crisis dinámicas
Cada **30-60 días in-game** dispara un desastre semialeatorio. 12 tipos:
- 🌍 Terremoto, 🌊 inundación, 🔥 incendio fábrica, 🦠 pandemia, ⚡ apagón, 🌪️ huracán, 🐛 hack ransomware, 📉 flash crash, 💸 hiperinflación, 🚛 huelga logística, 🛒 boicot, 🔫 robo a mano armada.
- Cada desastre tiene:
  - **Severidad** (1-5)
  - **Daños inmediatos** (cash drain, building dañado, empleados desaparecen, recursos perdidos)
  - **Modificadores temporales** (X días con producción -30%, mercado -15%, etc.)
  - **Ventana de respuesta** (24h in-game): activar seguro, evacuar, contratar PR, hacer donaciones → reduce el impacto.
- **Insurance system**: paga prima diaria → cobertura cuando llegan desastres.
- **Resilience XP**: superar desastres da puntos para **trait tree** permanente.

### 3. 🎯 DailyChallengeEngine — Retos rotatorios
- Cada **24h in-game** se generan **3 retos diarios** + **1 reto semanal**.
- Ejemplos:
  - 💰 "Gana 500k € en 1 día" → 50k bonus + 100 XP
  - 🏗️ "Construye 3 edificios sin demoler ninguno" → +5 reputación
  - 📈 "Compra 100 acciones de cualquier ticker" → token CRYPTO gratis
  - 🏁 "Gana 1 carrera F1 esta semana" → 200k €
  - ❤️ "Llega a 50 felicidad sin gastar en casino" → +10% energía perma 7 días
- **Rachas (streaks)**: completar X días seguidos da multiplicador acumulativo (cap x5).
- **Reto del finde** (dom): super-reto con recompensa permanente.

### 4. 🦹 HeistEngine — Atracos planificados
Mini-juego **roguelike** dentro del tycoon. **Inspiración: GTA Online, Payday**.
- **8 tipos de golpe** desbloqueables progresivamente:
  - Robo a tienda (fácil, 50k€)
  - Atraco a banco (medio, 800k€)
  - Hackeo a corporación rival (medio, 1.2M€ + dossier sobre rival)
  - Asalto al casino (difícil, 4M€)
  - Heist al aeropuerto (difícil, 6M€ + jet privado)
  - Robo en concesionario (fácil, coche random)
  - Atraco a yate de magnate (legendario, 15M€)
  - **The Big One** (final, 50M€ + empresa rival cae)
- **Tripulación**: 4 roles (líder, hacker, conductor, francotirador). Cada uno con skill.
- **Planificación**: elige enfoque (ruidoso vs sigiloso vs negociación), gasta cash en equipo.
- **Resolución**: tirada modificada por skills + plan + suerte → 4 outcomes (ÉXITO TOTAL · ÉXITO · ESCAPE · DESASTRE).
- **Heat**: cada golpe sube **policial heat**. Heat alto → eventos de policía persiguiéndote, embargo de cuentas, miembros de la tripulación caen.
- **Karma**: cada heist baja karma fuerte. Path "héroe" puede saltarse heists. Path "villano" desbloquea misiones extra.

---

## 🔮 Sistemas planeados (siguientes tandas)

### 5. 🌐 MultiCityEngine — Imperio global
- 5 ciudades nuevas (Tokio, Dubai, Lagos, NYC, Berlín) con economías propias.
- Logística entre ciudades, peajes, aranceles, tipos de cambio.

### 6. 🧬 TraitTreeEngine — Talentos permanentes
- 60 traits divididos en 5 ramas (Magnate, Visionario, Político, Artista, Outlaw).
- Se desbloquean con XP de Resilience + acciones específicas.
- Permanentes a través del prestigio.

### 7. ⚔️ HostileTakeoverEngine — Adquisiciones agresivas
- Comprar acciones de empresa rival hasta 51% → la absorbes.
- Defensa: poison pill, white knight, golden parachute.

### 8. 🎮 ArcadeEngine — Mini-juegos diversos
- 5 mini-juegos clásicos jugables en la oficina (snake, 2048, breakout, pong, tetris) con apuestas.

### 9. 🎭 EventSeasonsEngine — Temporadas y festivales
- Halloween, Navidad, Año Nuevo Chino, etc. — modifican mundo, NPCs, recompensas.

### 10. 🤖 AICompanionEngine — Asistente IA
- Un "asesor" que aprende tus patrones y sugiere acciones (con personalidad).

---

## 🎯 KPIs de éxito

Antes de llamarlo "1000× mejor", verificar:

- [ ] Una sesión típica dura **>2 horas** sin sentirse repetitiva (vs ~30 min hoy).
- [ ] Cada **20 min** ocurre algo *no rutinario* (crisis, oferta, reto, heist propuesto).
- [ ] La curva de poder permite **NG+ × 10** sin romper la economía.
- [ ] El jugador tiene **3+ paths** distintos (limpio, gris, criminal) con consecuencias visibles.
- [ ] Un día in-game (24 min reales) genera **>5 decisiones interesantes**.

---

## 🛠️ Decisiones técnicas

- **No** romper el contrato `GameState -> GameState` — engines puros.
- **No** mover archivos existentes — solo añadir.
- Todo `@Serializable` con defaults para que **saves antiguos sigan cargando**.
- Cada engine tickea con un período propio (crypto: 10s, disaster: 1440 ticks, daily: 1440, heist: on-demand).
- UI: una pantalla nueva por sistema, integrada en el menú "More".

---

*Actualizado: 2026-05-05 · Plan implementable en 4 tandas de QA.*
