package br.com.louvor4.api.services;

import br.com.louvor4.api.models.ProjectSkill;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectSkillRequestDTO;

import java.util.List;
import java.util.UUID;

public interface ProjectSkillService {
    ProjectSkill createSkill(UUID projectId, ProjectSkillRequestDTO dto);
    List<ProjectSkill> listByProject(UUID projectId);
    void deleteSkill(UUID skillId);
}
