package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MusicProjectMemberRepository extends JpaRepository<MusicProjectMember, UUID> {
    boolean existsByMusicProject_IdAndUser_Id(UUID pojectId, UUID userId);
    List<MusicProjectMember> getMusicProjectMembersByUser_Id(UUID userId);
    List<MusicProjectMember> getMusicProjectMembersByMusicProject_Id(UUID projectId);
    UUID user(User user);
}
