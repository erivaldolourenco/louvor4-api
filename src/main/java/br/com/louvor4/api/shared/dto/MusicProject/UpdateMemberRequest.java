package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectPermission;

import java.util.Set;
import java.util.UUID;

// permissions null = não altera (versões antigas do app não enviam o campo).
public record UpdateMemberRequest(ProjectMemberRole projectRole,
                                  Set<UUID> skillIds,
                                  Set<ProjectPermission> permissions) {
}
