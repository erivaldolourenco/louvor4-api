package br.com.louvor4.api.shared.dto.Responsibility;

import br.com.louvor4.api.models.ProjectResponsibility;

import java.util.UUID;

/**
 * Responsabilidade com o resumo do histórico já calculado para o mural:
 * {@code current} = período que contém hoje, {@code next} = próximo agendado,
 * {@code last} = último período já encerrado.
 */
public record ProjectResponsibilityDTO(
        UUID id,
        String name,
        String description,
        Integer position,
        ResponsibilityAssignmentDTO current,
        ResponsibilityAssignmentDTO next,
        ResponsibilityAssignmentDTO last
) {
    public static ProjectResponsibilityDTO fromEntity(ProjectResponsibility responsibility) {
        return fromEntity(responsibility, null, null, null);
    }

    public static ProjectResponsibilityDTO fromEntity(ProjectResponsibility responsibility,
                                                      ResponsibilityAssignmentDTO current,
                                                      ResponsibilityAssignmentDTO next,
                                                      ResponsibilityAssignmentDTO last) {
        return new ProjectResponsibilityDTO(
                responsibility.getId(),
                responsibility.getName(),
                responsibility.getDescription(),
                responsibility.getPosition(),
                current,
                next,
                last
        );
    }
}
