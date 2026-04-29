---
description: "Use when writing, adding, or refactoring Java production code. Enforces SOLID and KISS principles within the hexagonal architecture of this project."
applyTo: "src/main/**/*.java"
---

# SOLID & KISS Principles

Apply these principles to every production Java file, keeping the hexagonal architecture layers in mind.

---

## Single Responsibility Principle (SRP)

Each class must have **one reason to change**.

- **Services** (`application/service/`): orchestrate use-case logic only — no I/O, no HTTP, no formatting.
- **Adapters** (`adapter/in/`, `adapter/out/`): translate between the external world and domain types only.
- **Filters** (`adapter/in/web/filter/`): one filter = one cross-cutting concern (audit, rate-limiting, auth…).
- **Domain models** (`domain/model/`): data + invariants only — no Spring annotations, no serialization logic.

```java
// BAD — service doing I/O and formatting
@Service
public class CalculationService {
    public String calculateAndFormat(BigDecimal a, BigDecimal b) {
        BigDecimal pct = httpClient.fetch(); // I/O inside the service
        return "Result: " + Calculation.of(a, b, pct).result(); // formatting inside the service
    }
}

// GOOD — each responsibility in its own layer
@Service
public class CalculationService implements CalculationUseCase {
    public Mono<Calculation> calculate(BigDecimal a, BigDecimal b) {
        return percentageService.resolvePercentage()
                .map(pct -> Calculation.of(a, b, pct));
    }
}
```

---

## Open/Closed Principle (OCP)

Extend behaviour through **new implementations of existing ports**, not by modifying existing classes.

- Adding a new percentage source → implement `PercentageProvider`, register with Spring, do not modify `PercentageService`.
- Adding a new audit sink → implement `AuditEventPublisher`, do not modify `AuditLogService` or existing adapters.

---

## Liskov Substitution Principle (LSP)

Every port implementation must honour the **contract** defined by the interface.

- `PercentageProvider` implementations must return a non-null `Mono<BigDecimal>` and propagate errors through the reactive stream — never return `Mono.empty()` silently when a value is expected.
- `AuditEventPublisher` implementations must not swallow errors; let the caller decide how to handle them.

---

## Interface Segregation Principle (ISP)

Ports must be **narrow and role-specific**.

- Use-case ports (`domain/port/in/`): one interface per use case (`CalculationUseCase`, `AuditLogUseCase`).
- Output ports (`domain/port/out/`): split by capability (`PercentageCacheStore` vs `PercentageProvider` vs `AuditLogRepository`).
- Do **not** merge two unrelated methods into one interface to avoid creating a fat port.

---

## Dependency Inversion Principle (DIP)

High-level modules must depend on **abstractions (ports)**, never on concrete adapters.

- Services inject port interfaces, never adapter classes.
- Adapters inject port interfaces (or other adapters of a lower layer), never sibling adapters.
- Use constructor injection (`@RequiredArgsConstructor`) — never field injection (`@Autowired` on fields).

```java
// BAD — service depends on a concrete adapter
@Service
public class PercentageService {
    private final RedisPercentageCacheAdapter cache; // concrete class
}

// GOOD — service depends on the port abstraction
@Service
public class PercentageService {
    private final PercentageCacheStore cache; // port interface
}
```

---

## KISS — Keep It Simple

Prefer **the simplest solution that correctly solves the problem**.

### Method size

- A method should fit on screen (~20 lines). Extract a private helper only when the extracted piece has a clear name that improves readability.

### Reactive chains

- Compose operators linearly; avoid nested `flatMap` chains deeper than two levels.
- Prefer `map` over `flatMap` when no new publisher is introduced.

### Avoid speculative complexity

- Do not add abstractions, generics, or extension points for requirements that do not exist yet.
- Do not create helper classes, utilities, or factories for logic used only once.

### Naming

- Classes, methods, and variables must reveal intent. No abbreviations, no generic names (`data`, `obj`, `helper`).
- Boolean methods: `isExcluded()`, `hasExpired()` — not `check()`, `validate()`.

### Configuration

- Keep application configuration in `application*.yaml`; do not hard-code values in Java classes.
- Use `@ConfigurationProperties` for groups of related settings rather than multiple `@Value` fields.

---

## Quick Checklist

Before finishing any Java change, confirm:

- [ ] This class has a single, clearly named responsibility
- [ ] New behaviour was added by implementing a port, not by modifying an existing class
- [ ] Dependencies are injected via constructor using port interfaces
- [ ] No adapter class is imported inside `application/service/` or `domain/`
- [ ] No method exceeds ~20 lines; reactive chains have at most two nested levels
- [ ] No abstraction was added for a requirement that doesn't exist yet
