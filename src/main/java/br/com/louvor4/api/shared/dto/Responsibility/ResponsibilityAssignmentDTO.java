package br.com.louvor4.api.shared.dto.Responsibility;

import br.com.louvor4.api.models.ProjectResponsibilityAssignment;
import br.com.louvor4.api.models.User;

import java.time.LocalDate;
import java.util.UUID;

public record ResponsibilityAssignmentDTO(
        UUID id,
        UUID memberId,
        UUID userId,
        String firstName,
        String lastName,
        String profileImage,
        boolean accountDeleted,
        LocalDate startDate,
        LocalDate endDate
) {
    public static ResponsibilityAssignmentDTO fromEntity(ProjectResponsibilityAssignment assignment) {
        User user = assignment.getMember().getUser();
        return new ResponsibilityAssignmentDTO(
                assignment.getId(),
                assignment.getMember().getId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getProfileImage(),
                user.getDeletedAt() != null,
                assignment.getStartDate(),
                assignment.getEndDate()
        );
    }
}
