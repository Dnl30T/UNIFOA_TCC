# PsyTrack — Frontend Integration Guide

This document contains everything needed to build a frontend for the PsyTrack API.

---

## Base URL

```
http://localhost:8080
```

Swagger UI (for manual testing): `http://localhost:8080/swagger-ui.html`

---

## Authentication

The API uses **JWT Bearer tokens**. After login or registration, store the token and send it in every subsequent request:

```
Authorization: Bearer <token>
```

Tokens are valid for **24 hours** (`86400000 ms`).

---

## User Roles

| Role | Description |
|------|-------------|
| `EMPLOYEE` | Regular team member — can submit form responses |
| `MANAGER` | Team manager |
| `COUNSELOR` | Psychological counselor |
| `ADMIN` | Full access |
| `PENDING` | Staff account awaiting role assignment (transitional state) |

---

## Authentication Endpoints

### `POST /auth/login`

> No authentication required.

**Request**
```json
{
  "username": "john",
  "password": "secret"
}
```

**Response `200`**
```json
{ "token": "<jwt>" }
```

**Response `401`** — Invalid credentials
```json
{ "message": "Bad credentials" }
```

---

### `POST /auth/register/employee`

> No authentication required. Creates an account with role `EMPLOYEE`.

**Request**
```json
{
  "username": "john",
  "password": "secret",
  "email": "john@example.com",
  "teamCode": "ALPHA-01"
}
```

- `teamCode` must match an existing team's code (case-insensitive, auto-uppercased by the API).

**Response `201`**
```json
{ "token": "<jwt>" }
```

**Response `400`** — Blank fields or team code not found
```json
{ "message": "Team not found: ALPHA-01" }
```

---

### `POST /auth/register/staff`

> No authentication required. Creates an account with role `PENDING`.
> After this call, the user must call `/auth/claim-role` to get a real role.

**Request**
```json
{
  "username": "jane",
  "password": "secret",
  "email": "jane@example.com"
}
```

**Response `201`**
```json
{ "token": "<jwt>" }
```

---

### `POST /auth/claim-role`

> Requires authentication as `PENDING` user.
> Returns a **new token** with the definitive role — replace the stored token.

**Request**
```json
{ "role": "MANAGER" }
```

Accepted values: `MANAGER`, `COUNSELOR`

**Response `200`**
```json
{ "token": "<jwt>" }
```

**Response `400`** — Invalid role (e.g. tried to claim `EMPLOYEE` or `ADMIN`)
```json
{ "message": "Only COUNSELOR or MANAGER roles can be claimed" }
```

---

## Employees

### `POST /employees` — Hire employee

> Requires `ADMIN` role.

**Request**
```json
{
  "name": "John Doe",
  "appUserId": "uuid",
  "teamId": "uuid"
}
```

**Response `201`**
```json
{
  "id": "uuid",
  "name": "John Doe",
  "appUserId": "uuid",
  "teamId": "uuid",
  "status": "ACTIVE"
}
```

---

### `GET /employees` — List all employees

**Response `200`** — Array of employee objects (same schema as above)

---

### `GET /employees/{id}` — Get employee by ID

**Response `200`** — Single employee object
**Response `404`** — Employee not found

---

### `PUT /employees/{id}` — Update employee

**Request**
```json
{
  "name": "John Doe",
  "appUserId": "uuid",
  "teamId": "uuid",
  "status": "INACTIVE"
}
```

`status` is optional. Values: `ACTIVE`, `INACTIVE`

---

### `DELETE /employees/{id}` — Delete employee

**Response `204`** — No content

---

## Teams

### `POST /teams` — Create team

> Requires `ADMIN` role.

**Request**
```json
{
  "name": "Engineering",
  "managerId": "uuid"
}
```

**Response `201`**
```json
{
  "id": "uuid",
  "name": "Engineering"
}
```

> The team code (used for employee registration) is generated internally. It is not returned here — retrieve it via `GET /teams/{id}` if exposed, or note it from the database.

**Response `409`** — Team name already exists

---

### `GET /teams` — List all teams

**Response `200`** — Array of `{ id, name }`

---

### `GET /teams/{id}` — Get team by ID

**Response `200`** — `{ id, name }`

---

### `PUT /teams/{id}` — Update team

**Request**
```json
{ "name": "Platform", "managerId": "uuid" }
```

---

### `DELETE /teams/{id}` — Delete team

**Response `204`**

---

## Forms

### `POST /forms` — Create form

> Requires `ADMIN` role.

**Request**
```json
{
  "title": "Q1 Assessment",
  "description": "First quarter psychological check-in",
  "status": "CREATED"
}
```

`status` values: `CREATED`, `ACTIVE`, `ENDED`

**Response `201`**
```json
{
  "id": "uuid",
  "title": "Q1 Assessment",
  "description": "First quarter psychological check-in",
  "status": "CREATED",
  "questions": []
}
```

**Response `409`** — Duplicate title

---

### `GET /forms` — List forms

Optional query param: `?status=ACTIVE`

**Response `200`** — Array of form objects

---

### `GET /forms/by-title?title=Q1 Assessment` — Get form by title

---

### `GET /forms/{id}` — Get form by ID

> **Form Renderer optimisation:** this endpoint returns the complete form including all embedded questions in a single response — no additional `/questions` call required.

**Response `200`**
```json
{
  "id": "uuid",
  "title": "Q1 Assessment",
  "description": "First quarter psychological check-in",
  "status": "ACTIVE",
  "questions": [
    {
      "id": "uuid",
      "formId": "uuid",
      "text": "How stressed are you today?",
      "type": "SCALE",
      "required": true,
      "config": { "min": "1", "max": "10" },
      "order": 1
    }
  ]
}
```

`GET /forms` and `GET /forms/by-title` return the same shape.

---

### `PUT /forms/{id}` — Update form

Same body as create.

---

### `DELETE /forms/{id}` — Delete form

---

## Questions

### `POST /questions` — Create question

> Requires `ADMIN` role.

**Request**
```json
{
  "formId": "uuid",
  "text": "How stressed are you today?",
  "type": "SCALE",
  "required": true,
  "config": { "min": "1", "max": "10" },
  "order": 1
}
```

`formId` is **required** — questions are stored embedded inside their parent form.

`type` values: `SCALE`, `MULTIPLE_CHOICE`, `TEXT`, `NUMERIC`, `BOOLEAN`

`config` is a free `Map<String, String>` — use it to store type-specific metadata (e.g. scale range, choices list).

**Response `201`**
```json
{
  "id": "uuid",
  "formId": "uuid",
  "text": "How stressed are you today?",
  "type": "SCALE",
  "required": true,
  "config": { "min": "1", "max": "10" },
  "order": 1
}
```

---

### `GET /questions` — List questions

Optional query param: `?type=SCALE`

---

### `GET /questions/{id}` — Get question by ID

---

### `PUT /questions/{id}` — Update question

---

### `DELETE /questions/{id}` — Delete question

---

## Form Responses

### `POST /form-responses` — Submit a response

> Authenticated employees submit one response per question.

**Request**
```json
{
  "formId": "uuid",
  "questionId": "uuid",
  "employeeId": "uuid",
  "value": 7,
  "responseTimestamp": "2026-04-25T10:00:00Z"
}
```

`formId` is **required** — used as part of the partition key for fast writes.
`responseTimestamp` is optional (defaults to server time).
`value` is an integer — the numeric representation of the employee's answer (e.g. scale 1–10, choice index, etc).

**Response `201`**
```json
{
  "id": "uuid",
  "formId": "uuid",
  "questionId": "uuid",
  "employeeId": "uuid",
  "value": 7,
  "responseTimestamp": "2026-04-25T10:00:00Z"
}
```

---

### `GET /form-responses` — List responses

Optional query params:
- `?employeeId=uuid`
- `?questionId=uuid`

---

### `GET /form-responses/{id}` — Get response by ID

---

### `PUT /form-responses/{id}` — Update response

---

### `DELETE /form-responses/{id}` — Delete response

---

## Employee Results

Stores the calculated psychological health result per employee per form.

### `POST /employee-results` — Create result

> Requires `ADMIN` role.

**Request**
```json
{
  "employeeId": "uuid",
  "formId": "uuid",
  "score": 72,
  "riskLevel": "LOW",
  "calculatedAt": "2026-04-25T12:00:00Z"
}
```

`score`: integer `0–100`
`riskLevel` values: `LOW`, `MEDIUM`, `HIGH`
`calculatedAt` is optional (defaults to server time).

**Response `201`**
```json
{
  "id": "uuid",
  "employeeId": "uuid",
  "formId": "uuid",
  "score": 72,
  "riskLevel": "LOW",
  "calculatedAt": "2026-04-25T12:00:00Z"
}
```

---

### `GET /employee-results` — List results

Optional query params:
- `?employeeId=uuid`
- `?formId=uuid`

---

### `GET /employee-results/{id}` — Get by ID

---

### `PUT /employee-results/{id}` — Update result

---

### `DELETE /employee-results/{id}` — Delete result

---

## Team Results

Stores the consolidated psychological health result per team per form.

### `POST /team-results` — Create result

> Requires `ADMIN` role.

**Request**
```json
{
  "teamId": "uuid",
  "formId": "uuid",
  "averageScore": 68.5,
  "riskLevelDistribution": {
    "LOW": 10,
    "MEDIUM": 4,
    "HIGH": 1
  },
  "calculatedAt": "2026-04-25T12:00:00Z"
}
```

**Response `201`**
```json
{
  "id": "uuid",
  "teamId": "uuid",
  "formId": "uuid",
  "averageScore": 68.5,
  "riskLevelDistribution": {
    "LOW": 10,
    "MEDIUM": 4,
    "HIGH": 1
  },
  "calculatedAt": "2026-04-25T12:00:00Z"
}
```

---

### `GET /team-results` — List results

Optional query params:
- `?teamId=uuid`
- `?formId=uuid`

---

### `GET /team-results/{id}` — Get by ID

---

### `PUT /team-results/{id}` — Update result

---

### `DELETE /team-results/{id}` — Delete result

---

## Error Response Format

All errors return:

```json
{ "message": "Human-readable error description" }
```

| HTTP Status | When |
|-------------|------|
| `400` | Validation failure, blank required fields, invalid enum value |
| `401` | Missing or invalid JWT token |
| `403` | Authenticated but insufficient role |
| `404` | Resource not found |
| `409` | Duplicate name/title |

---

## Enum Reference

### `UserRole`
`ADMIN` · `MANAGER` · `COUNSELOR` · `EMPLOYEE` · `PENDING`

### `EmployeeStatus`
`ACTIVE` · `INACTIVE`

### `Status` (Form)
`CREATED` · `ACTIVE` · `ENDED`

### `QuestionType`

| Value | Description | Suggested `config` keys |
|-------|-------------|-------------------------|
| `SCALE` | Numeric scale with configurable range | `min`, `max`, `step` |
| `SINGLE_CHOICE` | Select one option (radio) | `options` (comma-separated) |
| `MULTIPLE_CHOICE` | Select many options (checkboxes) | `options` (comma-separated) |
| `LIKERT` | 5-point agreement scale (Strongly Disagree → Strongly Agree) | — |
| `RANKING` | Rank a list of items by preference | `items` (comma-separated) |
| `MATRIX` | Rate multiple sub-items on the same scale (grid) | `rows`, `columns` |
| `SLIDER` | Continuous visual slider | `min`, `max`, `step` |
| `TEXT` | Open-ended free text | — |
| `NUMERIC` | Integer or decimal input | `min`, `max` |
| `BOOLEAN` | Yes / No | — |
| `DATE` | Date picker | — |
| `MAP` | 2D canvas where the user positions a dot; answer is saved as `x,y` coordinates | `xLabel`, `yLabel`, `xMin`, `xMax`, `yMin`, `yMax` |

### `RiskLevel`
`LOW` · `MEDIUM` · `HIGH`

---

## Typical User Journeys

### Employee completes a form
1. `POST /auth/register/employee` → receive token
2. `GET /forms?status=ACTIVE` → pick a form (response includes the embedded `questions` list)
3. `POST /form-responses` (once per question, sending `formId` + `questionId`) → submit answers

> **No separate `/questions` call is needed.** The full form including all questions is returned by the forms endpoint in a single query.

### Admin reviews team health
1. `GET /team-results?teamId=uuid` → see consolidated results
2. `GET /employee-results?formId=uuid` → see individual scores

### Staff account setup
1. `POST /auth/register/staff` → receive `PENDING` token
2. `POST /auth/claim-role` body `{ "role": "MANAGER" }` → receive final token
