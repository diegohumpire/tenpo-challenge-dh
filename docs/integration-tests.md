# Integration Tests y BDD con Cucumber

## ¿Qué es BDD y para qué sirve aquí?

**BDD (Behavior-Driven Development)** es una práctica donde los tests se escriben en lenguaje natural que describe el comportamiento esperado del sistema desde la perspectiva del negocio. En este proyecto usamos **Cucumber** como framework BDD.

Cada test de integración está escrito en dos partes:

1. **Feature file** (`.feature`) — describe el escenario en español usando la sintaxis Gherkin (`Given / When / Then`).
2. **Step definitions** (`.java`) — contienen el código Java que implementa cada paso del escenario.

Esto permite que los escenarios sean legibles por personas no técnicas y sirvan al mismo tiempo como tests automatizados.

---

## Estructura de archivos

```
src/test/
├── java/.../
│   ├── AbstractIntegrationTest.java       # Base: levanta contenedores con Testcontainers
│   ├── bdd/
│   │   ├── CucumberRunnerTest.java        # Punto de entrada: ejecuta todos los features
│   │   ├── CucumberSpringConfig.java      # Conecta Cucumber con el contexto de Spring
│   │   ├── BddWebTestClientConfig.java    # Configura el cliente HTTP para los tests
│   │   └── steps/
│   │       ├── ScenarioResponseContext.java   # Estado compartido entre steps del mismo escenario
│   │       ├── CalculationSteps.java          # Steps para cálculos
│   │       ├── AuditLogSteps.java             # Steps para audit logs
│   │       ├── RateLimitingSteps.java         # Steps para rate limiting
│   │       └── PercentageCacheSteps.java      # Steps para caché de porcentaje
└── resources/
    └── features/
        ├── calculation.feature
        ├── audit_logs.feature
        ├── rate_limiting.feature
        └── percentage_cache.feature
```

---

## Cómo se ejecutan los tests

### Solo tests unitarios (por defecto)
```bash
./mvnw test
```
Los tests BDD están **excluidos** de la ejecución por defecto porque requieren Docker para levantar PostgreSQL y Redis.

### Tests de integración (incluye BDD)
```bash
./mvnw test -P integration-tests
```
El perfil `integration-tests` en `pom.xml` elimina la exclusión de `CucumberRunnerTest`, permitiendo que Cucumber corra.

**Requisitos:** Docker disponible en el sistema (para Testcontainers). Si Docker no está disponible, Testcontainers intentará conectarse a instancias locales de PostgreSQL (`localhost:5432`) y Redis (`localhost:6379`) según `application-test.yaml`.

---

## Arquitectura de la infraestructura de tests

### 1. `AbstractIntegrationTest` — Base con Testcontainers

```java
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = ...
    @Container static final GenericContainer<?> redis = ...

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) { ... }
}
```

Levanta dos contenedores Docker antes de que Spring arranque:
- **`postgres:16-alpine`** — base de datos real con Flyway ejecutando las migraciones
- **`redis:7-alpine`** — instancia real de Redis para rate limiting y caché

Con `@DynamicPropertySource` inyecta los puertos aleatorios asignados a los contenedores en las propiedades de Spring, sobreescribiendo las URLs de `application-test.yaml`.

---

### 2. `CucumberSpringConfig` — Puente entre Cucumber y Spring

```java
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfig extends AbstractIntegrationTest {}
```

Esta clase cumple dos roles:
- `@CucumberContextConfiguration` — le dice a Cucumber que use el contexto de Spring.
- `extends AbstractIntegrationTest` — hereda los contenedores Testcontainers.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` — levanta el servidor Netty real en un puerto aleatorio (aunque no se usa directamente, ver siguiente punto).
- `@ActiveProfiles("test")` — activa `application-test.yaml` con configuraciones como el `InMemoryPercentageProvider`.

---

### 3. `BddWebTestClientConfig` — Cliente HTTP para los tests

```java
@Bean
public WebTestClient webTestClient(ApplicationContext applicationContext) {
    return WebTestClient.bindToApplicationContext(applicationContext)
            .configureClient()
            .build();
}
```

**`bindToApplicationContext`** (en lugar de `bindToServer`) es fundamental: en vez de hacer llamadas TCP reales al puerto del servidor, el `WebTestClient` se conecta **directamente al `DispatcherHandler` de Spring WebFlux**. Esto:

- Evita problemas de timing donde el cliente se crea antes de que el servidor registre su puerto.
- Es más rápido (sin overhead de red).
- **Incluye todos los `WebFilter`** del contexto: `RateLimitingFilter`, `AuditLogFilter`, etc.

---

### 4. `CucumberRunnerTest` — El lanzador

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.tenpo.dh.challenge.dhapi.bdd")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-report.html")
@Testcontainers(disabledWithoutDocker = true)
public class CucumberRunnerTest {}
```

- `@Suite` + `@IncludeEngines("cucumber")` — usa JUnit Platform Suite para lanzar el motor de Cucumber.
- `@SelectClasspathResource("features")` — busca todos los `.feature` en `src/test/resources/features/`.
- `GLUE_PROPERTY_NAME` — le dice a Cucumber dónde encontrar las clases con step definitions (`bdd` package y subpaquetes).
- Genera un reporte HTML en `target/cucumber-report.html`.

---

## El paquete `bdd/steps/` — Glue code

Los step definitions son las clases Java que implementan cada línea Gherkin. Cucumber las descubre automáticamente porque están dentro del paquete configurado en `GLUE_PROPERTY_NAME`.

### Inyección de dependencias

Todas las clases de steps son beans de Spring sin `@Component` explícito — Cucumber-Spring las registra automáticamente cuando encuentra anotaciones de Cucumber (`@When`, `@Then`, `@Before`, etc.) en clases dentro del glue path. Pueden usar `@Autowired` normalmente.

### Scope de los steps: `cucumber-glue`

Cada escenario crea una **nueva instancia** de todos los step classes. Esto significa que campos como `private boolean externalServiceDown` se reinician automáticamente entre escenarios.

### `ScenarioResponseContext` — Estado compartido entre clases

```java
@Component
@Scope("cucumber-glue")
public class ScenarioResponseContext {
    private WebTestClient.ResponseSpec lastResponse;
    // getter + setter
}
```

El problema: un escenario puede tener steps de varias clases distintas (`CalculationSteps` hace el POST, `AuditLogSteps` verifica el resultado). Como cada clase tiene su propia instancia, **no pueden compartir variables directamente**.

`ScenarioResponseContext` es un bean con scope `cucumber-glue` — una sola instancia por escenario, compartida entre todos los step classes que la inyecten. Almacena el último `ResponseSpec` (la respuesta HTTP pendiente de verificación).

---

## Hooks `@Before` — Limpieza entre escenarios

Cada step class puede tener métodos `@Before` que Cucumber ejecuta **antes de cada escenario**, independientemente de qué steps use ese escenario.

| Clase | Hook | Qué limpia |
|---|---|---|
| `CalculationSteps` | `resetState()` | Claves `rate_limit:*` en Redis, flags de fallo del servicio externo |
| `AuditLogSteps` | `clearAuditLogs()` | Todos los registros de la tabla `audit_logs` |
| `PercentageCacheSteps` | `clearCache()` | La clave `percentage:current` en Redis |
| `RateLimitingSteps` | `resetRateLimitState()` | Variables locales: IP del cliente, lista de respuestas |

Sin estos hooks, el estado acumulado entre escenarios causaría fallos intermitentes:
- El rate limiter usa Redis, por lo que requests de un escenario cuentan para el siguiente.
- Los audit logs de un escenario aparecerían en los conteos del siguiente.

---

## Feature files y sus scenarios

### `calculation.feature`
Valida el endpoint `POST /api/v1/calculations`:
- Cálculo exitoso con porcentaje del servicio externo (resultado = sum + sum * %)
- Validación de campos requeridos (400 si falta `num1`)
- Error 503 cuando el servicio externo no responde y no hay caché

### `audit_logs.feature`
Valida el registro y consulta de audit logs:
- Paginación del historial (`GET /api/v1/audit-logs?page=0&size=20`)
- Que cada llamada queda registrada con `action`, `actionType`, `callDirection`, `statusCode`
- Que un fallo al guardar el log no afecta la respuesta principal (fire-and-forget)

### `rate_limiting.feature`
Valida el límite de 3 requests por minuto por IP:
- 3 solicitudes dentro del límite → 201
- La 4ª en el mismo minuto → 429 con headers `X-RateLimit-Remaining: 0` y `Retry-After`
- Rutas de actuator (`/actuator/health`) no están limitadas

### `percentage_cache.feature`
Valida el comportamiento del caché de porcentaje en Redis:
- El porcentaje se almacena en caché tras una llamada exitosa al servicio externo
- Se usa el valor cacheado cuando el servicio externo falla
- Error 503 si el servicio falla y no hay caché
- Se realizan 3 reintentos antes de usar el fallback de caché

---

## Detalles técnicos importantes

### `WebTestClient.exchange()` es lazy

`exchange()` **no envía la petición HTTP** en el momento en que se llama. La petición se ejecuta cuando se llama a un método terminal como `expectStatus()`, `expectBody()`, o `returnResult()`.

En `AuditLogSteps`, cuando el escenario hace un POST y luego espera a que el log asíncrono se guarde, el POST debe ser ejecutado antes del `Thread.sleep`. Por eso `esperoAQueElRegistroAsincrono` consume el `ResponseSpec` antes de dormir:

```java
@And("espero a que el registro asíncrono se complete")
public void esperoAQueElRegistroAsincrono() throws InterruptedException {
    WebTestClient.ResponseSpec pending = scenarioContext.getLastResponse();
    if (pending != null) {
        pending.expectStatus().is2xxSuccessful(); // fuerza la ejecución del POST
    }
    Thread.sleep(500); // tiempo para que el audit log asíncrono se persista
}
```

### Audit log asíncrono y `SinkAuditEventPublisher`

El `AuditLogFilter` no guarda los logs directamente: publica eventos en un `Sinks.Many` (cola en memoria) que es consumida en segundo plano por `SinkAuditEventPublisher` en un hilo del `Schedulers.boundedElastic()`. Esto desacopla el guardado de la respuesta HTTP y hace que fácilmente se pueda reemplazar el sink por un productor de Kafka.

El `Thread.sleep(500)` en el step da tiempo a que este procesamiento asíncrono complete antes de consultar la base de datos.

### Cucumber Expressions vs. Regex en steps con URLs

Cucumber usa su propio lenguaje de expresiones para los patrones de los steps. Los caracteres `/` y `?` tienen significado especial en Cucumber Expressions (alternación y opcionalidad). Los steps que contienen URLs se definen con **expresiones regulares Java** para evitar errores de parseo:

```java
// ✅ Regex — el ^ y $ delimitan el patrón como regex
@When("^envío GET /api/v1/audit-logs\\?page=(\\d+)&size=(\\d+)$")

// ❌ Cucumber Expression — la / se interpreta como alternación
@When("envío GET /api/v1/audit-logs?page={int}&size={int}")
```

---

## Reporte de resultados

Tras ejecutar con `-P integration-tests`, Cucumber genera:
- **Consola**: salida `pretty` con cada escenario y paso marcado como ✓ o ✗
- **HTML**: `target/cucumber-report.html` con reporte navegable

```
Scenario: Cálculo exitoso con porcentaje del servicio externo
  ✓ Given el servicio externo retorna un porcentaje de 10%
  ✓ When envío POST /api/v1/calculations con num1=5.0 y num2=5.0
  ✓ Then la respuesta es 201 Created
  ✓ And el campo "result" es 11.0
```
