# Design: Upload de Áudio para Song

**Data:** 2026-06-06  
**Status:** Aprovado

---

## Contexto

A entidade `Song` permite cadastrar uma música com título, tom, BPM e URL do YouTube. O requisito é adicionar suporte a upload de arquivos de áudio de referência. No futuro, haverá também um áudio de VS (playback), mas apenas o áudio de referência será implementado agora.

---

## Decisões de Design

- **Endpoint separado** do create/update da música, para manter a simplicidade dos fluxos existentes.
- **Entidade `SongAudio`** com enum `SongAudioType` (REFERENCE, VS), permitindo múltiplos tipos de áudio por música sem adicionar colunas novas a cada tipo.
- **Upsert por `(song_id, type)`** — substitui o arquivo existente ao fazer novo upload do mesmo tipo. O arquivo antigo no S3 é abandonado (não deletado), mantendo o fluxo de upload simples e livre de falhas parciais.
- **Formatos aceitos:** qualquer `audio/*` (MP3, WAV, OGG, M4A, etc.), pois bibliotecas de pitch detection no frontend usam a Web Audio API e trabalham com PCM decodificado, independente do formato.
- **Limite de tamanho:** 20 MB configurado no `application.yml`.

---

## Schema

### Nova tabela: `song_audio_files`

```sql
CREATE TABLE song_audio_files (
    id          UUID PRIMARY KEY,
    song_id     UUID NOT NULL REFERENCES songs(id),
    type        VARCHAR(20) NOT NULL,
    audio_url   VARCHAR(500) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_song_audio_type UNIQUE (song_id, type)
);
```

> O projeto usa `ddl-auto: update` em dev e `ddl-auto: none` em prod. A tabela será criada automaticamente em dev via JPA. Em produção, executar o DDL acima manualmente.

---

## Enum

### `SongAudioType`

```java
public enum SongAudioType {
    REFERENCE,
    VS
}
```

Apenas `REFERENCE` é exposto via endpoint nesta implementação. `VS` existe no enum para que o schema já suporte o tipo sem futura migration.

---

## FileCategory

Adicionar ao enum existente `FileCategory`:

```java
SONG_AUDIO("audio/songs")
```

---

## API

### Endpoint

```
POST /songs/{songId}/audio?type=REFERENCE
Content-Type: multipart/form-data
Authorization: Bearer <token>

Body: file (MultipartFile)
```

### Resposta `200 OK`

```json
{
  "songId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "REFERENCE",
  "audioUrl": "https://s3.amazonaws.com/bucket/audio/songs/2026/06/06/uuid-hash.mp3"
}
```

### Erros

| Situação | Status |
|---|---|
| Música não encontrada | `404 Not Found` |
| Arquivo ausente | `400 Bad Request` |
| Content-Type não é `audio/*` | `400 Bad Request` |
| Arquivo maior que 20 MB | `413 Payload Too Large` (Spring Boot automático) |
| `type` inválido (não existe no enum) | `400 Bad Request` |

---

## Componentes

### Criados

| Arquivo | Responsabilidade |
|---|---|
| `enums/SongAudioType.java` | Enum com os tipos de áudio (REFERENCE, VS) |
| `models/SongAudio.java` | Entidade JPA mapeada para `song_audio_files` |
| `repositories/SongAudioRepository.java` | `findBySongIdAndType`, `findBySongId` |
| `services/SongAudioService.java` | Interface: `uploadAudio(songId, type, file)` |
| `services/impl/SongAudioServiceImpl.java` | Lógica de validação, upload S3 e upsert |
| `shared/dto/Song/SongAudioDTO.java` | Record de resposta: `(songId, type, audioUrl)` |

### Modificados

| Arquivo | Mudança |
|---|---|
| `enums/FileCategory.java` | Adiciona `SONG_AUDIO("audio/songs")` |
| `controllers/SongController.java` | Adiciona `POST /{songId}/audio` |
| `application.yml` | Atualiza `max-file-size` e `max-request-size` para `20MB` |

---

## Fluxo de Upload

```
POST /songs/{songId}/audio?type=REFERENCE
        │
        ▼
SongController
        │ valida type (enum)
        │ delega para SongAudioService
        ▼
SongAudioServiceImpl
        │ 1. Verifica que a música existe (SongRepository)
        │ 2. Valida Content-Type começa com "audio/"
        │ 3. Upload via StorageService.uploadFileWithPrefix(file, SONG_AUDIO, songId)
        │ 4. Upsert: busca (songId, type) no SongAudioRepository
        │    → se existe: atualiza audioUrl
        │    → se não existe: insere novo SongAudio
        │ 5. Retorna SongAudioDTO
        ▼
    200 OK com SongAudioDTO
```

---

## Configuração

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB
```

---

## O que NÃO está no escopo

- Endpoint para `VS` (estrutura preparada, implementação futura)
- Deleção do arquivo antigo no S3 ao substituir
- Endpoint para remover o áudio de uma música
- Endpoint `GET` para listar os áudios de uma música
