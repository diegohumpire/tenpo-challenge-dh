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
- (Opcional) Maven 3.9+ para desarrollo local

## Ejecución con Docker Compose

```bash
# Construir imagen y levantar todo el stack (API + PostgreSQL + Redis)
docker compose up --build

# En background
docker compose up --build -d

# Ver logs de la API
docker compose logs -f api

# Detener todo
docker compose down -v
```

La API quedará disponible en **http://localhost:8080**

## Ejecución local (sin Docker)

Requiere PostgreSQL y Redis corriendo localmente:

```bash
# Instalar dependencias y compilar
./mvnw clean package -DskipTests

# Levantar servicios locales (alternativa)
docker compose up postgres redis -d

# Ejecutar la aplicación
./mvnw spring-boot:run
```

## Endpoints

### POST /api/1/calculations
Calcula `(num1 + num2) * (1 + porcentaje/100)`.

```bash
curl -X POST http://localhost:8080/api/1/calculations \
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

### GET /api/1/audit-logs
Lista el historial de todas las llamadas API con paginación.

```bash
curl "http://localhost:8080/api/1/audit-logs?page=0&size=10"
```

**Respuesta 200:**
```json
{
  "content": [
    {
      "id": 1,
      "timestamp": "2024-01-15T10:30:00",
      "endpoint": "/api/1/calculations",
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

### GET /mock/percentage
Endpoint de prueba que simula el servicio externo de porcentaje.

```bash
curl http://localhost:8080/mock/percentage
```

Configurable via `application.yaml`:
```yaml
mock:
  percentage:
    value: 10.0    # porcentaje retornado
    fail: false    # true = simula fallo del servicio
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
./mvnw test -P integration-tests
```

**Suite de tests:**
- **19 tests unitarios** — CalculationService, PercentageService, RateLimitingFilter, GlobalExceptionHandler
- **Tests de integración** — DhapiApplicationTests (Spring Boot + Testcontainers)
- **BDD/Cucumber** — 4 features: cálculo, caché, rate limiting, audit logs

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

```
com.tenpo.dh.challenge.dhapi
├── domain/
│   ├── model/        ← Calculation, AuditLog, enums (sin dependencias de infra)
│   ├── port/in/      ← CalculationUseCase, AuditLogUseCase
│   ├── port/out/     ← PercentageProvider, PercentageCacheStore, AuditLogRepository
│   └── exception/    ← PercentageNotAvailableException, RateLimitExceededException
├── application/
│   └── service/      ← CalculationService, PercentageService, AuditLogService
└── adapter/
    ├── in/web/       ← Controllers, Filters, DTOs, GlobalExceptionHandler
    └── out/          ← R2DBC persistence, Redis cache, HTTP client
```

**Decisiones técnicas:**
- **Arquitectura hexagonal**: el dominio es agnóstico a Spring/infra; facilita testing y reemplazabilidad de adaptadores
- **Spring Boot 4 API Versioning**: usa el soporte nativo de Spring Framework 7 (`@PostMapping(version = "1")`) con path-segment routing
- **Fire-and-forget audit**: el filtro de auditoría usa `subscribe()` sin bloquear el response; los errores de persistencia son logueados pero no afectan la respuesta principal
- **Redis sliding window**: para rate limiting en entornos multi-réplica; INCR + EXPIRE es atómico por primera llamada
- **Retry con backoff exponencial**: el cliente HTTP usa `Retry.backoff(3, 1s, max 5s)` para reintentos ante fallos transitorios

## Variables de entorno (Docker)

| Variable | Default | Descripción |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `docker` | Perfil Spring activo |
| `SPRING_R2DBC_URL` | — | URL R2DBC de PostgreSQL |
| `SPRING_R2DBC_USERNAME` | — | Usuario de la base de datos |
| `SPRING_R2DBC_PASSWORD` | — | Contraseña de la base de datos |
| `SPRING_FLYWAY_URL` | — | URL JDBC para migraciones Flyway |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host de Redis |
| `SPRING_DATA_REDIS_PORT` | `6379` | Puerto de Redis |
