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