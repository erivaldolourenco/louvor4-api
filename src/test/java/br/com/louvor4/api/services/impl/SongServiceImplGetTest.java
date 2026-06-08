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
