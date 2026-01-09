package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.ProjectSkill;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.ProjectSkillRepository;
import br.com.louvor4.api.services.ProjectSkillService;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectSkillRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectSkillServiceImpl implements ProjectSkillService {

    private final ProjectSkillRepository repository;
    private final MusicProjectRepository projectRepository;

    public ProjectSkillServiceImpl(ProjectSkillRepository repository, MusicProjectRepository projectRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
    }


    @Transactional
    public ProjectSkill createSkill(UUID projectId, ProjectSkillRequestDTO dto) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ValidationException("Projeto não encontrado."));

        ProjectSkill skill = new ProjectSkill();
        skill.setName(dto.name());
        skill.setMusicProject(project);

        return repository.save(skill);
    }

    public List<ProjectSkill> listByProject(UUID projectId) {
        return repository.findByMusicProject_Id(projectId);
    }

    @Transactional
    public void deleteSkill(UUID skillId) {
        repository.deleteById(skillId);
    }
}
