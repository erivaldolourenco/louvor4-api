package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.MusicProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MusicProjectMemberRepository extends JpaRepository<MusicProjectMember, UUID> {
    boolean existsByMusicProject_IdAndUser_Id(UUID pojectId, UUID userId);
    List<MusicProjectMember> getMusicProjectMembersByUser_Id(UUID userId);
}
