# Audio Files — Suporte a Medley

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Renomear a estrutura `SongAudio` / `song_audio_files` para `AudioFile` / `audio_files`, adicionando suporte a FK nullable para `medley_id`, e expor endpoint `POST /medleys/{medleyId}/audio` para upload de áudio de medley.

**Architecture:** Tabela única `audio_files` com `song_id` e `medley_id` ambos nullable — exatamente um preenchido por linha, garantido por CHECK constraint no banco. A lógica de upload (validação de arquivo, upsert por owner+type) é centralizada em `AudioFileServiceImpl`. O renomear ocorre em etapas sem quebrar compilação: novas classes criadas primeiro, referências migradas, antigas deletadas por último.

**Tech Stack:** Spring Boot 3.5 / Java 17 / JPA + Hibernate / PostgreSQL / JUnit 5 + Mockito / MapStruct / AWS S3 (StorageService)

---

## Mapa de Arquivos

| Ação | Arquivo |
|------|---------|
| Criar | `enums/AudioType.java` |
| Criar | `models/AudioFile.java` |
| Criar | `repositories/AudioFileRepository.java` |
| Criar | `shared/dto/Audio/AudioFileDTO.java` |
| Criar | `services/AudioFileService.java` |
| Criar | `services/impl/AudioFileServiceImpl.java` |
| Criar | `test/.../AudioFileServiceImplTest.java` |
| Criar | `resources/db/audio_files.sql` |
| Modificar | `enums/FileCategory.java` |
| Modificar | `controllers/SongController.java` |
| Modificar | `controllers/MedleyController.java` |
| Modificar | `services/impl/SongServiceImpl.java` |
| Modificar | `services/impl/EventServiceImpl.java` |
| Modificar | `test/.../SongServiceImplGetTest.java` |
| Deletar | `enums/SongAudioType.java` |
| Deletar | `models/SongAudio.java` |
| Deletar | `repositories/SongAudioRepository.java` |
| Deletar | `shared/dto/Song/SongAudioDTO.java` |
| Deletar | `services/SongAudioService.java` |
| Deletar | `services/impl/SongAudioServiceImpl.java` |
| Deletar | `test/.../SongAudioServiceImplTest.java` |
| Deletar | `resources/db/song_audio_files.sql` |

---

## Task 1: Criar AudioType enum

**Files:**
- Create: `src/main/java/br/com/louvor4/api/enums/AudioType.java`

- [ ] **Step 1: Criar o enum**

```java
package br.com.louvor4.api.enums;

public enum AudioType {
    REFERENCE,
    VS
}
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/enums/AudioType.java
git commit -m "feat: adiciona enum AudioType (substituto de SongAudioType)"
```

---

## Task 2: Criar entidade AudioFile

**Files:**
- Create: `src/main/java/br/com/louvor4/api/models/AudioFile.java`

A entidade tem `song` e `medley` ambos nullable. A constraint de unicidade (song+type, medley+type) será aplicada via índices parciais no banco (Task 14) — JPA `@UniqueConstraint` não suporta WHERE clause.

- [ ] **Step 1: Criar a entidade**

```java
package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.AudioType;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audio_files")
public class AudioFile {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", columnDefinition = "uuid")
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medley_id", columnDefinition = "uuid")
    private Medley medley;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AudioType type;

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

    public Medley getMedley() { return medley; }
    public void setMedley(Medley medley) { this.medley = medley; }

    public AudioType getType() { return type; }
    public void setType(AudioType type) { this.type = type; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/models/AudioFile.java
git commit -m "feat: adiciona entidade AudioFile com FKs nullable para Song e Medley"
```

---

## Task 3: Criar AudioFileRepository

**Files:**
- Create: `src/main/java/br/com/louvor4/api/repositories/AudioFileRepository.java`

- [ ] **Step 1: Criar o repository**

```java
package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.models.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, UUID> {
    Optional<AudioFile> findBySong_IdAndType(UUID songId, AudioType type);
    Optional<AudioFile> findByMedley_IdAndType(UUID medleyId, AudioType type);
    List<AudioFile> findBySong_IdInAndType(Collection<UUID> songIds, AudioType type);
}
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/repositories/AudioFileRepository.java
git commit -m "feat: adiciona AudioFileRepository com queries para song e medley"
```

---

## Task 4: Criar AudioFileDTO

**Files:**
- Create: `src/main/java/br/com/louvor4/api/shared/dto/Audio/AudioFileDTO.java`

O DTO usa `songId` e `medleyId` ambos nullable (um deles será nulo dependendo do owner).

- [ ] **Step 1: Criar o DTO**

```java
package br.com.louvor4.api.shared.dto.Audio;

import br.com.louvor4.api.enums.AudioType;

import java.util.UUID;

public record AudioFileDTO(UUID songId, UUID medleyId, AudioType type, String audioUrl) {}
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/shared/dto/Audio/AudioFileDTO.java
git commit -m "feat: adiciona AudioFileDTO com songId e medleyId nullable"
```

---

## Task 5: Adicionar MEDLEY_AUDIO ao FileCategory

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/enums/FileCategory.java`

- [ ] **Step 1: Adicionar o valor**

No arquivo `FileCategory.java`, adicionar após `SONG_AUDIO("audio/songs")`:

```java
MEDLEY_AUDIO("audio/medleys");
```

O enum completo após a mudança:

```java
package br.com.louvor4.api.enums;

public enum FileCategory {
    MINISTRY_PROFILE("images/ministry/profile"),
    MINISTRY_COVER("images/ministry/cover"),
    PROJECT_PROFILE("images/project/profile"),
    USER_PROFILE("images/user/profile"),
    DOCUMENTS("documents"),
    SONG_AUDIO("audio/songs"),
    MEDLEY_AUDIO("audio/medleys");

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

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/enums/FileCategory.java
git commit -m "feat: adiciona FileCategory.MEDLEY_AUDIO"
```

---

## Task 6: TDD — Escrever testes para AudioFileServiceImpl

**Files:**
- Create: `src/test/java/br/com/louvor4/api/services/impl/AudioFileServiceImplTest.java`

Estes testes devem FALHAR inicialmente porque `AudioFileServiceImpl` ainda não existe.

- [ ] **Step 1: Criar o arquivo de testes**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.models.Medley;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.repositories.AudioFileRepository;
import br.com.louvor4.api.repositories.MedleyRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
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
class AudioFileServiceImplTest {

    @Mock SongRepository songRepository;
    @Mock MedleyRepository medleyRepository;
    @Mock AudioFileRepository audioFileRepository;
    @Mock StorageService storageService;
    @Mock MultipartFile file;

    @InjectMocks AudioFileServiceImpl service;

    private UUID songId;
    private UUID medleyId;
    private Song song;
    private Medley medley;

    @BeforeEach
    void setUp() {
        songId = UUID.randomUUID();
        medleyId = UUID.randomUUID();
        song = new Song();
        song.setId(songId);
        medley = new Medley();
        medley.setId(medleyId);
    }

    // --- uploadSongAudio ---

    @Test
    void uploadSongAudioShouldInsertWhenNoExistingAudio() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("audio/mpeg");
        when(storageService.uploadFileWithPrefix(eq(file), eq(FileCategory.SONG_AUDIO), eq(songId.toString())))
                .thenReturn("https://s3.example.com/audio/song.mp3");
        when(audioFileRepository.findBySong_IdAndType(songId, AudioType.REFERENCE))
                .thenReturn(Optional.empty());

        AudioFile saved = new AudioFile();
        saved.setSong(song);
        saved.setType(AudioType.REFERENCE);
        saved.setAudioUrl("https://s3.example.com/audio/song.mp3");
        when(audioFileRepository.save(any())).thenReturn(saved);

        AudioFileDTO result = service.uploadSongAudio(songId, AudioType.REFERENCE, file);

        assertThat(result.songId()).isEqualTo(songId);
        assertThat(result.medleyId()).isNull();
        assertThat(result.type()).isEqualTo(AudioType.REFERENCE);
        assertThat(result.audioUrl()).isEqualTo("https://s3.example.com/audio/song.mp3");

        ArgumentCaptor<AudioFile> captor = ArgumentCaptor.forClass(AudioFile.class);
        verify(audioFileRepository).save(captor.capture());
        assertThat(captor.getValue().getSong()).isEqualTo(song);
        assertThat(captor.getValue().getMedley()).isNull();
        assertThat(captor.getValue().getType()).isEqualTo(AudioType.REFERENCE);
        assertThat(captor.getValue().getAudioUrl()).isEqualTo("https://s3.example.com/audio/song.mp3");
    }

    @Test
    void uploadSongAudioShouldUpdateWhenExistingAudioFound() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("audio/wav");
        when(storageService.uploadFileWithPrefix(eq(file), eq(FileCategory.SONG_AUDIO), eq(songId.toString())))
                .thenReturn("https://s3.example.com/audio/new.wav");

        AudioFile existing = new AudioFile();
        existing.setSong(song);
        existing.setType(AudioType.REFERENCE);
        existing.setAudioUrl("https://s3.example.com/audio/old.mp3");
        when(audioFileRepository.findBySong_IdAndType(songId, AudioType.REFERENCE))
                .thenReturn(Optional.of(existing));

        AudioFile saved = new AudioFile();
        saved.setSong(song);
        saved.setType(AudioType.REFERENCE);
        saved.setAudioUrl("https://s3.example.com/audio/new.wav");
        when(audioFileRepository.save(any())).thenReturn(saved);

        AudioFileDTO result = service.uploadSongAudio(songId, AudioType.REFERENCE, file);

        assertThat(result.audioUrl()).isEqualTo("https://s3.example.com/audio/new.wav");

        ArgumentCaptor<AudioFile> captor = ArgumentCaptor.forClass(AudioFile.class);
        verify(audioFileRepository).save(captor.capture());
        assertThat(captor.getValue().getAudioUrl()).isEqualTo("https://s3.example.com/audio/new.wav");
    }

    @Test
    void uploadSongAudioShouldThrowNotFoundWhenSongDoesNotExist() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadSongAudio(songId, AudioType.REFERENCE, file))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Música não encontrada");
    }

    @Test
    void uploadSongAudioShouldThrowValidationWhenFileIsEmpty() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> service.uploadSongAudio(songId, AudioType.REFERENCE, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("arquivo não pode estar vazio");
    }

    @Test
    void uploadSongAudioShouldThrowValidationWhenContentTypeIsNotAudio() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");

        assertThatThrownBy(() -> service.uploadSongAudio(songId, AudioType.REFERENCE, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("áudio válido");
    }

    // --- uploadMedleyAudio ---

    @Test
    void uploadMedleyAudioShouldInsertWhenNoExistingAudio() {
        when(medleyRepository.findById(medleyId)).thenReturn(Optional.of(medley));
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("audio/mpeg");
        when(storageService.uploadFileWithPrefix(eq(file), eq(FileCategory.MEDLEY_AUDIO), eq(medleyId.toString())))
                .thenReturn("https://s3.example.com/audio/medley.mp3");
        when(audioFileRepository.findByMedley_IdAndType(medleyId, AudioType.REFERENCE))
                .thenReturn(Optional.empty());

        AudioFile saved = new AudioFile();
        saved.setMedley(medley);
        saved.setType(AudioType.REFERENCE);
        saved.setAudioUrl("https://s3.example.com/audio/medley.mp3");
        when(audioFileRepository.save(any())).thenReturn(saved);

        AudioFileDTO result = service.uploadMedleyAudio(medleyId, AudioType.REFERENCE, file);

        assertThat(result.medleyId()).isEqualTo(medleyId);
        assertThat(result.songId()).isNull();
        assertThat(result.type()).isEqualTo(AudioType.REFERENCE);
        assertThat(result.audioUrl()).isEqualTo("https://s3.example.com/audio/medley.mp3");

        ArgumentCaptor<AudioFile> captor = ArgumentCaptor.forClass(AudioFile.class);
        verify(audioFileRepository).save(captor.capture());
        assertThat(captor.getValue().getMedley()).isEqualTo(medley);
        assertThat(captor.getValue().getSong()).isNull();
        assertThat(captor.getValue().getType()).isEqualTo(AudioType.REFERENCE);
        assertThat(captor.getValue().getAudioUrl()).isEqualTo("https://s3.example.com/audio/medley.mp3");
    }

    @Test
    void uploadMedleyAudioShouldUpdateWhenExistingAudioFound() {
        when(medleyRepository.findById(medleyId)).thenReturn(Optional.of(medley));
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("audio/wav");
        when(storageService.uploadFileWithPrefix(eq(file), eq(FileCategory.MEDLEY_AUDIO), eq(medleyId.toString())))
                .thenReturn("https://s3.example.com/audio/new.wav");

        AudioFile existing = new AudioFile();
        existing.setMedley(medley);
        existing.setType(AudioType.REFERENCE);
        existing.setAudioUrl("https://s3.example.com/audio/old.mp3");
        when(audioFileRepository.findByMedley_IdAndType(medleyId, AudioType.REFERENCE))
                .thenReturn(Optional.of(existing));

        AudioFile saved = new AudioFile();
        saved.setMedley(medley);
        saved.setType(AudioType.REFERENCE);
        saved.setAudioUrl("https://s3.example.com/audio/new.wav");
        when(audioFileRepository.save(any())).thenReturn(saved);

        AudioFileDTO result = service.uploadMedleyAudio(medleyId, AudioType.REFERENCE, file);

        assertThat(result.audioUrl()).isEqualTo("https://s3.example.com/audio/new.wav");
        ArgumentCaptor<AudioFile> captor = ArgumentCaptor.forClass(AudioFile.class);
        verify(audioFileRepository).save(captor.capture());
        assertThat(captor.getValue().getAudioUrl()).isEqualTo("https://s3.example.com/audio/new.wav");
    }

    @Test
    void uploadMedleyAudioShouldThrowNotFoundWhenMedleyDoesNotExist() {
        when(medleyRepository.findById(medleyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadMedleyAudio(medleyId, AudioType.REFERENCE, file))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Medley não encontrado");
    }

    @Test
    void uploadMedleyAudioShouldThrowValidationWhenFileIsEmpty() {
        when(medleyRepository.findById(medleyId)).thenReturn(Optional.of(medley));
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> service.uploadMedleyAudio(medleyId, AudioType.REFERENCE, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("arquivo não pode estar vazio");
    }

    @Test
    void uploadMedleyAudioShouldThrowValidationWhenContentTypeIsNotAudio() {
        when(medleyRepository.findById(medleyId)).thenReturn(Optional.of(medley));
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("video/mp4");

        assertThatThrownBy(() -> service.uploadMedleyAudio(medleyId, AudioType.REFERENCE, file))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("áudio válido");
    }
}
```

- [ ] **Step 2: Rodar testes e confirmar que FALHAM (AudioFileServiceImpl ainda não existe)**

```bash
./mvnw test -pl . -Dtest=AudioFileServiceImplTest -q 2>&1 | tail -10
```
Expected: FAILURE — `AudioFileServiceImpl` não encontrado / não compilável

- [ ] **Step 3: Commit dos testes**

```bash
git add src/test/java/br/com/louvor4/api/services/impl/AudioFileServiceImplTest.java
git commit -m "test: adiciona testes TDD para AudioFileServiceImpl (falham — impl pendente)"
```

---

## Task 7: Criar AudioFileService e AudioFileServiceImpl

**Files:**
- Create: `src/main/java/br/com/louvor4/api/services/AudioFileService.java`
- Create: `src/main/java/br/com/louvor4/api/services/impl/AudioFileServiceImpl.java`

- [ ] **Step 1: Criar a interface AudioFileService**

```java
package br.com.louvor4.api.services;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AudioFileService {
    AudioFileDTO uploadSongAudio(UUID songId, AudioType type, MultipartFile file);
    AudioFileDTO uploadMedleyAudio(UUID medleyId, AudioType type, MultipartFile file);
}
```

- [ ] **Step 2: Criar AudioFileServiceImpl**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.models.Medley;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.repositories.AudioFileRepository;
import br.com.louvor4.api.repositories.MedleyRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.services.AudioFileService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class AudioFileServiceImpl implements AudioFileService {

    private final SongRepository songRepository;
    private final MedleyRepository medleyRepository;
    private final AudioFileRepository audioFileRepository;
    private final StorageService storageService;

    public AudioFileServiceImpl(SongRepository songRepository,
                                MedleyRepository medleyRepository,
                                AudioFileRepository audioFileRepository,
                                StorageService storageService) {
        this.songRepository = songRepository;
        this.medleyRepository = medleyRepository;
        this.audioFileRepository = audioFileRepository;
        this.storageService = storageService;
    }

    @Override
    public AudioFileDTO uploadSongAudio(UUID songId, AudioType type, MultipartFile file) {
        Song song = songRepository.getSongById(songId)
                .orElseThrow(() -> new NotFoundException("Música não encontrada."));

        validateFile(file);

        String audioUrl = storageService.uploadFileWithPrefix(file, FileCategory.SONG_AUDIO, songId.toString());

        AudioFile audioFile = audioFileRepository.findBySong_IdAndType(songId, type)
                .orElseGet(AudioFile::new);
        audioFile.setSong(song);
        audioFile.setType(type);
        audioFile.setAudioUrl(audioUrl);

        AudioFile saved = audioFileRepository.save(audioFile);

        return new AudioFileDTO(saved.getSong().getId(), null, saved.getType(), saved.getAudioUrl());
    }

    @Override
    public AudioFileDTO uploadMedleyAudio(UUID medleyId, AudioType type, MultipartFile file) {
        Medley medley = medleyRepository.findById(medleyId)
                .orElseThrow(() -> new NotFoundException("Medley não encontrado."));

        validateFile(file);

        String audioUrl = storageService.uploadFileWithPrefix(file, FileCategory.MEDLEY_AUDIO, medleyId.toString());

        AudioFile audioFile = audioFileRepository.findByMedley_IdAndType(medleyId, type)
                .orElseGet(AudioFile::new);
        audioFile.setMedley(medley);
        audioFile.setType(type);
        audioFile.setAudioUrl(audioUrl);

        AudioFile saved = audioFileRepository.save(audioFile);

        return new AudioFileDTO(null, saved.getMedley().getId(), saved.getType(), saved.getAudioUrl());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("O arquivo não pode estar vazio.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new ValidationException("O arquivo deve ser um áudio válido (audio/*).");
        }
    }
}
```

- [ ] **Step 3: Rodar os testes e confirmar que PASSAM**

```bash
./mvnw test -pl . -Dtest=AudioFileServiceImplTest -q 2>&1 | tail -5
```
Expected: BUILD SUCCESS, todos os 10 testes passando

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/AudioFileService.java \
        src/main/java/br/com/louvor4/api/services/impl/AudioFileServiceImpl.java
git commit -m "feat: implementa AudioFileService com suporte a upload de áudio para Song e Medley"
```

---

## Task 8: Atualizar SongController para usar AudioFileService

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/SongController.java`

- [ ] **Step 1: Substituir dependências no SongController**

Conteúdo completo do arquivo após a mudança:

```java
package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.services.AudioFileService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("songs")
public class SongController {

    private final SongService songService;
    private final AudioFileService audioFileService;

    public SongController(SongService songService, AudioFileService audioFileService) {
        this.songService = songService;
        this.audioFileService = audioFileService;
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

    @PostMapping(value = "/{songId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioFileDTO> uploadAudio(
            @PathVariable UUID songId,
            @RequestParam AudioType type,
            @RequestParam("file") MultipartFile file) {
        AudioFileDTO dto = audioFileService.uploadSongAudio(songId, type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/SongController.java
git commit -m "refactor: SongController usa AudioFileService e AudioType"
```

---

## Task 9: Adicionar endpoint de áudio ao MedleyController

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/MedleyController.java`

- [ ] **Step 1: Adicionar endpoint de upload ao MedleyController**

Conteúdo completo do arquivo após a mudança:

```java
package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.services.AudioFileService;
import br.com.louvor4.api.services.MedleyService;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import br.com.louvor4.api.shared.dto.Medley.CreateMedleyRequest;
import br.com.louvor4.api.shared.dto.Medley.MedleyResponse;
import br.com.louvor4.api.shared.dto.Medley.UpdateMedleyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/medleys")
public class MedleyController {

    private final MedleyService medleyService;
    private final AudioFileService audioFileService;

    public MedleyController(MedleyService medleyService, AudioFileService audioFileService) {
        this.medleyService = medleyService;
        this.audioFileService = audioFileService;
    }

    @PostMapping("/create")
    public ResponseEntity<MedleyResponse> create(@RequestBody @Valid CreateMedleyRequest request) {
        MedleyResponse created = medleyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<MedleyResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateMedleyRequest request) {
        MedleyResponse updated = medleyService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medleyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{medleyId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioFileDTO> uploadAudio(
            @PathVariable UUID medleyId,
            @RequestParam AudioType type,
            @RequestParam("file") MultipartFile file) {
        AudioFileDTO dto = audioFileService.uploadMedleyAudio(medleyId, type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/MedleyController.java
git commit -m "feat: adiciona POST /medleys/{medleyId}/audio para upload de áudio de medley"
```

---

## Task 10: Migrar SongServiceImpl para AudioFileRepository

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/impl/SongServiceImpl.java`

- [ ] **Step 1: Substituir SongAudioRepository por AudioFileRepository no SongServiceImpl**

Localizar e substituir os imports e usos no arquivo:

Substituir imports:
```java
// Remover:
import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.models.SongAudio;
import br.com.louvor4.api.repositories.SongAudioRepository;

// Adicionar:
import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.repositories.AudioFileRepository;
```

Substituir no campo e construtor:
```java
// Antes:
private final SongAudioRepository songAudioRepository;
// ...
public SongServiceImpl(..., SongAudioRepository songAudioRepository, ...) {
    this.songAudioRepository = songAudioRepository;
// Depois:
private final AudioFileRepository audioFileRepository;
// ...
public SongServiceImpl(..., AudioFileRepository audioFileRepository, ...) {
    this.audioFileRepository = audioFileRepository;
```

Substituir nos dois usos do método `findBySong_IdAndType`:
```java
// Antes:
songAudioRepository.findBySong_IdAndType(song.getId(), SongAudioType.REFERENCE)
        .map(SongAudio::getAudioUrl)
// Depois:
audioFileRepository.findBySong_IdAndType(song.getId(), AudioType.REFERENCE)
        .map(AudioFile::getAudioUrl)
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/impl/SongServiceImpl.java
git commit -m "refactor: SongServiceImpl usa AudioFileRepository e AudioType"
```

---

## Task 11: Migrar EventServiceImpl para AudioFileRepository

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java`

- [ ] **Step 1: Substituir SongAudioRepository por AudioFileRepository no EventServiceImpl**

Substituir imports:
```java
// Remover:
import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.models.SongAudio;
import br.com.louvor4.api.repositories.SongAudioRepository;

// Adicionar:
import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.repositories.AudioFileRepository;
```

Substituir o campo e injeção no construtor:
```java
// Antes:
private final SongAudioRepository songAudioRepository;
// constructor param: SongAudioRepository songAudioRepository
// Depois:
private final AudioFileRepository audioFileRepository;
// constructor param: AudioFileRepository audioFileRepository
```

Substituir o uso na linha ~631:
```java
// Antes:
Map<UUID, String> audioUrlBySongId = songAudioRepository
        .findBySong_IdInAndType(songIds, SongAudioType.REFERENCE)
        ...
        .collect(Collectors.toMap(sa -> sa.getSong().getId(), SongAudio::getAudioUrl));
// Depois:
Map<UUID, String> audioUrlBySongId = audioFileRepository
        .findBySong_IdInAndType(songIds, AudioType.REFERENCE)
        ...
        .collect(Collectors.toMap(af -> af.getSong().getId(), AudioFile::getAudioUrl));
```

- [ ] **Step 2: Verificar compilação**

```bash
./mvnw compile -q
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java
git commit -m "refactor: EventServiceImpl usa AudioFileRepository e AudioType"
```

---

## Task 12: Atualizar SongServiceImplGetTest

**Files:**
- Modify: `src/test/java/br/com/louvor4/api/services/impl/SongServiceImplGetTest.java`

- [ ] **Step 1: Substituir imports e mocks no teste**

Conteúdo completo do arquivo após a mudança:

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.SongMapper;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.repositories.AudioFileRepository;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongServiceImplGetTest {

    @Mock SongRepository songRepository;
    @Mock AudioFileRepository audioFileRepository;
    @Mock SongMapper songMapper;
    @Mock CurrentUserProvider currentUserProvider;

    @InjectMocks SongServiceImpl service;

    private UUID songId;
    private Song song;

    @BeforeEach
    void setUp() {
        songId = UUID.randomUUID();
        song = new Song();
        song.setId(songId);
        song.setTitle("Hosana");
        song.setArtist("Hillsong");
        song.setKey("G");
        song.setYouTubeUrl("https://youtube.com/watch?v=abc");
    }

    @Test
    void getShouldIncludeReferenceAudioUrlWhenAudioExists() {
        AudioFile audio = new AudioFile();
        audio.setAudioUrl("https://s3.example.com/audio.mp3");

        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(audioFileRepository.findBySong_IdAndType(songId, AudioType.REFERENCE))
                .thenReturn(Optional.of(audio));

        SongDTO result = service.get(songId);

        assertThat(result.referenceAudioUrl()).isEqualTo("https://s3.example.com/audio.mp3");
        assertThat(result.id()).isEqualTo(songId);
        assertThat(result.title()).isEqualTo("Hosana");
    }

    @Test
    void getShouldReturnNullReferenceAudioUrlWhenNoAudioExists() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(audioFileRepository.findBySong_IdAndType(songId, AudioType.REFERENCE))
                .thenReturn(Optional.empty());

        SongDTO result = service.get(songId);

        assertThat(result.referenceAudioUrl()).isNull();
    }

    @Test
    void getShouldThrowWhenSongNotFound() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(songId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não encontrada");
    }
}
```

- [ ] **Step 2: Rodar todos os testes**

```bash
./mvnw test -q 2>&1 | tail -10
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/test/java/br/com/louvor4/api/services/impl/SongServiceImplGetTest.java
git commit -m "refactor: SongServiceImplGetTest usa AudioFileRepository e AudioType"
```

---

## Task 13: Deletar classes SongAudio* obsoletas

Neste ponto todos os consumidores já foram migrados para as novas classes.

**Files to delete:**
- `src/main/java/br/com/louvor4/api/enums/SongAudioType.java`
- `src/main/java/br/com/louvor4/api/models/SongAudio.java`
- `src/main/java/br/com/louvor4/api/repositories/SongAudioRepository.java`
- `src/main/java/br/com/louvor4/api/shared/dto/Song/SongAudioDTO.java`
- `src/main/java/br/com/louvor4/api/services/SongAudioService.java`
- `src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java`
- `src/test/java/br/com/louvor4/api/services/impl/SongAudioServiceImplTest.java`

- [ ] **Step 1: Deletar os arquivos obsoletos**

```bash
git rm src/main/java/br/com/louvor4/api/enums/SongAudioType.java \
       src/main/java/br/com/louvor4/api/models/SongAudio.java \
       src/main/java/br/com/louvor4/api/repositories/SongAudioRepository.java \
       "src/main/java/br/com/louvor4/api/shared/dto/Song/SongAudioDTO.java" \
       src/main/java/br/com/louvor4/api/services/SongAudioService.java \
       src/main/java/br/com/louvor4/api/services/impl/SongAudioServiceImpl.java \
       src/test/java/br/com/louvor4/api/services/impl/SongAudioServiceImplTest.java
```

- [ ] **Step 2: Rodar todos os testes para confirmar que nada quebrou**

```bash
./mvnw test -q 2>&1 | tail -10
```
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git commit -m "chore: remove classes SongAudio* substituídas por AudioFile*"
```

---

## Task 14: Atualizar DDL de referência

**Files:**
- Create: `src/main/resources/db/audio_files.sql`
- Delete: `src/main/resources/db/song_audio_files.sql`

O script inclui a migration da tabela existente (para ambientes com dados) e a criação nova (para ambientes limpos). Execute manualmente no banco antes do deploy.

- [ ] **Step 1: Criar audio_files.sql**

```sql
-- Migration: renomeia song_audio_files para audio_files
-- e adiciona suporte a medley_id
-- Executar manualmente antes do deploy

-- 1. Renomear tabela existente
ALTER TABLE song_audio_files RENAME TO audio_files;

-- 2. Renomear constraint e índice existentes
ALTER TABLE audio_files RENAME CONSTRAINT uq_song_audio_song_type TO uq_audio_files_song_type;
ALTER INDEX idx_song_audio_song_id RENAME TO idx_audio_files_song_id;

-- 3. Tornar song_id nullable (agora medley pode ser o owner)
ALTER TABLE audio_files ALTER COLUMN song_id DROP NOT NULL;

-- 4. Adicionar coluna medley_id
ALTER TABLE audio_files ADD COLUMN medley_id UUID REFERENCES medleys(id);

-- 5. CHECK: exatamente um dos dois FKs deve ser preenchido
ALTER TABLE audio_files ADD CONSTRAINT chk_audio_files_owner
    CHECK (
        (song_id IS NOT NULL AND medley_id IS NULL) OR
        (song_id IS NULL AND medley_id IS NOT NULL)
    );

-- 6. Substituir unique constraint de song por índice parcial
ALTER TABLE audio_files DROP CONSTRAINT uq_audio_files_song_type;
CREATE UNIQUE INDEX uq_audio_files_song_type
    ON audio_files(song_id, type)
    WHERE song_id IS NOT NULL;

-- 7. Índice parcial para medley
CREATE UNIQUE INDEX uq_audio_files_medley_type
    ON audio_files(medley_id, type)
    WHERE medley_id IS NOT NULL;

CREATE INDEX idx_audio_files_medley_id
    ON audio_files(medley_id)
    WHERE medley_id IS NOT NULL;

-- ============================================================
-- DDL completo para criação do zero (ambientes limpos)
-- ============================================================
-- CREATE TABLE IF NOT EXISTS audio_files (
--     id         UUID PRIMARY KEY,
--     song_id    UUID REFERENCES songs(id),
--     medley_id  UUID REFERENCES medleys(id),
--     type       VARCHAR(20) NOT NULL,
--     audio_url  VARCHAR(500) NOT NULL,
--     created_at TIMESTAMP NOT NULL,
--     CONSTRAINT chk_audio_files_owner CHECK (
--         (song_id IS NOT NULL AND medley_id IS NULL) OR
--         (song_id IS NULL AND medley_id IS NOT NULL)
--     )
-- );
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_audio_files_song_type
--     ON audio_files(song_id, type) WHERE song_id IS NOT NULL;
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_audio_files_medley_type
--     ON audio_files(medley_id, type) WHERE medley_id IS NOT NULL;
-- CREATE INDEX IF NOT EXISTS idx_audio_files_song_id
--     ON audio_files(song_id) WHERE song_id IS NOT NULL;
-- CREATE INDEX IF NOT EXISTS idx_audio_files_medley_id
--     ON audio_files(medley_id) WHERE medley_id IS NOT NULL;
```

- [ ] **Step 2: Deletar DDL antigo**

```bash
git rm src/main/resources/db/song_audio_files.sql
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/audio_files.sql
git commit -m "chore: atualiza DDL de referência — song_audio_files → audio_files com suporte a medley_id"
```
