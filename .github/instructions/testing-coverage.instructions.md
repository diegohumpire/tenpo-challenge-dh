---
description: "Use when modifying, adding, or refactoring Java production code. Enforces that unit tests and BDD feature files are reviewed and updated to stay in sync with every code change."
applyTo: "src/main/**/*.java"
---

# Testing Coverage on Code Changes

Whenever a production file under `src/main/` is created or modified, you **must** review and update the corresponding tests before considering the task complete.

## Checklist

- [ ] Identify all unit tests that exercise the changed class/method
- [ ] Identify all BDD feature files whose scenarios touch the changed behaviour
- [ ] Update or add unit tests so every changed code path is covered
- [ ] Update or add BDD scenarios if the public/API behaviour changed
- [ ] Confirm no existing test is left broken or silently obsolete

## Unit Tests

Location: `src/test/java/.../application/service/` and `src/test/java/.../adapter/in/web/`

Rules:

- Use `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks` for service-level tests
- Use `StepVerifier` for all reactive (`Mono`/`Flux`) assertions
- Name methods with the pattern `verb_condition_expectedResult`
    - Good: `calculate_withZeroPercentage_returnsSumOnly`
    - Bad: `testCalculate`, `shouldWork`
- One assertion concern per test method
- Cover both the happy path and error/edge cases for every changed method

```java
// Example structure
@Test
void calculate_withValidNumbers_returnsCorrectResult() {
    when(percentageService.resolvePercentage()).thenReturn(Mono.just(BigDecimal.valueOf(10)));

    StepVerifier.create(calculationService.calculate(BigDecimal.valueOf(5), BigDecimal.valueOf(5)))
            .assertNext(calc -> assertThat(calc.result()).isEqualByComparingTo("11.0"))
            .verifyComplete();
}
```

## BDD / Cucumber Feature Files

Location: `src/test/resources/features/` — written in Spanish, Gherkin syntax

Rules:

- Each user-facing behaviour change **must** have a corresponding scenario
- Write scenarios in Spanish following the existing style
- Scenario titles must be descriptive: describe the business rule, not the implementation
- Step definitions live in `src/test/java/.../bdd/steps/`
- Reuse existing `@Given`/`@When`/`@Then` steps where possible; add new ones only when needed

```gherkin
# Example — add/update in the relevant .feature file
Escenario: Cálculo con porcentaje cero retorna solo la suma
  Dado que el servicio de porcentaje retorna 0%
  Cuando se calcula con los números 3 y 7
  Entonces el resultado debe ser 10
```

## When Adding a New Class

- Create a matching `*Test.java` file in the mirrored test package
- Add at least one BDD scenario if the class is reachable through a REST endpoint
- Extend `AbstractIntegrationTest` for any test that needs PostgreSQL or Redis containers

## Non-negotiable

- Never leave a changed method without at least one unit test covering the new behaviour
- Never change an endpoint's request/response contract without updating the relevant `.feature` file
