# Tenpo Challenge API

REST API reactiva construida con **Spring Boot 4 / WebFlux** para el challenge técnico de Tenpo.

## Características

- **Cálculo con porcentaje dinámico**: `result = (num1 + num2) * (1 + pct/100)`, con retry automático (3 intentos) y fallback a caché
- **Caché Redis** con TTL 30 minutos para el último porcentaje obtenido
- **Rate limiting**: 3 RPM por IP usando Redis, con headers `X-RateLimit-*` y respuesta 429
- **Auditoría automática**: log de todas las llamadas API en PostgreSQL, con paginación
- **RFC 7807 Problem Details** para manejo de errores
- **Arquitectura hexagonal** (Ports & Adapters)

## Tech Stack

| Componente | Tecnología |
|---|---|
| Framework | Spring Boot 4.0.6 / Spring Framework 7 |
| Reactive | Spring WebFlux + Project Reactor |
| Persistencia | Spring Data R2DBC + PostgreSQL 16 |
| Migraciones | Flyway 11 |
| Caché | Spring Data Redis (Reactive) |
| Documentación | SpringDoc OpenAPI 3 (Swagger UI) |
| Tests | JUnit 5 + Mockito + Testcontainers + Cucumber |

## Prerrequisitos

- Java 21+
- Docker & Docker Compose
- (Opcional) Maven 3.9+

---

## Formas de ejecutar la aplicación

### Opción 1 — `./mvnw spring-boot:run` (perfil base)

Igual a ejecutar el JAR directamente. Conecta a PostgreSQL y Redis en `localhost` usando los
fallbacks de `application.yaml`. Requiere que los servicios ya estén corriendo.

```bash
# Opcional - Levantar infraestructura localmente con Docker Compose
docker compose up -d

# Ejecutar la aplicación (perfil base, sin docker-compose automático)
./mvnw spring-boot:run
```

---

### Opción 2 — `./mvnw spring-boot:run -Dspring-boot.run.profiles=docker` (recomendado)

Activa el perfil `docker`, que usa **spring-boot-docker-compose** para levantar PostgreSQL + Redis
automáticamente antes de iniciar la app. No se necesita ningún paso previo.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

Spring Boot levanta los servicios del `docker-compose.yml`, ejecuta las migraciones Flyway
y expone la API en **http://localhost:8080**.

Para detener: `Ctrl+C` (baja los contenedores automáticamente).

---

### Opción 3 — `docker compose -f docker-compose.full.yml up --build` (stack completo)

Construye la imagen de la API y levanta todo (API + PostgreSQL + Redis) como contenedores Docker.
No requiere Java ni Maven instalados.

```bash
# Construir imagen y levantar el stack completo
docker compose -f docker-compose.full.yml up --build

# En background
docker compose -f docker-compose.full.yml up --build -d

# Ver logs de la API
docker compose -f docker-compose.full.yml logs -f api

# Detener y limpiar volúmenes
docker compose -f docker-compose.full.yml down -v
```

La API quedará disponible en **http://localhost:8080**.

---

### Opción 4 — JAR standalone

```bash
# Construir el JAR
./mvnw clean package -DskipTests

# Ejecutar (perfil base, conecta a localhost por defecto)
java -jar target/dhapi-1.0.0.jar

# Con perfil docker (auto-inicia docker-compose)
java -jar target/dhapi-1.0.0.jar --spring.profiles.active=docker

# Con variables de entorno personalizadas
SPRING_R2DBC_URL=r2dbc:postgresql://myhost:5432/mydb \
SPRING_DATA_REDIS_HOST=myredis \
java -jar target/dhapi-1.0.0.jar
```

---

## Endpoints

### POST /api/v1/calculations
Calcula `(num1 + num2) * (1 + porcentaje/100)`.

```bash
curl -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -d '{"num1": 100, "num2": 50}'
```

**Respuesta 201:**
```json
{
  "num1": 100,
  "num2": 50,
  "percentage": 10.5,
  "result": 166.75
}
```

**Errores:**
- `400 Bad Request` — campos nulos o inválidos
- `429 Too Many Requests` — más de 3 RPM desde la misma IP
- `503 Service Unavailable` — servicio de porcentaje caído sin caché disponible

---

### GET /api/v1/audit-logs
Lista el historial de todas las llamadas API con paginación.

```bash
curl "http://localhost:8080/api/v1/audit-logs?page=0&size=10"
```

**Respuesta 200:**
```json
{
  "content": [
    {
      "id": 1,
      "timestamp": "2024-01-15T10:30:00",
      "endpoint": "/api/v1/calculations",
      "method": "POST",
      "requestBody": "{\"num1\":100,\"num2\":50}",
      "responseBody": "{\"result\":165.0,...}",
      "httpStatus": 201,
      "action": "CALCULATION",
      "direction": "INBOUND",
      "clientIp": "127.0.0.1"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```
---

### Swagger UI
Documentación interactiva disponible en:
```
http://localhost:8080/swagger-ui.html
```

## Tests

```bash
# Sólo tests unitarios (no requieren Docker)
./mvnw test

# Tests de integración + BDD (requieren Docker)
docker compose up -d
./mvnw test -P integration-tests
```

**Suite de tests:**
- **19 tests unitarios** — CalculationService, PercentageService, RateLimitingFilter, GlobalExceptionHandler
- **Tests de integración** — DhapiApplicationTests (Spring Boot + Testcontainers)
- **BDD/Cucumber** — 4 features: cálculo, caché, rate limiting, audit logs

> Nota: Test de integracion y BDD/Cucumber requieren que Docker esté corriendo, ya que usan Testcontainers para PostgreSQL y Redis.
> Ver mas detalles en [Integration Test](./docs/integration-tests.md)

## Percentage Provider

El porcentaje utilizado en el cálculo puede ser provisto por tres modos distintos, configurados con `PERCENTAGE_PROVIDER`:

| Modo | Descripción |
|---|---|
| `memory` | Responde siempre un valor fijo (default `10.0`). Ideal para desarrollo rápido. |
| `postman-mock` | Apunta a un mock de Postman. Permite controlar la respuesta via headers. |
| cualquier otro | Servicio externo real configurado con `PERCENTAGE_EXTERNAL_*`. |

### Headers de control para pruebas

Disponibles en modos `memory` y `postman-mock`:

| Header | Tipo | Descripción |
|---|---|---|
| `x-mock-percentage` | Numérico | **`memory`**: sobreescribe el valor de porcentaje.<br>**`postman-mock`**: se reenvía a Postman como header. |
| `x-mock-response-code` | HTTP code | **`memory`**: si es non-2xx → simula falla del servicio externo (sin caché → 503).<br>**`postman-mock`**: se reenvía a Postman como header. |

```bash
# Usar porcentaje custom
curl -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -H "x-mock-percentage: 25" \
  -d '{"num1": 100, "num2": 50}'

# Simular falla del servicio externo (vaciar cache Redis primero)
curl -X POST http://localhost:8080/api/v1/calculations \
  -H "Content-Type: application/json" \
  -H "x-mock-response-code: 503" \
  -d '{"num1": 100, "num2": 50}'
```

> **Nota:** cuando el provider falla, `PercentageService` hace fallback al último valor en caché Redis.
> Un 503 sólo se propaga si el caché también está vacío.

---

## Rate Limiting

Se implementa con Redis usando el patrón INCR + EXPIRE:
- **Ventana**: 60 segundos
- **Máximo**: 3 requests por IP por ventana
- **Headers de respuesta**:
  - `X-RateLimit-Limit: 3`
  - `X-RateLimit-Remaining: 2` (requests restantes)
  - `X-RateLimit-Reset: 45` (segundos hasta reset)
- **Respuesta 429** incluye `Retry-After` header

## Arquitectura

**Decisiones técnicas:**
- **Arquitectura hexagonal**: el dominio es agnóstico a Spring/infra; facilita testing y reemplazabilidad de adaptadores
- **Spring Boot 4 API Versioning**: usa el soporte nativo de Spring Framework 7 (`@PostMapping(version = "1")`) con path-segment routing
- **Fire-and-forget audit**: el filtro de auditoría usa `subscribe()` sin bloquear el response; los errores de persistencia son logueados pero no afectan la respuesta principal
- **Redis sliding window**: para rate limiting en entornos multi-réplica; INCR + EXPIRE es atómico por primera llamada
- **Retry con backoff exponencial**: el cliente HTTP usa `Retry.backoff(3, 1s, max 5s)` para reintentos ante fallos transitorios

## Variables de entorno

Todas las propiedades de conexión soportan variables de entorno con fallback a valores localhost:

| Variable | Fallback | Descripción |
|---|---|---|
| `SPRING_R2DBC_URL` | `r2dbc:postgresql://localhost:5432/tenpo_db` | URL R2DBC de PostgreSQL |
| `SPRING_R2DBC_USERNAME` | `tenpo` | Usuario R2DBC |
| `SPRING_R2DBC_PASSWORD` | `tenpo_pass` | Contraseña R2DBC |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/tenpo_db` | URL JDBC (Flyway) |
| `SPRING_DATASOURCE_USERNAME` | `tenpo` | Usuario JDBC |
| `SPRING_DATASOURCE_PASSWORD` | `tenpo_pass` | Contraseña JDBC |
| `SPRING_FLYWAY_URL` | `jdbc:postgresql://localhost:5432/tenpo_db` | URL JDBC para migraciones |
| `SPRING_FLYWAY_USER` | `tenpo` | Usuario Flyway |
| `SPRING_FLYWAY_PASSWORD` | `tenpo_pass` | Contraseña Flyway |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host de Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Puerto de Redis |
| `PERCENTAGE_PROVIDER` | `memory` | Proveedor de porcentaje (`memory`, `postman-mock`, o cualquier otro valor para externo) |
| `PERCENTAGE_CACHE_TTL` | `1800` | TTL del caché de porcentaje en segundos (default: 30 min) |
| `PERCENTAGE_MEMORY_VALUE` | `10.0` | Porcentaje fijo usado en modo `memory` |
| `PERCENTAGE_POSTMAN_BASE_URL` | `https://ec995055-c0c3-4482-aa85-89f5660540f0.mock.pstmn.io` | URL base del mock de Postman |
| `PERCENTAGE_POSTMAN_PATH` | `/mock/percentage` | Path del endpoint de Postman |
| `PERCENTAGE_EXTERNAL_BASE_URL` | `http://localhost:8080` | URL base del servicio externo real |
| `PERCENTAGE_EXTERNAL_PATH` | `/percentage` | Path del endpoint externo |
| `RATE_LIMIT_MAX_REQUESTS` | `3` | Requests máximos por ventana |
| `RATE_LIMIT_WINDOW_SECONDS` | `60` | Tamaño de la ventana en segundos |
