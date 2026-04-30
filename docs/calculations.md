# Flujo de cálculo

## Resumen

El endpoint de cálculo obtiene un porcentaje dinámico desde un proveedor configurable, lo aplica a la suma de dos números y devuelve el resultado. El proveedor activo se elige al arrancar la app con `percentage.provider`.

---

## Flujo: Proveedor en memoria

```mermaid
flowchart TD
    A([POST /api/v1/calculations]) --> B[CalculationController]
    B --> C[CalculationService]
    C --> D[PercentageService\n.resolvePercentage]

    D --> E[InMemoryPercentageProvider\ndevuelve valor fijo\npor defecto: 10.0]

    E --> F[(Redis\nCache PUT\nTTL 30 min)]
    F --> G[porcentaje resuelto]

    G --> H["Calculation.of(num1, num2, pct)\nresultado = (num1 + num2) × (1 + pct/100)"]
    H --> I([HTTP 201 CalculationResponse])
```

---

## Flujo: Proveedor Postman Mock

```mermaid
flowchart TD
    A([POST /api/v1/calculations]) --> B[CalculationController]
    B --> C[CalculationService]
    C --> D[PercentageService\n.resolvePercentage]

    D --> E{Circuit Breaker\nabierto?}

    E -- No --> F[PostmanMockPercentageClient\nHTTP GET Postman mock API\ntimeout: 10 s]

    F --> G{respuesta 2xx?}

    G -- Sí --> H[parsea el porcentaje\ndel body JSON]
    H --> I[(Redis\nCache PUT\nTTL 30 min)]
    I --> J[porcentaje resuelto]

    G -- No --> K{reintentos\nagotados?\nmáx 3, backoff exp}
    K -- No --> F
    K -- Sí --> L[excepción]

    E -- Sí --> L

    L --> M[(Redis\nCache GET)]
    M --> N{hay algo\nen caché?}

    N -- Sí --> O[devuelve el porcentaje\ncacheado]
    O --> J

    N -- No --> P([HTTP 503\nPercentageNotAvailableException])

    J --> Q["Calculation.of(num1, num2, pct)\nresultado = (num1 + num2) × (1 + pct/100)"]
    Q --> R([HTTP 201 CalculationResponse])
```

---

## Configuración de resiliencia (Postman Mock / HTTP)

| Parámetro | Valor por defecto |
|-----------|------------------|
| Timeout | 10 s |
| Máximo de reintentos | 3 |
| Backoff inicial | 1 s |
| Backoff máximo | 5 s |
| Ventana deslizante del circuit breaker | 10 llamadas |
| Mínimo de llamadas para evaluar | 5 |
| Umbral de tasa de fallos | 50 % |
| Umbral de llamadas lentas (> 3 s) | 100 % |
| Duración del estado abierto | 30 s |
| Llamadas permitidas en half-open | 3 |
| TTL del caché | 1800 s (30 min) |

---

## Selección de proveedor

Se configura con `percentage.provider` en `application.yaml`:

| Valor | Implementación | Cuándo usarlo |
|-------|---------------|---------------|
| `memory` | `InMemoryPercentageProvider` | Dev local / tests unitarios |
| `postman-mock` | `PostmanMockPercentageClient` | Integración / demo |
| _(por defecto)_ | `HttpPercentageClient` | Servicio externo en producción |
