package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.models.ProjectSkill;

import java.util.UUID;

/**
 * DTO para representar uma função (skill) dentro de um projeto.
 * Utilizado para listagens e para popular os componentes de seleção no Angular.
 */
public record ProjectSkillDTO(
        UUID id,
        String name
) {
    // Você também pode adicionar um construtor de conveniência
    // para converter da Entidade para o DTO
    public static ProjectSkillDTO fromEntity(ProjectSkill skill) {
        return new ProjectSkillDTO(skill.getId(), skill.getName());
    }
}
