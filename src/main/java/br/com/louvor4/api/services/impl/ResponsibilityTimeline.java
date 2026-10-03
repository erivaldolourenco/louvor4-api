package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.ProjectResponsibilityAssignment;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentDTO;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Resume o histórico de uma responsabilidade em relação a uma data:
 * período atual (contém a data), próximo (começa depois) e último (já encerrado).
 */
record ResponsibilityTimeline(
        ResponsibilityAssignmentDTO current,
        ResponsibilityAssignmentDTO next,
        ResponsibilityAssignmentDTO last
) {
    static final ResponsibilityTimeline EMPTY = new ResponsibilityTimeline(null, null, null);

    static ResponsibilityTimeline of(List<ProjectResponsibilityAssignment> assignments, LocalDate today) {
        if (assignments == null || assignments.isEmpty()) {
            return EMPTY;
        }
        Optional<ProjectResponsibilityAssignment> current = assignments.stream()
                .filter(a -> a.contains(today))
                .findFirst();
        Optional<ProjectResponsibilityAssignment> next = assignments.stream()
                .filter(a -> a.getStartDate().isAfter(today))
                .min(Comparator.comparing(ProjectResponsibilityAssignment::getStartDate));
        Optional<ProjectResponsibilityAssignment> last = assignments.stream()
                .filter(a -> a.getEndDate().isBefore(today))
                .max(Comparator.comparing(ProjectResponsibilityAssignment::getEndDate));

        return new ResponsibilityTimeline(
                current.map(ResponsibilityAssignmentDTO::fromEntity).orElse(null),
                next.map(ResponsibilityAssignmentDTO::fromEntity).orElse(null),
                last.map(ResponsibilityAssignmentDTO::fromEntity).orElse(null)
        );
    }
}
