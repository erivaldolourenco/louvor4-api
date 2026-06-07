package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.SongMapper;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.SongAudio;
import br.com.louvor4.api.repositories.SongAudioRepository;
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
    @Mock SongAudioRepository songAudioRepository;
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
        SongAudio audio = new SongAudio();
        audio.setAudioUrl("https://s3.example.com/audio.mp3");

        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(songAudioRepository.findBySong_IdAndType(songId, SongAudioType.REFERENCE))
                .thenReturn(Optional.of(audio));

        SongDTO result = service.get(songId);

        assertThat(result.referenceAudioUrl()).isEqualTo("https://s3.example.com/audio.mp3");
        assertThat(result.id()).isEqualTo(songId);
        assertThat(result.title()).isEqualTo("Hosana");
    }

    @Test
    void getShouldReturnNullReferenceAudioUrlWhenNoAudioExists() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(songAudioRepository.findBySong_IdAndType(songId, SongAudioType.REFERENCE))
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
