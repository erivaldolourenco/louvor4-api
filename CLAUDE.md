    # Louvor4 — Contexto do Projeto

## Stack
- Backend: Spring Boot + PostgreSQL
- Frontend: Flutter
- Infraestrutura: Docker / (futuramente) Kubernetes

---

## Módulo Entitlement

### O que é
Sistema de controle de acesso baseado em planos de assinatura.
Define o que cada usuário tem direito de usar com base no plano que paga.

### Pacote
`com.louvor4.entitlement`

### Regra de isolamento
- O pacote `api` pode importar do `entitlement`
- O pacote `entitlement` NUNCA importa do `api`
- O `api` só enxerga a interface `EntitlementService` — nunca os repositories ou models internos

---

## Planos

| Plano   | Preço      |
|---------|------------|
| FREE    | R$ 0       |
| STARTER | R$ 19,90   |
| PRO     | R$ 49,90   |
| ELITE   | R$ 99,90   |

---

## Tipos de Entitlement

| Tipo  | O que controla                          | Exemplo                    |
|-------|-----------------------------------------|----------------------------|
| FLAG  | Liga/desliga uma funcionalidade         | `cifra_editor = true`      |
| LIMIT | Limite numérico permanente              | `max_musicas = 30`         |
| QUOTA | Limite que reseta por período           | `api_calls_per_month = 500`|

Valor `-1` = ilimitado (convenção universal).

---

## Entitlements do Louvor4

| key                    | tipo  | FREE  | STARTER | PRO   | ELITE |
|------------------------|-------|-------|---------|-------|-------|
| max_musicas            | LIMIT | 30    | 200     | 1000  | -1    |
| max_repertorios        | LIMIT | 1     | 5       | 20    | -1    |
| cifra_editor           | FLAG  | false | false   | true  | true  |
| key_detection          | FLAG  | false | false   | true  | true  |
| export_pdf             | FLAG  | false | true    | true  | true  |
| multi_workspace        | FLAG  | false | false   | false | true  |
| api_calls_per_month    | QUOTA | 0     | 500     | 5000  | -1    |

---

## Tabelas do banco (PostgreSQL)

Ordem de criação (respeitar por causa das foreign keys):

1. `plans`
2. `entitlements`
3. `plan_entitlements` → FK para `plans`
4. `subscriptions` → FK para `plans`
5. `subscription_overrides` → FK para `subscriptions`
6. `usage_counters` → FK para `subscriptions`

### Responsabilidade de cada tabela

- **plans** — os planos disponíveis (FREE, STARTER, PRO, ELITE)
- **entitlements** — catálogo de tudo que pode ser controlado no sistema
- **plan_entitlements** — o que cada plano inclui e com qual valor
- **subscriptions** — assinatura ativa de cada usuário
- **subscription_overrides** — exceções por cliente (suporte ajusta sem redeploy)
- **usage_counters** — contador de uso para QUOTAs (reseta mensalmente)

---

## Entidades JPA criadas

Pacote `com.louvor4.entitlement.model`:
- `Plan`
- `Entitlement`
- `PlanEntitlement`
- `Subscription`
- `SubscriptionOverride`
- `UsageCounter`

Pacote `com.louvor4.entitlement.domain`:
- `EntitlementType` (enum: FLAG, LIMIT, QUOTA)
- `SubscriptionStatus` (enum: ACTIVE, CANCELLED, PAST_DUE, TRIAL)

### Decisões técnicas nas entidades
- `FetchType.LAZY` em todos os `@ManyToOne` — sem joins automáticos
- `SubscriptionOverride` e `UsageCounter` guardam `subscriptionId` como UUID simples — evita joins desnecessários
- `GenerationType.UUID` em todos os `@Id`

---

## Regra de precedência do EntitlementService

```
override ativo do cliente
        ↓ (se não encontrar)
valor do plano (plan_entitlements)
        ↓ (se não encontrar)
default do sistema ("false")
```

---

## Fluxo de validação (exemplo: cadastrar música)

```
POST /musicas (Flutter)
        ↓
EntitlementAspect          ← intercepta @RequiresPlan(limit="max_musicas")
        ↓
EntitlementService.enforceLimit()
        ↓
SubscriptionRepository     ← busca assinatura ativa do usuário
        ↓
PlanEntitlementRepository  ← busca valor de "max_musicas" do plano
        ↓
MusicaRepository.countByUser()  ← conta músicas atuais
        ↓
atual >= limite?
   sim → PlanLimitExceededException → HTTP 403
   não → MusicaService.salvar()   → HTTP 201
```

---

## Estrutura de pacotes

```
com.louvor4
├── api/                        ← pacote existente
│   ├── musica/
│   ├── repertorio/
│   └── ...
│
└── entitlement/                ← módulo novo
    ├── domain/
    │   ├── EntitlementType.java
    │   └── SubscriptionStatus.java
    ├── model/
    │   ├── Plan.java
    │   ├── Entitlement.java
    │   ├── PlanEntitlement.java
    │   ├── Subscription.java
    │   ├── SubscriptionOverride.java
    │   └── UsageCounter.java
    ├── repository/             ← próximo passo
    ├── service/                ← próximo passo
    │   ├── EntitlementService.java      (interface)
    │   └── EntitlementServiceImpl.java
    └── aspect/                 ← próximo passo
        ├── RequiresPlan.java
        └── EntitlementAspect.java
```

---

## Estratégia de cache

- Cachear o resultado de `resolve(subscriptionId, key)` — muda raramente
- Invalidar o cache ao trocar de plano ou aplicar override
- O `COUNT` de recursos (músicas, repertórios) NÃO é cacheado — precisa ser sempre atual
- Cache key: `subscriptionId + ':' + entitlementKey`

---

## Próximos passos (em ordem)

- [ ] Repositories (um por entidade + query customizada em `SubscriptionOverrideRepository`)
- [ ] Interface `EntitlementService` com métodos: `hasFeature()`, `getLimit()`, `enforceLimit()`, `consumeQuota()`
- [ ] `EntitlementServiceImpl` com lógica de precedência e cache
- [ ] Annotation `@RequiresPlan`
- [ ] `EntitlementAspect` (AOP)
- [ ] `GlobalExceptionHandler` para converter exceções em HTTP 403
- [ ] Migrations Flyway com os SQLs das 6 tabelas + dados iniciais
