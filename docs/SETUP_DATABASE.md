# Setup de base de datos FamiCup

## Requisitos

- Docker y Docker Compose.
- Java 21.
- Maven 3.9+ o el wrapper `./mvnw`.
- Variables de entorno locales para secretos cuando aplique.

## Levantar PostgreSQL y PgAdmin

```bash
docker compose up -d
```

Servicios:

- PostgreSQL: `localhost:5432`
- Base de datos: `famicup_db`
- Usuario: `famicup_user`
- Password desarrollo: `famicup_dev_password`
- PgAdmin: <http://localhost:5050>
- PgAdmin email: `admin@famicup.dev`
- PgAdmin password desarrollo: `famicup_pgadmin_password`

Estas credenciales son solo para desarrollo local y estan documentadas para que no se confundan con secretos productivos.

## Detener contenedores

```bash
docker compose down
```

## Reiniciar

```bash
docker compose down
docker compose up -d
```

## Borrar volumenes y limpiar la BD

Esto elimina datos locales:

```bash
docker compose down -v
docker compose up -d
```

## Registrar PostgreSQL en PgAdmin

1. Abre <http://localhost:5050>.
2. Ingresa con `admin@famicup.dev` y `famicup_pgadmin_password`.
3. Crea un server nuevo.
4. En `General`, usa nombre `FamiCup Local`.
5. En `Connection`, usa:
   - Host: `postgres`
   - Port: `5432`
   - Maintenance database: `famicup_db`
   - Username: `famicup_user`
   - Password: `famicup_dev_password`

Desde tu maquina el host es `localhost`; desde PgAdmin dentro de Docker el host es `postgres`.

## Configurar application.properties

La configuracion por defecto ya apunta a Docker local:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/famicup_db
spring.datasource.username=famicup_user
spring.datasource.password=famicup_dev_password
```

Para API-Football, define la variable sin quemarla en archivos:

```bash
export API_FOOTBALL_KEY=tu_api_key
```

Para JWT en un entorno real:

```bash
export JWT_SECRET=una_clave_larga_de_minimo_64_caracteres
```

## Ejecutar backend

```bash
mvn clean spring-boot:run
```

o:

```bash
./mvnw clean spring-boot:run
```

Flyway crea el esquema inicial automaticamente al arrancar.

## Ejecutar pruebas

```bash
mvn test
```

## Validar Swagger

Con el backend arriba:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Validar conexion a BD

Puedes revisar logs del backend para ver Flyway y el datasource, o entrar a PgAdmin y confirmar tablas como `users`, `matches`, `payments`, `global_predictions` y `api_sync_logs`.

Login de desarrollo:

```txt
usuario: andres.administrador
contraseña: 123admin
rol: ADMIN
```
