# BoardWar - Entorno local

## Estado actual

El backend arranca contra los contenedores locales de Docker con esta configuracion:

- PostgreSQL publicado en `localhost:5433`
- Redis publicado en `localhost:6379`
- API Spring Boot en `localhost:8080`

La configuracion del backend esta en `backend/src/main/resources/application.yml` y admite variables de entorno:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:boardwar}
    username: ${DB_USER:boardwar}
    password: ${DB_PASSWORD:boardwar1234}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

## Historial de problemas y correcciones

### Problema 1: Puertos mal mapeados en docker-compose.yml

El error original: `docker-compose.yml` exponia PostgreSQL como `5000:5000`.

Eso es incorrecto por dos razones:
- PostgreSQL escucha en el puerto `5432` dentro del contenedor, no en `5000`
- `application.yml` usaba `5433` como default del host

El fix: cambiar el mapeo a `5433:5432` (host:contenedor).

En Docker hay dos puertos distintos:
- Puerto del host: el que usas desde tu maquina (`5433`)
- Puerto interno del contenedor: el servicio escucha en `5432` dentro del contenedor

### Problema 2: Dependencias de test inventadas en build.gradle.kts

Se habian declarado starters de test que no existen en Spring Boot:
- `spring-boot-starter-data-jpa-test`
- `spring-boot-starter-data-redis-test`
- `spring-boot-starter-flyway-test`
- `spring-boot-starter-security-test`
- `spring-boot-starter-validation-test`
- `spring-boot-starter-webmvc-test`
- `spring-boot-starter-websocket-test`

El fix: reemplazarlos con `spring-boot-starter-test` (el unico starter de test real) y `spring-security-test` para los tests de seguridad.

### Problema 3: Starters de implementacion incorrectos

- `spring-boot-starter-flyway` no existe → se usa `org.flywaydb:flyway-core`
- `spring-boot-starter-webmvc` no existe → se usa `spring-boot-starter-web`

## Arranque correcto

### 1. Levantar infraestructura

```powershell
docker compose up -d
docker compose ps
```

Debes ver:

- `boardwar_db` healthy en `0.0.0.0:5433->5432`
- `boardwar_redis` healthy en `0.0.0.0:6379->6379`

### 2. Arrancar backend

```powershell
cd backend
.\gradlew.bat bootRun
```

### 3. Ejecutar tests

```powershell
cd backend
.\gradlew.bat test
```

## Variables opcionales

Si cambias puertos, arranca con variables de entorno en lugar de tocar el YAML:

```powershell
$env:DB_PORT="5434"
$env:REDIS_PORT="6380"
.\gradlew.bat bootRun
```

## Schema de base de datos (V1__initial_schema.sql)

Tablas definidas con Flyway en `backend/src/main/resources/db/migration/`:

| Tabla | Proposito |
|---|---|
| `usuarios` | Cuentas de usuario con ELO |
| `diccionario_unidades` | Catalogo de tipos de unidad |
| `diccionario_cartas` | Catalogo de cartas |
| `partida` | Instancias de partidas |
| `estado_jugador_partida` | Recursos por jugador por partida |
| `registro_acciones_partida` | Log de acciones para replay/auditoria |

Hibernate esta configurado con `ddl-auto: validate` — no toca el schema, solo verifica que las entidades JPA coincidan con las tablas que Flyway crea.

## Stack tecnico

- **Backend**: Spring Boot, Java 21, Gradle (Kotlin DSL)
- **Base de datos**: PostgreSQL 16
- **Cache**: Redis 7
- **Migraciones**: Flyway
- **ORM**: Spring Data JPA / Hibernate
- **Seguridad**: Spring Security
- **Tiempo real**: Spring WebSocket
- **Tests**: JUnit 5 + Spring Boot Test

## Git: lo que se ignora

- `backend/build/` y `backend/.gradle/` — artefactos de compilacion
- `.gradle-user-home/` — cache local de Gradle
- `.env` y `.env.*` — secretos locales
- IDEs: `.idea/`, `.vscode/`, etc.
