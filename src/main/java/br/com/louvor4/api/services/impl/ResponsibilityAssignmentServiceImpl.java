package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.ProjectResponsibility;
import br.com.louvor4.api.models.ProjectResponsibilityAssignment;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityAssignmentRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityRepository;
import br.com.louvor4.api.services.ResponsibilityAssignmentService;
import br.com.louvor4.api.shared.dto.Responsibility.MyResponsibilityDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class ResponsibilityAssignmentServiceImpl implements ResponsibilityAssignmentService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProjectResponsibilityRepository responsibilityRepository;
    private final ProjectResponsibilityAssignmentRepository assignmentRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final CurrentUserProvider currentUserProvider;

    public ResponsibilityAssignmentServiceImpl(ProjectResponsibilityRepository responsibilityRepository,
                                               ProjectResponsibilityAssignmentRepository assignmentRepository,
                                               MusicProjectMemberRepository musicProjectMemberRepository,
                                               CurrentUserProvider currentUserProvider) {
        this.responsibilityRepository = responsibilityRepository;
        this.assignmentRepository = assignmentRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponsibilityAssignmentDTO> listHistory(UUID projectId, UUID responsibilityId) {
        findResponsibilityOrThrow(projectId, responsibilityId);
        return assignmentRepository.findHistoryByResponsibilityId(responsibilityId).stream()
                .map(ResponsibilityAssignmentDTO::fromEntity)
                .toList();
    }

    /**
     * Define um responsável para o período informado (troca de responsável).
     * Um período anterior que avance sobre o novo é encerrado no dia anterior ao novo início,
     * preservando o histórico. Períodos que começam no mesmo dia ou depois do novo início
     * não são alterados: a troca é recusada para não apagar nada sem querer.
     */
    @Override
    @Transactional
    public ResponsibilityAssignmentDTO assign(UUID projectId, UUID responsibilityId, ResponsibilityAssignmentRequestDTO requestDto) {
        ProjectResponsibility responsibility = findResponsibilityOrThrow(projectId, responsibilityId);
        validatePeriod(requestDto.startDate(), requestDto.endDate());
        MusicProjectMember member = findActiveMemberOrThrow(projectId, requestDto.memberId());

        LocalDate start = requestDto.startDate();
        LocalDate end = requestDto.endDate();
        List<ProjectResponsibilityAssignment> overlapping = assignmentRepository
                .findHistoryByResponsibilityId(responsibilityId).stream()
                .filter(a -> a.overlaps(start, end))
                .toList();

        for (ProjectResponsibilityAssignment existing : overlapping) {
            if (!existing.getStartDate().isBefore(start)) {
                throw conflict(existing);
            }
        }
        for (ProjectResponsibilityAssignment existing : overlapping) {
            existing.setEndDate(start.minusDays(1));
        }
        assignmentRepository.saveAll(overlapping);

        ProjectResponsibilityAssignment assignment = new ProjectResponsibilityAssignment();
        assignment.setResponsibility(responsibility);
        assignment.setMember(member);
        assignment.setStartDate(start);
        assignment.setEndDate(end);
        return ResponsibilityAssignmentDTO.fromEntity(assignmentRepository.save(assignment));
    }

    /** Corrige um período existente (membro e/ou datas). Aqui não há ajuste automático: sobreposição é recusada. */
    @Override
    @Transactional
    public ResponsibilityAssignmentDTO update(UUID projectId, UUID responsibilityId, UUID assignmentId,
                                             ResponsibilityAssignmentRequestDTO requestDto) {
        ProjectResponsibilityAssignment assignment = findAssignmentOrThrow(projectId, responsibilityId, assignmentId);
        validatePeriod(requestDto.startDate(), requestDto.endDate());

        // Manter o mesmo membro é permitido mesmo que ele já tenha saído do projeto (correção de histórico);
        // trocar exige um membro ativo.
        if (!assignment.getMember().getId().equals(requestDto.memberId())) {
            assignment.setMember(findActiveMemberOrThrow(projectId, requestDto.memberId()));
        }

        assignmentRepository.findHistoryByResponsibilityId(responsibilityId).stream()
                .filter(a -> !a.getId().equals(assignmentId))
                .filter(a -> a.overlaps(requestDto.startDate(), requestDto.endDate()))
                .findFirst()
                .ifPresent(a -> {
                    throw conflict(a);
                });

        assignment.setStartDate(requestDto.startDate());
        assignment.setEndDate(requestDto.endDate());
        return ResponsibilityAssignmentDTO.fromEntity(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public void delete(UUID projectId, UUID responsibilityId, UUID assignmentId) {
        assignmentRepository.delete(findAssignmentOrThrow(projectId, responsibilityId, assignmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyResponsibilityDTO> listMine() {
        UUID userId = currentUserProvider.get().getId();
        LocalDate today = LocalDate.now();
        return assignmentRepository.findUpcomingByUserId(userId, ProjectMemberStatus.ACTIVE, today).stream()
                .map(a -> MyResponsibilityDTO.fromEntity(a, today))
                .toList();
    }

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new ValidationException("A data de fim não pode ser anterior à data de início.");
        }
    }

    private ValidationException conflict(ProjectResponsibilityAssignment existing) {
        String name = existing.getMember().getUser().getFirstName();
        return new ValidationException(String.format(
                "O período conflita com o de %s (%s a %s). Ajuste as datas ou edite esse período.",
                name, existing.getStartDate().format(DATE_FORMAT), existing.getEndDate().format(DATE_FORMAT)));
    }

    private ProjectResponsibility findResponsibilityOrThrow(UUID projectId, UUID responsibilityId) {
        return responsibilityRepository.findByIdAndMusicProject_Id(responsibilityId, projectId)
                .orElseThrow(() -> new ValidationException("Responsabilidade não encontrada."));
    }

    private ProjectResponsibilityAssignment findAssignmentOrThrow(UUID projectId, UUID responsibilityId, UUID assignmentId) {
        return assignmentRepository.findInProject(assignmentId, responsibilityId, projectId)
                .orElseThrow(() -> new ValidationException("Período não encontrado."));
    }

    // Membro de outro projeto recebe a mesma mensagem de "não encontrado", para não revelar que o id existe.
    private MusicProjectMember findActiveMemberOrThrow(UUID projectId, UUID memberId) {
        MusicProjectMember member = musicProjectMemberRepository.findById(memberId)
                .filter(m -> m.getMusicProject().getId().equals(projectId))
                .orElseThrow(() -> new ValidationException("Membro não encontrado."));
        if (member.getStatus() != ProjectMemberStatus.ACTIVE) {
            throw new ValidationException("Só é possível definir como responsável um membro ativo do projeto.");
        }
        return member;
    }
}
