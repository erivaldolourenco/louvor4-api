package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.ProjectResponsibility;
import br.com.louvor4.api.models.ProjectResponsibilityAssignment;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityAssignmentRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityRepository;
import br.com.louvor4.api.shared.dto.Responsibility.MyResponsibilityDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponsibilityAssignmentServiceImplTest {

    @Mock ProjectResponsibilityRepository responsibilityRepository;
    @Mock ProjectResponsibilityAssignmentRepository assignmentRepository;
    @Mock MusicProjectMemberRepository musicProjectMemberRepository;
    @Mock CurrentUserProvider currentUserProvider;

    @InjectMocks ResponsibilityAssignmentServiceImpl service;

    private static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate OCT_31 = LocalDate.of(2026, 10, 31);
    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    private UUID projectId;
    private UUID responsibilityId;
    private MusicProject project;
    private ProjectResponsibility responsibility;
    private MusicProjectMember joao;
    private MusicProjectMember maria;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        responsibilityId = UUID.randomUUID();
        project = new MusicProject();
        project.setId(projectId);
        responsibility = new ProjectResponsibility();
        responsibility.setId(responsibilityId);
        responsibility.setName("Direção musical");
        responsibility.setMusicProject(project);
        joao = member("João", project, ProjectMemberStatus.ACTIVE);
        maria = member("Maria", project, ProjectMemberStatus.ACTIVE);
    }

    private MusicProjectMember member(String name, MusicProject memberProject, ProjectMemberStatus status) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName(name);
        MusicProjectMember member = new MusicProjectMember();
        member.setId(UUID.randomUUID());
        member.setUser(user);
        member.setMusicProject(memberProject);
        member.setStatus(status);
        return member;
    }

    private ProjectResponsibilityAssignment assignment(MusicProjectMember member, LocalDate start, LocalDate end) {
        ProjectResponsibilityAssignment a = new ProjectResponsibilityAssignment();
        a.setId(UUID.randomUUID());
        a.setResponsibility(responsibility);
        a.setMember(member);
        a.setStartDate(start);
        a.setEndDate(end);
        return a;
    }

    private void givenResponsibility() {
        when(responsibilityRepository.findByIdAndMusicProject_Id(responsibilityId, projectId))
                .thenReturn(Optional.of(responsibility));
    }

    private void givenMember(MusicProjectMember member) {
        when(musicProjectMemberRepository.findById(member.getId())).thenReturn(Optional.of(member));
    }

    // ---------- assign ----------

    @Test
    void assign_criaPeriodoSemHistoricoAnterior() {
        givenResponsibility();
        givenMember(joao);
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of());
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponsibilityAssignmentDTO dto = service.assign(projectId, responsibilityId,
                new ResponsibilityAssignmentRequestDTO(joao.getId(), OCT_1, OCT_31));

        assertThat(dto.memberId()).isEqualTo(joao.getId());
        assertThat(dto.firstName()).isEqualTo("João");
        assertThat(dto.startDate()).isEqualTo(OCT_1);
        assertThat(dto.endDate()).isEqualTo(OCT_31);
    }

    @Test
    void assign_trocaDeResponsavelEncerraPeriodoAnteriorNoDiaAnterior() {
        givenResponsibility();
        givenMember(maria);
        // João estava até o fim de dezembro; Maria assume em 01/11
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, OCT_1, LocalDate.of(2026, 12, 31));
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of(joaoPeriod));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assign(projectId, responsibilityId, new ResponsibilityAssignmentRequestDTO(maria.getId(), NOV_1, NOV_30));

        assertThat(joaoPeriod.getEndDate()).isEqualTo(OCT_31);
        verify(assignmentRepository).saveAll(List.of(joaoPeriod));
        ArgumentCaptor<ProjectResponsibilityAssignment> captor = ArgumentCaptor.forClass(ProjectResponsibilityAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isSameAs(maria);
    }

    @Test
    void assign_naoMexeEmPeriodoQueNaoSobrepoe() {
        givenResponsibility();
        givenMember(maria);
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, OCT_1, OCT_31);
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of(joaoPeriod));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.assign(projectId, responsibilityId, new ResponsibilityAssignmentRequestDTO(maria.getId(), NOV_1, NOV_30));

        assertThat(joaoPeriod.getEndDate()).isEqualTo(OCT_31);
        verify(assignmentRepository).saveAll(List.of());
    }

    @Test
    void assign_recusaQuandoPeriodoExistenteComecaNoMesmoDiaOuDepois() {
        givenResponsibility();
        givenMember(maria);
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, NOV_1, NOV_30);
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of(joaoPeriod));

        assertThatThrownBy(() -> service.assign(projectId, responsibilityId,
                new ResponsibilityAssignmentRequestDTO(maria.getId(), OCT_1, NOV_30)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("João")
                .hasMessageContaining("01/11/2026");
        // Nada foi alterado
        assertThat(joaoPeriod.getStartDate()).isEqualTo(NOV_1);
        verify(assignmentRepository, never()).saveAll(anyList());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void assign_recusaDataFimAntesDoInicio() {
        givenResponsibility();

        assertThatThrownBy(() -> service.assign(projectId, responsibilityId,
                new ResponsibilityAssignmentRequestDTO(joao.getId(), OCT_31, OCT_1)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("data de fim");
    }

    @Test
    void assign_aceitaPeriodoDeUmDia() {
        givenResponsibility();
        givenMember(joao);
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of());
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponsibilityAssignmentDTO dto = service.assign(projectId, responsibilityId,
                new ResponsibilityAssignmentRequestDTO(joao.getId(), OCT_1, OCT_1));

        assertThat(dto.startDate()).isEqualTo(dto.endDate());
    }

    @Test
    void assign_recusaMembroNaoAtivo() {
        givenResponsibility();
        MusicProjectMember pending = member("Pedro", project, ProjectMemberStatus.PENDING_INVITE);
        givenMember(pending);

        assertThatThrownBy(() -> service.assign(projectId, responsibilityId,
                new ResponsibilityAssignmentRequestDTO(pending.getId(), OCT_1, OCT_31)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("membro ativo");
    }

    @Test
    void assign_membroDeOutroProjetoNaoEncontrado() {
        givenResponsibility();
        MusicProject otherProject = new MusicProject();
        otherProject.setId(UUID.randomUUID());
        MusicProjectMember outsider = member("Ana", otherProject, ProjectMemberStatus.ACTIVE);
        givenMember(outsider);

        assertThatThrownBy(() -> service.assign(projectId, responsibilityId,
                new ResponsibilityAssignmentRequestDTO(outsider.getId(), OCT_1, OCT_31)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Membro não encontrado.");
    }

    @Test
    void assign_responsabilidadeDeOutroProjetoNaoEncontrada() {
        when(responsibilityRepository.findByIdAndMusicProject_Id(responsibilityId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(projectId, responsibilityId,
                new ResponsibilityAssignmentRequestDTO(joao.getId(), OCT_1, OCT_31)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Responsabilidade não encontrada.");
    }

    // ---------- update ----------

    @Test
    void update_ajustaDatasMantendoMembro() {
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, OCT_1, OCT_31);
        when(assignmentRepository.findInProject(joaoPeriod.getId(), responsibilityId, projectId)).thenReturn(Optional.of(joaoPeriod));
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of(joaoPeriod));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponsibilityAssignmentDTO dto = service.update(projectId, responsibilityId, joaoPeriod.getId(),
                new ResponsibilityAssignmentRequestDTO(joao.getId(), OCT_1, NOV_30));

        assertThat(dto.endDate()).isEqualTo(NOV_30);
        verify(musicProjectMemberRepository, never()).findById(any());
    }

    @Test
    void update_permiteManterMembroQueJaSaiuDoProjeto() {
        joao.setStatus(ProjectMemberStatus.REMOVED);
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, OCT_1, OCT_31);
        when(assignmentRepository.findInProject(joaoPeriod.getId(), responsibilityId, projectId)).thenReturn(Optional.of(joaoPeriod));
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of(joaoPeriod));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponsibilityAssignmentDTO dto = service.update(projectId, responsibilityId, joaoPeriod.getId(),
                new ResponsibilityAssignmentRequestDTO(joao.getId(), LocalDate.of(2026, 10, 5), OCT_31));

        assertThat(dto.startDate()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void update_trocaMembroExigeMembroAtivo() {
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, OCT_1, OCT_31);
        when(assignmentRepository.findInProject(joaoPeriod.getId(), responsibilityId, projectId)).thenReturn(Optional.of(joaoPeriod));
        givenMember(maria);
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId)).thenReturn(List.of(joaoPeriod));
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponsibilityAssignmentDTO dto = service.update(projectId, responsibilityId, joaoPeriod.getId(),
                new ResponsibilityAssignmentRequestDTO(maria.getId(), OCT_1, OCT_31));

        assertThat(dto.memberId()).isEqualTo(maria.getId());
    }

    @Test
    void update_recusaSobreposicaoComOutroPeriodo() {
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, OCT_1, OCT_31);
        ProjectResponsibilityAssignment mariaPeriod = assignment(maria, NOV_1, NOV_30);
        when(assignmentRepository.findInProject(joaoPeriod.getId(), responsibilityId, projectId)).thenReturn(Optional.of(joaoPeriod));
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId))
                .thenReturn(new ArrayList<>(List.of(mariaPeriod, joaoPeriod)));

        assertThatThrownBy(() -> service.update(projectId, responsibilityId, joaoPeriod.getId(),
                new ResponsibilityAssignmentRequestDTO(joao.getId(), OCT_1, LocalDate.of(2026, 11, 10))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Maria");
        assertThat(joaoPeriod.getEndDate()).isEqualTo(OCT_31);
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void update_periodoDeOutraResponsabilidadeNaoEncontrado() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findInProject(assignmentId, responsibilityId, projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(projectId, responsibilityId, assignmentId,
                new ResponsibilityAssignmentRequestDTO(joao.getId(), OCT_1, OCT_31)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Período não encontrado.");
    }

    // ---------- delete / history / mine ----------

    @Test
    void delete_removePeriodoDoProjeto() {
        ProjectResponsibilityAssignment joaoPeriod = assignment(joao, OCT_1, OCT_31);
        when(assignmentRepository.findInProject(joaoPeriod.getId(), responsibilityId, projectId)).thenReturn(Optional.of(joaoPeriod));

        service.delete(projectId, responsibilityId, joaoPeriod.getId());

        verify(assignmentRepository).delete(joaoPeriod);
    }

    @Test
    void listHistory_validaResponsabilidadeERetornaPeriodos() {
        givenResponsibility();
        when(assignmentRepository.findHistoryByResponsibilityId(responsibilityId))
                .thenReturn(List.of(assignment(maria, NOV_1, NOV_30), assignment(joao, OCT_1, OCT_31)));

        List<ResponsibilityAssignmentDTO> history = service.listHistory(projectId, responsibilityId);

        assertThat(history).extracting(ResponsibilityAssignmentDTO::firstName).containsExactly("Maria", "João");
    }

    @Test
    void listMine_marcaPeriodoVigenteComoAtual() {
        LocalDate today = LocalDate.now();
        User me = joao.getUser();
        when(currentUserProvider.get()).thenReturn(me);
        project.setName("Louvor Central");
        ProjectResponsibilityAssignment currentPeriod = assignment(joao, today.minusDays(3), today.plusDays(3));
        ProjectResponsibilityAssignment futurePeriod = assignment(joao, today.plusDays(10), today.plusDays(20));
        when(assignmentRepository.findUpcomingByUserId(me.getId(), ProjectMemberStatus.ACTIVE, today))
                .thenReturn(List.of(currentPeriod, futurePeriod));

        List<MyResponsibilityDTO> mine = service.listMine();

        assertThat(mine).extracting(MyResponsibilityDTO::current).containsExactly(true, false);
        assertThat(mine.get(0).projectName()).isEqualTo("Louvor Central");
        assertThat(mine.get(0).responsibilityName()).isEqualTo("Direção musical");
    }
}
