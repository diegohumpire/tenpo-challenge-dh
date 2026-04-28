Feature: Historial de Audit Logs
  Como operador de la API
  Quiero consultar un historial paginado de todas las llamadas
  Para auditar el uso del sistema

  Scenario: Consultar el historial paginado
    Given existen 25 registros en audit_logs
    When envío GET /api/v1/audit-logs?page=0&size=20
    Then la respuesta es 200 OK
    And el campo "totalElements" es 25
    And el campo "totalPages" es 2
    And el campo "content" contiene 20 registros

  Scenario: El audit log registra los detalles de una llamada exitosa
    When envío POST /api/v1/calculations con num1=5.0 y num2=5.0
    And espero a que el registro asíncrono se complete
    And consulto GET /api/v1/audit-logs?page=0&size=1
    Then el último registro tiene action="CREATE_CALCULATION"
    And el último registro tiene actionType="CALCULATION"
    And el último registro tiene callDirection="IN"
    And el último registro tiene statusCode=201

  Scenario: Si el registro falla, la respuesta principal no se ve afectada
    Given el servicio de persistencia de audit logs lanza una excepción
    When envío POST /api/v1/calculations con num1=5.0 y num2=5.0
    Then la respuesta es 201 Created
    And el campo "result" es 11.0
