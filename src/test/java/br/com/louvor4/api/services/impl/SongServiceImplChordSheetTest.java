package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.SongRepository;
import br.com.louvor4.api.shared.dto.Song.ChordSheetDTO;
import br.com.louvor4.api.validations.ChordSheetValidation;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongServiceImplChordSheetTest {

    @Mock SongRepository songRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock ChordSheetValidation chordSheetValidation;

    @InjectMocks SongServiceImpl service;

    private UUID songId;
    private UUID ownerId;
    private Song song;
    private User owner;

    private static final String VALID_CHORD_SHEET_JSON =
            "{\"schemaVersion\":1,\"sections\":[{\"name\":\"Verso 1\",\"lines\":[" +
                    "{\"text\":\"Deus e bom\",\"chords\":[{\"position\":0,\"chord\":\"G\"}]}]}]}";

    @BeforeEach
    void setUp() {
        songId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        owner = new User();
        owner.setId(ownerId);

        song = new Song();
        song.setId(songId);
        song.setTitle("Hosana");
        song.setArtist("Hillsong");
        song.setKey("G");
        song.setYouTubeUrl("https://youtube.com/watch?v=abc");
        song.setUser(owner);
    }

    @Test
    void getChordSheetShouldReturnStoredJson() {
        song.setChordSheetJson(VALID_CHORD_SHEET_JSON);
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));

        ChordSheetDTO result = service.getChordSheet(songId);

        assertThat(result.songId()).isEqualTo(songId);
        assertThat(result.chordSheetJson()).isEqualTo(VALID_CHORD_SHEET_JSON);
    }

    @Test
    void getChordSheetShouldThrowWhenSongNotFound() {
        when(songRepository.getSongById(songId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getChordSheet(songId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("não encontrada");
    }

    @Test
    void updateChordSheetShouldPersistWhenOwnerAndValid() {
        when(currentUserProvider.get()).thenReturn(owner);
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(songRepository.save(song)).thenReturn(song);

        ChordSheetDTO result = service.updateChordSheet(songId, VALID_CHORD_SHEET_JSON);

        verify(chordSheetValidation).validate(VALID_CHORD_SHEET_JSON);
        assertThat(result.chordSheetJson()).isEqualTo(VALID_CHORD_SHEET_JSON);
        assertThat(song.getChordSheetJson()).isEqualTo(VALID_CHORD_SHEET_JSON);
    }

    @Test
    void updateChordSheetShouldThrowWhenNotOwner() {
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        when(currentUserProvider.get()).thenReturn(otherUser);
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));

        assertThatThrownBy(() -> service.updateChordSheet(songId, VALID_CHORD_SHEET_JSON))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("permissão");

        verify(chordSheetValidation, never()).validate(anyString());
    }

    @Test
    void updateChordSheetShouldPropagateValidationFailure() {
        when(currentUserProvider.get()).thenReturn(owner);
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        doThrow(new ValidationException("JSON de cifra mal formado."))
                .when(chordSheetValidation).validate("invalid-json");

        assertThatThrownBy(() -> service.updateChordSheet(songId, "invalid-json"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("mal formado");

        verify(songRepository, never()).save(any());
    }

    @Test
    void deleteChordSheetShouldClearFieldWhenOwner() {
        song.setChordSheetJson(VALID_CHORD_SHEET_JSON);
        when(currentUserProvider.get()).thenReturn(owner);
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));

        service.deleteChordSheet(songId);

        assertThat(song.getChordSheetJson()).isNull();
        verify(songRepository).save(song);
    }

    @Test
    void deleteChordSheetShouldThrowWhenNotOwner() {
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        when(currentUserProvider.get()).thenReturn(otherUser);
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));

        assertThatThrownBy(() -> service.deleteChordSheet(songId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("permissão");

        verify(songRepository, never()).save(any());
    }

    @Test
    void importChordSheetShouldDelegateToUpdateChordSheet() {
        when(currentUserProvider.get()).thenReturn(owner);
        when(songRepository.getSongById(songId)).thenReturn(Optional.of(song));
        when(songRepository.save(song)).thenReturn(song);

        ChordSheetDTO result = service.importChordSheet(songId, VALID_CHORD_SHEET_JSON);

        verify(chordSheetValidation).validate(VALID_CHORD_SHEET_JSON);
        assertThat(result.chordSheetJson()).isEqualTo(VALID_CHORD_SHEET_JSON);
    }
}
