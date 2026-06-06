# FamiCup API

Backend Spring Boot de FamiCup. Actua como middleware entre el frontend React, PostgreSQL y API-Football v3. El frontend debe consultar esta API; los datos de partidos, equipos y resultados se sirven desde PostgreSQL y se actualizan por sincronizaciones controladas.

## Stack

- Java 21
- Spring Boot 3.5.x
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Flyway
- Spring Cache + Caffeine
- RestClient para API-Football
- Springdoc OpenAPI
- JUnit 5 + Mockito
- Docker Compose con PostgreSQL y PgAdmin

## Ejecutar local

```bash
docker compose up -d
export API_FOOTBALL_KEY=tu_api_key
mvn clean spring-boot:run
```

Swagger queda en:

```txt
http://localhost:8080/swagger-ui.html
```

Pruebas:

```bash
mvn test
```

## Credenciales de desarrollo

```txt
ADMIN
usuario: andres.administrador
contraseña: 123admin

PLAYER
usuario: tia.maria
contraseña: 123player
```

Las contraseñas se guardan con BCrypt en la migracion inicial.

## Base de datos

- DBML: `docs/database/schema.dbml`
- Migracion inicial: `src/main/resources/db/migration/V1__create_initial_schema.sql`
- Guia paso a paso: `docs/SETUP_DATABASE.md`

## Seguridad

El login expone `accessToken` de vida corta y `refreshToken` persistido como hash en `auth_sessions`. La caducidad por inactividad se controla con `last_used_at` y `jwt.refresh-inactivity-expiration-seconds`. El logout revoca el refresh token.

Roles soportados:

- `ADMIN`: administra usuarios, pagos, parametros y sincronizaciones.
- `PLAYER`: registra sus apuestas y pronosticos, consulta ranking e historial.

## Middleware, cache y sincronizacion

El flujo esperado es:

```txt
Frontend -> FamiCup API -> PostgreSQL
```

API-Football solo se consulta mediante endpoints administrativos o schedulers. Se usa cache para parametros, equipos, proximos partidos y ranking; no se cachean tokens ni datos sensibles.

Endpoints API-Football usados por el cliente:

- `GET /fixtures?league={leagueId}&season={season}`
- `GET /fixtures?id={fixtureId}`

La documentacion oficial de API-Football v3 indica el host `https://v3.football.api-sports.io` y autenticacion con header `x-apisports-key`. Para calendario se usa `fixtures` con filtros como `league`, `season`, `date`, `from`, `to`, `round`, `team` o `status`; `teams/statistics` no se usa para listar partidos, solo para estadisticas acumuladas de un equipo. Los endpoints `PLAYER` leen partidos, logos, resultados, apuestas y pronosticos desde PostgreSQL.

## Endpoints principales

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/admin/dashboard`
- `GET /api/admin/notifications`
- `GET /api/admin/users`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`
- `GET /api/admin/payments`
- `PUT /api/admin/payments/{id}/mark-paid`
- `PUT /api/admin/payments/{id}/mark-pending`
- `GET /api/admin/parameters`
- `PUT /api/admin/parameters`
- `POST /api/admin/sync/fixtures`
- `POST /api/admin/sync/results`
- `GET /api/player/dashboard`
- `PATCH /api/player/me/password`
- `GET /api/player/history`
- `GET /api/player/ranking`
- `GET /api/player/parameters`
- `GET /api/player/matches`
- `GET /api/player/colombia-matches`
- `POST /api/player/colombia-bets`
- `PATCH /api/player/colombia-bets/{id}`
- `DELETE /api/player/colombia-bets/{id}`
- `GET /api/player/global-predictions`
- `POST /api/player/global-predictions`

## Estructura

```txt
src/main/java/com/famicup
├── cliente
├── configuracion
├── controlador
├── excepcion
├── modelo
│   ├── dto
│   ├── entidad
│   ├── enumeracion
│   └── mapper
├── repositorio
├── scheduler
├── seguridad
├── servicio
└── util
```
