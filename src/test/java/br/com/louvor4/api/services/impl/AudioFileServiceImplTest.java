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
