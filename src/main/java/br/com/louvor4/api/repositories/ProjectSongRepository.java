package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.ProjectSong;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectSongRepository extends JpaRepository<ProjectSong, UUID> {

    @EntityGraph(attributePaths = "categories")
    List<ProjectSong> findByMusicProject_IdAndDeletedAtIsNullOrderByTitleAsc(UUID projectId);

    Optional<ProjectSong> findByIdAndMusicProject_IdAndDeletedAtIsNull(UUID id, UUID projectId);
}
