# BoardWar — Estrategia de desarrollo MVP

Cada fase termina con algo visible en el navegador. No se avanza a la siguiente hasta que el checkpoint visual funciona.

**Referencias:**
- `README.md` — descripción del producto, reglas del juego, assets necesarios, checklist maestro
- `fase2-mvp.md` — firmas Java, SQL completo, fórmulas, estructuras Redis
- `boardwar.md` — arranque del entorno local

---

## Stack completo

```
Backend   →  Java 21 + Spring Boot 4 · PostgreSQL 16 · Redis 7 · WebSocket STOMP
Frontend  →  React 18 + TypeScript · Vite · Phaser 3
Repo      →  monorepo: /backend  +  /frontend  (carpeta ya existe vacía)
```

---

## Fase 0 — Scaffold del frontend y CORS

### Objetivo visual
Abrir `http://localhost:5173` y ver una pantalla en blanco con el texto "BoardWar" — el frontend arranca y habla con el backend.

### Backend
- Añadir `@CrossOrigin` o `CorsConfigurationSource` en Spring Security permitiendo `http://localhost:5173`
- Crear endpoint público `GET /api/health` que devuelve `{ status: "ok" }`

### Frontend
Dentro de `/frontend`:
```
npm create vite@latest . -- --template react-ts
npm install phaser
npm install axios
```

Estructura inicial:
```
frontend/
├── src/
│   ├── main.tsx           punto de entrada React
│   ├── App.tsx            router raíz
│   ├── api/
│   │   └── client.ts      axios instance apuntando a localhost:8080
│   └── scenes/            Phaser scenes (vacío por ahora)
├── index.html
├── vite.config.ts
└── package.json
```

`client.ts`: instancia axios con `baseURL: http://localhost:8080` y `withCredentials: true`.

`App.tsx`: llama a `GET /api/health` al montar y muestra el resultado.

### Checkpoint
- `npm run dev` arranca en `localhost:5173`
- La consola del navegador muestra `{ status: "ok" }` sin errores CORS

---

## Fase 1 — El tablero hexagonal se pinta

### Objetivo visual
Un mapa hexagonal ~20×20 con colores por tipo de terreno aparece en el navegador. Sin datos reales — el backend genera un mapa mock con la semilla.

### Backend
- Implementar `V2__game_schema.sql` (ver `fase2-mvp.md`) — migracion Flyway
- Crear `BoardState` como record Java y `BoardStateRepository` que lo serializa/deserializa en Redis
- Crear endpoint `POST /api/matches` que:
  - Genera un mapa 20×20 con terrenos aleatorios usando `semilla_mapa`
  - Persiste `hexagonos_partida` en PostgreSQL
  - Construye el `BoardState` inicial y lo guarda en Redis
  - Devuelve el `BoardState` completo como JSON
- Crear endpoint `GET /api/matches/{id}` que lee el `BoardState` de Redis

### Frontend

**Phaser Scene: `BoardScene.ts`**
```
- Recibe el BoardState del backend (GET /api/matches/{id})
- Convierte coordenadas axiales (q,r) a píxeles con orientación flat-top:
    pixel_x = size * (3/2 * q)
    pixel_y = size * (sqrt(3)/2 * q + sqrt(3) * r)
- Dibuja cada hex con Graphics.fillHex() coloreado por terreno:
    llanura   → #a8d5a2  (verde claro)
    bosque    → #4a7c59  (verde oscuro)
    montana   → #8b7355  (marrón)
    agua      → #4a90d9  (azul)
    trinchera → #c8b560  (amarillo tierra)
```

**React wrapper: `BoardPage.tsx`**
- Crea una partida al montar (`POST /api/matches`)
- Monta el canvas de Phaser pasándole el `matchId`
- `BoardScene` carga el estado del backend y pinta el mapa

### Checkpoint
- Abrir la app y ver el mapa hexagonal coloreado
- El mapa cambia si recargas (semilla diferente) o es determinista si se fija la semilla

---

## Fase 2 — Autenticación

### Objetivo visual
Pantalla de login/registro. Tras hacer login, el usuario ve su nombre en la esquina superior y puede acceder al tablero.

### Backend
- Implementar entidad JPA `Usuario` mapeada a la tabla `usuarios`
- Endpoints públicos:
  - `POST /api/auth/register` → crea usuario, devuelve JWT
  - `POST /api/auth/login`    → valida credenciales, devuelve JWT
- El JWT se firma con una clave en `application.yml` (`boardwar.jwt.secret`)
- `SecurityFilterChain`: rutas `/api/auth/**` y `/api/health` públicas; el resto requiere JWT válido
- El `playerId` del JWT se inyecta automáticamente en los controladores protegidos

### Frontend

**Páginas React:**
```
/login     → LoginPage.tsx    formulario email + contraseña
/register  → RegisterPage.tsx formulario nombre + email + contraseña
/          → HomePage.tsx     si hay token: muestra nombre + botón "Nueva Partida"
                              si no hay token: redirige a /login
```

**Gestión del token:**
- `localStorage.setItem('bw_token', jwt)` tras login exitoso
- `axios` interceptor añade `Authorization: Bearer {token}` a todas las peticiones
- React Context o Zustand store con `{ playerId, nombre, token }`

### Checkpoint
- Registrar usuario, hacer login, ver el nombre en la UI
- Intentar acceder a `GET /api/matches/{id}` sin token → 401
- Con token → 200

---

## Fase 3 — Unidades en el tablero

### Objetivo visual
Tras crear una partida, aparecen fichas de ambos jugadores colocadas en el mapa. Hacer clic en una ficha muestra sus stats (HP, ataque, defensa) en un panel lateral.

### Backend
- Implementar entidad JPA `UnidadEnPartida` mapeada a `unidades_en_partida`
- Al crear la partida (`POST /api/matches`): sembrar posiciones iniciales de unidades para ambos jugadores (hardcoded para el MVP: 3 unidades por jugador en posiciones fijas por zona)
- Poblar el diccionario `diccionario_unidades` con al menos 2 tipos de unidad via script SQL de datos
- El `BoardState` ya incluye `units: Map<String, UnitOnBoardState>` — verificar que llega al frontend

Script SQL de datos iniciales (ejecutar manualmente o como migration `V3__seed_units.sql`):
```sql
INSERT INTO diccionario_unidades (nombre, vida_base, ataque_base, defensa_base, rango_movimiento, coste_oro, faccion)
VALUES
  ('Infantería',  100, 30, 20, 1, 50,  null),
  ('Caballería',  80,  45, 15, 2, 80,  null),
  ('Arquero',     60,  40, 10, 1, 60,  null);
```

### Frontend

**En `BoardScene.ts`:**
- Después de pintar los hexes, iterar `boardState.units`
- Por cada unidad: dibujar un círculo o sprite sobre el hex correspondiente
  - Color azul = jugador propio, rojo = enemigo
  - Mostrar número de HP encima
- `setInteractive()` en cada ficha: al hacer clic emitir evento `unit-selected`

**Panel lateral React: `UnitInfoPanel.tsx`**
- Escucha el evento `unit-selected` de Phaser (via EventBus o callback)
- Muestra: nombre, HP actual / HP máx, ataque, defensa, propietario

### Checkpoint
- Ver fichas de ambos jugadores en el mapa
- Clicar una ficha → ver sus stats en el panel

---

## Fase 4 — Movimiento

### Objetivo visual
Seleccionar una ficha propia, ver los hexes a los que puede moverse resaltados en verde, hacer clic en uno y la ficha se mueve.

### Backend
- Implementar `HexMovementService` (lógica de distancia, vecinos, `validateGroupMove`, `applyGroupMove`)
- Ver fórmulas y firmas en `fase2-mvp.md` — Sprint 1
- Endpoint `POST /api/matches/{id}/units/move`:
  ```json
  { "unitIds": ["uuid"], "dq": 1, "dr": 0 }
  ```
  Responde con el `BoardState` actualizado
- Descontar 1 PM del jugador activo

### Frontend

**Flujo de selección/movimiento en `BoardScene.ts`:**
```
Estado local de la scene:
  selectedUnitId: string | null
  reachableHexes: HexCoord[]

Al clicar una ficha propia:
  1. selectedUnitId = unitId
  2. calcular reachableHexes en cliente (misma fórmula de distancia axial)
  3. resaltar hexes alcanzables en verde semitransparente

Al clicar un hex resaltado:
  1. calcular (dq, dr) = (hex.q - unit.q, hex.r - unit.r)
  2. POST /api/matches/{id}/units/move
  3. recibir nuevo BoardState → redibujar toda la scene
  4. limpiar selección
```

**HUD React: `HudPanel.tsx`**
- Mostrar PM restantes del jugador activo
- Actualizar tras cada movimiento

### Checkpoint
- Seleccionar ficha → hexes verdes aparecen
- Clicar hex → ficha se mueve, PM baja en el HUD
- Intentar mover cuando PM = 0 → el backend rechaza, nada cambia

---

## Fase 5 — Turnos y Economía

### Objetivo visual
HUD con recursos (oro / suministro / maná) y PM. Botón "Fin de turno" que pasa el turno al otro jugador. Los recursos se regeneran al inicio del siguiente turno.

### Backend
- Implementar `TurnService` y `EconomyService` (firmas en `fase2-mvp.md` — Sprint 2)
- Endpoint `POST /api/matches/{id}/turn/end`
  - Persiste el estado en `registro_acciones_partida`
  - Actualiza `partida.jugador_activo`
  - Regenera recursos del siguiente jugador (ver tabla de regeneración en `fase2-mvp.md`)
  - Resetea PM a 3
  - Devuelve `BoardState` actualizado
- Endpoint `POST /api/matches/{id}/units/buy`:
  ```json
  { "tipoUnidadId": 1, "q": 5, "r": 3 }
  ```
  Descuenta oro y suministro, crea la unidad en el hex indicado

### Frontend

**`HudPanel.tsx` completo:**
```
┌─────────────────────────────────────────────────┐
│  Turno 3  │  Tu turno  │  PM: ●●○               │
│  Oro: 140 │  Suministro: 60 │  Maná: 25         │
│                              [Fin de turno]      │
└─────────────────────────────────────────────────┘
```

**Shop panel (básico):**
- Lista de unidades del diccionario con su coste
- Botón "Comprar" → `POST /api/matches/{id}/units/buy`
- Solo activo durante `FASE_PRINCIPAL` del jugador activo

### Checkpoint
- Pulsar "Fin de turno" → el HUD indica "Turno del rival"
- Recursos se actualizan al volver a ser tu turno
- Comprar una unidad → aparece en el mapa, el oro baja

---

## Fase 6 — Combate

### Objetivo visual
Seleccionar una ficha propia adyacente a un enemigo, hacer clic en el enemigo. Aparece una animación de ataque, la barra de HP del enemigo baja, y si llega a 0 la ficha desaparece.

### Backend
- Implementar `CombatService` con `calculateDamage()` (fórmula en `fase2-mvp.md` — Sprint 3)
- Endpoint `POST /api/matches/{id}/combat`:
  ```json
  { "attackerUnitId": "uuid", "defenderUnitId": "uuid" }
  ```
  Devuelve `CombatResult` + `BoardState` actualizado
- Guardar el resultado en `registro_acciones_partida.datos` con el `rngFactor` para auditoría

### Frontend

**En `BoardScene.ts`:**
- Al seleccionar ficha propia: si hay fichas enemigas adyacentes, resaltarlas en rojo
- Al clicar una ficha enemiga adyacente: modo combate
  - `POST /api/matches/{id}/combat`
  - Recibir `CombatResult`
  - Animación: flash en la ficha atacada (tween de escala)
  - Actualizar barra HP

**`CombatResultPopup.tsx`:**
```
┌──────────────────────┐
│  Infantería ataca    │
│  Daño: 28  (×1.12)  │
│  HP restante: 72     │
└──────────────────────┘
```
Desaparece solo después de 2 segundos.

### Checkpoint
- Atacar una ficha → popup con daño y factor RNG
- HP de la ficha defensora baja visualmente
- Si HP llega a 0: la ficha desaparece del mapa

---

## Fase 7 — Tiempo real con WebSocket

### Objetivo visual
Abrir la misma partida en dos pestañas del navegador. Mover una ficha en una pestaña y ver cómo se actualiza en la otra sin recargar.

### Backend
- Configurar STOMP sobre WebSocket en Spring
- Al finalizar cualquier acción (movimiento, combate, fin de turno): broadcast del `BoardState` al topic `/topic/match/{id}/state`
- Canal privado `/user/queue/match/{id}/hand` para la mano del jugador (no visible al rival)
- Ver canales en `fase2-mvp.md` — sección WebSocket

### Frontend
- Instalar `@stomp/stompjs`
- `useMatchSocket(matchId)` — hook que:
  - Conecta al WebSocket al montar
  - Se suscribe a `/topic/match/{id}/state`
  - Al recibir nuevo `BoardState`: actualiza el estado React → Phaser redibuja
  - Se desconecta al desmontar

### Checkpoint
- Pestaña A mueve una ficha → Pestaña B la ve moverse sola
- Pestaña B solo ve sus cartas en mano, no las de Pestaña A

---

## Fase 8 — Sistema de cartas

### Objetivo visual
Panel de cartas en la mano del jugador. Arrastrar una carta sobre una ficha o hex para colocarla boca abajo. El rival ve que hay "algo" en ese hex pero no qué carta es.

### Backend
- Implementar `TriggerEvaluationService` y el event bus (ver `fase2-mvp.md` — Sprint 4)
- Al inicio de la partida: asignar 5 cartas mezcladas a cada jugador en `mazo_partida`
- Robar 1 carta al inicio de cada turno
- Endpoints:
  - `POST /api/matches/{id}/cards/play` → jugar carta de mano activamente
  - `POST /api/matches/{id}/cards/place-hidden` → colocar boca abajo sobre unidad o hex
- Poblar `diccionario_cartas` con al menos 3 cartas de prueba:

```sql
INSERT INTO diccionario_cartas (nombre, tipo_carta, tipo_disparador, tipo_objetivo, efecto, coste_mana)
VALUES
  ('Escudo', 'buff', 'INICIO_COMBATE', 'UNIDAD_PROPIA',
   '{"defensa_bonus": 10}', 5),
  ('Emboscada', 'trampa', 'UNIDAD_ENTRA_HEX', 'HEX',
   '{"danio_directo": 20}', 8),
  ('Curación', 'buff', 'MANUAL', 'UNIDAD_PROPIA',
   '{"hp_restore": 30}', 10);
```

### Frontend

**`HandPanel.tsx`:**
```
Parte inferior de la pantalla — cartas horizontales.
Cada carta: nombre + coste maná + descripción corta.
Draggable sobre el canvas de Phaser.
```

**Interacción en `BoardScene.ts`:**
- Drop sobre una ficha propia → `POST /cards/place-hidden` con `objetivoUnitId`
- Drop sobre un hex vacío → `POST /cards/place-hidden` con `objetivoQ/R`
- El hex/ficha muestra un icono de carta boca abajo (solo visible al propietario)
- Al rival le llega un `BoardState` donde el hex/ficha tiene un efecto oculto sin revelar el tipo de carta

**Animación de trigger:**
- Cuando el servidor emite un `TriggerFiredEvent` vía WebSocket
- La carta boca abajo se anima: flip → revela el nombre → ejecuta efecto visual

### Checkpoint
- Ver la mano de cartas en el panel inferior
- Arrastrar "Emboscada" a un hex → el hex muestra icono de carta
- En la otra pestaña: solo se ve que "hay algo" en ese hex
- Mover una ficha rival al hex → la carta se revela + el daño se aplica

---

## Fase 9 — Lobby y fin de partida

### Objetivo visual
Pantalla de inicio con lista de partidas disponibles. Al terminar una partida (un jugador pierde todas las fichas o se rinde), aparece la pantalla de victoria/derrota con el cambio de ELO.

### Backend
- `GET /api/matches?estado=esperando` → lista de partidas en espera
- `POST /api/matches/{id}/join` → el segundo jugador se une
- Lógica de fin: al quedar 0 unidades o acción `rendirse`
  - Actualizar `partida.estado = 'terminada'`, `partida.ganador_id`
  - Recalcular ELO de ambos jugadores (fórmula Elo estándar K=32)
  - Broadcast de evento `GAME_OVER` vía WebSocket

### Frontend

**`LobbyPage.tsx`:**
```
[Crear partida]
────────────────────────
Partidas disponibles:
  • Partida de Marcos  [Unirse]
  • Partida de Ana     [Unirse]
```

**`GameOverScreen.tsx`** (overlay sobre el tablero):
```
┌──────────────────────────────────┐
│           VICTORIA               │
│   Tu ELO: 1000 → 1016  (+16)    │
│        [Volver al lobby]         │
└──────────────────────────────────┘
```

### Checkpoint
- Crear partida desde una sesión → aparece en el lobby de otra sesión
- Segunda sesión se une → ambas sesiones entran al tablero simultáneamente
- Un jugador pierde todas las fichas → aparece la pantalla de resultado en ambas pestañas

---

## Resumen de fases y dependencias

```
Fase 0  →  Scaffold frontend + CORS + health endpoint
  │
Fase 1  →  Tablero hexagonal pintado (mapa desde backend)
  │
Fase 2  →  Login/registro + JWT
  │
Fase 3  →  Unidades en el tablero + panel de stats
  │
Fase 4  →  Movimiento (clic para mover)
  │
Fase 5  →  Turnos + economía + compra de unidades
  │
Fase 6  →  Combate (daño, HP, eliminación)
  │
Fase 7  →  WebSocket (sincronización en tiempo real)
  │
Fase 8  →  Sistema de cartas y triggers
  │
Fase 9  →  Lobby + fin de partida + ELO
```

Cada fase puede hacerse de forma lineal. Las Fases 0–3 se pueden desarrollar con datos mock para no bloquear el frontend mientras el backend no está completo. A partir de la Fase 4 el backend es bloqueante.

---

## Archivos a crear por fase

```
Fase 0
  backend/  → SecurityConfig.java (CORS), HealthController.java
  frontend/ → vite + react bootstrap, client.ts, App.tsx

Fase 1
  backend/  → V2__game_schema.sql, BoardState.java (record), BoardStateRepository.java,
              MatchController.java (POST + GET /api/matches)
  frontend/ → BoardScene.ts, BoardPage.tsx

Fase 2
  backend/  → Usuario.java (entity), AuthController.java, JwtService.java, SecurityConfig.java
  frontend/ → LoginPage.tsx, RegisterPage.tsx, authStore.ts, axios interceptor

Fase 3
  backend/  → UnidadEnPartida.java (entity), V3__seed_units.sql
  frontend/ → UnitInfoPanel.tsx, dibujo de fichas en BoardScene.ts

Fase 4
  backend/  → HexMovementServiceImpl.java, MoveController.java
  frontend/ → lógica de selección + resaltado en BoardScene.ts, HudPanel.tsx (PM)

Fase 5
  backend/  → TurnServiceImpl.java, EconomyServiceImpl.java, TurnController.java
  frontend/ → HudPanel.tsx completo, ShopPanel.tsx

Fase 6
  backend/  → CombatServiceImpl.java, CombatController.java
  frontend/ → animación combate en BoardScene.ts, CombatResultPopup.tsx

Fase 7
  backend/  → WebSocketConfig.java, GameEventPublisher.java
  frontend/ → useMatchSocket.ts

Fase 8
  backend/  → TriggerEvaluationServiceImpl.java, CardController.java, V4__seed_cards.sql
  frontend/ → HandPanel.tsx, drag & drop en BoardScene.ts, animación trigger

Fase 9
  backend/  → LobbyController.java, EloService.java, lógica de fin de partida
  frontend/ → LobbyPage.tsx, GameOverScreen.tsx
```
