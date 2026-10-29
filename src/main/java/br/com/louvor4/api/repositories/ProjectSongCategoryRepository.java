package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.ProjectSongCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectSongCategoryRepository extends JpaRepository<ProjectSongCategory, UUID> {

    List<ProjectSongCategory> findByMusicProject_IdOrderByNameAsc(UUID projectId);

    List<ProjectSongCategory> findByIdInAndMusicProject_Id(Collection<UUID> ids, UUID projectId);

    Optional<ProjectSongCategory> findByIdAndMusicProject_Id(UUID id, UUID projectId);

    boolean existsByMusicProject_IdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByMusicProject_IdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    // A FK da tabela de associação não tem cascade: tira a categoria das músicas antes de excluí-la.
    @Modifying
    @Query(value = "DELETE FROM project_song_category_assignments WHERE project_song_category_id = :categoryId",
            nativeQuery = true)
    void deleteAssignmentsByCategoryId(@Param("categoryId") UUID categoryId);
}
