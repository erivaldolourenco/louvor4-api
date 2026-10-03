package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.ProjectResponsibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectResponsibilityRepository extends JpaRepository<ProjectResponsibility, UUID> {

    List<ProjectResponsibility> findByMusicProject_IdOrderByPositionAscNameAsc(UUID projectId);

    Optional<ProjectResponsibility> findByIdAndMusicProject_Id(UUID id, UUID projectId);

    boolean existsByMusicProject_IdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByMusicProject_IdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    @Query("SELECT COALESCE(MAX(r.position), -1) FROM ProjectResponsibility r WHERE r.musicProject.id = :projectId")
    int findMaxPositionByProjectId(@Param("projectId") UUID projectId);
}
