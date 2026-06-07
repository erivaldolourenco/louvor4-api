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
