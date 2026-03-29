# BoardWar — Índice Maestro de Desarrollo

> Cada bloque = una funcionalidad completa. Backend y frontend se desarrollan juntos.
> No se avanza al siguiente bloque hasta que el checkpoint visual funciona.

---

## Documentación del proyecto

| Doc | Qué contiene |
|---|---|
| `README.md` | Producto, reglas, mecánicas, assets, arranque rápido |
| `docs/boardwar.md` | Entorno local, Docker, historial de errores resueltos |
| `docs/fase2-mvp.md` | SQL, interfaces Java, fórmulas, estructuras Redis — referencia técnica |
| `docs/estrategia.md` | Descripción detallada de cada fase con archivos a crear |
| `docs/INDICE.md` | Este archivo — checklist maestro de todo el desarrollo |

---

## BLOQUE 0 — Entorno operativo

**Ya hecho:**
- [x] Docker Compose: PostgreSQL `5433:5432` + Redis `6379:6379`
- [x] Spring Boot arranca y conecta a ambos servicios
- [x] Flyway activo — `V1__initial_schema.sql` aplicado
- [x] `.gitignore` correcto — primer commit en `main` pusheado a GitHub

**Ya hecho:**
- [x] **Backend** — `GET /api/health` público devuelve `{ status: "ok" }`
- [x] **Backend** — CORS configurado en Spring Security para `http://localhost:5173`
- [x] **Frontend** — Vite + React + TypeScript + Phaser 3 + Axios en `/frontend`
- [x] **Frontend** — `client.ts`: axios con `baseURL: localhost:8080`
- [x] **Frontend** — `App.tsx` llama a `/api/health` al montar y muestra el resultado

**Checkpoint:** `localhost:5173` muestra "BoardWar ok" sin errores CORS en consola. ✅

---

## BLOQUE 1 — El mapa hexagonal se pinta

**Backend y frontend juntos:**
- [ ] **Backend** — `V2__game_schema.sql` (tablas: `hexagonos_partida`, `unidades_en_partida`, `mazo_partida`, `mano_jugador`, `efectos_ocultos`)
- [ ] **Backend** — Records Java del `BoardState` completo (`BoardState`, `HexTileState`, `UnitOnBoardState`, `PlayerState`, `HiddenEffectState`)
- [ ] **Backend** — `BoardStateRepository`: guarda/lee `BoardState` en Redis como JSON
- [ ] **Backend** — `POST /api/matches`: genera mapa 20×20 con semilla, persiste hexes en PostgreSQL, guarda `BoardState` en Redis, devuelve JSON
- [ ] **Backend** — `GET /api/matches/{id}`: lee `BoardState` de Redis
- [ ] **Frontend** — `BoardScene.ts`: recibe `BoardState`, convierte axial→píxeles (flat-top), pinta hex por hex con color según terreno
- [ ] **Frontend** — `BoardPage.tsx`: crea partida al montar, monta el canvas Phaser

**Checkpoint:** mapa hexagonal de colores visible en el navegador, generado desde el backend con semilla.

---

## BLOQUE 2 — Login y acceso protegido

**Backend y frontend juntos:**
- [ ] **Backend** — Entidad JPA `Usuario` mapeada a tabla `usuarios`
- [ ] **Backend** — `POST /api/auth/register`: crea usuario (BCrypt), devuelve JWT
- [ ] **Backend** — `POST /api/auth/login`: valida credenciales, devuelve JWT
- [ ] **Backend** — `JwtService`: firma (HS256) y validación con clave en `application.yml`
- [ ] **Backend** — `JwtAuthFilter`: carga `SecurityContext` en cada request
- [ ] **Backend** — `SecurityFilterChain`: `/api/auth/**` y `/api/health` públicos, resto requiere JWT
- [ ] **Frontend** — `LoginPage.tsx` y `RegisterPage.tsx` con formularios
- [ ] **Frontend** — `authStore.ts`: guarda JWT en `localStorage`, expone `playerId` y `nombre`
- [ ] **Frontend** — Axios interceptor: añade `Authorization: Bearer {token}` en cada petición
- [ ] **Frontend** — Rutas protegidas: redirige a `/login` si no hay token

**Checkpoint:** registrar usuario → login → ver nombre en pantalla. Sin token → 401 en consola.

---

## BLOQUE 3 — Fichas en el tablero

**Backend y frontend juntos:**
- [ ] **Backend** — `V3__seed_units.sql`: Infantería, Caballería, Arquero en `diccionario_unidades`
- [ ] **Backend** — Entidad JPA `UnidadEnPartida` mapeada a tabla `unidades_en_partida`
- [ ] **Backend** — Al crear partida (`POST /api/matches`): siembra 3 unidades por jugador en posiciones iniciales fijas
- [ ] **Backend** — `BoardState.units` incluye todas las unidades con stats completos
- [ ] **Frontend** — `BoardScene.ts`: dibuja una ficha (círculo azul = propia, rojo = rival) sobre cada hex ocupado
- [ ] **Frontend** — HP visible encima de cada ficha
- [ ] **Frontend** — `UnitInfoPanel.tsx`: clic en ficha → panel lateral con nombre, HP, ataque, defensa

**Checkpoint:** fichas de ambos jugadores en el mapa. Clic en ficha → panel de stats.

---

## BLOQUE 4 — Mover fichas

**Backend y frontend juntos:**
- [ ] **Backend** — `HexMovementService.distance(q1,r1,q2,r2)` — distancia axial→cúbica
- [ ] **Backend** — `HexMovementService.neighbors(q,r)` — 6 vecinos en axial
- [ ] **Backend** — `HexMovementService.isConnectedGroup(unitIds, board)` — valida grupo adyacente
- [ ] **Backend** — `HexMovementService.validateGroupMove(unitIds, dq, dr, board)` — terreno, colisiones, rango
- [ ] **Backend** — `HexMovementService.applyGroupMove(unitIds, dq, dr, board)` — aplica movimiento, descuenta 1 PM
- [ ] **Backend** — `POST /api/matches/{id}/units/move` — body: `{ unitIds, dq, dr }`
- [ ] **Backend** — Tests unitarios de `distance()` y `validateGroupMove()`
- [ ] **Frontend** — Clic en ficha propia → hexes alcanzables resaltados en verde
- [ ] **Frontend** — Clic en hex verde → `POST /units/move` → tablero se redibuja
- [ ] **Frontend** — `HudPanel.tsx`: PM restantes visibles, se actualizan tras cada movimiento

**Checkpoint:** seleccionar ficha → hexes verdes. Clicar hex → ficha se mueve. PM baja en el HUD.

---

## BLOQUE 5 — Turnos y recursos

**Backend y frontend juntos:**
- [ ] **Backend** — `TurnService.startTurn()`: regenera recursos, roba carta (si hay mazo), resetea PM=3
- [ ] **Backend** — `TurnService.endTurn()`: persiste en `registro_acciones_partida`, rota `jugador_activo`
- [ ] **Backend** — `EconomyService.spendOro/Suministro/Mana()`: valida saldo y descuenta
- [ ] **Backend** — `EconomyService.buyUnit()`: descuenta recursos, crea unidad en hex
- [ ] **Backend** — `POST /api/matches/{id}/turn/end`
- [ ] **Backend** — `POST /api/matches/{id}/units/buy` — body: `{ tipoUnidadId, q, r }`
- [ ] **Frontend** — `HudPanel.tsx` completo: oro, suministro, maná, PM, indicador "Tu turno / Turno rival"
- [ ] **Frontend** — Botón "Fin de turno" → `POST /turn/end` → HUD actualiza indicador
- [ ] **Frontend** — `ShopPanel.tsx`: lista unidades con coste, activo solo en tu turno
- [ ] **Frontend** — Comprar unidad → aparece en el mapa inmediatamente

**Checkpoint:** HUD con recursos visibles. "Fin de turno" funciona. Comprar unidad la pone en el mapa.

---

## BLOQUE 6 — Combate

**Backend y frontend juntos:**
- [ ] **Backend** — `CombatService.calculateDamage()`: fórmula completa con modificadores de terreno y RNG ±20%
- [ ] **Backend** — `CombatService.resolveAttack()`: evalúa triggers pre-combate, calcula daño, actualiza HP, persiste en `registro_acciones_partida`
- [ ] **Backend** — `POST /api/matches/{id}/combat` — body: `{ attackerUnitId, defenderUnitId }`
- [ ] **Frontend** — Al seleccionar ficha propia: fichas enemigas adyacentes resaltadas en rojo
- [ ] **Frontend** — Clic en ficha enemiga roja → `POST /combat`
- [ ] **Frontend** — `CombatResultPopup.tsx`: daño, factor RNG, HP resultante (desaparece a los 2s)
- [ ] **Frontend** — Tween de flash en la ficha atacada (Phaser)
- [ ] **Frontend** — Barra de HP se actualiza visualmente
- [ ] **Frontend** — HP = 0 → ficha desaparece del mapa con animación

**Checkpoint:** atacar ficha → popup con daño. Barra HP baja. HP 0 → ficha desaparece.

---

## BLOQUE 7 — Tiempo real (dos jugadores simultáneos)

**Backend y frontend juntos:**
- [ ] **Backend** — `WebSocketConfig`: STOMP sobre `/ws`, broker en `/topic` y `/user`
- [ ] **Backend** — Broadcast `BoardState` a `/topic/match/{id}/state` tras cada acción
- [ ] **Backend** — Mano del jugador → `/user/queue/match/{id}/hand` (privado, solo al dueño)
- [ ] **Backend** — Efectos ocultos → `/user/queue/match/{id}/hidden` (privado, solo al dueño)
- [ ] **Frontend** — `npm install @stomp/stompjs`
- [ ] **Frontend** — `useMatchSocket(matchId)`: conecta al montar, suscribe a `/topic/match/{id}/state`, desconecta al desmontar
- [ ] **Frontend** — Al recibir `BoardState` por WebSocket → Phaser redibuja sin polling

**Checkpoint:** dos pestañas del mismo match. Mover ficha en una pestaña → la otra se actualiza sola.

---

## BLOQUE 8 — Sistema de cartas y efectos ocultos

**Backend y frontend juntos:**
- [ ] **Backend** — `V4__seed_cards.sql`: Escudo, Emboscada, Curación, Sabotaje, Refuerzo en `diccionario_cartas`
- [ ] **Backend** — Al crear partida: asignar 5 cartas mezcladas a cada jugador en `mazo_partida`
- [ ] **Backend** — `TurnService.startTurn()`: roba 1 carta del mazo → inserta en `mano_jugador`
- [ ] **Backend** — Eventos de dominio: `UnitMovedEvent`, `PreDamageEvent`, `PostDamageEvent`, `HexEnteredEvent`, `UnitEliminatedEvent`, `TurnStartedEvent`
- [ ] **Backend** — `TriggerEvaluationService.matchesTrigger()`: evalúa si el efecto debe dispararse
- [ ] **Backend** — `TriggerEvaluationService.applyEffect()`: aplica JSONB del efecto al `BoardState`
- [ ] **Backend** — `TriggerEvaluationService.evaluate()`: escanea todos los efectos activos contra el evento recibido
- [ ] **Backend** — `POST /api/matches/{id}/cards/play` — carta de mano activa
- [ ] **Backend** — `POST /api/matches/{id}/cards/place-hidden` — body: `{ manoId, objetivoUnitId?, objetivoQ?, objetivoR? }`
- [ ] **Frontend** — `HandPanel.tsx`: franja inferior con cartas en mano, nombre + coste maná
- [ ] **Frontend** — Carta draggable sobre el canvas de Phaser
- [ ] **Frontend** — Drop en ficha/hex → `POST /cards/place-hidden`
- [ ] **Frontend** — Icono de carta boca abajo visible al propietario en el hex/ficha
- [ ] **Frontend** — Al rival le llega el efecto en el `BoardState` pero sin revelar la carta
- [ ] **Frontend** — Animación flip cuando llega `TriggerFiredEvent` por WebSocket

**Checkpoint:** cartas en la mano. Arrastrar "Emboscada" a un hex. Rival solo ve el icono. Rival mueve ficha al hex → carta se revela + daño aplicado en ambas pantallas.

---

## BLOQUE 9 — Lobby y fin de partida

**Backend y frontend juntos:**
- [ ] **Backend** — `GET /api/matches?estado=esperando`: lista partidas abiertas
- [ ] **Backend** — `POST /api/matches/{id}/join`: segundo jugador se une, estado → `en_curso`
- [ ] **Backend** — Detección de victoria: 0 unidades del rival o acción `rendirse`
- [ ] **Backend** — `EloService`: recalcula ELO (fórmula estándar K=32)
- [ ] **Backend** — Actualiza `partida.ganador_id`, `partida.estado = 'terminada'`, `usuarios.puntuacion_elo`
- [ ] **Backend** — Broadcast `GAME_OVER` con resultado y delta ELO por WebSocket
- [ ] **Frontend** — `LobbyPage.tsx`: lista partidas + botón "Crear partida" + botón "Unirse"
- [ ] **Frontend** — Crear partida → espera en `BoardPage` hasta que llegue rival
- [ ] **Frontend** — `GameOverScreen.tsx`: victoria/derrota, ELO antes → después, botón "Volver al lobby"

**Checkpoint:** dos sesiones distintas crean y unen partida. Juegan. Al terminar, ambas sesiones ven la pantalla de resultado con cambio de ELO.

---

## BLOQUE 10 — Assets

### Tiles de terreno `assets/tiles/` — 128×148 px flat-top
- [ ] `llanura.png`
- [ ] `bosque.png`
- [ ] `montana.png`
- [ ] `agua.png`
- [ ] `trinchera.png`

### Sprites de unidades `assets/units/` — 64×64 px fondo transparente
- [ ] `infanteria_a.png` / `infanteria_b.png`
- [ ] `caballeria_a.png` / `caballeria_b.png`
- [ ] `arquero_a.png` / `arquero_b.png`

### Arte de cartas `assets/cards/` — 120×180 px
- [ ] `card_back.png`
- [ ] `card_frame.png`
- [ ] `escudo.png` / `emboscada.png` / `curacion.png` / `sabotaje.png` / `refuerzo.png`

### Iconos UI `assets/ui/` — 32×32 px
- [ ] `icon_oro.png` / `icon_suministro.png` / `icon_mana.png` / `icon_pm.png` / `icon_hp.png` / `card_hidden.png`

### Efectos visuales `assets/fx/`
- [ ] Sprite sheet impacto de combate
- [ ] Sprite sheet flip de carta
- [ ] Efecto de curación
- [ ] Efecto de muerte de unidad

---

## Progreso global

```
BLOQUE 0  — Entorno operativo          ██████████  9/9   ✅ COMPLETO
BLOQUE 1  — Mapa hexagonal             ░░░░░░░░░░  0/7
BLOQUE 2  — Login y acceso             ░░░░░░░░░░  0/10
BLOQUE 3  — Fichas en tablero          ░░░░░░░░░░  0/7
BLOQUE 4  — Movimiento                 ░░░░░░░░░░  0/10
BLOQUE 5  — Turnos y recursos          ░░░░░░░░░░  0/10
BLOQUE 6  — Combate                    ░░░░░░░░░░  0/9
BLOQUE 7  — Tiempo real WebSocket      ░░░░░░░░░░  0/7
BLOQUE 8  — Cartas y triggers          ░░░░░░░░░░  0/15
BLOQUE 9  — Lobby y fin de partida     ░░░░░░░░░░  0/9
BLOQUE 10 — Assets                     ░░░░░░░░░░  0/18
```
