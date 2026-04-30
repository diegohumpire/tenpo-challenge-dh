# API Endpoints

Base URL: `http://localhost:8080`

---

## Calculations

### POST /api/v1/calculations

Sums `num1` and `num2`, then applies a percentage obtained from an external service.

**Headers (required)**

| Header | Description | Example |
|--------|-------------|---------|
| `X-Transactional-Id` | Correlation ID for distributed tracing, injected by the API Gateway | `550e8400-e29b-41d4-a716-446655440000` |
| `X-User-Id` | Authenticated user identity, injected by the API Gateway | `user-123` |

**Request body** (`application/json`)

```json
{
  "num1": 5.0,
  "num2": 5.0
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `num1` | number | yes | First number |
| `num2` | number | yes | Second number |

**Responses**

| Status | Description | Content-Type |
|--------|-------------|--------------|
| `201` | Calculation successful | `*/*` → `CalculationResponse` |
| `400` | Invalid input or missing headers | `application/problem+json` → `ProblemDetail` |
| `429` | Rate limit exceeded | `application/problem+json` → `ProblemDetail` |
| `503` | External percentage service unavailable | `application/problem+json` → `ProblemDetail` |

**201 response body example**

```json
{
  "num1": 5.0,
  "num2": 5.0,
  "sum": 10.0,
  "percentage": 10.0,
  "result": 11.0
}
```

**201 response headers**

| Header | Description |
|--------|-------------|
| `X-Transactional-Id` | Echoed correlation ID |
| `X-RateLimit-Limit` | Maximum requests allowed per time window |
| `X-RateLimit-Remaining` | Remaining requests in the current window |

**429 response headers**

| Header | Description |
|--------|-------------|
| `X-RateLimit-Limit` | Maximum requests allowed per time window |
| `X-RateLimit-Remaining` | `0` |
| `X-RateLimit-Reset` | Unix timestamp when the window resets |
| `Retry-After` | Seconds to wait before retrying |

---

## Audit Logs

### GET /api/v1/audit-logs

Returns a lightweight paginated list of all audit log entries. Supports optional filters on indexed fields. Excludes no entries by default.

**Headers (required)**

| Header | Example |
|--------|---------|
| `X-Transactional-Id` | `550e8400-e29b-41d4-a716-446655440000` |
| `X-User-Id` | `user-123` |

**Query parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | `0` | Page number (0-indexed) |
| `size` | integer | `20` | Page size (max 100) |
| `sort` | string | `createdAt,desc` | Sort field and direction |
| `userId` | string | — | Filter by userId |
| `transactionalId` | string | — | Filter by transactionalId |
| `action` | string | — | Filter by action |
| `actionType` | enum | — | `HTTP_REQUEST` \| `CALCULATION` \| `EXTERNAL_CALL` \| `CACHE_ACCESS` \| `SYSTEM` |
| `callDirection` | enum | — | `IN` \| `OUT` |

**Responses**

| Status | Description | Schema |
|--------|-------------|--------|
| `200` | Audit logs retrieved | `PageResponseAuditLogSummaryResponse` |
| `429` | Rate limit exceeded | `ProblemDetail` |

---

### GET /api/v1/audit-logs/{id}

Returns all fields for a single audit log entry, including HTTP headers and bodies.

**Headers (required)**

| Header | Example |
|--------|---------|
| `X-Transactional-Id` | `550e8400-e29b-41d4-a716-446655440000` |
| `X-User-Id` | `user-123` |

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | integer (int64) | Audit log ID |

**Responses**

| Status | Description | Schema |
|--------|-------------|--------|
| `200` | Audit log found | `AuditLogDetailResponse` |
| `404` | Audit log not found | `ProblemDetail` |
| `429` | Rate limit exceeded | `ProblemDetail` |

---

### GET /api/v1/audit-logs/users/{userId}

Returns all audit log entries for a given user. Excludes `GET_AUDIT_LOGS` action entries.

**Headers (required)**

| Header | Example |
|--------|---------|
| `X-Transactional-Id` | `550e8400-e29b-41d4-a716-446655440000` |
| `X-User-Id` | `user-123` |

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `userId` | string | User ID to filter by |

**Query parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | `0` | Page number (0-indexed) |
| `size` | integer | `20` | Page size (max 100) |
| `sort` | string | `createdAt,asc` | Sort field and direction |

**Responses**

| Status | Description | Schema |
|--------|-------------|--------|
| `200` | Audit logs retrieved | `PageResponseAuditLogDetailResponse` |
| `429` | Rate limit exceeded | `ProblemDetail` |

---

### GET /api/v1/audit-logs/transactions/{transactionalId}

Returns all audit log entries for a given transactional ID. Excludes `GET_AUDIT_LOGS` action entries.

**Headers (required)**

| Header | Example |
|--------|---------|
| `X-Transactional-Id` | `550e8400-e29b-41d4-a716-446655440000` |
| `X-User-Id` | `user-123` |

**Path parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `transactionalId` | string | Transactional ID to filter by |

**Query parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | integer | `0` | Page number (0-indexed) |
| `size` | integer | `20` | Page size (max 100) |
| `sort` | string | `createdAt,asc` | Sort field and direction |

**Responses**

| Status | Description | Schema |
|--------|-------------|--------|
| `200` | Audit logs retrieved | `PageResponseAuditLogDetailResponse` |
| `429` | Rate limit exceeded | `ProblemDetail` |

---

## Schemas

### CalculationRequest

```json
{
  "num1": 5.0,
  "num2": 5.0
}
```

### CalculationResponse

```json
{
  "num1": 5.0,
  "num2": 5.0,
  "sum": 10.0,
  "percentage": 10.0,
  "result": 11.0
}
```

### AuditLogDetailResponse

| Field | Type | Description |
|-------|------|-------------|
| `id` | integer (int64) | |
| `createdAt` | string (date-time) | |
| `action` | string | |
| `actionType` | enum | `HTTP_REQUEST` \| `CALCULATION` \| `EXTERNAL_CALL` \| `CACHE_ACCESS` \| `SYSTEM` |
| `callDirection` | enum | `IN` \| `OUT` |
| `userId` | string | |
| `transactionalId` | string | |
| `method` | string | HTTP method |
| `endpoint` | string | |
| `params` | string | |
| `requestHeaders` | string | |
| `requestBody` | string | |
| `responseHeaders` | string | |
| `responseBody` | string | |
| `statusCode` | integer (int32) | |
| `errorMessage` | string | |
| `durationMs` | integer (int64) | |

### AuditLogSummaryResponse

Same as `AuditLogDetailResponse` but excludes `requestHeaders`, `requestBody`, `responseHeaders`, and `responseBody`. Includes a `_links` map with HATEOAS-style `href` references.

### ProblemDetail

| Field | Type |
|-------|------|
| `type` | string (uri) |
| `title` | string |
| `status` | integer (int32) |
| `detail` | string |
| `instance` | string (uri) |
| `properties` | object |

### PageResponse (generic wrapper)

| Field | Type |
|-------|------|
| `content` | array |
| `page` | integer (int32) |
| `size` | integer (int32) |
| `totalElements` | integer (int64) |
| `totalPages` | integer (int32) |
