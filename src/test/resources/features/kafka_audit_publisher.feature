Feature: Publicación de eventos de auditoría a través de Kafka
  Como operador de la API
  Quiero que los eventos de auditoría sean publicados a una cola Kafka
  Para garantizar la persistencia desacoplada del ciclo de respuesta HTTP

  @kafka
  Scenario: El audit log se registra en base de datos cuando el publisher es Kafka
    When envío POST /api/v1/calculations con num1=5.0 y num2=5.0
    And espero a que el registro Kafka se complete
    And consulto GET /api/v1/audit-logs?page=0&size=20
    Then existe un registro de audit con action="CREATE_CALCULATION" y actionType="CALCULATION"

  @kafka
  Scenario: El flujo principal no se interrumpe cuando el publisher es Kafka
    When envío POST /api/v1/calculations con num1=10.0 y num2=20.0
    Then la respuesta es 201 Created
    And el campo "result" es 33.0

  @kafka
  Scenario: El audit log del servicio externo se registra via Kafka
    When envío POST /api/v1/calculations con num1=5.0 y num2=5.0
    And espero a que el registro Kafka se complete
    And consulto GET /api/v1/audit-logs?page=0&size=20
    Then existe un registro de audit con action="GET_EXTERNAL_PERCENTAGE" y actionType="EXTERNAL_CALL"

  @kafka
  Scenario: La respuesta de cálculo incluye el resultado correcto con publisher Kafka
    When envío POST /api/v1/calculations con num1=3.0 y num2=7.0
    Then la respuesta es 201 Created
    And el campo "result" es 11.0
