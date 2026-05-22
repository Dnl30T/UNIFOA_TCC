# PsyTrack — Overview Técnico

> Data de referência: 19 maio 2026  
> Representa o estado atual do repositório; itens marcados com ⚠️ indicam lacunas ou premissas.

---

## 1. Visão Geral do Sistema

PsyTrack é uma plataforma de monitoramento de saúde psicológica de equipes corporativas. O sistema segue uma arquitetura cliente-servidor: uma SPA Angular com SSR consome uma API REST Spring Boot, respaldada por dois bancos de dados com responsabilidades distintas.

```
┌──────────────────────────────────────────┐
│  Browser / SSR Node (Angular 21 + SSR)   │
│  localhost:4200 (dev) / servidor (prod)  │
└────────────────────┬─────────────────────┘
                     │ HTTP/JSON  Authorization: Bearer <JWT>
┌────────────────────▼─────────────────────┐
│  REST API  (Spring Boot 4 / Kotlin+Java) │
│  localhost:8080                          │
│  Swagger UI: /swagger-ui.html            │
└───────────┬──────────────────┬───────────┘
            │                  │
   ┌────────▼───────┐  ┌───────▼────────────┐
   │  PostgreSQL 16 │  │  ScyllaDB 5.x       │
   │  (dados op.)   │  │  (wide-column)      │
   └────────────────┘  └─────────────────────┘
                     │
           ┌─────────▼─────────┐
           │  Azure Blob (fotos │
           │  de perfil)        │
           └───────────────────┘
```

---

## 2. Backend

### 2.1 Stack

| Item | Versão / Detalhe |
|------|-----------------|
| Linguagem | Kotlin 2.2.20 + Java 17 (código misto) |
| Framework | Spring Boot 4.0.5 |
| ORM / Cassandra | Spring Data JPA + Spring Data Cassandra |
| Migrations | Flyway (somente PostgreSQL) |
| Autenticação | Spring Security + JWT (jjwt 0.12.6) |
| Validação | Spring Validation / Jakarta Bean Validation |
| Docs da API | SpringDoc OpenAPI 2.8.13 (Swagger UI) |
| Blob Storage | Azure Blob SDK 12.29.0 (Azurite localmente) |
| Build | Gradle (Kotlin DSL), Java toolchain 17 |
| Testes | JUnit 5 + Mockito-Kotlin 5.4.0 |
| Utilitários | Lombok, jmail 2.1.0, Jackson Kotlin module |

### 2.2 Arquitetura — Hexagonal (Ports & Adapters)

```
adapter/in/web/         ← Controllers REST + DTOs (Kotlin)
application/service/    ← Orquestração de negócio (9 serviços)
application/port/out/   ← Interfaces de repositório e blob storage
domain/                 ← Modelos, enums, value objects, exceções
adapter/out/
  persistence/          ← Adaptadores JPA (PostgreSQL) e Cassandra (ScyllaDB)
  security/             ← JwtTokenService
  storage/              ← BlobStorageAdapter (Azure)
infrastructure/         ← ScyllaDbConfig, JpaConfig
```

**Convenção importante:** controllers são escritos em Kotlin; entidades de domínio e adaptadores de persistência são em Java.

### 2.3 Endpoints da API (grupos)

| Grupo | Base path | Autenticação |
|-------|-----------|-------------|
| Autenticação | `/auth` | Pública (login/register); `claim-role` protegido |
| Funcionários | `/employees` | Bearer JWT |
| Equipes | `/teams` | Bearer JWT |
| Formulários | `/forms` | Bearer JWT |
| Questões | `/questions` | Bearer JWT |
| Submissão de formulários | `/form-submissions` | Bearer JWT |
| Respostas | `/form-responses` | Bearer JWT |
| Resultados individuais | `/employee-results` | Bearer JWT |
| Resultados de equipe | `/team-results` | Bearer JWT |
| Avaliações clínicas | `/evaluations` | Bearer JWT (COUNSELOR / ADMIN) |
| Usuários | `/users` | Bearer JWT |

Swagger UI completo disponível em: `http://localhost:8080/swagger-ui.html`

### 2.4 Autenticação e Autorização

**Fluxo JWT:**
1. POST `/auth/login` ou `/auth/register/employee` → retorna JWT (validade 24 h)
2. Todas as requisições protegidas: `Authorization: Bearer <token>`
3. `JwtAuthenticationFilter` valida o token a cada requisição
4. Token contém: `username` + `role`

**Roles:**

| Role | Nível de acesso |
|------|----------------|
| `EMPLOYEE` | Responde formulários, vê próprios resultados |
| `MANAGER` | Cria equipe, vê relatório consolidado da equipe |
| `COUNSELOR` | Dono do processo clínico; full access ao fluxo de avaliação |
| `ADMIN` | Acesso global analítico; não interfere no fluxo clínico |
| `PENDING` | Conta staff aguardando `claim-role` |

**Arquivos de segurança:**
- `adapter/in/security/SecurityConfig.java`
- `adapter/in/security/JwtAuthenticationFilter.java`
- `adapter/out/security/JwtTokenService.java`

### 2.5 Persistência — Dual-Store

#### PostgreSQL (dados operacionais)

Gerenciado por Flyway; 8 migrations (`V1` a `V8`).

| Tabela | Responsabilidade |
|--------|-----------------|
| `app_users` | Identidade, hash de senha, role, campos LGPD |
| `employees` | Funcionário vinculado a `app_users` e `teams` |
| `teams` | Equipe com `manager_id`, `counselor_id`, `team_code` |
| `team_memberships` | Relação many-to-many employee ↔ team |
| `forms` (legacy) | Schema original de formulários (PostgreSQL) |
| `questions` | Perguntas (schema original) |
| `question_configs` | Configurações de perguntas |
| `form_responses` (legacy) | Respostas (schema original) |
| `employee_results` | Score 0–100 + `risk_level` por employee/form |
| `team_results` + `team_result_risk_level_distribution` | Score médio + distribuição de risco por equipe |

> ⚠️ **Premissa:** `forms`, `questions` e `form_responses` no PostgreSQL são schema legado. O fluxo operacional atual utiliza ScyllaDB para essas entidades.

**Constraints notáveis:**
- `app_users.username` e `.email`: únicos
- `teams.name`: único
- `forms.title`: único
- `teams.manager_id`: único (1 equipe por manager)
- `teams.counselor_id`: único (1 equipe por counselor)
- `employee_results.score`: CHECK entre 0 e 100

**Histórico de migrations:**
- V1: tabela `employees`
- V2: tabelas `teams`, `forms`, `questions`, `question_configs`, `form_responses`, `employee_results`, `team_results`, `app_users`
- V3: campos LGPD em `app_users` (consent, anonymized, soft-delete)
- V4: gerenciamento de equipes (manager, counselor, team_code)
- V5: vínculo `employees.app_user_id`
- V6: constraint UNIQUE em `manager_id`
- V7: campos de perfil (name, phone_number, company, job_title, profile_picture_url)
- V8: constraint UNIQUE em `counselor_id`

#### ScyllaDB — keyspace `psytrack` (dados wide-column)

Schema aplicado automaticamente via container `scylladb-init` no Docker Compose.

| Tabela | Chave de partição | Responsabilidade |
|--------|-----------------|-----------------|
| `forms` | `form_id` | Formulário + questões embutidas (UDT `question_structure`) |
| `form_responses` | `(form_id, employee_id)` | Todas as respostas de um employee a um form em 1 linha |
| `therapist_evaluations` | `(form_id, employee_id)` | Avaliação clínica completa do counselor |

**UDT `question_structure`:** `id`, `form_id`, `text`, `type`, `required`, `config` (map), `display_order`

**Estratégia zero-join:** a busca de um formulário retorna metadados + todas as perguntas em uma única leitura de partição.

**Estratégia de escrita:** a submissão completa de um employee é uma única escrita atômica na partição `(form_id, employee_id)`.

### 2.6 Integrações Externas

| Integração | Uso | Ambiente local |
|------------|-----|----------------|
| Azure Blob Storage | Upload e remoção de fotos de perfil | Azurite (emulador local, porta 10000) |

### 2.7 Execução Local e Build

```bash
# 1. Subir dependências (PostgreSQL, ScyllaDB, Azurite)
docker compose up -d

# 2. Iniciar aplicação
./gradlew bootRun

# 3. Rodar testes
./gradlew test

# 4. Popular banco com dados de teste
python scripts/seed.py [--reset] [--db-url ...]
```

**Variáveis de ambiente (principais):**

| Variável | Default | Descrição |
|----------|---------|-----------|
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_PORT` | `5432` | Porta PostgreSQL |
| `DB_NAME` | `unformulieren` | Database |
| `DB_USER` | `unformulieren` | Usuário |
| `DB_PASSWORD` | `unformulieren` | Senha |
| `SCYLLA_HOST` | `localhost` | Host ScyllaDB |
| `SCYLLA_PORT` | `9042` | Porta CQL |
| `SCYLLA_KEYSPACE` | `psytrack` | Keyspace |
| `SCYLLA_DATACENTER` | `datacenter1` | Datacenter local |

### 2.8 Tratamento de Erros

`EmployeeControllerAdvice.kt` centraliza o mapeamento de exceções de domínio para respostas HTTP:
- `*NotFoundException` → HTTP 404
- `*AlreadyExistsException` / conflitos → HTTP 409
- Erros de validação → HTTP 400

---

## 3. Frontend

### 3.1 Stack

| Item | Versão / Detalhe |
|------|-----------------|
| Framework | Angular 21.2.0 (standalone components) |
| UI | Angular Material 21.2.8 |
| SSR | Angular SSR 21.2.6 + Express.js |
| Estado | Angular Signals + Computed (nativo) |
| HTTP | Angular HttpClient + 13 serviços |
| Linguagem | TypeScript 5.9.2 (strict mode) |
| Estilos | SCSS + Material theming |
| Testes | Vitest |
| Build | Angular CLI 21.2.6 |

### 3.2 Arquitetura

```
src/app/
├── core/
│   ├── guards/          authGuard, roleGuard
│   ├── interceptors/    authInterceptor (injeta JWT em todo request HTTP)
│   └── services/        13 serviços HTTP (um por recurso da API)
├── features/
│   ├── auth/            login, register, forgot-password
│   ├── dashboard/       shell + 16 páginas (lazy-loaded por role)
│   └── landing/         página pública de apresentação
└── shared/
    ├── components/      ⚠️ vazio atualmente
    ├── directives/      ⚠️ vazio atualmente
    └── pipes/           ⚠️ vazio atualmente
```

**Padrão predominante:** standalone components com `inject()`, signals locais por componente, `forkJoin` para paralelizar chamadas à API.

### 3.3 Roteamento

| Rota | Guard | Componente |
|------|-------|-----------|
| `/` | — | Landing |
| `/auth/login` | — | Login |
| `/auth/register` | — | Register |
| `/auth/forgot-password` | — | ForgotPassword ⚠️ skeleton |
| `/dashboard` | `authGuard` | DashboardShell |
| `/dashboard/home` | — | DashboardHome |
| `/dashboard/my-assessments` | `roleGuard('EMPLOYEE')` | MyAssessments |
| `/dashboard/my-reports` | `roleGuard('EMPLOYEE')` | MyReports |
| `/dashboard/manager-assessments` | `roleGuard('MANAGER','ADMIN')` | ManagerAssessments |
| `/dashboard/team-overview` | `roleGuard('MANAGER','ADMIN')` | TeamOverview |
| `/dashboard/report-history` | `roleGuard('MANAGER','ADMIN')` | ReportHistory |
| `/dashboard/counselor-assessments` | `roleGuard('COUNSELOR','ADMIN')` | CounselorAssessments |
| `/dashboard/counselor-reports` | `roleGuard('COUNSELOR','ADMIN')` | CounselorReports |
| `/dashboard/my-teams` | `roleGuard('COUNSELOR','ADMIN')` | MyTeams |
| `/dashboard/teams/add` | `roleGuard('COUNSELOR','ADMIN')` | TeamAdd |
| `/dashboard/forms/builder` | `roleGuard('COUNSELOR','ADMIN')` | FormBuilder |
| `/dashboard/forms/review` | `roleGuard('COUNSELOR','ADMIN')` | FormReview |
| `/dashboard/forms/view/:id` | `roleGuard('COUNSELOR','ADMIN','EMPLOYEE')` | FormViewer |
| `/dashboard/forms/update/:id` | `roleGuard('COUNSELOR','ADMIN')` | FormUpdate |
| `/dashboard/forms/publish/:id` | `roleGuard('COUNSELOR','ADMIN')` | FormPublish |
| `/dashboard/employee-responses` | `roleGuard('COUNSELOR','ADMIN')` | EmployeeResponses |
| `/dashboard/settings` | — | Settings ⚠️ sem Save() conectado à API |

### 3.4 Serviços HTTP

| Serviço | Recurso backend |
|---------|----------------|
| `AuthService` | `/auth` — login, register, logout, token signal |
| `EmployeeService` | `/employees` — CRUD + `getMe()` |
| `TeamService` | `/teams` — CRUD + `getMyTeam()`, `getByCode()` |
| `FormService` | `/forms` — CRUD por status |
| `QuestionService` | `/questions` — CRUD por formId |
| `FormSubmissionService` | `/form-submissions` — submete respostas |
| `FormResponseService` | `/form-responses` — lê respostas individuais |
| `EmployeeResultService` | `/employee-results` — score + riskLevel |
| `TeamResultService` | `/team-results` — score médio da equipe |
| `TherapistEvaluationService` | `/evaluations` — upsert de avaliação clínica |
| `FormRendererService` | (local) — converte `QuestionResponseDto` → `FormField` |
| `RoleService` | (local) — mapeia role do backend para role da UI |
| `CounselorTeamPoolService` | (localStorage) — pool de equipes monitoradas pelo counselor |

**Interceptor:** `authInterceptor` clona cada request e adiciona `Authorization: Bearer <token>` quando `AuthService.token()` estiver preenchido.

**Mapeamento de role (backend → UI):**
- `ADMIN` → `counselor` (acesso a todas as rotas de counselor)
- `COUNSELOR` → `counselor`
- `MANAGER` → `manager`
- `EMPLOYEE` / default → `employee`

### 3.5 Estado e Comunicação

- Estado local por componente via `signal<T>()` e `computed()`
- `AuthService._token` signal → derivados: `username`, `backendRole`, `isLoggedIn`
- Draft de formulário: persiste em `sessionStorage` (form-builder → form-review)
- Pool de equipes do counselor: persiste em `localStorage` (`psytrack_team_pool_<username>`)
- Diálogos contextuais: `MatDialog.open(Component, { data })` — e.g. membros de equipe

### 3.6 Encoding de Respostas de Formulário

O frontend codifica respostas complexas em inteiros para o campo `value` da API:

| Tipo | Encoding |
|------|---------|
| TEXT | Salvo em `text_answers` (string) |
| NUMERIC / SCALE / SLIDER | Valor numérico direto |
| BOOLEAN | 0 = false, 1 = true |
| SINGLE_CHOICE | Índice da opção |
| MULTIPLE_CHOICE | Bitmask (bit i = opção i selecionada) |
| RANKING | Decimal encoding (e.g. rank [2,0,1] → 201) |
| MATRIX | Índice da coluna por linha, codificado em posições decimais |
| MAP | Coordenadas X e Y no range [0,1] |

### 3.7 Ciclo de Build e SSR

```bash
# Desenvolvimento
ng serve                            # SPA local em localhost:4200

# Build de produção
ng build                            # Artefato em dist/frontend-tcc/

# SSR (produção)
npm run serve:ssr:frontend-tcc      # Node.js + Express servindo server.mjs

# Testes unitários
ng test                             # Vitest
```

**Ambientes:**
- Dev: `apiUrl = 'http://localhost:8080'` (`environment.ts`)
- Prod: `apiUrl = ''` — ⚠️ **não configurado**; requer injeção em build-time

---

## 4. Riscos e Lacunas Técnicas

### Backend

| Item | Severidade | Detalhe |
|------|-----------|---------|
| Schema duplo (PostgreSQL + ScyllaDB) para forms/questions/responses | Alta | Potencial de inconsistência; sem mecanismo de sincronização entre stores |
| Sem transações distribuídas | Alta | Operações que afetam ambas as stores não são atômicas |
| JWT sem refresh token | Média | Sessão expira após 24 h sem renovação automática |
| Sem rate limiting em endpoints públicos | Média | `/auth/login`, `/auth/register` sem proteção contra força bruta |
| LGPD: soft-delete sem purga automática | Média | Dados anonimizados persistem indefinidamente |
| Sem paginação nos endpoints de listagem | Baixa | Retorno completo de coleções grandes |
| Sem versionamento de API | Baixa | Mudanças breaking afetam todos os clientes simultaneamente |
| Sem CI/CD, Dockerfile, health checks, logging estruturado | Baixa | Infraestrutura de produção não configurada no repositório |

### Frontend

| Item | Severidade | Detalhe |
|------|-----------|---------|
| `environment.prod.ts` com `apiUrl` vazio | Alta | Build de produção não funciona sem configuração manual |
| `shared/` vazio (components, directives, pipes) | Média | Alto risco de duplicação de código entre páginas |
| `catchError(() => of(null\|[]))` silencioso | Média | UI pode exibir estado ambíguo quando API retornar erro |
| Draft de formulário em `sessionStorage` | Média | Frágil; perdido ao fechar aba ou trocar de aba |
| Settings sem persistência via API | Baixa | Preferências de notificação salvas apenas em `localStorage` |
| `forgot-password` não implementado | Baixa | Página é um skeleton sem lógica |
| Sem E2E tests (Cypress / Playwright) | Baixa | Apenas testes unitários mínimos com Vitest |
