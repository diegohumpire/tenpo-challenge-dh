Feature: Validación de cabeceras obligatorias
  Como microservicio detrás de un API Gateway
  Quiero que toda solicitud incluya X-Transactional-Id y X-User-Id
  Para garantizar trazabilidad y atribución de usuario en cada llamada

  Scenario: Solicitud con ambas cabeceras obligatorias es procesada correctamente
    When envío POST /api/v1/calculations con cabeceras obligatorias y num1=5.0 y num2=5.0
    Then la respuesta es 201 Created

  Scenario: Solicitud sin X-Transactional-Id es rechazada con 400
    When envío POST /api/v1/calculations sin el header "X-Transactional-Id"
    Then la respuesta es 400 Bad Request
    And el body contiene un Problem Detail con status 400

  Scenario: Solicitud sin X-User-Id es rechazada con 400
    When envío POST /api/v1/calculations sin el header "X-User-Id"
    Then la respuesta es 400 Bad Request
    And el body contiene un Problem Detail con status 400

  Scenario: Solicitud sin ninguna cabecera obligatoria es rechazada con 400
    When envío POST /api/v1/calculations sin cabeceras obligatorias
    Then la respuesta es 400 Bad Request
    And el body contiene un Problem Detail con status 400
