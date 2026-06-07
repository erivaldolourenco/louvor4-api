# Song Audio Upload — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir o upload de arquivos de áudio de referência por música via endpoint dedicado `POST /songs/{songId}/audio?type=REFERENCE`, armazenando no S3 e suportando substituição (upsert por tipo).

**Architecture:** Nova entidade `SongAudio` com constraint `UNIQUE(song_id, type)` e enum `SongAudioType` (REFERENCE, VS). A infraestrutura S3 já existe via `StorageService`/`AmazonS3StorageService`. A lógica fica em `SongAudioServiceImpl` e um novo endpoint é adicionado ao `SongController` existente.

**Tech Stack:** Java 17, Spring Boot, JPA/Hibernate, PostgreSQL, AWS S3 SDK, JUnit 5, Mockito, AssertJ

---

### Task 1: Enums — `SongAudioType` e `FileCategory.SONG_AUDIO`

**Files:**
- Create: `src/main/java/br/com/louvor4/api/enums/SongAudioType.java`
- Modify: `src/main/java/br/com/louvor4/api/enums/FileCategory.java`

- [ ] **Step 1: Criar `SongAudioType`**

```java
// src/main/java/br/com/louvor4/api/enums/SongAudioType.java
package br.com.louvor4.api.enums;

public enum SongAudioType {
    REFERENCE,
    VS
}
```

- [ ] **Step 2: Adicionar `SONG_AUDIO` ao `FileCategory`**

Abrir `src/main/java/br/com/louvor4/api/enums/FileCategory.java` e adicionar o novo valor antes do fechamento da enum:

```java
SONG_AUDIO("audio/songs");
```

O arquivo ficará assim:

```java
package br.com.louvor4.api.enums;

public enum FileCategory {
    MINISTRY_PROFILE("images/ministry/profile"),
    MINISTRY_COVER("images/ministry/cover"),
    PROJECT_PROFILE("images/project/profile"),
    USER_PROFILE("images/user/profile"),
    DOCUMENTS("documents"),
    SONG_AUDIO("audio/songs");

    private final String path;

    FileCategory(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path;
    }
}
```

- [ ] **Step 3: Compilar para verificar**

```bash
./mvnw compile -q
```

Esperado: BUILD SUCCESS sem erros.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/louvor4/api/enums/SongAudioType.java \
        src/main/java/br/com/louvor4/api/enums/FileCategory.java
git commit -m "feat: adiciona SongAudioType enum e SONG_AUDIO ao FileCategory"
```

---

### Task 2: Entidade `SongAudio` e `SongAudioRepository`

**Files:**
- Create: `src/main/java/br/com/louvor4/api/models/SongAudio.java`
- Create: `src/main/java/br/com/louvor4/api/repositories/SongAudioRepository.java`

- [ ] **Step 1: Criar a entidade `SongAudio`**

```java
// src/main/java/br/com/louvor4/api/models/SongAudio.java
package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.SongAudioType;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "song_audio_files",
    uniqueConstraints = @UniqueConstraint(columnNames = {"song_id", "type"})
)
public class SongAudio {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false, columnDefinition = "uuid")
    private Song song;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SongAudioType type;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }

    public SongAudioType getType() { return type; }
    public void setType(SongAudioType type) { this.type = type; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Criar o `SongAudioRepository`**

```java
// src/main/java/br/com/louvor4/api/repositories/SongAudioRepository.java
package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.models.SongAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SongAudioRepository extends JpaRepository<SongAudio, UUID> {
    Optional<SongAudio> findBySong_IdAndType(UUID songId, SongAudioType type);
}
```

- [ ] **Step 3: Compilar para verificar**

```bash
./mvnw compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/louvor4/api/models/SongAudio.java \
        src/main/java/br/com/louvor4/api/repositories/SongAudioRepository.java
git commit -m "feat: adiciona entidade SongAudio e SongAudioRepository"
```

---

### Task 3: DTO `SongAudioDTO` e interface `SongAudioService`

**Files:**
- Create: `src/main/java/br/com/louvor4/api/shared/dto/Song/SongAudioDTO.java`
- Create: `src/main/java/br/com/louvor4/api/services/SongAudioService.java`
- Create: `src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java` (stub)

- [ ] **Step 1: Criar `SongAudioDTO`**

```java
// src/main/java/br/com/louvor4/api/shared/dto/Song/SongAudioDTO.java
package br.com.louvor4.api.shared.dto.Song;

import br.com.louvor4.api.enums.SongAudioType;

import java.util.UUID;

public record SongAudioDTO(UUID songId, SongAudioType type, String audioUrl) {}
```

- [ ] **Step 2: Criar a interface `SongAudioService`**

```java
// src/main/java/br/com/louvor4/api/services/SongAudioService.java
package br.com.louvor4.api.services;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface SongAudioService {
    SongAudioDTO uploadAudio(UUID songId, SongAudioType type, MultipartFile file);
}
```

- [ ] **Step 3: Criar stub de `SongAudioServiceImpl`**

Stub mínimo para os testes compilarem no próximo passo:

```java
// src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.repositories.SongAudioRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.SongAudioService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class SongAudioServiceImpl implements SongAudioService {

    private final SongRepository songRepository;
    private final SongAudioRepository songAudioRepository;
    private final StorageService storageService;

    public SongAudioServiceImpl(SongRepository songRepository,
                                SongAudioRepository songAudioRepository,
                                StorageService storageService) {
        this.songRepository = songRepository;
        this.songAudioRepository = songAudioRepository;
        this.storageService = storageService;
    }

    @Override
    public SongAudioDTO uploadAudio(UUID songId, SongAudioType type, MultipartFile file) {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
```

- [ ] **Step 4: Compilar para verificar**

```bash
./mvnw compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/br/com/louvor4/api/shared/dto/Song/SongAudioDTO.java \
        src/main/java/br/com/louvor4/api/services/SongAudioService.java \
        src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java
git commit -m "feat: adiciona SongAudioDTO, SongAudioService e stub do SongAudioServiceImpl"
```

---

### Task 4: Testes para `SongAudioServiceImpl` (TDD — devem falhar)

**Files:**
- Create: `src/test/java/br/com/louvor4/api/services/impl/SongAudioServiceImplTest.java`

- [ ] **Step 1: Criar o arquivo de testes**

```java
// src/test/java/br/com/louvor4/api/services/impl/SongAudioServiceImplTest.java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.SongAudio;
import br.com.louvor4.api.repositories.SongAudioRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongAudioServiceImplTest {

    @Mock SongRepository songRepository;
    @Mock SongAudioRepository songAudioRepository;
    @Mock StorageService storageService;
    @Mock MultipartFile file;

    @InjectMocks SongAudioServiceImpl service;

    private UUID songId;
    private Song song;

    @BeforeEach
    void setUp() {
        songId = UUID.randomUUID();
        song = new Song();
        song.setId(songId);
    }

    @Test
    void uploadAudioShouldInsertWhenNoExistingAudio() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.getContentType()).thenReturn("audio/mpeg");
        when(storageService.uploadFileWithPrefix(eq(file), eq(FileCategory.SONG_AUDIO), eq(songId.toString())))
                .thenReturn("https://s3.example.com/audio/song.mp3");
        when(songAudioRepository.findBySong_IdAndType(songId, SongAudioType.REFERENCE))
                .thenReturn(Optional.empty());

        SongAudio saved = new SongAudio();
        saved.setType(SongAudioType.REFERENCE);
        saved.setAudioUrl("https://s3.example.com/audio/song.mp3");
        when(songAudioRepository.save(any())).thenReturn(saved);

        SongAudioDTO result = service.uploadAudio(songId, SongAudioType.REFERENCE, file);

        assertThat(result.songId()).isEqualTo(songId);
        assertThat(result.type()).isEqualTo(SongAudioType.REFERENCE);
        assertThat(result.audioUrl()).isEqualTo("https://s3.example.com/audio/song.mp3");

        ArgumentCaptor<SongAudio> captor = ArgumentCaptor.forClass(SongAudio.class);
        verify(songAudioRepository).save(captor.capture());
        assertThat(captor.getValue().getSong()).isEqualTo(song);
        assertThat(captor.getValue().getType()).isEqualTo(SongAudioType.REFERENCE);
        assertThat(captor.getValue().getAudioUrl()).isEqualTo("https://s3.example.com/audio/song.mp3");
    }

    @Test
    void uploadAudioShouldUpdateAudioUrlWhenExistingAudioFound() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.getContentType()).thenReturn("audio/wav");
        when(storageService.uploadFileWithPrefix(eq(file), eq(FileCategory.SONG_AUDIO), eq(songId.toString())))
                .thenReturn("https://s3.example.com/audio/new.wav");

        SongAudio existing = new SongAudio();
        existing.setType(SongAudioType.REFERENCE);
        existing.setAudioUrl("https://s3.example.com/audio/old.mp3");
        when(songAudioRepository.findBySong_IdAndType(songId, SongAudioType.REFERENCE))
                .thenReturn(Optional.of(existing));

        SongAudio saved = new SongAudio();
        saved.setType(SongAudioType.REFERENCE);
        saved.setAudioUrl("https://s3.example.com/audio/new.wav");
        when(songAudioRepository.save(any())).thenReturn(saved);

        SongAudioDTO result = service.uploadAudio(songId, SongAudioType.REFERENCE, file);

        assertThat(result.audioUrl()).isEqualTo("https://s3.example.com/audio/new.wav");

        ArgumentCaptor<SongAudio> captor = ArgumentCaptor.forClass(SongAudio.class);
        verify(songAudioRepository).save(captor.capture());
        assertThat(captor.getValue().getAudioUrl()).isEqualTo("https://s3.example.com/audio/new.wav");
    }

    @Test
    void uploadAudioShouldThrowNotFoundWhenSongDoesNotExist() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadAudio(songId, SongAudioType.REFERENCE, file))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Música não encontrada");
    }

    @Test
    void uploadAudioShouldThrowValidationExceptionWhenContentTypeIsNotAudio() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.getContentType()).thenReturn("image/png");

        assertThatThrownBy(() -> service.uploadAudio(songId, SongAudioType.REFERENCE, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("áudio válido");
    }

    @Test
    void uploadAudioShouldThrowValidationExceptionWhenContentTypeIsNull() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> service.uploadAudio(songId, SongAudioType.REFERENCE, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("áudio válido");
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que falham**

```bash
./mvnw test -pl . -Dtest=SongAudioServiceImplTest -q 2>&1 | tail -20
```

Esperado: `FAILED` com `UnsupportedOperationException: not implemented yet` nos testes de happy path, e os testes de exceção também falham porque a impl lança `UnsupportedOperationException` em vez das exceções corretas.

- [ ] **Step 3: Commit dos testes**

```bash
git add src/test/java/br/com/louvor4/api/services/impl/SongAudioServiceImplTest.java
git commit -m "test: adiciona testes para SongAudioServiceImpl (TDD — falham)"
```

---

### Task 5: Implementar `SongAudioServiceImpl`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java`

- [ ] **Step 1: Substituir o stub pela implementação completa**

```java
// src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.SongAudio;
import br.com.louvor4.api.repositories.SongAudioRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.SongAudioService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class SongAudioServiceImpl implements SongAudioService {

    private final SongRepository songRepository;
    private final SongAudioRepository songAudioRepository;
    private final StorageService storageService;

    public SongAudioServiceImpl(SongRepository songRepository,
                                SongAudioRepository songAudioRepository,
                                StorageService storageService) {
        this.songRepository = songRepository;
        this.songAudioRepository = songAudioRepository;
        this.storageService = storageService;
    }

    @Override
    public SongAudioDTO uploadAudio(UUID songId, SongAudioType type, MultipartFile file) {
        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new NotFoundException("Música não encontrada."));

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ValidationException("O arquivo deve ser um áudio válido (audio/*).");
        }

        String audioUrl = storageService.uploadFileWithPrefix(file, FileCategory.SONG_AUDIO, songId.toString());

        SongAudio songAudio = songAudioRepository.findBySong_IdAndType(songId, type)
                .orElseGet(SongAudio::new);
        songAudio.setSong(song);
        songAudio.setType(type);
        songAudio.setAudioUrl(audioUrl);

        SongAudio saved = songAudioRepository.save(songAudio);

        return new SongAudioDTO(songId, saved.getType(), saved.getAudioUrl());
    }
}
```

- [ ] **Step 2: Rodar os testes e confirmar que passam**

```bash
./mvnw test -pl . -Dtest=SongAudioServiceImplTest -q 2>&1 | tail -10
```

Esperado:
```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 3: Rodar toda a suíte para garantir nenhuma regressão**

```bash
./mvnw test -q 2>&1 | tail -15
```

Esperado: BUILD SUCCESS, todos os testes passando.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java
git commit -m "feat: implementa SongAudioServiceImpl com upload e upsert de áudio"
```

---

### Task 6: Endpoint `POST /songs/{songId}/audio` no `SongController`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/SongController.java`

- [ ] **Step 1: Injetar `SongAudioService` e adicionar o endpoint**

O `SongController` atual injeta apenas `SongService`. Adicionar `SongAudioService` via construtor e o novo método:

```java
// src/main/java/br/com/louvor4/api/controllers/SongController.java
package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.services.SongAudioService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("songs")
public class SongController {

    private final SongService songService;
    private final SongAudioService songAudioService;

    public SongController(SongService songService, SongAudioService songAudioService) {
        this.songService = songService;
        this.songAudioService = songAudioService;
    }

    @GetMapping("/{songId}")
    public ResponseEntity<SongDTO> get(@PathVariable UUID songId) {
        SongDTO dto = songService.get(songId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/create")
    public ResponseEntity<SongDTO> create(@RequestBody @Valid SongDTO createDto) {
        SongDTO dto = songService.create(createDto);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/update")
    public ResponseEntity<SongDTO> update(@RequestBody @Valid SongDTO updateDto) {
        SongDTO dto = songService.update(updateDto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{songId}/audio")
    public ResponseEntity<SongAudioDTO> uploadAudio(
            @PathVariable UUID songId,
            @RequestParam SongAudioType type,
            @RequestPart("file") MultipartFile file) {
        SongAudioDTO dto = songAudioService.uploadAudio(songId, type, file);
        return ResponseEntity.ok(dto);
    }
}
```

> **Nota:** O método `GET /{songId}` estava nomeado `create` no código original — isso é um bug de nome. Corrija para `get` conforme mostrado acima.

- [ ] **Step 2: Compilar para verificar**

```bash
./mvnw compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Rodar toda a suíte de testes**

```bash
./mvnw test -q 2>&1 | tail -15
```

Esperado: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/SongController.java
git commit -m "feat: adiciona endpoint POST /songs/{songId}/audio para upload de áudio"
```

---

### Task 7: Atualizar limite de upload no `application.yml`

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Alterar o limite de 10MB para 20MB**

Localizar a seção `spring.servlet.multipart` em `src/main/resources/application.yml` e alterar:

```yaml
  servlet:
    multipart:
      enabled: true
      max-file-size: 20MB
      max-request-size: 20MB
```

- [ ] **Step 2: Compilar para verificar**

```bash
./mvnw compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "chore: aumenta limite de upload multipart para 20MB"
```

---

### Task 8: DDL para produção

O ambiente de desenvolvimento usa `ddl-auto: update` e criará a tabela automaticamente. Para produção (`ddl-auto: none`), executar o DDL abaixo manualmente no banco antes do deploy:

```sql
CREATE TABLE song_audio_files (
    id         UUID PRIMARY KEY,
    song_id    UUID NOT NULL REFERENCES songs(id),
    type       VARCHAR(20) NOT NULL,
    audio_url  VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_song_audio_type UNIQUE (song_id, type)
);
```

- [ ] **Step 1: Criar arquivo de referência com o DDL**

```bash
mkdir -p src/main/resources/db
```

Criar `src/main/resources/db/song_audio_files.sql`:

```sql
-- DDL para produção: executar manualmente antes do deploy
CREATE TABLE song_audio_files (
    id         UUID PRIMARY KEY,
    song_id    UUID NOT NULL REFERENCES songs(id),
    type       VARCHAR(20) NOT NULL,
    audio_url  VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_song_audio_type UNIQUE (song_id, type)
);
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/song_audio_files.sql
git commit -m "chore: adiciona DDL de referência para tabela song_audio_files"
```
