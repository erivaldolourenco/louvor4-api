package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.ProjectSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectSkillRepository extends JpaRepository<ProjectSkill, UUID> {
    Optional<ProjectSkill> findById(UUID id);
    List<ProjectSkill> findByMusicProject_Id(UUID projectId);
}
