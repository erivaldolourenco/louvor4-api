package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectResponsibility;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityAssignmentRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityRepository;
import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectResponsibilityServiceImplTest {

    @Mock ProjectResponsibilityRepository responsibilityRepository;
    @Mock MusicProjectRepository musicProjectRepository;
    @Mock ProjectResponsibilityAssignmentRepository assignmentRepository;

    @InjectMocks ProjectResponsibilityServiceImpl service;

    private UUID projectId;
    private MusicProject project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        project = new MusicProject();
        project.setId(projectId);
    }

    private ProjectResponsibility responsibility(UUID id, String name) {
        ProjectResponsibility r = new ProjectResponsibility();
        r.setId(id);
        r.setName(name);
        r.setMusicProject(project);
        return r;
    }

    @Test
    void create_salvaComNomeAparadoDescricaoNulaEPosicaoNoFim() {
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(responsibilityRepository.existsByMusicProject_IdAndNameIgnoreCase(projectId, "Direção musical")).thenReturn(false);
        when(responsibilityRepository.findMaxPositionByProjectId(projectId)).thenReturn(2);
        when(responsibilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponsibilityDTO dto = service.create(projectId,
                new ProjectResponsibilityRequestDTO("  Direção musical  ", "   "));

        ArgumentCaptor<ProjectResponsibility> captor = ArgumentCaptor.forClass(ProjectResponsibility.class);
        verify(responsibilityRepository).save(captor.capture());
        ProjectResponsibility saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Direção musical");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getPosition()).isEqualTo(3);
        assertThat(saved.getMusicProject()).isSameAs(project);
        assertThat(dto.name()).isEqualTo("Direção musical");
    }

    @Test
    void create_primeiraResponsabilidadeFicaNaPosicaoZero() {
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(responsibilityRepository.findMaxPositionByProjectId(projectId)).thenReturn(-1);
        when(responsibilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponsibilityDTO dto = service.create(projectId, new ProjectResponsibilityRequestDTO("Som", null));

        assertThat(dto.position()).isZero();
    }

    @Test
    void create_recusaNomeDuplicadoNoProjeto() {
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(responsibilityRepository.existsByMusicProject_IdAndNameIgnoreCase(projectId, "Som")).thenReturn(true);

        assertThatThrownBy(() -> service.create(projectId, new ProjectResponsibilityRequestDTO("Som", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("já tem uma responsabilidade");
        verify(responsibilityRepository, never()).save(any());
    }

    @Test
    void create_recusaProjetoInexistente() {
        when(musicProjectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(projectId, new ProjectResponsibilityRequestDTO("Som", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Projeto não encontrado.");
    }

    @Test
    void update_alteraNomeEDescricao() {
        UUID id = UUID.randomUUID();
        ProjectResponsibility existing = responsibility(id, "Som");
        when(responsibilityRepository.findByIdAndMusicProject_Id(id, projectId)).thenReturn(Optional.of(existing));
        when(responsibilityRepository.existsByMusicProject_IdAndNameIgnoreCaseAndIdNot(projectId, "Mesa de som", id)).thenReturn(false);
        when(responsibilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectResponsibilityDTO dto = service.update(projectId, id,
                new ProjectResponsibilityRequestDTO("Mesa de som", " Operar a mesa nos cultos "));

        assertThat(dto.name()).isEqualTo("Mesa de som");
        assertThat(dto.description()).isEqualTo("Operar a mesa nos cultos");
    }

    @Test
    void update_recusaNomeDeOutraResponsabilidade() {
        UUID id = UUID.randomUUID();
        when(responsibilityRepository.findByIdAndMusicProject_Id(id, projectId))
                .thenReturn(Optional.of(responsibility(id, "Som")));
        when(responsibilityRepository.existsByMusicProject_IdAndNameIgnoreCaseAndIdNot(projectId, "Mídia", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(projectId, id, new ProjectResponsibilityRequestDTO("Mídia", null)))
                .isInstanceOf(ValidationException.class);
        verify(responsibilityRepository, never()).save(any());
    }

    @Test
    void update_responsabilidadeDeOutroProjetoNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(responsibilityRepository.findByIdAndMusicProject_Id(id, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(projectId, id, new ProjectResponsibilityRequestDTO("Som", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Responsabilidade não encontrada.");
    }

    @Test
    void delete_removeQuandoPertenceAoProjeto() {
        UUID id = UUID.randomUUID();
        ProjectResponsibility existing = responsibility(id, "Som");
        when(responsibilityRepository.findByIdAndMusicProject_Id(id, projectId)).thenReturn(Optional.of(existing));

        service.delete(projectId, id);

        verify(assignmentRepository).deleteByResponsibilityId(id);
        verify(responsibilityRepository).delete(existing);
    }

    @Test
    void delete_responsabilidadeDeOutroProjetoNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(responsibilityRepository.findByIdAndMusicProject_Id(id, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(projectId, id))
                .isInstanceOf(ValidationException.class);
        verify(responsibilityRepository, never()).delete(any());
    }

    @Test
    void listByProject_retornaNaOrdemDoRepositorio() {
        when(responsibilityRepository.findByMusicProject_IdOrderByPositionAscNameAsc(projectId))
                .thenReturn(List.of(responsibility(UUID.randomUUID(), "Direção musical"),
                        responsibility(UUID.randomUUID(), "Som")));

        List<ProjectResponsibilityDTO> result = service.listByProject(projectId);

        assertThat(result).extracting(ProjectResponsibilityDTO::name).containsExactly("Direção musical", "Som");
    }
}
