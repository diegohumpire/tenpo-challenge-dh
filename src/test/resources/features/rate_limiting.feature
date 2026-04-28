Feature: Control de tasas (Rate Limiting)
  Como operador de la API
  Quiero limitar a 3 requests por minuto por IP
  Para proteger el servicio de abuso

  Background:
    Given el IP del cliente es "192.168.1.100"

  Scenario: Tres solicitudes dentro del límite son aceptadas
    When envío 3 solicitudes POST /api/v1/calculations en menos de 60 segundos
    Then las 3 respuestas tienen status 201

  Scenario: La cuarta solicitud en el mismo minuto es rechazada
    Given ya se realizaron 3 solicitudes en el último minuto
    When envío una cuarta solicitud POST /api/v1/calculations
    Then la respuesta es 429 Too Many Requests
    And el header "X-RateLimit-Remaining" es "0"
    And el header "Retry-After" está presente
    And el body contiene un Problem Detail con status 429

  Scenario: El rate limit no aplica a rutas internas
    When envío GET /actuator/health
    Then la respuesta es 200 OK sin aplicar rate limiting
