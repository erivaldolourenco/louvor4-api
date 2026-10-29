package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.enums.MusicProjectType;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.AudioFile;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectSong;
import br.com.louvor4.api.models.ProjectSongCategory;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.AudioFileRepository;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.ProjectSongCategoryRepository;
import br.com.louvor4.api.repositories.ProjectSongRepository;
import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongDTO;
import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongRequestDTO;
import br.com.louvor4.api.validations.ChordSheetValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSongServiceImplTest {

    @Mock ProjectSongRepository projectSongRepository;
    @Mock MusicProjectRepository musicProjectRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock ChordSheetValidation chordSheetValidation;
    @Mock ProjectSongCategoryRepository categoryRepository;
    @Mock AudioFileRepository audioFileRepository;

    @InjectMocks ProjectSongServiceImpl service;

    private UUID projectId;
    private MusicProject project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new MusicProject();
        project.setId(projectId);
        project.setType(MusicProjectType.MINISTRY);
    }

    private ProjectSongRequestDTO request(String title, String notes) {
        return new ProjectSongRequestDTO(title, "  Aline Barros ", "G", 72,
                null, "  ", null, null, null, null, notes);
    }

    private ProjectSong song(UUID id) {
        ProjectSong song = new ProjectSong();
        song.setId(id);
        song.setMusicProject(project);
        song.setTitle("Ressuscita-me");
        song.setArtist("Aline Barros");
        song.setKey("G");
        return song;
    }

    @Test
    void create_salvaNoProjetoComQuemCriouECamposAparados() {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(currentUserProvider.get()).thenReturn(user);
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectSongRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectSongDTO dto = service.create(projectId, request("  Ressuscita-me ", "   "));

        ArgumentCaptor<ProjectSong> captor = ArgumentCaptor.forClass(ProjectSong.class);
        verify(projectSongRepository).save(captor.capture());
        ProjectSong saved = captor.getValue();
        assertThat(saved.getMusicProject()).isSameAs(project);
        assertThat(saved.getCreatedByUserId()).isEqualTo(user.getId());
        assertThat(saved.getTitle()).isEqualTo("Ressuscita-me");
        assertThat(saved.getArtist()).isEqualTo("Aline Barros");
        assertThat(saved.getAlbum()).isNull();
        assertThat(saved.getNotes()).isNull();
        assertThat(dto.projectId()).isEqualTo(projectId);
        assertThat(dto.hasChordSheet()).isFalse();
    }

    @Test
    void create_recusaProjetoDeMidia() {
        project.setType(MusicProjectType.MEDIA);
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.create(projectId, request("Ressuscita-me", null)))
                .isInstanceOf(ValidationException.class);
        verify(projectSongRepository, never()).save(any());
    }

    @Test
    void update_recusaMusicaDeOutroProjetoOuExcluida() {
        UUID songId = UUID.randomUUID();
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(projectId, songId, request("Ressuscita-me", null)))
                .isInstanceOf(ValidationException.class);
        verify(projectSongRepository, never()).save(any());
    }

    @Test
    void update_alteraOsDados() {
        UUID songId = UUID.randomUUID();
        ProjectSong existing = song(songId);
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(existing));
        when(projectSongRepository.save(existing)).thenReturn(existing);

        ProjectSongDTO dto = service.update(projectId, songId, request("Novo título", "Entrada suave"));

        assertThat(dto.title()).isEqualTo("Novo título");
        assertThat(dto.notes()).isEqualTo("Entrada suave");
        assertThat(dto.bpm()).isEqualTo(72);
    }

    @Test
    void delete_marcaComoExcluidaSemApagar() {
        UUID songId = UUID.randomUUID();
        ProjectSong existing = song(songId);
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(existing));

        service.delete(projectId, songId);

        assertThat(existing.isDeleted()).isTrue();
        verify(projectSongRepository).save(existing);
        verify(projectSongRepository, never()).delete(any());
    }

    @Test
    void updateChordSheet_validaESalva() {
        UUID songId = UUID.randomUUID();
        ProjectSong existing = song(songId);
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(existing));
        when(projectSongRepository.save(existing)).thenReturn(existing);

        service.updateChordSheet(projectId, songId, "{\"schemaVersion\":1}");

        verify(chordSheetValidation).validate("{\"schemaVersion\":1}");
        assertThat(existing.getChordSheetJson()).isEqualTo("{\"schemaVersion\":1}");
    }

    @Test
    void updateChordSheet_naoSalvaCifraInvalida() {
        UUID songId = UUID.randomUUID();
        ProjectSong existing = song(songId);
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(existing));
        doThrow(new ValidationException("Cifra inválida.")).when(chordSheetValidation).validate("ruim");

        assertThatThrownBy(() -> service.updateChordSheet(projectId, songId, "ruim"))
                .isInstanceOf(ValidationException.class);
        assertThat(existing.getChordSheetJson()).isNull();
        verify(projectSongRepository, never()).save(any());
    }

    @Test
    void assignCategories_substituiPelasCategoriasDoProjeto() {
        UUID songId = UUID.randomUUID();
        ProjectSong existing = song(songId);
        ProjectSongCategory category = new ProjectSongCategory();
        category.setId(UUID.randomUUID());
        category.setName("Adoração");
        category.setMusicProject(project);
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdInAndMusicProject_Id(Set.of(category.getId()), projectId))
                .thenReturn(List.of(category));
        when(projectSongRepository.save(existing)).thenReturn(existing);

        ProjectSongDTO dto = service.assignCategories(projectId, songId, Set.of(category.getId()));

        assertThat(existing.getCategories()).containsExactly(category);
        assertThat(dto.categories()).extracting("name").containsExactly("Adoração");
    }

    @Test
    void assignCategories_recusaCategoriaDeOutroProjeto() {
        UUID songId = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(song(songId)));
        when(categoryRepository.findByIdInAndMusicProject_Id(Set.of(foreign), projectId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.assignCategories(projectId, songId, Set.of(foreign)))
                .isInstanceOf(ValidationException.class);
        verify(projectSongRepository, never()).save(any());
    }

    @Test
    void assignCategories_listaVaziaLimpaAsCategorias() {
        UUID songId = UUID.randomUUID();
        ProjectSong existing = song(songId);
        existing.getCategories().add(new ProjectSongCategory());
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(existing));
        when(projectSongRepository.save(existing)).thenReturn(existing);

        service.assignCategories(projectId, songId, Set.of());

        assertThat(existing.getCategories()).isEmpty();
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void listByProject_includesReferenceAndVsAudios() {
        ProjectSong song = new ProjectSong();
        song.setId(UUID.randomUUID());
        song.setMusicProject(project);
        song.setTitle("Grande é o Senhor");
        song.setArtist("Adhemar");
        song.setKey("G");

        AudioFile reference = new AudioFile();
        reference.setProjectSong(song);
        reference.setType(AudioType.REFERENCE);
        reference.setAudioUrl("https://s3/ref.mp3");
        AudioFile vs = new AudioFile();
        vs.setProjectSong(song);
        vs.setType(AudioType.VS);
        vs.setAudioUrl("https://s3/vs.mp3");

        when(projectSongRepository.findByMusicProject_IdAndDeletedAtIsNullOrderByTitleAsc(projectId))
                .thenReturn(List.of(song));
        when(audioFileRepository.findByProjectSong_IdIn(List.of(song.getId()))).thenReturn(List.of(reference, vs));

        List<ProjectSongDTO> result = service.listByProject(projectId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).referenceAudioUrl()).isEqualTo("https://s3/ref.mp3");
        assertThat(result.get(0).vsAudioUrl()).isEqualTo("https://s3/vs.mp3");
    }
}
