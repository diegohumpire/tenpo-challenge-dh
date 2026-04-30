# Plan: Idempotency Key para `POST /api/v1/calculations`

## Motivación

Si un cliente manda `POST /api/v1/calculations` y no recibe respuesta (timeout de red, caída del proxy, etc.), lo natural es reintentar. Sin idempotencia, el servidor ejecuta el cálculo dos veces — lo cual es inocuo para este caso de uso, pero puede generar audit logs duplicados y comportamiento inesperado si en el futuro el endpoint tuviera efectos secundarios más costosos.

---

## Enfoque elegido: reutilizar `X-Transactional-Id` (Opción A)

No se agrega ningún header nuevo. El `X-Transactional-Id` ya es requerido, ya es un UUID único por request, y Redis ya está disponible. La idempotencia se implementa como un `WebFilter` sin tocar el dominio.

### Por qué no un header `Idempotency-Key` explícito

El header explícito (patrón Stripe) es más limpio semánticamente, pero requiere que el cliente gestione un UUID adicional además del `X-Transactional-Id`. Para este proyecto, donde el API Gateway ya inyecta `X-Transactional-Id` con un UUID fresco por request, reutilizarlo evita duplicar responsabilidades en el cliente.

---

## Clave Redis

```
idempotency:v1:calculations:{transactionalId}
│            │   │
│            │   └── endpoint específico — evita colisiones con futuros endpoints
│            └────── versión — permite invalidar si el formato de respuesta cambia
└─────────────────── prefijo global de idempotencia
```

**Ejemplo:** `idempotency:v1:calculations:3760c8d0-aff2-42f1-9195-85211c4b1afd`

El mismo `transactionalId` puede aparecer en otras claves Redis sin conflicto:
```
percentage:current                                          ← caché de porcentaje
rate_limit:192.168.1.1                                      ← rate limiting
idempotency:v1:calculations:3760c8d0-...                    ← idempotencia (este plan)
```

---

## Qué se cachea

No solo el JSON del body — también el status code, para devolver exactamente la misma respuesta HTTP:

```json
{
  "statusCode": 201,
  "body": {
    "num1": 100,
    "num2": 50,
    "sum": 150,
    "percentage": 10.0,
    "result": 165.0
  }
}
```

> **Nota:** los errores (4xx, 5xx) no se cachean. Si la primera request falló, el retry debe procesarse normalmente.

---

## TTL

**24 horas.** Cubre retries de red normales y workers batch sin ocupar Redis indefinidamente. Para un endpoint de cálculo simple no tiene sentido el TTL de 7 días de Stripe.

---

## Comportamiento ante conflicto

Si el mismo `transactionalId` llega con un body diferente (`num1`/`num2` distintos), se devuelve la respuesta cacheada sin error. El contrato del API ya documenta que el `X-Transactional-Id` debe ser un UUID fresco por request — si el cliente lo reutiliza con inputs distintos, es su responsabilidad.

La variante defensiva (retornar `409 Conflict` si el body difiere) requeriría guardar un hash del request body en el caché y compararlo en cada hit. No vale la complejidad para este caso de uso.

---

## Dónde vive la lógica

Un nuevo `WebFilter` (`IdempotencyFilter`) a `@Order(0)`, antes del rate limit:

```
Request
  → [IdempotencyFilter   @Order(0)]  ← verifica/guarda en Redis
  → [RateLimitingFilter  @Order(1)]
  → [RequestHeadersFilter @Order(2)]
  → [ExchangeContextFilter @Order(2)]
  → [AuditLogFilter      @Order(3)]
  → CalculationController
```

### Flujo del filtro

```
POST /api/v1/calculations llega
    │
    ├─ ¿tiene X-Transactional-Id? (garantizado por RequestHeadersFilter,
    │   pero IdempotencyFilter va antes — leer el header directamente)
    │
    ├─ Redis GET idempotency:v1:calculations:{txId}
    │       │
    │       ├─ HIT → devolver respuesta cacheada (201 + body), no pasar al chain
    │       │
    │       └─ MISS → pasar al chain normal
    │                   │
    │                   └─ interceptar respuesta de salida
    │                       │
    │                       ├─ si statusCode 2xx → Redis SET con TTL 24h
    │                       └─ si error → no cachear
```

### Reutilización de infraestructura existente

La interceptación del response body puede reutilizar `ResponseCapturingDecorator` que ya existe en `adapter/in/web/filter/`. El filtro solo necesita leer el body capturado y serializarlo a Redis.

---

## Archivos a crear/modificar

| Archivo | Acción |
|---|---|
| `adapter/in/web/filter/IdempotencyFilter.java` | Crear — lógica del filtro |
| `config/IdempotencyProperties.java` | Crear — TTL configurable vía env var |
| `application.yaml` | Modificar — agregar `idempotency.ttl-seconds: 86400` |
| `docker-compose.full.yml` | Modificar si se quiere override del TTL |

---

## Variables de entorno propuestas

| Variable | Default | Descripción |
|---|---|---|
| `IDEMPOTENCY_TTL_SECONDS` | `86400` | TTL del caché de idempotencia (24 h) |
| `IDEMPOTENCY_ENABLED` | `true` | Feature flag para habilitar/deshabilitar |

---

## Consideraciones de testing

- Test unitario del filtro: mock de Redis, verificar HIT devuelve respuesta cacheada sin llamar al chain
- Test unitario del filtro: mock de Redis, verificar MISS deja pasar y cachea la respuesta 2xx
- Test unitario: errores 4xx/5xx no se cachean
- Test de integración (Testcontainers Redis): dos requests con mismo `X-Transactional-Id` → segunda devuelve respuesta cacheada sin llamar al servicio
