Feature: Cálculo con porcentaje dinámico
  Como cliente de la API
  Quiero enviar dos números y recibir el resultado con un porcentaje aplicado
  Para obtener el cálculo correcto según el porcentaje del servicio externo

  Scenario: Cálculo exitoso con porcentaje del servicio externo
    Given el servicio externo retorna un porcentaje de 10%
    When envío POST /api/v1/calculations con num1=5.0 y num2=5.0
    Then la respuesta es 201 Created
    And el campo "result" es 11.0
    And el campo "sum" es 10.0
    And el campo "percentage" es 10.0

  Scenario: Cálculo con campo requerido faltante
    When envío POST /api/v1/calculations con num1=null y num2=5.0
    Then la respuesta es 400 Bad Request
    And el body contiene un Problem Detail con status 400

  Scenario: Cálculo cuando el servicio externo no está disponible y no hay caché
    Given el servicio externo no está disponible
    And no hay valor de porcentaje en caché
    When envío POST /api/v1/calculations con num1=5.0 y num2=5.0
    Then la respuesta es 503 Service Unavailable
    And el body contiene un Problem Detail con status 503
