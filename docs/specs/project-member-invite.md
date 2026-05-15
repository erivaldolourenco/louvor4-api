# Spec: Convite para Participar de Projeto Musical

## Contexto

Atualmente, quando um admin adiciona um usuário a um projeto via `POST /music-project/{projectId}/members`, o usuário entra automaticamente com status `ACTIVE`. Esta spec descreve a mudança para um fluxo de convite, onde o usuário precisa aceitar ou recusar antes de fazer parte do projeto.

---

## Comportamento Atual

```
Admin chama POST /music-project/{projectId}/members
  → MusicProjectMember criado com status ACTIVE
  → Usuário já aparece no projeto
```

## Comportamento Esperado

```
Admin chama POST /music-project/{projectId}/members
  → MusicProjectMember criado com status PENDING_INVITE
  → Notificação enviada ao usuário convidado
  → Usuário responde via POST /music-project/{projectId}/members/invite/respond
  → Se ACCEPTED → status vira ACTIVE
  → Se DECLINED → status vira DECLINED (registro mantido para auditoria)
```

---

## Mudanças de Modelo

### 1. `ProjectMemberStatus` (enum)

Adicionar dois novos valores:

```java
public enum ProjectMemberStatus {
    ACTIVE,
    REMOVED,
    PENDING_INVITE,  // novo: aguardando resposta do convidado
    DECLINED         // novo: convite recusado
}
```

### 2. `NotificationType` (enum)

Adicionar novo tipo:

```java
PROJECT_MEMBER_INVITE,          // novo: convite para entrar no projeto
PROJECT_MEMBER_INVITE_ACCEPTED, // novo: admin recebe confirmação de aceite
PROJECT_MEMBER_INVITE_DECLINED  // novo: admin recebe confirmação de recusa
```

### 3. `MusicProjectMember` (entidade)

Adicionar campos:

| Campo | Tipo | Nullable | Descrição |
|---|---|---|---|
| `invited_at` | `LocalDateTime` | true | Quando o convite foi enviado |
| `responded_at` | `LocalDateTime` | true | Quando o usuário respondeu |

---

## Mudanças de API

### `POST /music-project/{projectId}/members` — sem alteração de contrato

O corpo continua igual (`AddMemberDTO` com `username`). O que muda é o comportamento interno:

- Cria `MusicProjectMember` com `status = PENDING_INVITE` e `invited_at = now()`
- **Não** adiciona o usuário à lista ativa do projeto
- Cria `UserNotification` para o usuário convidado:
  - `type`: `PROJECT_MEMBER_INVITE`
  - `title`: `"Convite para projeto"`
  - `message`: `"Você foi convidado para participar do projeto {projectName}. Aceite ou recuse o convite."`
  - `dataJson`: `{ "projectId": "...", "projectName": "...", "invitedByUserId": "...", "memberId": "..." }`
- Retorna `201 Created` (igual ao atual)

**Validações existentes mantidas:**
- Usuário já é membro ativo → lançar erro (já existe)
- Usuário já tem convite pendente → lançar erro com mensagem específica

---

### `GET /music-project/invites` — novo endpoint

Lista os convites pendentes do usuário autenticado.

**Request:**
```
GET /music-project/invites
Authorization: Bearer {token}
```

**Response `200 OK`:**
```json
[
  {
    "memberId": "uuid-do-member",
    "projectId": "uuid-do-projeto",
    "projectName": "Louvor da Manhã",
    "projectProfileImage": "https://...",
    "invitedByUserId": "uuid-do-admin",
    "invitedAt": "2026-05-12T10:00:00"
  }
]
```

---

### `POST /music-project/{projectId}/members/invite/respond` — novo endpoint

O usuário autenticado aceita ou recusa o convite para o projeto indicado.

**Request:**
```
POST /music-project/{projectId}/members/invite/respond
Authorization: Bearer {token}
Content-Type: application/json

{
  "accepted": true
}
```

**Campos:**

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `accepted` | `boolean` | sim | `true` aceita, `false` recusa |

**Comportamento:**
1. Busca `MusicProjectMember` do usuário autenticado no projeto, onde `status = PENDING_INVITE`
2. Se não encontrado → `404 Not Found`
3. Se `accepted = true`:
   - `status → ACTIVE`
   - `responded_at = now()`
   - Notifica o admin que adicionou (`addedByUserId`) com tipo `PROJECT_MEMBER_INVITE_ACCEPTED`
4. Se `accepted = false`:
   - `status → DECLINED`
   - `responded_at = now()`
   - Notifica o admin que adicionou (`addedByUserId`) com tipo `PROJECT_MEMBER_INVITE_DECLINED`

**Response `200 OK`** (sem corpo)

---

## Impacto em Endpoints Existentes

| Endpoint | Impacto |
|---|---|
| `GET /music-project/members` | Deve retornar apenas membros com `status = ACTIVE` |
| `GET /users/me/projects` (`getFromUser`) | Deve filtrar apenas membros com `status = ACTIVE` |
| `GET /music-project/{id}/member-role` | Deve validar que o membro está `ACTIVE`; retornar `403` se `PENDING_INVITE` |
| Qualquer endpoint que valida acesso ao projeto | Deve rejeitar usuários com status diferente de `ACTIVE` |

---

## Fluxo Completo

```
[Admin]                          [Sistema]                        [Usuário]
   |                                |                                |
   |-- POST /members (username) --> |                                |
   |                                |-- cria PENDING_INVITE -------> DB
   |                                |-- cria notificação ----------> DB
   |<-- 201 Created ----------------|                                |
   |                                |                                |
   |                                |<-- GET /invites ---------------|
   |                                |-- retorna lista de convites -->|
   |                                |                                |
   |                                |<-- POST /invite/respond -------|
   |                                |    { "accepted": true }        |
   |                                |-- atualiza status ACTIVE ----> DB
   |                                |-- notifica admin ------------> DB
   |<-- notificação de aceite ------|                                |
```

---

## DTOs necessários

### `ProjectInviteResponseDTO` (request)
```java
public class ProjectInviteResponseDTO {
    @NotNull
    private Boolean accepted;
}
```

### `ProjectInviteDTO` (response do GET /invites)
```java
public class ProjectInviteDTO {
    private UUID memberId;
    private UUID projectId;
    private String projectName;
    private String projectProfileImage;
    private UUID invitedByUserId;
    private LocalDateTime invitedAt;
}
```

---

## Migração de Banco

```sql
-- Adicionar novos status ao enum (se banco usa enum nativo, ex: PostgreSQL)
ALTER TYPE project_member_status ADD VALUE IF NOT EXISTS 'PENDING_INVITE';
ALTER TYPE project_member_status ADD VALUE IF NOT EXISTS 'DECLINED';

-- Adicionar colunas na tabela music_project_members
ALTER TABLE music_project_members
    ADD COLUMN IF NOT EXISTS invited_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS responded_at TIMESTAMP;

-- Dados existentes: todos os membros ACTIVE já foram aceitos implicitamente
-- Nenhuma migração de dados necessária
```

---

## Casos de Borda

| Cenário | Comportamento esperado |
|---|---|
| Admin tenta convidar usuário já `ACTIVE` | `400 Bad Request` — "Usuário já é membro do projeto" |
| Admin tenta convidar usuário já `PENDING_INVITE` | `400 Bad Request` — "Já existe um convite pendente para este usuário" |
| Admin tenta convidar usuário `DECLINED` | Permitir reenvio: sobrescreve status para `PENDING_INVITE`, atualiza `invited_at` |
| Usuário tenta responder convite inexistente | `404 Not Found` |
| Usuário tenta acessar projeto com status `PENDING_INVITE` | `403 Forbidden` |
