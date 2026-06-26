package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
    long countByUser_IdAndProjectRoleAndStatus(UUID userId, br.com.louvor4.api.enums.ProjectMemberRole projectRole, ProjectMemberStatus status);
    boolean existsByMusicProject_IdAndUser_IdAndStatus(UUID projectId, UUID userId, ProjectMemberStatus status);
    List<MusicProjectMember> findByUser_IdAndStatus(UUID userId, ProjectMemberStatus status);
    Optional<MusicProjectMember> findByMusicProject_IdAndUser_IdAndStatus(UUID projectId, UUID userId, ProjectMemberStatus status);
    List<MusicProjectMember> findByMusicProject_IdAndStatus(UUID projectId, ProjectMemberStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE MusicProjectMember m SET m.status = br.com.louvor4.api.enums.ProjectMemberStatus.REMOVED WHERE m.musicProject.id = :projectId AND m.status <> br.com.louvor4.api.enums.ProjectMemberStatus.REMOVED")
    void markAllRemovedByProjectId(@Param("projectId") UUID projectId);
}
