package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectPermission;

import java.util.Set;

// Permissões do usuário logado no projeto, já resolvidas: admin/owner voltam com todas.
public record ProjectPermissionsResponseDTO(
        ProjectMemberRole role,
        boolean isProjectAdmin,
        Set<ProjectPermission> permissions
) {
}
