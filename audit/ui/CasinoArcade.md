# Auditoría QA UX — Casino & Arcade

## 1. Bet input: límites visibles

- **CasinoScreen L112-121**: `OutlinedTextField` con label "Apuesta (€)". Solo limita a 8 dígitos (L114) y valida `cash >= betAmt` (L125). **No muestra min/max explícito** — el usuario puede teclear cualquier cifra hasta 99.999.999.
- **ArcadeScreen L207-210** (`BetSelector`): muestra rango `MIN_BET–MAX_BET €` correctamente. Buen patrón. Chips rápidos L200-205 (50→5k).

## 2. Resultado claro (won/lost amount)

- **CasinoScreen L155-165**: tarjeta con borde Emerald/Ruby, texto "+payout" o "-bet" más número/color resultantes. Claro.
- **ArcadeScreen L294-297**: `RecentPlayRow` muestra `winnings - bet` con signo y color. L459-461 game over muestra recompensa. Correcto.

## 3. Animaciones

- **CasinoScreen L216-220**: `angle.animateTo` 2000 ms con `LinearOutSlowInEasing`. Ruleta gira 6 vueltas + offset. Bien.
- **ArcadeScreen L330-368**: game loop Snake con `delay(SNAKE_TICK_MS=220ms)`. Sin animaciones de transición ni feedback al comer (L363-366: solo incrementa score).

## 4. Anti-spam (cooldown)

- **CasinoScreen L125, L129-139**: `canBet = !spinning` desactiva botones durante el giro. Suficiente.
- **ArcadeScreen**: NO hay cooldown entre partidas. `arcadePlaceBet` (L157, L162, L165) se dispara sin throttle — un usuario puede spamear "Jugar" en stubs (L157) o reentrar SNAKE inmediatamente. **Riesgo medio**.

## 5. House edge visible

- **CasinoScreen L146-151**: explica payouts (rojo/negro 2x, 0 paga 18x) pero **NO muestra probabilidad real ni edge**. Con 18 casillas y 1 verde el edge es ~5,5% — invisible al jugador.
- **ArcadeScreen L522-527**: tabla de recompensas Snake (4-9 proporcional, 10=×1.5...) visible. Mejor transparencia que casino.

## Hallazgos críticos

1. Casino sin min-bet ni house-edge declarado.
2. Arcade sin cooldown anti-spam.
3. CasinoScreen L207 `vm.companyToPersonal(0.0)` parece código zombi/no-op confuso.
