package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.ProjectMemberRole;

import java.util.Set;
import java.util.UUID;

public record UpdateMemberRequest(ProjectMemberRole projectRole,
                                  Set<UUID> skillIds) {
}
