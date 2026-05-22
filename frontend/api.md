# PsyTrack API

Backend for a team psychological health monitoring platform.
Built with **Spring Boot 4 + Kotlin/Java**, **PostgreSQL**, **ScyllaDB**, and **JWT** authentication.

---

## Requirements

- Java 17+
- Docker & Docker Compose

---

## Running locally

```bash
# Start PostgreSQL + ScyllaDB (waits for ScyllaDB health before applying schema)
docker compose up -d

# Run the application
./gradlew bootRun
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Architecture — Dual-Store

| Store | Purpose |
|-------|---------|
| **PostgreSQL** | Identity & organisational data: users, employees, teams, results |
| **ScyllaDB** | Denormalised wide-column data: forms (with embedded questions) and form responses |

Forms are stored with all questions embedded in a single ScyllaDB partition (`form_id` as partition key). The Form Renderer fetches an entire form — metadata + all questions — in **one query** with no JOINs.

Form responses use a composite partition key `(form_id, employee_id)`, so a complete form submission by one employee hits **a single partition** — coordination-free and write-optimised.

---

## Authentication

### Employee
| Step | Endpoint | Description |
|------|----------|-------------|
| 1 | `POST /auth/register/employee` | Create account with `teamCode` |
| 2 | `POST /auth/login` | Receive JWT token |

### Manager / Counselor
| Step | Endpoint | Description |
|------|----------|-------------|
| 1 | `POST /auth/register/staff` | Create account (role set to `PENDING`) |
| 2 | `POST /auth/claim-role` | Claim `MANAGER` or `COUNSELOR` role and receive updated JWT |
| 3 | `POST /auth/login` | Subsequent authentications |

All protected endpoints require:
```
Authorization: Bearer <token>
```

---

## Roles

| Role | Description |
|------|-------------|
| `EMPLOYEE` | Regular team member |
| `MANAGER` | Team manager |
| `COUNSELOR` | Psychological counselor |
| `ADMIN` | Full platform access |
| `PENDING` | Staff account awaiting role assignment |

---

## Business Rules

### Users & Authentication
- Usernames and emails must be unique across the platform.
- Staff accounts are created with role `PENDING` and **must** call `POST /auth/claim-role` before accessing any resource.
- Only `MANAGER` and `COUNSELOR` can be claimed; `EMPLOYEE` and `ADMIN` cannot be self-assigned.
- Tokens are valid for **24 hours**; after expiry the user must log in again.

### Roles & Responsibilities

| Role | Responsibility |
|------|----------------|
| `COUNSELOR` | **Owner of the clinical process.** Creates forms, adds questions, distributes forms to teams, reads raw responses, and submits individual + consolidated results. Can manage multiple teams simultaneously. |
| `MANAGER` | Creates their team (generates `teamCode`). Each manager owns **exactly one team**. Can only view their team's consolidated result report — never individual employee responses. |
| `EMPLOYEE` | Answers active forms assigned to their team. Can view **only their own individual result**, and only after the Counselor has submitted it. |
| `ADMIN` | Purely analytical and technical-support role (global metrics, user management). Does **not** interfere in the clinical flow. |

### Teams & Employees
- Team names must be unique.
- A `Manager` creates their team; the API generates a unique `teamCode` for that team.
- Each `Manager` owns exactly one team.
- An employee links to a team at registration time via `teamCode`.
- An employee account (`AppUser`) is separate from the employee record (`Employee`); an admin links them via `POST /employees`.

### Form Lifecycle
Forms follow a strict linear lifecycle managed by the **Counselor**:

```
CREATED → ACTIVE → ENDED
```

1. **CREATED** — The Counselor creates the form and adds all questions.
2. **ACTIVE** — The Counselor distributes the form to one or more teams. Employees can now submit responses.
3. **ENDED** — The Counselor closes the form after the deadline or once all responses are collected. The form returns to the Counselor for evaluation.

Business constraints:
- Form titles must be unique.
- A form can be distributed to **N teams** simultaneously (one form → many teams).
- Questions are always associated with a specific form via `formId` and are stored **embedded** inside the form row in ScyllaDB.
- Updating a form's metadata preserves the existing embedded questions.
- Question `order` must be a non-negative integer.
- `config` is a free `Map<String, String>` for type-specific metadata (e.g. `min`/`max` for `SCALE`, `options` for `MULTIPLE_CHOICE`).

### Form Responses
- Each response records one employee's answer to one question, always referencing the parent `formId`.
- `value` is an integer — the numeric encoding of the answer (scale position, choice index, etc.).
- All answers from one employee for one form are written to a **single ScyllaDB partition** `(form_id, employee_id)` — fast and coordination-free.

### Evaluation & Results (Manual — no automatic scoring)
- The system does **not** calculate scores automatically.
- After a form reaches `ENDED`, the **Counselor** reads the raw responses and manually submits:
  - An **individual result** (`POST /employee-results`) for each employee — score `0–100` + `riskLevel`.
  - A **consolidated result** (`POST /team-results`) for the whole team — average score + risk distribution.
- Visibility rules:
  - An `EMPLOYEE` gains access to their own individual result **only after** the Counselor submits it.
  - A `MANAGER` gains access to their team's consolidated report **only after** the Counselor submits it.
  - A `MANAGER` **never** sees individual employee responses or scores.
- `riskLevel` values: `LOW`, `MEDIUM`, `HIGH`.

---

## Application Flow

```
[MANAGER]
  ├─ POST /auth/register/staff   → PENDING account
  ├─ POST /auth/claim-role       → become MANAGER
  └─ POST /teams                 → create team (teamCode is generated)

[EMPLOYEE]
  ├─ POST /auth/register/employee  → account linked to team via teamCode
  ├─ GET  /forms?status=ACTIVE     → list active forms assigned to their team
  │                                  (each form includes all embedded questions)
  ├─ POST /form-responses          → submit one answer per question (formId + questionId)
  └─ GET  /employee-results        → view OWN results (only after Counselor submits them)

[COUNSELOR]  ← protagonist of the clinical process
  ├─ POST /auth/register/staff   → PENDING account
  ├─ POST /auth/claim-role       → become COUNSELOR
  │
  │  ── Form creation ──
  ├─ POST /forms                 → create assessment form (status: CREATED)
  ├─ POST /questions             → add questions to the form (requires formId)
  │
  │  ── Distribution ──
  ├─ PUT  /forms/{id}            → set status to ACTIVE (employees can now respond)
  │                                (form is distributed to the target teams)
  │
  │  ── Collection ──
  ├─ GET  /form-responses?formId=...       → monitor incoming responses
  │
  │  ── Closing ──
  ├─ PUT  /forms/{id}            → set status to ENDED
  │
  │  ── Evaluation (manual) ──
  ├─ GET  /form-responses?employeeId=...   → read raw answers per employee
  ├─ POST /employee-results                → submit individual score + riskLevel
  │                                          (employee gains visibility immediately)
  └─ POST /team-results                    → submit consolidated score + risk distribution
                                             (manager gains visibility immediately)

[ADMIN]  ← analytical / technical support only
  ├─ GET  /employee-results      → global individual metrics
  └─ GET  /team-results          → global team metrics
```

---

## API Overview

| Tag | Base path | Description |
|-----|-----------|-------------|
| Authentication | `/auth` | Register, login, claim role |
| Employees | `/employees` | Manage employees linked to teams |
| Teams | `/teams` | Create and manage teams |
| Forms | `/forms` | Psychological assessment forms (includes embedded questions) |
| Questions | `/questions` | Add/update questions inside a form |
| Form Responses | `/form-responses` | Responses submitted by employees |
| Employee Results | `/employee-results` | Individual psychological health results |
| Team Results | `/team-results` | Consolidated results per team |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/psytrack/unformulieren/
│   │   ├── adapter/
│   │   │   ├── in/security/          # JWT filter, SecurityConfig, OpenAPI config
│   │   │   └── out/
│   │   │       ├── persistence/
│   │   │       │   ├── form/         # Cassandra entity, UDT, repository, adapter, mapper
│   │   │       │   ├── formResponse/ # Cassandra entity, composite PK, repository, adapter
│   │   │       │   ├── question/     # Adapter/mapper operating on embedded UDTs
│   │   │       │   ├── employee/     # JPA entity, repository, adapter
│   │   │       │   ├── team/         # JPA entity, repository, adapter
│   │   │       │   ├── user/         # JPA entity, repository, adapter
│   │   │       │   └── …             # employeeResult, teamResult
│   │   │       └── security/         # JwtTokenService
│   │   ├── application/
│   │   │   ├── port/out/             # Repository port interfaces
│   │   │   └── service/              # Business logic (FormService, QuestionService, …)
│   │   ├── domain/
│   │   │   ├── enums/                # UserRole, RiskLevel, Status, QuestionType, …
│   │   │   ├── exception/            # Domain exceptions
│   │   │   ├── model/                # Aggregates (Form, Question, FormResponse, …)
│   │   │   ├── validation/           # DomainValidations
│   │   │   └── valueobject/          # Email
│   │   └── infrastructure/
│   │       ├── ScyllaDbConfig.java   # Cassandra session + keyspace bootstrap
│   │       └── JpaConfig.java        # JPA repository scope
│   ├── kotlin/com/psytrack/unformulieren/
│   │   └── adapter/in/web/           # REST controllers + DTOs
│   └── resources/
│       ├── application.properties
│       ├── db/migration/             # Flyway SQL migrations (V1–V5, PostgreSQL)
│       └── db/scylla/
│           └── schema.cql            # ScyllaDB keyspace, UDT and table definitions
└── test/
```

---

## Environment Variables

### PostgreSQL

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `unformulieren` | Database name |
| `DB_USER` | `unformulieren` | Database user |
| `DB_PASSWORD` | `unformulieren` | Database password |

### ScyllaDB

| Variable | Default | Description |
|----------|---------|-------------|
| `SCYLLA_HOST` | `localhost` | ScyllaDB contact point |
| `SCYLLA_PORT` | `9042` | CQL native transport port |
| `SCYLLA_KEYSPACE` | `psytrack` | Keyspace name |
| `SCYLLA_DATACENTER` | `datacenter1` | Local datacenter name |

### Application

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | *(dev default)* | HS256 secret — **change in production** |

