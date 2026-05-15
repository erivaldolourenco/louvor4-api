package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.SkillIcon;
import br.com.louvor4.api.models.ProjectSkill;

import java.util.UUID;

public record ProjectSkillDTO(
        UUID id,
        String name,
        SkillIcon iconKey
) {
    public static ProjectSkillDTO fromEntity(ProjectSkill skill) {
        return new ProjectSkillDTO(skill.getId(), skill.getName(), skill.getIconKey());
    }
}
