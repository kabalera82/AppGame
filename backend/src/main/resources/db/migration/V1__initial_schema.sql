-- EXTENSIONES
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- USUARIOS
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre_usuario VARCHAR(32) UNIQUE NOT NULL,
    correo VARCHAR(255) UNIQUE NOT NULL,
    contrasena_hash TEXT NOT NULL,
    puntuacion_elo INTEGER NOT NULL DEFAULT 1000,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- DICCIONARIO DE UNIDADES
CREATE TABLE diccionario_unidades (
    id_tipo_unidad SERIAL PRIMARY KEY,
    nombre VARCHAR(64) NOT NULL,
    vida_base INTEGER NOT NULL,
    ataque_base INTEGER NOT NULL,
    defensa_base INTEGER NOT NULL,
    rango_movimiento INTEGER NOT NULL,
    coste_oro INTEGER NOT NULL DEFAULT 0,
    coste_suministro INTEGER NOT NULL DEFAULT 0,
    coste_mana INTEGER NOT NULL DEFAULT 0,
    faccion VARCHAR(32),
    metadatos JSONB
);

-- DICCIONARIO DE CARTAS
CREATE TABLE diccionario_cartas (
    id_carta SERIAL PRIMARY KEY,
    nombre VARCHAR(64) NOT NULL,
    descripcion TEXT,
    tipo_carta VARCHAR(32) NOT NULL,
    tipo_disparador VARCHAR(32) NOT NULL,
    tipo_objetivo VARCHAR(32) NOT NULL,
    efecto JSONB NOT NULL,
    coste_oro INTEGER NOT NULL DEFAULT 0,
    coste_mana INTEGER NOT NULL DEFAULT 0,
    faccion VARCHAR(32)
);

-- PARTIDAS
CREATE TABLE partida (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jugador_a_id UUID NOT NULL REFERENCES usuarios(id),
    jugador_b_id UUID NOT NULL REFERENCES usuarios(id),
    estado VARCHAR(16) NOT NULL DEFAULT 'esperando',
    ganador_id UUID REFERENCES usuarios(id),
    turno_actual INTEGER NOT NULL DEFAULT 1,
    jugador_activo UUID NOT NULL,
    semilla_mapa INTEGER NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_finalizacion TIMESTAMPTZ,
    CONSTRAINT jugadores_distintos CHECK (jugador_a_id <> jugador_b_id)
);

-- ESTADO DE RECURSOS POR JUGADOR
CREATE TABLE estado_jugador_partida (
    id SERIAL PRIMARY KEY,
    partida_id UUID NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    jugador_id UUID NOT NULL REFERENCES usuarios(id),
    oro INTEGER NOT NULL DEFAULT 100,
    suministro INTEGER NOT NULL DEFAULT 50,
    mana INTEGER NOT NULL DEFAULT 30,
    puntos_mando_restantes INTEGER NOT NULL DEFAULT 3,
    UNIQUE(partida_id, jugador_id)
);

-- REGISTRO DE ACCIONES
CREATE TABLE registro_acciones_partida (
    id BIGSERIAL PRIMARY KEY,
    partida_id UUID NOT NULL REFERENCES partida(id) ON DELETE CASCADE,
    turno INTEGER NOT NULL,
    jugador_id UUID NOT NULL REFERENCES usuarios(id),
    tipo_accion VARCHAR(32) NOT NULL,
    datos JSONB NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ÍNDICES
CREATE INDEX idx_registro_acciones_partida ON registro_acciones_partida(partida_id, turno);
CREATE INDEX idx_estado_jugador_partida ON estado_jugador_partida(partida_id);
CREATE INDEX idx_estado_partida ON partida(estado);