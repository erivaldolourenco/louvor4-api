## Contexto

O projeto Louvor4 (Spring Boot/JHipster) já tem a entidade `Song` com um campo `lyrics` (texto simples) e agora precisa de um novo campo `chordSheetJson` (JSON com estrutura de cifra: seções, linhas, acordes posicionados por caractere). Os dois campos são independentes — não há sincronização automática entre `lyrics` e `chordSheetJson` por enquanto.

O padrão de nomenclatura do projeto é em inglês. O termo usado pro conceito de "cifra" é `chordSheet` (não usar "cifra" em nenhum nome de classe, campo, rota ou variável).

## O que já existe (referência de padrão a seguir)

- Entidade `Song` em `br.com.louvor4.api.models.Song`
- Controller `SongController` em `br.com.louvor4.api.controllers`, com endpoints estilo `/{songId}/lyrics` (GET e PUT) já implementados pra `lyrics` — use esse par de endpoints como modelo de convenção pros novos.
- DTOs em `br.com.louvor4.api.shared.dto.Song` (ex: `SongDTO`, `SongLyricsDTO`)
- Service `SongService` já injetado no controller

## Tarefa

Adicionar ao domínio de `Song` o suporte a `chordSheet`, seguindo o mesmo padrão arquitetural já usado por `lyrics`.

### 1. Entidade `Song`

Adicionar campo:
```java
@Column(name = "chord_sheet_json", columnDefinition = "jsonb")
private String chordSheetJson;
```
Com getter/setter correspondentes. Não adicionar `capo` nem entidade separada — a cifra fica embutida na própria `Song`, sem relação 1:N.

### 2. DTO

Criar `ChordSheetDTO` em `br.com.louvor4.api.shared.dto.Song`, seguindo o padrão de `SongLyricsDTO` (provavelmente um `record` com o campo do JSON de cifra — confirme a assinatura de `SongLyricsDTO` existente e replique o estilo).

Sugestão de campo: `chordSheetJson` (String) ou, se preferir validação estruturada em vez de String crua, avaliar usar um Map/JsonNode — mas mantenha consistência com como `lyrics` foi implementado (aparentemente como String simples).

### 3. Endpoints no `SongController`

Seguir exatamente o estilo dos endpoints de `lyrics` já existentes (rota aninhada em `/{songId}/...`, sem prefixo REST verboso):

```java
@GetMapping("/{songId}/chord-sheet")
public ResponseEntity<ChordSheetDTO> getChordSheet(@PathVariable UUID songId) {
    ChordSheetDTO dto = songService.getChordSheet(songId);
    return ResponseEntity.ok(dto);
}

@PutMapping("/{songId}/chord-sheet")
public ResponseEntity<ChordSheetDTO> updateChordSheet(
        @PathVariable UUID songId,
        @RequestBody @Valid ChordSheetDTO chordSheetDto) {
    ChordSheetDTO dto = songService.updateChordSheet(songId, chordSheetDto.chordSheetJson());
    return ResponseEntity.ok(dto);
}

@DeleteMapping("/{songId}/chord-sheet")
public ResponseEntity<Void> deleteChordSheet(@PathVariable UUID songId) {
    songService.deleteChordSheet(songId);
    return ResponseEntity.noContent().build();
}

@PostMapping("/{songId}/chord-sheet/import")
public ResponseEntity<ChordSheetDTO> importChordSheet(
        @PathVariable UUID songId,
        @RequestBody @Valid ChordSheetDTO chordSheetDto) {
    ChordSheetDTO dto = songService.importChordSheet(songId, chordSheetDto.chordSheetJson());
    return ResponseEntity.ok(dto);
}
```

(`import` recebe o JSON já parseado/validado pelo client — não é upload de arquivo multipart, é o conteúdo do `.l4` já em JSON no corpo da requisição.)

### 4. `SongService`

Adicionar métodos correspondentes:
- `getChordSheet(UUID songId): ChordSheetDTO`
- `updateChordSheet(UUID songId, String chordSheetJson): ChordSheetDTO`
- `deleteChordSheet(UUID songId): void`
- `importChordSheet(UUID songId, String chordSheetJson): ChordSheetDTO`

`updateChordSheet` e `importChordSheet` podem compartilhar a mesma lógica interna de persistência (upsert do campo `chordSheetJson`) — a diferença entre os dois endpoints é semântica de origem (edição manual vs. importação de arquivo), não de implementação. Se fizer sentido, `importChordSheet` pode simplesmente delegar pra `updateChordSheet` após uma etapa de validação estrutural do JSON (checar `schemaVersion`, range de `position` de cada `chord` dentro do tamanho do `text` da linha correspondente, formato do campo `chord` via regex).

### 5. Validação do JSON de cifra

Antes de persistir em `updateChordSheet`/`importChordSheet`, validar estrutura do `chordSheetJson`:
- JSON bem formado
- `schemaVersion` reconhecida
- Para cada linha, todo `chord.position` deve estar entre `0` e `text.length()` (inclusive)
- Campo `chord` deve bater com um regex de acorde válido (raiz `A-G` opcional `#`/`b`, sufixo livre, baixo opcional após `/`)

Se inválido, lançar exceção de validação (usar o padrão de exception handling já existente no projeto, se houver um `@ControllerAdvice` ou similar).

### 6. Não incluir nesta tarefa

- Endpoint de export (`GET /{songId}/chord-sheet/export`) — pode ser adicionado depois, não é bloqueante agora
- Checagem de permissão `canAddChordSheet` via evento (`EventParticipant`) — depende de entidade que ainda não foi revisada, tratar em tarefa separada
- Qualquer lógica de transposição — isso é 100% client-side (Flutter), não deve existir no backend

## Resultado esperado

`SongController` com os 4 novos endpoints (`GET`, `PUT`, `DELETE`, `POST /import`), `SongService` com os métodos correspondentes, `ChordSheetDTO` criado seguindo o padrão de `SongLyricsDTO`, e a entidade `Song` com o campo `chordSheetJson` persistido como `jsonb`.