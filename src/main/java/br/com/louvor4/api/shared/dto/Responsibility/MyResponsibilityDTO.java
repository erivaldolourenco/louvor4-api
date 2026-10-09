package br.com.louvor4.api.shared.dto.Responsibility;

import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectResponsibility;
import br.com.louvor4.api.models.ProjectResponsibilityAssignment;

import java.time.LocalDate;
import java.util.UUID;

/** Responsabilidade do usuário logado (vigente ou agendada), para o card da tela de Início. */
public record MyResponsibilityDTO(
        UUID assignmentId,
        UUID responsibilityId,
        String responsibilityName,
        String responsibilityDescription,
        UUID projectId,
        String projectName,
        String projectImage,
        LocalDate startDate,
        LocalDate endDate,
        boolean current
) {
    public static MyResponsibilityDTO fromEntity(ProjectResponsibilityAssignment assignment, LocalDate today) {
        ProjectResponsibility responsibility = assignment.getResponsibility();
        MusicProject project = responsibility.getMusicProject();
        return new MyResponsibilityDTO(
                assignment.getId(),
                responsibility.getId(),
                responsibility.getName(),
                responsibility.getDescription(),
                project.getId(),
                project.getName(),
                project.getProfileImage(),
                assignment.getStartDate(),
                assignment.getEndDate(),
                assignment.contains(today)
        );
    }
}
