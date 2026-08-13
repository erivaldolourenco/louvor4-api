# Correções e padronização de segurança — autorização

Levantamento feito a partir da análise do fluxo de permissões de evento (`EventPermission`,
roles de projeto owner/admin/member) e de como isso é aplicado hoje nos controllers/services
da API. Cada item da lista de problemas tem uma padronização correspondente na segunda lista,
pensada para ser implementada uma de cada vez sem depender das outras (exceto onde indicado).

## 1. Problemas encontrados

### Críticos (exploráveis hoje)

1. **`SongController` sem autorização granular.**
   `src/main/java/br/com/louvor4/api/controllers/SongController.java` não tem nenhum
   `@PreAuthorize` em nenhum dos 12 endpoints.
   - `GET /songs/{songId}` — nenhuma checagem: qualquer usuário autenticado lê qualquer
     música do sistema pelo UUID.
   - `PUT /songs/update` — nenhuma checagem de dono: qualquer usuário autenticado pode
     editar título/artista/tom/bpm de qualquer música.
   - `GET /songs/{songId}/lyrics`, `GET /songs/{songId}/chord-sheet` — nenhuma checagem.
   - Broken Object Level Authorization (OWASP API1) — a autenticação existe, a
     autorização de posse/escopo não.

2. **5 das 6 `EventPermission` não são verificadas em nenhum lugar do servidor.**
   Só `EDIT_SETLIST`, `REMOVE_SONG`, `MANAGE_PARTICIPANTS`, `EDIT_EVENT`,
   `EDIT_CHORD_SHEET` foram buscadas em todo `src/main/java` e não aparecem fora da
   declaração do enum (`enums/EventPermission.java`) e da persistência/serialização.
   Só `ADD_SONG` é checada, em `validations/EventValidation.java`:
   ```java
   public void canAddSong(EventParticipant participant){
       if (!participant.getPermissions().contains(EventPermission.ADD_SONG)) {
           throw new ValidationException("Você não tem permissão para remover músicas neste evento.");
       }
   }
   ```
   (mensagem de erro também está errada: valida "adicionar" mas fala "remover").
   O app concede essas permissões pela UI e o backend as persiste, mas nenhuma delas
   bloqueia a ação correspondente via chamada direta à API.

3. **Negação de acesso não retorna 403.**
   Não existe `@ExceptionHandler(AccessDeniedException.class)` em
   `exceptions/handler/ResponseExceptionHandler.java`. `AccessDeniedException` lançada
   pelo `@PreAuthorize` cai no handler genérico de `RuntimeException` → **400 Bad
   Request**. Negações manuais (`ValidationException` em `SongServiceImpl`/
   `EventValidation`) caem no handler de `ValidationException` → **409 Conflict**.
   Nenhum dos dois caminhos usa 403, quebrando a semântica HTTP esperada pelo client.

### Importantes (inconsistências)

4. **Padrão `@PreAuthorize` + `ProjectSecurity` não chegou a todos os controllers.**
   Presente em `EventController`, `EventProgramController`, `MusicProjectController`.
   Ausente em `SongController`, `MedleyController`, `UserController`,
   `NotificationController`, `SkillsController`, `ExternalMusicController`. Há inclusive
   um resquício morto de um padrão anterior (`config/security/MinistrySecurity.java`,
   comentado), indicando que a migração para `ProjectSecurity` não foi concluída em
   todo o código.

5. **Escalada de admin/owner é inconsistente entre métodos do mesmo domínio.**
   Em `EventServiceImpl.removeSetlistItemFromEvent`, há bypass manual explícito para
   owner/admin do projeto. Em `EventServiceImpl.addSetListItemToEvent`, esse bypass não
   existe — mesmo o owner do projeto precisa ter um `EventParticipant` com `ADD_SONG`
   explícito, senão recebe "Usuário não está escalado como participante deste evento".

6. **`UserDetailsImpl.getRoles()` hardcoded.**
   Retorna `["ROLE_USER", "ROLE_ADMIN"]` para qualquer usuário; `getAuthorities()`
   sempre retorna `ROLE_USER`. Não é explorável hoje porque nada lê esse claim para
   autorizar, mas é uma armadilha para qualquer `@PreAuthorize("hasRole('ADMIN')")`
   futuro — daria admin a todo mundo.

7. **Sem endpoint de "minhas permissões resolvidas" para um evento.**
   Não existe algo como `GET /events/{id}/me/permissions`. O client precisa buscar
   todos os participantes (`GET /events/{eventId}/participants`) e filtrar o próprio
   para montar suas permissões, reimplementando a regra de escalada de admin no client
   — risco de a regra divergir entre client e servidor.

### Maturidade / profissionalização

8. **Sem auditoria de concessão/revogação de permissão.**
   `EventParticipant` não tem `grantedBy`/`grantedAt`/histórico. `POST
   /events/{eventId}/participants` sobrescreve o `Set<EventPermission>` inteiro sem
   deixar rastro de quem alterou o quê e quando.

9. **Sem teste de autorização fim-a-fim.**
   `spring-security-test` está no `pom.xml` mas não é usado. O único teste relacionado
   é `ProjectSecurityTest.java`, unitário, mockando repositórios — não confirma que o
   `@PreAuthorize` bloqueia a chamada HTTP nem qual status code é retornado.

10. **Schema de permissões sem controle de migração versionado.**
    `hibernate.ddl-auto: none`, sem Flyway/Liquibase — scripts SQL soltos em
    `docs/migrations/`. Dificulta rastrear/reverter mudanças no schema de permissões
    (ex.: o `CHECK` constraint de `event_participant_permissions`) com segurança.

---

## 2. Padronizações necessárias

Cada item abaixo corresponde ao problema de mesmo número acima.

1. **Adicionar autorização a todos os endpoints de `SongController`.**
   Definir e aplicar uma regra única: usuário precisa pertencer ao projeto dono da
   música para ler (`get`, `getLyrics`, `getChordSheet`); para escrever (`update`,
   `delete`, `updateLyrics`, `updateChordSheet`, `updateChordSheetEditPermission`),
   precisa ser dono da música OU ter a permissão de evento equivalente
   (`EDIT_CHORD_SHEET`). Preferir estender `ProjectSecurity` com um método
   `isMemberBySongId(UUID songId)` / `canEditSong(UUID songId)` reutilizando o padrão
   `@PreAuthorize("@projectSecurity....")` já estabelecido, em vez de checagem manual
   dentro do service.

2. **Criar um validador único de `EventPermission` e aplicá-lo a cada ação correspondente.**
   Generalizar `EventValidation.canAddSong` para algo como
   `EventValidation.require(EventParticipant participant, EventPermission permission)`,
   e chamar essa validação nos endpoints/services de: editar setlist
   (`EDIT_SETLIST`), remover música (`REMOVE_SONG`), gerenciar participantes
   (`MANAGE_PARTICIPANTS`), editar evento (`EDIT_EVENT`), editar cifra
   (`EDIT_CHORD_SHEET`). Corrigir a mensagem de erro trocada de `canAddSong` nesse
   mesmo esforço.

3. **Padronizar resposta de autorização negada como 403.**
   Adicionar `@ExceptionHandler(AccessDeniedException.class)` em
   `ResponseExceptionHandler` retornando `HttpStatus.FORBIDDEN`. Avaliar criar uma
   exceção própria (`ForbiddenException`) para as negações manuais hoje feitas via
   `ValidationException` em `SongServiceImpl`/`EventValidation`, e mapear essa nova
   exceção também para 403 — deixando `ValidationException` só para erros de
   validação de entrada (400/422) e `ForbiddenException` só para autorização (403).

4. **Propagar `@PreAuthorize` + `ProjectSecurity` para todos os controllers que faltam.**
   Revisar `MedleyController`, `UserController`, `NotificationController`,
   `SkillsController`, `ExternalMusicController` e aplicar o mesmo padrão de
   `EventController`/`MusicProjectController`. Remover o código morto de
   `MinistrySecurity` se confirmado que não é mais usado.

5. **Unificar a regra de escalada de admin/owner em um único ponto.**
   Extrair a checagem `role == OWNER || role == ADMIN` (hoje duplicada manualmente em
   `removeSetlistItemFromEvent`) para um método reutilizável em `ProjectSecurity` ou
   `EventValidation`, e usá-lo em todo `EventServiceImpl` de forma consistente —
   decidir explicitamente se owner/admin do projeto sempre bypassa a checagem granular
   de `EventParticipant.permissions`, e aplicar essa decisão em todos os métodos
   (`addSetListItemToEvent` incluso), não caso a caso.

6. **Remover ou corrigir o hardcode de roles em `UserDetailsImpl`.**
   Substituir `getRoles()`/`getAuthorities()` hardcoded por um retorno real (mesmo que
   hoje seja só `ROLE_USER` para todos) ou documentar explicitamente no código que
   nenhuma autorização deve depender desse claim, para não virar uma armadilha em
   `@PreAuthorize("hasRole(...))")` futuro.

7. **Criar endpoint de permissões resolvidas por evento.**
   `GET /events/{eventId}/me/permissions` retornando algo como
   `{ "isProjectAdmin": true, "permissions": ["ADD_SONG", "EDIT_CHORD_SHEET", ...] }`,
   já com a regra de escalada de admin aplicada no servidor (reaproveitando o método
   unificado do item 5), para o client parar de recalcular essa regra localmente.

8. **Registrar auditoria de concessão/revogação de permissão.**
   Adicionar `grantedBy`/`grantedAt` em `EventParticipant` (ou uma tabela de histórico
   separada) e gravar essa informação em `POST /events/{eventId}/participants` sempre
   que o conjunto de `permissions` de um participante for alterado.

9. **Criar suíte de testes de autorização fim-a-fim.**
   Usar `spring-security-test` (`@WithMockUser` /
   `SecurityMockMvcRequestPostProcessors`) para cobrir, nos controllers principais, a
   matriz: role de projeto (owner/admin/member) × cada `EventPermission` × dono/não-dono
   → status HTTP esperado (200 vs 403). Incluir teste que trave o status code 403
   definido no item 3.

10. **Adotar controle de migração versionado para o schema.**
    Introduzir Flyway ou Liquibase e migrar os scripts soltos de `docs/migrations/`
    para o novo mecanismo, começando pelas tabelas relacionadas a permissão
    (`event_participant_permissions`) para já validar o processo num escopo pequeno.

---

## Ordem sugerida de implementação

1. Item 1 (IDOR em `SongController`) — único ponto já explorável hoje.
2. Item 3 (status 403) — mudança pequena e isolada, facilita validar os itens seguintes.
3. Item 2 (permissões de evento mortas).
4. Item 5 (escalada de admin consistente).
5. Item 4 (propagar `@PreAuthorize` aos controllers restantes).
6. Itens 7, 9, 8, 10 — nessa ordem de valor/esforço.
