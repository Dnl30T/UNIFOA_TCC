# PsyTrack — Overview Funcional

> Data de referência: 19 maio 2026  
> Representa o estado atual do repositório; itens marcados com ⚠️ indicam lacunas, premissas ou comportamento parcialmente implementado.

---

## 1. Propósito do Produto

PsyTrack é uma plataforma de monitoramento contínuo de saúde psicológica de equipes corporativas. O produto permite que psicólogos (Counselors) criem e distribuam avaliações para funcionários de equipes, coletem respostas, façam avaliações clínicas individuais e apresentem relatórios consolidados para gestores. O sistema é projetado para preservar a confidencialidade clínica: gestores nunca têm acesso às respostas individuais dos funcionários.

---

## 2. Papéis e Responsabilidades

### EMPLOYEE (Funcionário)
- Integra uma equipe via `teamCode` no momento do cadastro
- Responde formulários de avaliação distribuídos pelo Counselor da sua equipe
- Visualiza apenas seu próprio resultado individual — e somente após o Counselor publicá-lo

### MANAGER (Gestor)
- Cria a equipe e recebe um `teamCode` único para compartilhar com funcionários
- Cada Manager gerencia exatamente uma equipe
- Visualiza o relatório consolidado da equipe (score médio + distribuição de risco)
- Nunca acessa respostas individuais de funcionários nem scores individuais

### COUNSELOR (Psicólogo / Conselheiro)
- Protagonista do processo clínico
- Cria formulários, adiciona perguntas, distribui para equipes, monitora respostas
- Realiza avaliações clínicas individuais (5 módulos) após encerrar a coleta
- Publica resultados individuais e consolidados (visibilidade é controlada pelo ato de publicação)
- Pode acompanhar múltiplas equipes simultaneamente

### ADMIN
- Papel analítico e de suporte técnico
- Acesso global a métricas de resultados individuais e de equipes
- Não interfere no fluxo clínico
- No frontend, recebe as mesmas rotas e permissões do Counselor

### PENDING
- Estado transitório de contas staff (Manager / Counselor) após o primeiro registro
- Obrigatoriamente chama `claim-role` antes de acessar qualquer recurso protegido

---

## 3. Regras de Negócio Fundamentais

- Usernames e emails são únicos em toda a plataforma
- Títulos de formulários são únicos
- Nomes de equipes são únicos
- Um Manager possui exatamente uma equipe (constraint no banco)
- Um Counselor pode ser associado a no máximo uma equipe como counselor responsável (constraint no banco), mas pode monitorar outras equipes via pool no frontend
- Um Employee é vinculado à equipe no momento do cadastro (via `teamCode`) e não troca de equipe
- Scores de resultados individuais são de 0 a 100
- Níveis de risco: `LOW`, `MEDIUM`, `HIGH`
- O sistema **não calcula scores automaticamente** — o Counselor os insere manualmente após leitura das respostas
- Tokens JWT têm validade de 24 horas; após esse prazo o usuário deve refazer o login

---

## 4. Ciclo de Vida do Formulário

```
CREATED ──→ ACTIVE ──→ ENDED
```

| Estado | Quem controla | O que acontece |
|--------|--------------|---------------|
| `CREATED` | Counselor | Formulário criado; perguntas sendo adicionadas; employees não veem |
| `ACTIVE` | Counselor | Formulário distribuído para N equipes; employees podem responder |
| `ENDED` | Counselor | Coleta encerrada; Counselor avalia respostas e publica resultados |

**Restrições:**
- Transição é linear e irreversível
- Um formulário pode ser distribuído para múltiplas equipes simultaneamente
- Perguntas ficam embutidas no formulário (ScyllaDB); atualizar metadados preserva as perguntas existentes

---

## 5. Fluxos por Papel

### 5.1 Onboarding — Manager

| Passo | Tela (Frontend) | Endpoint (Backend) |
|-------|-----------------|--------------------|
| 1. Cadastro como staff | `/auth/register` | `POST /auth/register/staff` |
| 2. Claim de role | (automático após register) | `POST /auth/claim-role { role: "MANAGER" }` |
| 3. Login | `/auth/login` | `POST /auth/login` |
| 4. Criar equipe | `/dashboard/home` ou menu | `POST /teams` → gera `teamCode` |
| 5. Compartilhar `teamCode` | Cópia manual | — |

### 5.2 Onboarding — Employee

| Passo | Tela (Frontend) | Endpoint (Backend) |
|-------|-----------------|--------------------|
| 1. Cadastro com teamCode | `/auth/register` | `POST /auth/register/employee { teamCode }` |
| 2. Login | `/auth/login` | `POST /auth/login` |
| 3. Visualizar formulários ativos | `/dashboard/my-assessments` | `GET /forms?status=ACTIVE` |

### 5.3 Onboarding — Counselor

| Passo | Tela (Frontend) | Endpoint (Backend) |
|-------|-----------------|--------------------|
| 1. Cadastro como staff | `/auth/register` | `POST /auth/register/staff` |
| 2. Claim de role | (automático após register) | `POST /auth/claim-role { role: "COUNSELOR" }` |
| 3. Login | `/auth/login` | `POST /auth/login` |

---

### 5.4 Fluxo de Criação e Distribuição de Formulário (Counselor)

| Passo | Tela | Endpoint | Detalhe |
|-------|------|----------|---------|
| 1. Construir formulário | `/dashboard/forms/builder` | — | Draft em `sessionStorage`; reordenamento por drag-and-drop; 12 tipos de campo |
| 2. Revisar formulário | `/dashboard/forms/review` | — | Preview antes de salvar |
| 3. Salvar formulário | (salva em `/review`) | `POST /forms` + `POST /questions` (sequencial, 1 por pergunta) | Cria form no status `CREATED` |
| 4. Publicar para equipe | `/dashboard/forms/publish/:id` | `PUT /forms/{id}` com status `ACTIVE` e `teamIds` | Employees da equipe passam a ver o formulário |
| 5. Monitorar coleta | `/dashboard/counselor-assessments` | `GET /forms`, `GET /form-responses?formId=...` | Taxa de conclusão por formulário |
| 6. Encerrar coleta | Botão em `/counselor-assessments` | `PUT /forms/{id}` com status `ENDED` | Formulário retorna para avaliação |

**Tipos de campo disponíveis no construtor:**
`TEXT`, `NUMERIC`, `BOOLEAN`, `DATE`, `SCALE`, `SLIDER`, `LIKERT`, `SINGLE_CHOICE`, `MULTIPLE_CHOICE`, `RANKING`, `MATRIX`, `MAP`

---

### 5.5 Fluxo de Resposta ao Formulário (Employee)

| Passo | Tela | Endpoint | Detalhe |
|-------|------|----------|---------|
| 1. Ver formulários pendentes | `/dashboard/my-assessments` | `GET /forms?status=ACTIVE` | Mostra status: pendente / concluído |
| 2. Abrir formulário | `/dashboard/forms/view/:id` | `GET /forms/{id}` | Renderização dinâmica por tipo de campo |
| 3. Responder e enviar | (submit no form-viewer) | `POST /form-submissions` | Todas as respostas enviadas em uma única chamada |
| 4. Ver resultado (quando disponível) | `/dashboard/my-reports` | `GET /employee-results` | Visível somente após Counselor publicar |

---

### 5.6 Fluxo de Avaliação Clínica (Counselor)

| Passo | Tela | Endpoint | Detalhe |
|-------|------|----------|---------|
| 1. Selecionar formulário encerrado | `/dashboard/counselor-reports` | `GET /forms?status=ENDED` | — |
| 2. Ver respostas por employee | `/dashboard/employee-responses` | `GET /form-responses?formId=...&employeeId=...` | Respostas brutas decodificadas pelo frontend |
| 3. Preencher avaliação clínica | Formulário embutido em `/employee-responses` | `POST /evaluations` (upsert) | 5 módulos: estressores, sintomatologia, dimensões, opinião clínica, visibilidade |
| 4. Publicar resultado individual | (ao salvar avaliação) | `POST /employee-results { employeeId, formId, score, riskLevel }` | Employee ganha visibilidade imediatamente |
| 5. Publicar resultado da equipe | `/dashboard/counselor-reports` ou `/my-teams` | `POST /team-results { teamId, formId, averageScore, distribution }` | Manager ganha visibilidade imediatamente |

**Módulos da avaliação clínica (`therapist_evaluations`):**
1. **Estressores:** contexto ocupacional, dinâmica da equipe
2. **Sintomatologia:** sintomas psicossomáticos, mudanças cognitivo-emocionais
3. **Dimensões:** score de exaustão, score de despersonalização + justificações
4. **Opinião clínica:** nota interna (privada — nunca visível para HR/Manager)
5. **Visibilidade:** resumo para RH (sanitizado, sem detalhes clínicos)

---

### 5.7 Fluxo de Visualização de Resultados (Manager)

| Passo | Tela | Endpoint | Detalhe |
|-------|------|----------|---------|
| 1. Ver equipe | `/dashboard/team-overview` | `GET /teams/my-team`, `GET /employees` | Lista membros, status de resposta |
| 2. Ver formulários ativos | `/dashboard/manager-assessments` | `GET /forms` | Quantidade de respostas vs total |
| 3. Ver relatório consolidado | `/dashboard/report-history` | `GET /team-results` | Score médio + distribuição `LOW/MEDIUM/HIGH` |

> O Manager **nunca** vê respostas individuais ou scores por funcionário.

---

### 5.8 Fluxo de Gestão de Equipes (Counselor)

| Passo | Tela | Endpoint | Detalhe |
|-------|------|----------|---------|
| 1. Adicionar equipe ao pool | `/dashboard/teams/add` | `GET /teams/by-code/{code}` | Team salva em localStorage (`CounselorTeamPoolService`) |
| 2. Ver equipes monitoradas | `/dashboard/my-teams` | `GET /teams` (filtrado pelo pool local) | Taxa de conclusão por equipe |
| 3. Ver membros | Diálogo a partir de `/my-teams` | `GET /employees` | Lista de membros da equipe |
| 4. Gerar resultado manualmente | Diálogo em `/my-teams` | `POST /team-results` | Trigger manual de consolidação |

> ⚠️ **Premissa:** a tela de "adicionar equipe ao pool" (`/teams/add`) usa `teamCode` para buscar equipes; não há UI explícita para remover equipes do pool.

---

## 6. Regras de Visibilidade Detalhadas

| Entidade | EMPLOYEE | MANAGER | COUNSELOR | ADMIN |
|----------|----------|---------|-----------|-------|
| Formulários ativos da sua equipe | ✅ Leitura | — | ✅ Full | ✅ Full |
| Respostas brutas do formulário | ❌ | ❌ | ✅ (por employee) | ✅ |
| Resultado individual próprio | ✅ (após publicação) | ❌ | ✅ | ✅ |
| Resultado individual de outros | ❌ | ❌ | ✅ | ✅ |
| Relatório consolidado da equipe | ❌ | ✅ (após publicação) | ✅ | ✅ |
| Avaliação clínica (nota interna) | ❌ | ❌ | ✅ | ✅ |
| Resumo para RH (hr_summary) | ❌ | ✅ | ✅ | ✅ |
| Gestão de usuários global | ❌ | ❌ | ❌ | ✅ |

---

## 7. Compliance LGPD

O sistema implementa mecanismos de conformidade com a LGPD nos campos de `app_users`:

| Mecanismo | Campo / Comportamento |
|-----------|----------------------|
| Consentimento | `consent_given` (boolean) + `consent_date` (timestamp) |
| Soft-delete | `deleted_at` (timestamp); conta desativada mas dado preservado |
| Anonimização | Flag `anonymized`; `username` e `email` substituídos por UUID |
| Auditoria | `created_at`, `updated_at` em toda conta |

> ⚠️ **Lacuna:** não há mecanismo automático de purga de dados anonimizados após período definido. O script `seed.py` define `consent_given = true` hardcoded — em produção esse fluxo precisa de UI dedicada.

---

## 8. Telas e Jornadas por Role — Resumo

### Employee
```
/auth/login
/dashboard/home              ← visão geral
/dashboard/my-assessments    ← formulários pendentes e concluídos
/dashboard/forms/view/:id    ← responder formulário
/dashboard/my-reports        ← ver resultado próprio (após publicação)
/dashboard/settings
```

### Manager
```
/auth/register → /auth/login
/dashboard/home              ← visão geral da equipe
/dashboard/team-overview     ← membros, status de resposta, teamCode
/dashboard/manager-assessments ← formulários e taxa de resposta
/dashboard/report-history    ← relatório consolidado da equipe
/dashboard/settings
```

### Counselor
```
/auth/register → /auth/login
/dashboard/home              ← visão geral
/dashboard/my-teams          ← equipes monitoradas, membros, taxa de conclusão
/dashboard/teams/add         ← adicionar equipe ao pool via teamCode
/dashboard/counselor-assessments ← formulários (criar, publicar, encerrar)
/dashboard/forms/builder     ← construtor de formulário (drag-and-drop, 12 tipos)
/dashboard/forms/review      ← preview e salvar formulário
/dashboard/forms/publish/:id ← distribuir formulário para equipes
/dashboard/forms/update/:id  ← editar formulário existente
/dashboard/forms/view/:id    ← visualizar formulário
/dashboard/counselor-reports ← resultados por equipe/formulário
/dashboard/employee-responses ← respostas individuais + avaliação clínica
/dashboard/settings
```

### Admin
```
(mesmo acesso que Counselor no frontend, mapeado via roleGuard e RoleService)
```

---

## 9. Lacunas Funcionais Conhecidas

| Lacuna | Impacto | Observação |
|--------|---------|-----------|
| `forgot-password` não implementado | Alto | Usuários sem recuperação de senha disponível |
| `settings` sem persistência via API | Médio | Preferências de notificação salvas apenas em localStorage |
| Score calculado manualmente pelo Counselor | Médio | Risco de inconsistência nos scores; não há fórmula automatizada |
| Pool de equipes do Counselor baseado em localStorage | Médio | Pool é perdido ao trocar de dispositivo ou limpar cache do browser |
| Sem UI para remover equipe do pool | Baixo | Counselor não consegue desvincular uma equipe monitorada pela tela atual |
| Real-time não implementado | Baixo | Dashboard pode exibir dados desatualizados sem reload manual |
| Tela de aprovação de avaliações clínicas ausente | Baixo | Sem workflow de revisão após o Counselor submeter avaliação |
| Landing page com dados fictícios | Cosmético | Stats "2.400+ funcionários", "98% de detecção" são hardcoded no HTML |
