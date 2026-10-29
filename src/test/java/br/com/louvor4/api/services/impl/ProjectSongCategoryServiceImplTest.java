package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectSongCategory;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.ProjectSongCategoryRepository;
import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;
import br.com.louvor4.api.shared.dto.Song.SongCategoryRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSongCategoryServiceImplTest {

    @Mock ProjectSongCategoryRepository categoryRepository;
    @Mock MusicProjectRepository musicProjectRepository;

    @InjectMocks ProjectSongCategoryServiceImpl service;

    private UUID projectId;
    private MusicProject project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new MusicProject();
        project.setId(projectId);
    }

    @Test
    void create_salvaNoProjetoComNomeAparado() {
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SongCategoryDTO dto = service.create(projectId, new SongCategoryRequestDTO("  Adoração "));

        assertThat(dto.name()).isEqualTo("Adoração");
    }

    @Test
    void create_recusaNomeDuplicadoNoProjeto() {
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(categoryRepository.existsByMusicProject_IdAndNameIgnoreCase(projectId, "Adoração")).thenReturn(true);

        assertThatThrownBy(() -> service.create(projectId, new SongCategoryRequestDTO("Adoração")))
                .isInstanceOf(ValidationException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_recusaCategoriaDeOutroProjeto() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdAndMusicProject_Id(categoryId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(projectId, categoryId, new SongCategoryRequestDTO("Nova")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void delete_tiraDasMusicasAntesDeExcluir() {
        UUID categoryId = UUID.randomUUID();
        ProjectSongCategory category = new ProjectSongCategory();
        category.setId(categoryId);
        when(categoryRepository.findByIdAndMusicProject_Id(categoryId, projectId)).thenReturn(Optional.of(category));

        service.delete(projectId, categoryId);

        InOrder inOrder = inOrder(categoryRepository);
        inOrder.verify(categoryRepository).deleteAssignmentsByCategoryId(categoryId);
        inOrder.verify(categoryRepository).delete(category);
    }
}
