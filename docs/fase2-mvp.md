# BoardWar — Manual de Desarrollo MVP

Stack: Java 21 + Spring Boot · PostgreSQL + Flyway · Redis · WebSocket (STOMP)

---

## Sprint 0 — Base de Datos y Estado en Memoria

### V2 Migración SQL — tablas de juego

```sql
-- V2__game_schema.sql

-- TIPOS DE TERRENO
CREATE TYPE tipo_terreno AS ENUM (
    'llanura', 'bosque', 'montana', 'agua', 'trinchera'
);

-- HEXAGONOS POR PARTIDA
-- Un registro por celda del mapa. Generado al crear la partida usando semilla_mapa.
CREATE TABLE hexagonos_partida (
    id          BIGSERIAL PRIMARY KEY,
    partida_id  UUID NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    q           INTEGER NOT NULL,
    r           INTEGER NOT NULL,
    terreno     tipo_terreno NOT NULL DEFAULT 'llanura',
    bloqueado   BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (partida_id, q, r)
);

-- UNIDADES EN TABLERO
CREATE TABLE unidades_en_partida (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partida_id      UUID NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    propietario_id  UUID NOT NULL REFERENCES usuarios(id),
    tipo_unidad_id  INTEGER NOT NULL REFERENCES diccionario_unidades(id_tipo_unidad),
    q               INTEGER NOT NULL,
    r               INTEGER NOT NULL,
    hp_actual       INTEGER NOT NULL,
    turno_creacion  INTEGER NOT NULL DEFAULT 1,
    eliminada       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (partida_id, q, r)   -- una unidad por hex
);

-- MAZO DE CARTAS POR JUGADOR EN PARTIDA
-- Al inicio se asignan N cartas mezcladas con la semilla. orden_en_mazo define el tope.
CREATE TABLE mazo_partida (
    id              BIGSERIAL PRIMARY KEY,
    partida_id      UUID NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    jugador_id      UUID NOT NULL REFERENCES usuarios(id),
    id_carta        INTEGER NOT NULL REFERENCES diccionario_cartas(id_carta),
    orden_en_mazo   INTEGER NOT NULL,
    robada          BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (partida_id, jugador_id, orden_en_mazo)
);

-- MANO DEL JUGADOR (cartas robadas no jugadas)
CREATE TABLE mano_jugador (
    id              BIGSERIAL PRIMARY KEY,
    partida_id      UUID NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    jugador_id      UUID NOT NULL REFERENCES usuarios(id),
    id_carta        INTEGER NOT NULL REFERENCES diccionario_cartas(id_carta),
    jugada          BOOLEAN NOT NULL DEFAULT FALSE
);

-- EFECTOS OCULTOS EN TABLERO
-- Carta colocada boca abajo sobre una unidad, un hex o el jugador enemigo.
-- Se revela cuando se cumple el tipo_disparador.
CREATE TABLE efectos_ocultos (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partida_id      UUID NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    propietario_id  UUID NOT NULL REFERENCES usuarios(id),
    id_carta        INTEGER NOT NULL REFERENCES diccionario_cartas(id_carta),
    -- objetivo puede ser una unidad, un hex o NULL (afecta al jugador)
    objetivo_unidad_id  UUID REFERENCES unidades_en_partida(id),
    objetivo_q          INTEGER,
    objetivo_r          INTEGER,
    revelado        BOOLEAN NOT NULL DEFAULT FALSE,
    ejecutado       BOOLEAN NOT NULL DEFAULT FALSE,
    turno_colocado  INTEGER NOT NULL
);

-- ÍNDICES
CREATE INDEX idx_unidades_partida     ON unidades_en_partida(partida_id) WHERE NOT eliminada;
CREATE INDEX idx_hexagonos_partida    ON hexagonos_partida(partida_id);
CREATE INDEX idx_efectos_partida      ON efectos_ocultos(partida_id) WHERE NOT ejecutado;
CREATE INDEX idx_mazo_orden           ON mazo_partida(partida_id, jugador_id, orden_en_mazo);
```

---

### Estado en Redis — estructura JSON

Clave: `boardwar:match:{partida_id}`
Expiración: 24h
Serializado como JSON, mapeado con Jackson a records Java.

```
BoardState {
    matchId:        String (UUID)
    phase:          TurnPhase                     // enum: INICIO | MAIN | COMBATE | FIN
    activePlayerId: String (UUID)
    turnNumber:     int
    lastUpdated:    Instant

    playerA:  PlayerState
    playerB:  PlayerState

    hexGrid:  Map<String, HexTileState>           // clave: "q,r"
    units:    Map<String, UnitOnBoardState>       // clave: UUID unidad
    hidden:   List<HiddenEffectState>             // solo visibles al propietario
}

PlayerState {
    playerId:               String (UUID)
    oro:                    int
    suministro:             int
    mana:                   int
    puntosMandoRestantes:   int
    cartasEnMano:           List<CardInHandState>
}

CardInHandState {
    manoId:     long
    idCarta:    int
    nombre:     String
    tipo:       String
    disparador: String
    efecto:     Map<String, Object>              // JSONB del diccionario
    costesOro:  int
    costesMana: int
}

HexTileState {
    q:          int
    r:          int
    terreno:    String                           // llanura | bosque | montana | agua | trinchera
    bloqueado:  boolean
    unitId:     String | null                    // UUID de la unidad que lo ocupa
}

UnitOnBoardState {
    unitId:         String (UUID)
    ownerId:        String (UUID)
    tipoUnidadId:   int
    nombre:         String
    q:              int
    r:              int
    hpActual:       int
    hpMax:          int
    ataque:         int
    defensa:        int
    rangoMovimiento: int
}

HiddenEffectState {
    efectoId:       String (UUID)
    ownerId:        String (UUID)
    idCarta:        int
    disparador:     String
    objetivoUnitId: String | null
    objetivoQ:      Integer | null
    objetivoR:      Integer | null
    revelado:       boolean
}
```

---

## Sprint 1 — Motor Hexagonal y Movimiento

### Coordenadas axiales → cúbicas

```
axial_to_cube(q, r):
    x = q
    z = r
    y = -x - z

cube_distance(a, b):
    return max(|a.x - b.x|, |a.y - b.y|, |a.z - b.z|)

hex_distance(q1, r1, q2, r2):
    return cube_distance(axial_to_cube(q1,r1), axial_to_cube(q2,r2))
```

Los 6 vecinos en axial:

```
DIRECTIONS = [
    (+1,  0), (+1, -1), ( 0, -1),
    (-1,  0), (-1, +1), ( 0, +1)
]

neighbors(q, r) = [(q + dq, r + dr) for (dq, dr) in DIRECTIONS]
```

---

### Firma: `HexMovementService`

```java
interface HexMovementService {

    // Calcula distancia entre dos hexágonos
    int distance(int q1, int r1, int q2, int r2);

    // Devuelve las 6 celdas vecinas (pueden estar fuera del mapa)
    List<HexCoord> neighbors(int q, int r);

    // True si las unidades forman un grupo conectado (adyacencia por distancia == 1)
    boolean isConnectedGroup(List<String> unitIds, BoardState board);

    // Valida que todas las unidades del grupo pueden moverse en dirección (dq, dr):
    //   - destino existe en el mapa
    //   - destino no es bloqueado
    //   - destino no ocupado por otra unidad
    //   - la unidad tiene rango_movimiento >= 1
    MoveValidationResult validateGroupMove(
        List<String> unitIds,
        int dq, int dr,
        BoardState board
    );

    // Aplica el movimiento al BoardState en memoria y devuelve el estado actualizado.
    // Descuenta 1 PM del jugador activo.
    BoardState applyGroupMove(
        List<String> unitIds,
        int dq, int dr,
        BoardState board
    );
}

record HexCoord(int q, int r) {}

record MoveValidationResult(
    boolean valid,
    String reason       // null si valid == true
) {}
```

---

### Modificadores de terreno para movimiento

```
llanura   → coste_movimiento = 1   bloqueado = false
bosque    → coste_movimiento = 2   bloqueado = false
montana   → coste_movimiento = 3   bloqueado = false
agua      → coste_movimiento = ∞   bloqueado = true
trinchera → coste_movimiento = 1   bloqueado = false  (defensiva, no bloquea)
```

Un grupo de unidades gasta **1 PM** si todas caben en el movimiento con su `rango_movimiento`.
Si alguna unidad no tiene `rango_movimiento` suficiente para el terreno destino, el movimiento es inválido.

---

## Sprint 2 — Gestión de Turnos y Economía

### Máquina de estados del turno

```
INICIO_TURNO
    │
    │ [regenerar recursos, robar carta, resetear PM]
    ▼
FASE_PRINCIPAL
    │  acciones posibles:
    │    · mover unidad/grupo     (-1 PM por acción)
    │    · comprar unidad         (-oro, -suministro)
    │    · jugar carta de mano    (-oro/-mana)
    │    · colocar carta oculta   (-mana)
    │    · declarar combate  ─────────────────────────┐
    │    · pasar turno ────────────────────────────┐  │
    │                                              │  │
    ▼                                              │  ▼
FASE_COMBATE  ◄────────────────────────────────────┘
    │  acciones posibles:
    │    · resolver ataque (calculadora de daño)
    │    · evaluar triggers de cartas ocultas
    │    · aplicar efectos post-combate
    │
    ▼
FIN_TURNO
    │
    │ [persistir BoardState en PostgreSQL como registro_acciones]
    │ [actualizar estado_jugador_partida]
    │ [cambiar jugador_activo en partida]
    │
    ▼
INICIO_TURNO (jugador contrario)
```

---

### Firmas: `TurnService` y `EconomyService`

```java
interface TurnService {

    // Inicia el turno: regenera recursos según reglas, roba carta, resetea PM.
    // Actualiza BoardState en Redis y partida.jugador_activo en PostgreSQL.
    BoardState startTurn(UUID matchId, UUID playerId);

    // Transiciona FASE_PRINCIPAL → FASE_COMBATE
    BoardState declareCombat(UUID matchId, UUID attackingUnitId, UUID targetUnitId);

    // Finaliza el turno: persiste acciones, rota jugador activo.
    BoardState endTurn(UUID matchId, UUID playerId);

    TurnPhase currentPhase(UUID matchId);
}

enum TurnPhase { INICIO_TURNO, FASE_PRINCIPAL, FASE_COMBATE, FIN_TURNO }

interface EconomyService {

    // Valida saldo y descuenta. Lanza InsufficientResourcesException si no alcanza.
    PlayerState spendOro(UUID matchId, UUID playerId, int cantidad);
    PlayerState spendSuministro(UUID matchId, UUID playerId, int cantidad);
    PlayerState spendMana(UUID matchId, UUID playerId, int cantidad);

    // Compra una unidad: valida recursos, crea UnitOnBoardState, actualiza Redis y PostgreSQL.
    // Retorna la unidad creada.
    UnitOnBoardState buyUnit(UUID matchId, UUID playerId, int tipoUnidadId, int q, int r);
}
```

---

### Regeneración de recursos por turno

```
Cada inicio de turno el jugador activo recibe:
    oro        += 20
    suministro += 10
    mana       += 5
    PM          = 3   (reset, no acumula)

Techo máximo:
    oro        ≤ 300
    suministro ≤ 150
    mana       ≤ 60
```

---

## Sprint 3 — Resolución de Combate

### Modificadores de terreno para combate

```
Terreno del DEFENSOR:
    llanura   → def_modifier = 1.0
    bosque    → def_modifier = 1.2
    montana   → def_modifier = 1.4
    trinchera → def_modifier = 1.5

Terreno del ATACANTE:
    llanura   → atk_modifier = 1.0
    montana   → atk_modifier = 0.8   (cuesta atacar desde montaña)
    bosque    → atk_modifier = 0.9
    trinchera → atk_modifier = 1.0
```

---

### Fórmula de daño

```
effective_attack  = attacker.ataque  * atk_terrain_modifier
effective_defense = defender.defensa * def_terrain_modifier

base_damage = max(1, effective_attack - effective_defense)
rng_factor  = 0.8 + (random_seed * 0.4)     // rango [0.8, 1.2]  → ±20%
                                              // random_seed ∈ [0.0, 1.0)
final_damage = round(base_damage * rng_factor)

defender.hp_nuevo = max(0, defender.hpActual - final_damage)
```

La semilla RNG se toma de `SecureRandom` en el servidor y se guarda en `registro_acciones_partida.datos` para reproducibilidad y auditoría.

---

### Firma: `CombatService`

```java
interface CombatService {

    // Punto de entrada único para resolver un ataque.
    // Evalúa triggers PRE-combate antes de calcular daño.
    // Actualiza HP en BoardState (Redis) y persiste en PostgreSQL.
    CombatResult resolveAttack(
        UUID matchId,
        UUID attackerUnitId,
        UUID defenderUnitId
    );
}

record CombatResult(
    UUID attackerUnitId,
    UUID defenderUnitId,
    int  attackerHpBefore,
    int  defenderHpBefore,
    int  finalDamage,
    double rngFactor,          // para mostrar en UI
    int  attackerHpAfter,
    int  defenderHpAfter,
    boolean defenderEliminated,
    List<TriggerFiredEvent> triggersActivated
) {}

record DamageCalcInput(
    int ataque,
    int defensa,
    int hpActual,
    String terreno
) {}

// Uso interno en CombatService:
// int damage = calculateDamage(attacker, defender, atkTerrain, defTerrain, rng)
int calculateDamage(
    DamageCalcInput attacker,
    DamageCalcInput defender,
    String attackerTerrain,
    String defenderTerrain,
    double rngSeed              // [0.0, 1.0) generado por SecureRandom
);
```

---

## Sprint 4 — Sistema de Cartas y Triggers

### Tipos de disparador (`tipo_disparador` en `diccionario_cartas`)

```
INICIO_TURNO          → al iniciar el turno del propietario
INICIO_COMBATE        → antes de calcular daño, atacante o defensor
FIN_COMBATE           → después de calcular daño
UNIDAD_ENTRA_HEX      → cuando cualquier unidad pisa el hex objetivo
UNIDAD_MUERE          → cuando la unidad objetivo llega a 0 HP
MANUAL                → el jugador la juega activamente desde la mano
```

---

### Event Bus — eventos del dominio

```java
// Todos los eventos extienden GameEvent
abstract class GameEvent {
    UUID matchId;
    UUID playerId;
    Instant timestamp;
}

class UnitMovedEvent     extends GameEvent { UUID unitId; int fromQ; int fromR; int toQ; int toR; }
class CombatDeclaredEvent extends GameEvent { UUID attackerId; UUID defenderId; }
class PreDamageEvent     extends GameEvent { UUID attackerId; UUID defenderId; }
class PostDamageEvent    extends GameEvent { CombatResult result; }
class UnitEliminatedEvent extends GameEvent { UUID unitId; UUID killedByPlayerId; }
class TurnStartedEvent   extends GameEvent { int turnNumber; }
class HexEnteredEvent    extends GameEvent { UUID unitId; int q; int r; }

// Evento disparado cuando una carta oculta se revela y ejecuta
class TriggerFiredEvent  extends GameEvent {
    UUID efectoId;
    String triggerType;
    Map<String, Object> efectoAplicado;
}
```

El bus usa `ApplicationEventPublisher` de Spring. Cada servicio publica eventos; `TriggerEvaluationService` los escucha con `@EventListener`.

---

### Firma: `TriggerEvaluationService`

```java
interface TriggerEvaluationService {

    // Evalúa todos los efectos ocultos activos en la partida
    // cuyo tipo_disparador coincide con el evento recibido.
    // Devuelve los efectos que se revelaron y ejecutaron.
    List<TriggerFiredEvent> evaluate(GameEvent event, BoardState board);

    // Condición de disparo para un efecto concreto contra un evento.
    // Retorna true si el efecto debe revelarse.
    boolean matchesTrigger(HiddenEffectState effect, GameEvent event);

    // Aplica el efecto del JSONB `efecto` al BoardState.
    // Modifica board en memoria; el caller persiste el resultado.
    BoardState applyEffect(HiddenEffectState effect, BoardState board);
}
```

---

### Flujo completo de evaluación pre-combate

```
CombatService.resolveAttack(matchId, attackerId, defenderId)
    │
    ├─ publish(PreDamageEvent)
    │       │
    │       └─ TriggerEvaluationService.evaluate(event, board)
    │               │
    │               ├─ para cada HiddenEffectState no ejecutado:
    │               │       matchesTrigger(effect, PreDamageEvent)?
    │               │           sí → applyEffect(effect, board)
    │               │                marcar revelado=true, ejecutado=true
    │               │                emit TriggerFiredEvent
    │               │
    │               └─ retorna List<TriggerFiredEvent>
    │
    ├─ calculateDamage(attacker_stats_post_efectos, defender_stats_post_efectos, rng)
    │
    ├─ actualizar HP en BoardState (Redis)
    ├─ persistir en registro_acciones_partida (PostgreSQL)
    ├─ si defender.hp == 0 → publish(UnitEliminatedEvent)
    │
    └─ retorna CombatResult
```

---

## Estructura de paquetes Java

```
k82studio.backend
├── domain
│   ├── model          // Records/entidades de dominio (no JPA)
│   ├── port           // Interfaces de servicios (los de arriba)
│   └── event          // GameEvent y subclases
├── application        // Implementaciones de los puertos
│   ├── HexMovementServiceImpl
│   ├── TurnServiceImpl
│   ├── EconomyServiceImpl
│   ├── CombatServiceImpl
│   └── TriggerEvaluationServiceImpl
├── infrastructure
│   ├── persistence    // Repositorios JPA
│   ├── redis          // BoardStateRepository (serialización JSON ↔ Redis)
│   └── websocket      // STOMP controllers, SessionRegistry
└── api
    ├── rest           // @RestController endpoints HTTP
    └── ws             // @MessageMapping handlers WebSocket
```

---

## Endpoints REST mínimos para el MVP

```
POST   /api/matches                          Crear partida
GET    /api/matches/{id}                     Estado actual (desde Redis)
POST   /api/matches/{id}/turn/end            Finalizar turno
POST   /api/matches/{id}/units/move          Mover grupo de unidades
POST   /api/matches/{id}/units/buy           Comprar unidad
POST   /api/matches/{id}/combat              Declarar y resolver combate
POST   /api/matches/{id}/cards/play          Jugar carta de mano
POST   /api/matches/{id}/cards/place-hidden  Colocar carta oculta
```

---

## WebSocket — canales STOMP

```
SUBSCRIBE /topic/match/{id}/state       broadcast a ambos jugadores (BoardState público)
SUBSCRIBE /user/queue/match/{id}/hand   solo al jugador conectado (mano privada)
SUBSCRIBE /user/queue/match/{id}/hidden solo al propietario (efectos ocultos propios)

SEND /app/match/{id}/action             mensaje de acción del cliente → servidor
```

---

## Orden de implementación recomendado

```
Sprint 0  →  Migraciones SQL · Records Java del BoardState · BoardStateRepository (Redis)
Sprint 1  →  HexMovementService · tests unitarios de distance() y validateGroupMove()
Sprint 2  →  TurnService · EconomyService · endpoint /turn/end y /units/move
Sprint 3  →  CombatService · calculateDamage() · endpoint /combat
Sprint 4  →  Event bus · TriggerEvaluationService · endpoint /cards/place-hidden
WebSocket →  Capa de transporte encima de los servicios ya testeados
Frontend  →  Grilla hexagonal (render) → movimiento → combate → cartas
```
