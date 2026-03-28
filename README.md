# BoardWar

Wargame táctico digital 1v1 por turnos. Dos jugadores se enfrentan en un tablero hexagonal, gestionando recursos, moviendo unidades y jugando cartas de efecto oculto para superar al rival.

---

## El Juego

### Concepto

BoardWar combina la táctica de un wargame clásico con un sistema de cartas de efecto oculto al estilo Gwent/Stratego. No hay suerte pura — cada decisión importa, pero las cartas ocultas introducen incertidumbre estratégica: nunca sabes exactamente qué tiene preparado el rival.

Una partida dura entre 20 y 45 minutos. Termina cuando un jugador pierde todas sus unidades o se rinde.

---

## El Tablero

### Mapa hexagonal

- Cuadrícula de aproximadamente **20×20 hexágonos** con orientación flat-top
- Coordenadas **axiales (q, r)** — el sistema más eficiente para cálculos de distancia y vecindad
- El mapa se genera proceduralmente usando una **semilla numérica** guardada en la partida, garantizando que ambos jugadores vean el mismo mapa y que la partida sea reproducible

### Tipos de terreno

| Terreno | Color | Efecto sobre movimiento | Efecto sobre combate |
|---|---|---|---|
| Llanura | Verde claro | Coste 1 | Sin modificador |
| Bosque | Verde oscuro | Coste 2 | Defensa +20% |
| Montaña | Marrón | Coste 3 | Defensa +40% / Ataque -20% desde aquí |
| Agua | Azul | Infranqueable | — |
| Trinchera | Amarillo tierra | Coste 1 | Defensa +50% |

### Sistema de Puntos de Mando (PM)

Cada turno el jugador activo dispone de **3 PM**. Cada acción de movimiento cuesta **1 PM**:

- Mover **una unidad** individualmente → 1 PM
- Mover un **grupo de unidades adyacentes** en la misma dirección → 1 PM (independientemente del tamaño del grupo)

Los PM no se acumulan entre turnos.

---

## Las Unidades

### Stats de cada unidad

| Stat | Descripción |
|---|---|
| HP | Puntos de vida. La unidad muere al llegar a 0 |
| Ataque | Potencia ofensiva base |
| Defensa | Resistencia base (reduce el daño recibido) |
| Rango de movimiento | Número de hexes que puede recorrer por acción de movimiento |

### Unidades base (MVP)

| Unidad | HP | Ataque | Defensa | Mov | Coste |
|---|---|---|---|---|---|
| Infantería | 100 | 30 | 20 | 1 | 50 oro |
| Caballería | 80 | 45 | 15 | 2 | 80 oro |
| Arquero | 60 | 40 | 10 | 1 | 60 oro |

### Fórmula de combate

```
effective_attack  = atacante.ataque  × modificador_terreno_atacante
effective_defense = defensor.defensa × modificador_terreno_defensor
base_damage       = max(1, effective_attack − effective_defense)
rng_factor        = 0.8 + random × 0.4     → ±20% de variación
daño_final        = round(base_damage × rng_factor)
```

El combate no es determinista pero **una unidad no puede morir de un solo golpe** si tiene HP suficiente. El factor RNG se guarda para reproducibilidad.

---

## Las Cartas

### Concepto

Al inicio de la partida cada jugador recibe un mazo de cartas mezcladas. Se roba 1 carta al inicio de cada turno. Las cartas pueden:

1. **Jugarse activamente** desde la mano (efecto inmediato, cuesta maná/oro)
2. **Colocarse boca abajo** sobre una unidad propia, una unidad enemiga o un hexágono

Las cartas colocadas boca abajo son **invisibles para el rival** — solo ve que "hay algo". Se revelan y ejecutan automáticamente cuando se cumple su condición de disparo.

### Tipos de disparador

| Disparador | Cuándo se activa |
|---|---|
| `INICIO_TURNO` | Al empezar el turno del propietario |
| `INICIO_COMBATE` | Antes de calcular daño (puede modificar stats) |
| `FIN_COMBATE` | Después de calcular daño |
| `UNIDAD_ENTRA_HEX` | Cuando cualquier unidad pisa el hex objetivo |
| `UNIDAD_MUERE` | Cuando la unidad objetivo llega a 0 HP |
| `MANUAL` | Jugada activamente desde la mano |

### Cartas base (MVP)

| Carta | Tipo | Disparador | Efecto | Coste |
|---|---|---|---|---|
| Escudo | Buff | `INICIO_COMBATE` | Defensa +10 en el siguiente combate | 5 maná |
| Emboscada | Trampa | `UNIDAD_ENTRA_HEX` | 20 de daño directo a quien pise el hex | 8 maná |
| Curación | Buff | `MANUAL` | Restaura 30 HP a una unidad propia | 10 maná |
| Sabotaje | Debuff | `INICIO_TURNO` | El rival pierde 1 PM en su siguiente turno | 12 maná |
| Refuerzo | Buff | `FIN_COMBATE` | Si la unidad sobrevive, recupera 15 HP | 7 maná |

---

## La Economía

Tres recursos independientes. Se regeneran cada inicio de turno del jugador activo.

| Recurso | Uso | Regeneración / turno | Techo |
|---|---|---|---|
| Oro | Comprar unidades | +20 | 300 |
| Suministro | Construir trincheras (futuro) | +10 | 150 |
| Maná | Jugar cartas | +5 | 60 |

---

## Assets necesarios

### Sprites de terreno (hex tiles)

```
assets/tiles/
├── llanura.png       hex plano, verde claro, textura hierba
├── bosque.png        hex con árboles esquemáticos
├── montana.png       hex con silueta de montaña
├── agua.png          hex azul con ondas simples
└── trinchera.png     hex con líneas de trinchera, tierra removida
```

Tamaño recomendado: **128×148 px** (hex flat-top, relación 1:√3/2 aproximada)

### Sprites de unidades

```
assets/units/
├── infanteria_a.png   versión jugador A (azul)
├── infanteria_b.png   versión jugador B (rojo)
├── caballeria_a.png
├── caballeria_b.png
├── arquero_a.png
└── arquero_b.png
```

Tamaño recomendado: **64×64 px**, fondo transparente, encajan dentro del hex

### Sprites de cartas

```
assets/cards/
├── card_back.png         reverso genérico para cartas ocultas
├── card_frame.png        marco reutilizable para todas las cartas
├── escudo.png            ilustración de carta Escudo
├── emboscada.png
├── curacion.png
├── sabotaje.png
└── refuerzo.png
```

Tamaño recomendado: **120×180 px** (proporción 2:3, estándar de cartas)

### Iconos de UI

```
assets/ui/
├── icon_oro.png          moneda dorada
├── icon_suministro.png   caja de suministros
├── icon_mana.png         cristal azul
├── icon_pm.png           estrella o chevron para Puntos de Mando
├── icon_hp.png           corazón o escudo para vida
├── btn_endturn.png       botón "Fin de turno"
└── card_hidden.png       icono de carta boca abajo (visible al rival)
```

### Efectos visuales (partículas / tweens)

```
assets/fx/
├── hit_flash.png         sprite sheet de impacto para combate
├── heal_sparkle.png      destellos de curación
├── card_reveal.png       sprite sheet de flip de carta
└── unit_death.png        sprite sheet de desaparición de unidad
```

### Fuentes

- **UI / HUD**: fuente sans-serif limpia — Inter o Roboto
- **Títulos / Cartas**: fuente con personalidad táctica/medieval — MedievalSharp o Cinzel
- Cargar desde Google Fonts o incluir como archivo `.woff2` en `/assets/fonts/`

---

## Stack técnico

| Capa | Tecnología |
|---|---|
| Backend | Java 21 + Spring Boot 4 |
| Base de datos | PostgreSQL 16 (Docker) |
| Cache / estado en vivo | Redis 7 (Docker) |
| Migraciones | Flyway |
| Tiempo real | WebSocket STOMP |
| Frontend UI | React 18 + TypeScript + Vite |
| Frontend canvas | Phaser 3 |
| HTTP client | Axios |

---

## Estructura del repositorio

```
BoardWar/
├── backend/                  Spring Boot — API REST + WebSocket
│   └── src/main/
│       ├── java/k82studio/backend/
│       │   ├── domain/       modelos, puertos, eventos
│       │   ├── application/  implementaciones de servicios
│       │   ├── infrastructure/ JPA, Redis, WebSocket
│       │   └── api/          controllers REST y WS
│       └── resources/
│           ├── application.yml
│           └── db/migration/ scripts Flyway (V1, V2, V3...)
├── frontend/                 React + Phaser
│   └── src/
│       ├── api/              axios client
│       ├── pages/            LoginPage, LobbyPage, BoardPage
│       ├── components/       HudPanel, HandPanel, UnitInfoPanel...
│       ├── scenes/           Phaser scenes (BoardScene)
│       └── hooks/            useMatchSocket, useAuth...
├── docker-compose.yml        PostgreSQL + Redis
└── docs/
    ├── boardwar.md           entorno local y arranque
    ├── fase2-mvp.md          especificación técnica: SQL, interfaces, fórmulas
    ├── estrategia.md         fases de desarrollo con checkpoints visuales
    └── README.md             este archivo
```

---

## Arranque rápido

```powershell
# 1. Infraestructura
docker compose up -d

# 2. Backend
cd backend
.\gradlew.bat bootRun

# 3. Frontend (cuando esté scaffoldeado)
cd frontend
npm run dev
```

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`
- PostgreSQL: `localhost:5433` / base de datos: `boardwar`
- Redis: `localhost:6379`

---

## Índice maestro — Checklist de desarrollo

> Marca cada ítem al completarlo. El orden importa: cada bloque depende del anterior.

### Infraestructura y entorno
- [x] Docker Compose con PostgreSQL (`5433:5432`) y Redis (`6379:6379`)
- [x] Backend Spring Boot arranca y conecta a PostgreSQL y Redis
- [x] Flyway activo — `V1__initial_schema.sql` aplicado
- [x] `.gitignore` configurado — builds y caches excluidos
- [x] Primer commit en `master`
- [ ] Frontend scaffoldeado (`npm create vite`, Phaser, Axios instalados)
- [ ] CORS configurado en Spring Security para `localhost:5173`
- [ ] `GET /api/health` responde `200` desde el frontend

### Base de datos — Fase datos de juego
- [ ] `V2__game_schema.sql` — tablas: `hexagonos_partida`, `unidades_en_partida`, `mazo_partida`, `mano_jugador`, `efectos_ocultos`
- [ ] `V3__seed_units.sql` — 3 unidades base en `diccionario_unidades`
- [ ] `V4__seed_cards.sql` — 5 cartas base en `diccionario_cartas`

### Backend — Domain layer
- [ ] Records Java del `BoardState` y todos sus tipos anidados (`PlayerState`, `HexTileState`, `UnitOnBoardState`, `HiddenEffectState`)
- [ ] `BoardStateRepository` — serialización JSON ↔ Redis con Jackson
- [ ] Entidades JPA: `Usuario`, `Partida`, `UnidadEnPartida`, `EfectoOculto`, `MazoPartida`, `ManoJugador`

### Backend — Autenticación
- [ ] `POST /api/auth/register` — crea usuario, devuelve JWT
- [ ] `POST /api/auth/login` — valida credenciales, devuelve JWT
- [ ] `SecurityFilterChain` — rutas públicas y protegidas configuradas
- [ ] `JwtService` — firma y validación de tokens

### Backend — Partida y tablero
- [ ] `POST /api/matches` — crea partida, genera mapa hexagonal con semilla, siembra unidades iniciales, guarda `BoardState` en Redis
- [ ] `GET /api/matches/{id}` — lee `BoardState` de Redis
- [ ] `GET /api/matches?estado=esperando` — lista de partidas disponibles
- [ ] `POST /api/matches/{id}/join` — segundo jugador se une

### Backend — Motor hexagonal (Sprint 1)
- [ ] `HexMovementService.distance()` — distancia en coordenadas axiales
- [ ] `HexMovementService.neighbors()` — 6 vecinos de un hex
- [ ] `HexMovementService.isConnectedGroup()` — valida grupo adyacente
- [ ] `HexMovementService.validateGroupMove()` — terreno, colisiones, rango
- [ ] `HexMovementService.applyGroupMove()` — aplica movimiento al `BoardState`
- [ ] `POST /api/matches/{id}/units/move` — endpoint de movimiento
- [ ] Tests unitarios de `distance()` y `validateGroupMove()`

### Backend — Turnos y economía (Sprint 2)
- [ ] `TurnService.startTurn()` — regenera recursos, roba carta, resetea PM
- [ ] `TurnService.endTurn()` — persiste en PostgreSQL, rota jugador activo
- [ ] `EconomyService.spendOro/Suministro/Mana()` — valida y descuenta
- [ ] `EconomyService.buyUnit()` — compra y coloca unidad
- [ ] `POST /api/matches/{id}/turn/end`
- [ ] `POST /api/matches/{id}/units/buy`

### Backend — Combate (Sprint 3)
- [ ] `CombatService.calculateDamage()` — fórmula con modificadores de terreno y RNG ±20%
- [ ] `CombatService.resolveAttack()` — evalúa triggers pre-combate, calcula daño, persiste
- [ ] `POST /api/matches/{id}/combat`

### Backend — Cartas y triggers (Sprint 4)
- [ ] Event bus con `ApplicationEventPublisher` — eventos: `UnitMovedEvent`, `PreDamageEvent`, `PostDamageEvent`, `HexEnteredEvent`, `UnitEliminatedEvent`, `TurnStartedEvent`
- [ ] `TriggerEvaluationService.matchesTrigger()` — evalúa condición de disparo
- [ ] `TriggerEvaluationService.applyEffect()` — aplica JSONB del efecto al `BoardState`
- [ ] `POST /api/matches/{id}/cards/play`
- [ ] `POST /api/matches/{id}/cards/place-hidden`

### Backend — WebSocket
- [ ] `WebSocketConfig` — STOMP sobre WebSocket, endpoint `/ws`
- [ ] Broadcast `BoardState` a `/topic/match/{id}/state` tras cada acción
- [ ] Canal privado `/user/queue/match/{id}/hand` — mano del jugador
- [ ] Canal privado `/user/queue/match/{id}/hidden` — efectos ocultos propios

### Backend — Fin de partida
- [ ] Detección de condición de victoria (0 unidades o rendición)
- [ ] `EloService` — recalcula ELO de ambos jugadores (K=32)
- [ ] Actualiza `partida.estado`, `partida.ganador_id`, `usuarios.puntuacion_elo`
- [ ] Broadcast evento `GAME_OVER` vía WebSocket

### Frontend — Scaffold y auth
- [ ] Vite + React + TypeScript inicializado en `/frontend`
- [ ] Phaser 3 y Axios instalados
- [ ] `client.ts` — axios con baseURL y interceptor JWT
- [ ] `LoginPage.tsx` y `RegisterPage.tsx`
- [ ] Auth store (`localStorage` + React Context o Zustand)
- [ ] Rutas protegidas — redirige a `/login` si no hay token

### Frontend — Lobby
- [ ] `LobbyPage.tsx` — lista partidas disponibles + botón "Crear partida"
- [ ] Crear partida → redirige a `BoardPage` con el `matchId`
- [ ] Unirse a partida → redirige a `BoardPage`

### Frontend — Tablero hexagonal
- [ ] `BoardScene.ts` — Phaser scene que pinta el mapa hexagonal
- [ ] Conversión axial → píxeles (flat-top): `x = size × (3/2 × q)`, `y = size × (√3/2 × q + √3 × r)`
- [ ] Colores por tipo de terreno (o sprites si los assets están listos)
- [ ] `BoardPage.tsx` — React wrapper que monta el canvas Phaser

### Frontend — Unidades y HUD
- [ ] Fichas pintadas sobre los hexes (círculo coloreado o sprite)
- [ ] `UnitInfoPanel.tsx` — stats de la unidad seleccionada
- [ ] `HudPanel.tsx` — recursos, PM restantes, turno actual
- [ ] Selección de ficha con clic
- [ ] Resaltado de hexes alcanzables en verde

### Frontend — Movimiento
- [ ] Clic en hex resaltado → `POST /api/matches/{id}/units/move`
- [ ] Redibujado del tablero con el nuevo `BoardState`
- [ ] PM se actualiza en el HUD tras cada movimiento

### Frontend — Turnos y economía
- [ ] Botón "Fin de turno" → `POST /api/matches/{id}/turn/end`
- [ ] `ShopPanel.tsx` — lista de unidades comprables con costes
- [ ] Comprar unidad → aparece en el mapa
- [ ] Recursos se actualizan visualmente

### Frontend — Combate
- [ ] Fichas enemigas adyacentes resaltadas en rojo al seleccionar
- [ ] Clic en enemigo adyacente → `POST /api/matches/{id}/combat`
- [ ] `CombatResultPopup.tsx` — muestra daño y factor RNG
- [ ] Animación de impacto (tween en Phaser)
- [ ] Barra de HP actualizada visualmente
- [ ] Ficha eliminada desaparece del mapa

### Frontend — WebSocket
- [ ] `useMatchSocket.ts` — hook que conecta STOMP y suscribe al topic del match
- [ ] Recibir `BoardState` actualizado → Phaser redibuja automáticamente
- [ ] Sincronización probada con dos pestañas del navegador

### Frontend — Cartas
- [ ] `HandPanel.tsx` — cartas en la mano, parte inferior de la pantalla
- [ ] Drag & drop de carta sobre ficha o hex
- [ ] Carta colocada boca abajo: icono visible en el hex/ficha
- [ ] Animación de reveal cuando se activa el trigger

### Frontend — Fin de partida
- [ ] `GameOverScreen.tsx` — victoria/derrota con cambio de ELO
- [ ] Botón "Volver al lobby"

### Assets — Arte

#### Tiles de terreno
- [ ] `llanura.png` (128×148 px)
- [ ] `bosque.png`
- [ ] `montana.png`
- [ ] `agua.png`
- [ ] `trinchera.png`

#### Sprites de unidades
- [ ] `infanteria_a.png` / `infanteria_b.png` (64×64 px)
- [ ] `caballeria_a.png` / `caballeria_b.png`
- [ ] `arquero_a.png` / `arquero_b.png`

#### Arte de cartas
- [ ] `card_back.png` (120×180 px)
- [ ] `card_frame.png`
- [ ] `escudo.png`
- [ ] `emboscada.png`
- [ ] `curacion.png`
- [ ] `sabotaje.png`
- [ ] `refuerzo.png`

#### Iconos de UI
- [ ] `icon_oro.png`
- [ ] `icon_suministro.png`
- [ ] `icon_mana.png`
- [ ] `icon_pm.png`
- [ ] `icon_hp.png`
- [ ] `card_hidden.png`

#### Efectos visuales
- [ ] Sprite sheet de impacto de combate
- [ ] Sprite sheet de flip de carta
- [ ] Efecto de curación
- [ ] Efecto de muerte de unidad

---

## Documentación del proyecto

| Archivo | Contenido |
|---|---|
| `README.md` | Este archivo — producto, reglas, assets, checklist maestro |
| `docs/boardwar.md` | Entorno local, arranque, historial de errores de configuración |
| `docs/fase2-mvp.md` | Especificación técnica: SQL, interfaces Java, fórmulas, estructuras Redis |
| `docs/estrategia.md` | Fases de desarrollo con checkpoints visuales y archivos a crear |
