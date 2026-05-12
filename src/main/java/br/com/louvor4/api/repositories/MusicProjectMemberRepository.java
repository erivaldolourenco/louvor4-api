package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MusicProjectMemberRepository extends JpaRepository<MusicProjectMember, UUID> {
    boolean existsByMusicProject_IdAndUser_Id(UUID pojectId, UUID userId);
    List<MusicProjectMember> getMusicProjectMembersByUser_Id(UUID userId);
    List<MusicProjectMember> getMusicProjectMembersByMusicProject_Id(UUID projectId);
    UUID user(User user);
    Optional<MusicProjectMember> findByMusicProject_IdAndUser_Id(UUID projectId, UUID userId);
    Optional<MusicProjectMember> findById(UUID meberId);
    long countByUser_IdAndProjectRole(UUID userId, br.com.louvor4.api.enums.ProjectMemberRole projectRole);
    boolean existsByMusicProject_IdAndUser_IdAndStatus(UUID projectId, UUID userId, ProjectMemberStatus status);
    List<MusicProjectMember> findByUser_IdAndStatus(UUID userId, ProjectMemberStatus status);
    Optional<MusicProjectMember> findByMusicProject_IdAndUser_IdAndStatus(UUID projectId, UUID userId, ProjectMemberStatus status);
    List<MusicProjectMember> findByMusicProject_IdAndStatus(UUID projectId, ProjectMemberStatus status);
}
