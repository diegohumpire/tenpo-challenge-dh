Feature: Caché del porcentaje con Redis
  Como sistema
  Quiero cachear el porcentaje en Redis
  Para evitar llamadas innecesarias al servicio externo y soportar fallos

  Scenario: El porcentaje se almacena en caché tras una llamada exitosa
    Given el servicio externo retorna un porcentaje de 15.0
    When se resuelve el porcentaje
    Then el valor 15.0 se almacena en Redis con TTL de 30 minutos

  Scenario: Se usa el valor cacheado cuando el servicio externo falla
    Given hay un valor de porcentaje 10.0 en caché
    And el servicio externo no está disponible
    When se resuelve el porcentaje
    Then se retorna el valor cacheado 10.0
    And no se lanza ninguna excepción

  Scenario: Error 503 cuando el servicio falla y no hay caché
    Given el servicio externo no está disponible
    And no hay valor en caché
    When se resuelve el porcentaje
    Then se lanza PercentageNotAvailableException

  Scenario: Se reintenta 3 veces antes de usar el caché
    Given el servicio externo falla en todos los intentos
    When se resuelve el porcentaje
    Then se realizan exactamente 3 reintentos al servicio externo
    And se usa el valor en caché como fallback
