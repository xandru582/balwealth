package com.empiretycoon.game.world

/**
 * Distritos lógicos en los que está dividida la ciudad de BalWealth.
 *
 * Cada distrito tiene un look distinto (paleta de calles, tipo de
 * decoración, densidad…) que [CityBlueprint] aplica al pintar el
 * [WorldGrid]. La UI superior (minimapa, cartel de distrito) usa
 * [displayName] y [emoji] directamente.
 */
enum class District(val displayName: String, val emoji: String) {
    DOWNTOWN("Centro", "🏙"),       // 🏙
    INDUSTRIAL("Polígono", "🏭"),   // 🏭
    RESIDENTIAL("Residencial", "🏘"), // 🏘
    COMMERCIAL("Comercial", "🏬"),  // 🏬
    PARK("Parque", "🌳"),            // 🌳
    HARBOR("Puerto", "⚓"),                // ⚓
    SUBURB("Afueras", "🛤")          // 🛤
}

/** Categorías de "lugar" reconocidas por el flujo de interacciones. */
enum class PlaceKind {
    CITY_HALL,
    BANK,
    STOCK_EXCHANGE,
    MARKET,
    TAVERN,
    NEWSPAPER,
    PARK_FOUNTAIN,
    BUS_STOP,
    GYM,
    UNIVERSITY,
    HOSPITAL,
    POLICE,
    COURT,
    MUSEUM,
    CASINO,
    NIGHTCLUB,
    BLACK_MARKET_DOOR,
    MANSION,
    APARTMENT,
    FACTORY_SLOT,
    FARM_SLOT,
    MINE_SLOT
}

/**
 * Punto de interés concreto sobre el [WorldGrid]. Sirve a la vez como
 * etiqueta para el minimapa, payload para diálogos al interactuar, y
 * referencia para emparejar con edificios poseídos por el jugador
 * (cuando [kind] es un *_SLOT).
 *
 * `(x, y)` apuntan al tile de "puerta" del lugar, donde el avatar puede
 * pulsar A para entrar.
 */
data class CityPlace(
    val id: String,
    val name: String,
    val district: District,
    val x: Int,
    val y: Int,
    val kind: PlaceKind
)

/**
 * Plano de la ciudad. Genera el [WorldGrid] inicial pintando calles,
 * aceras, agua, plazas y los tiles "slot" donde se asentarán los
 * edificios. La generación es determinista (ningún `Random`): la misma
 * versión del juego produce la misma ciudad para todo jugador.
 *
 * El grid es 96x96. La distribución macro:
 *
 *   y=0..18   PARK / SUBURB norte (campos, lago, casas)
 *   y=18..30  RESIDENTIAL norte
 *   y=30..50  DOWNTOWN (centro con bolsa, ayuntamiento, banco…)
 *   y=50..62  COMMERCIAL (mercado, taberna, periódico, casino…)
 *   y=62..76  INDUSTRIAL (fábricas y minas, estética dura)
 *   y=76..96  HARBOR (puerto, mar al sur)
 *
 * Un río horizontal cruza la ciudad alrededor de y=20 con un puente.
 * Otro pequeño afluente vertical separa COMMERCIAL e INDUSTRIAL.
 */
object CityBlueprint {

    const val width: Int = 96
    const val height: Int = 96

    /**
     * Lista exhaustiva de lugares notables. Estos coordinados deben caer
     * sobre tiles `DOOR` (o `PLAZA_TILE` para los abiertos), de modo que
     * `vm.interactWithFront()` pueda devolver un payload coherente.
     */
    val places: List<CityPlace> = listOf(
        // ---- DOWNTOWN (centro, y=30..50) ----
        CityPlace("p_city_hall",     "Ayuntamiento",         District.DOWNTOWN,    48, 36, PlaceKind.CITY_HALL),
        CityPlace("p_bank",          "Banco Central",        District.DOWNTOWN,    40, 36, PlaceKind.BANK),
        CityPlace("p_stock",         "Bolsa de Valores",     District.DOWNTOWN,    56, 36, PlaceKind.STOCK_EXCHANGE),
        CityPlace("p_court",         "Palacio de Justicia",  District.DOWNTOWN,    36, 44, PlaceKind.COURT),
        CityPlace("p_police",        "Comisaría",            District.DOWNTOWN,    60, 44, PlaceKind.POLICE),
        CityPlace("p_museum",        "Museo de la Ciudad",   District.DOWNTOWN,    44, 48, PlaceKind.MUSEUM),
        CityPlace("p_university",    "Universidad",          District.DOWNTOWN,    52, 48, PlaceKind.UNIVERSITY),
        CityPlace("p_newspaper",     "Redacción El Diario",  District.DOWNTOWN,    32, 38, PlaceKind.NEWSPAPER),
        CityPlace("p_hospital",      "Hospital General",     District.DOWNTOWN,    64, 38, PlaceKind.HOSPITAL),

        // ---- RESIDENTIAL (norte y=18..30) ----
        CityPlace("p_apt_1",         "Mi Apartamento",       District.RESIDENTIAL, 24, 24, PlaceKind.APARTMENT),
        CityPlace("p_apt_2",         "Apartamento Lujo",     District.RESIDENTIAL, 70, 24, PlaceKind.APARTMENT),
        CityPlace("p_mansion",       "Mansión del Magnate",  District.RESIDENTIAL, 48, 18, PlaceKind.MANSION),
        CityPlace("p_gym",           "Gimnasio Atlas",       District.RESIDENTIAL, 36, 28, PlaceKind.GYM),
        CityPlace("p_bus_north",     "Parada Norte",         District.RESIDENTIAL, 60, 28, PlaceKind.BUS_STOP),

        // ---- COMMERCIAL (y=50..62) ----
        CityPlace("p_market",        "Mercado Central",      District.COMMERCIAL,  40, 54, PlaceKind.MARKET),
        CityPlace("p_tavern",        "Taberna de Pepe",      District.COMMERCIAL,  32, 56, PlaceKind.TAVERN),
        CityPlace("p_casino",        "Casino Fortuna",       District.COMMERCIAL,  56, 54, PlaceKind.CASINO),
        CityPlace("p_nightclub",     "Club Neón",            District.COMMERCIAL,  64, 56, PlaceKind.NIGHTCLUB),
        CityPlace("p_black_market",  "Puerta Trasera",       District.COMMERCIAL,  74, 58, PlaceKind.BLACK_MARKET_DOOR),

        // ---- INDUSTRIAL (y=62..76) ----
        CityPlace("p_factory_1",     "Solar fabril A",       District.INDUSTRIAL,  16, 68, PlaceKind.FACTORY_SLOT),
        CityPlace("p_factory_2",     "Solar fabril B",       District.INDUSTRIAL,  28, 68, PlaceKind.FACTORY_SLOT),
        CityPlace("p_factory_3",     "Solar fabril C",       District.INDUSTRIAL,  40, 70, PlaceKind.FACTORY_SLOT),
        CityPlace("p_factory_4",     "Solar fabril D",       District.INDUSTRIAL,  56, 70, PlaceKind.FACTORY_SLOT),
        CityPlace("p_factory_5",     "Solar fabril E",       District.INDUSTRIAL,  68, 68, PlaceKind.FACTORY_SLOT),
        CityPlace("p_mine_1",        "Bocamina Norte",       District.INDUSTRIAL,  82, 66, PlaceKind.MINE_SLOT),
        CityPlace("p_mine_2",        "Bocamina Sur",         District.INDUSTRIAL,  82, 74, PlaceKind.MINE_SLOT),

        // ---- HARBOR (sur y=76..96) ----
        CityPlace("p_harbor_office", "Capitanía",            District.HARBOR,      48, 80, PlaceKind.CITY_HALL),
        CityPlace("p_bus_south",     "Parada Sur",           District.HARBOR,      30, 80, PlaceKind.BUS_STOP),
        CityPlace("p_dock_market",   "Lonja del Puerto",     District.HARBOR,      62, 84, PlaceKind.MARKET),

        // ---- PARK + SUBURB (norte y=0..18) ----
        CityPlace("p_park_fountain", "Fuente del Parque",    District.PARK,        48, 8,  PlaceKind.PARK_FOUNTAIN),
        CityPlace("p_park_bus",      "Parada del Parque",    District.PARK,        16, 12, PlaceKind.BUS_STOP),
        CityPlace("p_farm_1",        "Solar agrícola A",     District.SUBURB,      12, 4,  PlaceKind.FARM_SLOT),
        CityPlace("p_farm_2",        "Solar agrícola B",     District.SUBURB,      80, 6,  PlaceKind.FARM_SLOT),
        CityPlace("p_farm_3",        "Solar agrícola C",     District.SUBURB,      80, 14, PlaceKind.FARM_SLOT)
    )

    /**
     * Devuelve el distrito al que pertenece la baldosa (x, y). Útil para
     * la UI superior y el minimapa.
     */
    fun districtAt(x: Int, y: Int): District {
        // Bandas verticales reales del plano:
        return when {
            y < 18 -> {
                // norte: parque central + afueras agrícolas a los lados
                if (x in 28..68) District.PARK else District.SUBURB
            }
            y < 30 -> District.RESIDENTIAL
            y < 50 -> District.DOWNTOWN
            y < 62 -> District.COMMERCIAL
            y < 76 -> District.INDUSTRIAL
            else   -> District.HARBOR
        }
    }

    /** Construye el [WorldGrid] inicial completo. */
    fun build(): WorldGrid {
        // Empezamos con todo en hierba, luego pintamos por capas.
        var g = WorldGrid.blank(width = width, height = height, fill = TileType.GRASS)

        // 1) Fondo por distrito (distintos rellenos base).
        g = paintDistrictBackgrounds(g)

        // 2) Río horizontal con puente y pequeño afluente vertical.
        g = paintRiver(g)

        // 3) Calles troncales (avenidas) horizontales y verticales.
        g = paintAvenues(g)

        // 4) Calles secundarias por distritos.
        g = paintSecondaryStreets(g)

        // 5) Aceras a ambos lados de cada calle.
        g = paintSidewalks(g)

        // 6) Plaza central (plaza mayor) y plazoletas.
        g = paintPlazas(g)

        // 7) Vías del tren al sur del polígono industrial.
        g = paintRailway(g)

        // 8) Decoración de parque (árboles, hierba alta).
        g = paintParkDecor(g)

        // 9) Costa: arena en el borde del puerto.
        g = paintHarborSand(g)

        // 10) Lugares: pintamos paredes alrededor de cada CityPlace y la puerta.
        g = paintPlaceBuildings(g)

        return g
    }

    // ----------- pintores internos -----------

    private fun paintDistrictBackgrounds(g0: WorldGrid): WorldGrid {
        var g = g0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val d = districtAt(x, y)
                val (type, deco) = when (d) {
                    District.PARK -> TileType.GRASS to ((x * 7 + y * 3) and 0x3)
                    District.SUBURB -> TileType.GRASS to ((x + y) and 0x3)
                    District.RESIDENTIAL -> TileType.GRASS to ((x * 5 + y) and 0x3)
                    District.DOWNTOWN -> TileType.PLAZA_TILE to ((x + y * 2) and 0x3)
                    District.COMMERCIAL -> TileType.PLAZA_TILE to ((x * 3 + y) and 0x3)
                    District.INDUSTRIAL -> TileType.SIDEWALK to ((x + y * 5) and 0x3)
                    District.HARBOR -> TileType.SAND to ((x * 11 + y * 7) and 0x3)
                }
                g = g.setTile(x, y, type, deco)
            }
        }
        return g
    }

    private fun paintRiver(g0: WorldGrid): WorldGrid {
        var g = g0
        // Río horizontal en y=19..21 (entre PARK/SUBURB y RESIDENTIAL).
        for (y in 19..21) {
            for (x in 0 until width) {
                g = g.setTile(x, y, TileType.WATER, (x + y) and 0x3)
            }
        }
        // Puente que cruza el río en x=46..50.
        for (y in 19..21) {
            for (x in 46..50) {
                g = g.setTile(x, y, TileType.BRIDGE, 0)
            }
        }
        // Pequeño afluente vertical entre COMMERCIAL e INDUSTRIAL (x=50..51, y=58..62).
        for (x in 50..51) {
            for (y in 58..62) {
                g = g.setTile(x, y, TileType.WATER, (x + y) and 0x3)
            }
        }
        // Puente sobre el afluente.
        for (x in 50..51) {
            g = g.setTile(x, 60, TileType.BRIDGE, 0)
        }
        // Mar al sur del puerto (a partir de y=92).
        for (y in 92 until height) {
            for (x in 0 until width) {
                g = g.setTile(x, y, TileType.WATER, (x + y) and 0x3)
            }
        }
        return g
    }

    private fun paintAvenues(g0: WorldGrid): WorldGrid {
        var g = g0
        // Avenidas verticales: x=24, 48 (avenida central), 72.
        listOf(24, 48, 72).forEach { ax ->
            for (y in 0 until height) {
                if (g.tileAt(ax, y) == TileType.WATER) {
                    // sobre agua, conserva puente o pinta puente nuevo
                    g = g.setTile(ax, y, TileType.BRIDGE, 0)
                } else {
                    val deco = if (y % 4 == 0) 1 else 0
                    g = g.setTile(ax,     y, TileType.ROAD_LINE_V, deco)
                }
                // Calzada a izq y der.
                if (ax - 1 >= 0 && g.tileAt(ax - 1, y) != TileType.WATER) {
                    g = g.setTile(ax - 1, y, TileType.ROAD, 0)
                }
                if (ax + 1 < width && g.tileAt(ax + 1, y) != TileType.WATER) {
                    g = g.setTile(ax + 1, y, TileType.ROAD, 0)
                }
            }
        }
        // Avenidas horizontales: y=12, 36 (av. principal), 60, 84.
        listOf(12, 36, 60, 84).forEach { ay ->
            for (x in 0 until width) {
                if (g.tileAt(x, ay) == TileType.WATER) {
                    g = g.setTile(x, ay, TileType.BRIDGE, 0)
                } else {
                    val deco = if (x % 4 == 0) 1 else 0
                    g = g.setTile(x,     ay, TileType.ROAD_LINE_H, deco)
                }
                if (ay - 1 >= 0 && g.tileAt(x, ay - 1) != TileType.WATER) {
                    g = g.setTile(x, ay - 1, TileType.ROAD, 0)
                }
                if (ay + 1 < height && g.tileAt(x, ay + 1) != TileType.WATER) {
                    g = g.setTile(x, ay + 1, TileType.ROAD, 0)
                }
            }
        }
        return g
    }

    private fun paintSecondaryStreets(g0: WorldGrid): WorldGrid {
        var g = g0
        // En DOWNTOWN, malla densa cada 8 tiles.
        for (y in 30 until 50 step 8) {
            for (x in 0 until width) {
                if (g.tileAt(x, y) == TileType.WATER) continue
                g = g.setTile(x, y, TileType.ROAD, 0)
            }
        }
        for (x in 32 until 64 step 8) {
            for (y in 30 until 50) {
                if (g.tileAt(x, y) == TileType.WATER) continue
                g = g.setTile(x, y, TileType.ROAD, 0)
            }
        }
        // En INDUSTRIAL, anchas pero pocas: cada 12 tiles.
        for (x in 12 until width step 12) {
            for (y in 62 until 76) {
                if (g.tileAt(x, y) == TileType.WATER) continue
                g = g.setTile(x, y, TileType.ROAD, 0)
            }
        }
        // En SUBURB, callejones cada 16.
        for (x in 16 until width step 16) {
            for (y in 0 until 18) {
                if (g.tileAt(x, y) == TileType.WATER) continue
                if (g.tileAt(x, y) == TileType.GRASS)
                    g = g.setTile(x, y, TileType.ROAD, 0)
            }
        }
        // Muelle del HARBOR: paseo continuo en y=88.
        for (x in 4 until width - 4) {
            g = g.setTile(x, 88, TileType.ROAD, 0)
        }
        return g
    }

    private fun paintSidewalks(g0: WorldGrid): WorldGrid {
        var g = g0
        // Recorremos el grid: cualquier ROAD/ROAD_LINE_*/BRIDGE rodeado de no-vía
        // recibe acera en la celda vecina si es hierba/plaza.
        for (y in 0 until height) {
            for (x in 0 until width) {
                val t = g.tileAt(x, y)
                if (!t.isRoadLike()) continue
                // 4-vecinos
                listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0).forEach { (dx, dy) ->
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until width || ny !in 0 until height) return@forEach
                    val nt = g.tileAt(nx, ny)
                    if (nt == TileType.GRASS || nt == TileType.PLAZA_TILE) {
                        g = g.setTile(nx, ny, TileType.SIDEWALK, (nx + ny) and 0x3)
                    }
                }
            }
        }
        return g
    }

    private fun paintPlazas(g0: WorldGrid): WorldGrid {
        var g = g0
        // Plaza central frente al ayuntamiento (x=44..52, y=38..44).
        for (y in 38..44) {
            for (x in 44..52) {
                g = g.setTile(x, y, TileType.PLAZA_TILE, ((x * 3 + y) and 0x3))
            }
        }
        // Plaza del mercado (x=36..44, y=52..58).
        for (y in 52..58) {
            for (x in 36..44) {
                g = g.setTile(x, y, TileType.PLAZA_TILE, ((x + y) and 0x3))
            }
        }
        // Plaza del parque (centro del parque).
        for (y in 6..12) {
            for (x in 44..52) {
                g = g.setTile(x, y, TileType.PLAZA_TILE, ((x + y * 2) and 0x3))
            }
        }
        return g
    }

    private fun paintRailway(g0: WorldGrid): WorldGrid {
        var g = g0
        // Vías al borde sur del polígono industrial, en y=75.
        for (x in 0 until width) {
            // Mantener cruces accesibles donde haya avenidas.
            if (x == 24 || x == 48 || x == 72) {
                g = g.setTile(x, 75, TileType.ROAD, 0)
            } else {
                g = g.setTile(x, 75, TileType.RAIL, x and 0x3)
            }
        }
        return g
    }

    private fun paintParkDecor(g0: WorldGrid): WorldGrid {
        var g = g0
        // Salpicar tiles de "bosque" (FOREST_FLOOR) en el parque.
        for (y in 0 until 18) {
            for (x in 28..68) {
                val current = g.tileAt(x, y)
                if (current != TileType.GRASS) continue
                // patrón pseudo-aleatorio determinista
                val n = (x * 31 + y * 17) and 0xF
                if (n < 4) {
                    g = g.setTile(x, y, TileType.FOREST_FLOOR, n and 0x3)
                }
            }
        }
        return g
    }

    private fun paintHarborSand(g0: WorldGrid): WorldGrid {
        var g = g0
        // Borde de arena entre el muelle y el mar.
        for (y in 89..91) {
            for (x in 0 until width) {
                if (g.tileAt(x, y).isRoadLike()) continue
                g = g.setTile(x, y, TileType.SAND, (x + y) and 0x3)
            }
        }
        return g
    }

    /**
     * Para cada CityPlace pinta un pequeño edificio:
     *   - 5x4 muros (o 7x5 para grandes equipamientos) detrás de la puerta
     *   - suelo interior
     *   - una baldosa DOOR justo en (x, y) con `setPayload` apuntando al place.id
     *
     * Los slots (FARM_SLOT, FACTORY_SLOT, MINE_SLOT) se dejan como puerta sobre
     * suelo neutro: cuando el jugador construye allí, el motor sustituirá el
     * payload por el `building.id` real.
     */
    private fun paintPlaceBuildings(g0: WorldGrid): WorldGrid {
        var g = g0
        for (place in places) {
            val isBig = when (place.kind) {
                PlaceKind.CITY_HALL, PlaceKind.STOCK_EXCHANGE,
                PlaceKind.UNIVERSITY, PlaceKind.HOSPITAL,
                PlaceKind.MUSEUM, PlaceKind.MANSION,
                PlaceKind.CASINO -> true
                else -> false
            }
            val w = if (isBig) 7 else 5
            val h = if (isBig) 5 else 4

            val left = place.x - w / 2
            val top = place.y - h     // edificio "encima" de la puerta
            for (yy in top until top + h) {
                for (xx in left until left + w) {
                    if (xx !in 0 until width || yy !in 0 until height) continue
                    val onEdge = (xx == left || xx == left + w - 1 ||
                                  yy == top  || yy == top + h - 1)
                    if (onEdge) {
                        g = g.setTile(xx, yy, TileType.WALL, ((xx + yy) and 0x3))
                    } else {
                        g = g.setTile(xx, yy, TileType.FLOOR_INDOOR, ((xx * yy) and 0x3))
                    }
                }
            }
            // La puerta justo en (place.x, place.y), con payload = place.id.
            g = g.setTile(place.x, place.y, TileType.DOOR, 0)
            g = g.setPayload(place.x, place.y, place.id)

            // Para slots vacíos (FACTORY_SLOT, FARM_SLOT, MINE_SLOT): destacamos
            // el suelo interior con FLOOR_INDOOR pero no marcamos la puerta como
            // edificio activo del jugador (sigue siendo place.id, que el flujo
            // de "construir aquí" reemplazará por el building real).
        }
        return g
    }

    private fun TileType.isRoadLike(): Boolean = when (this) {
        TileType.ROAD, TileType.ROAD_LINE_H, TileType.ROAD_LINE_V, TileType.BRIDGE -> true
        else -> false
    }
}
