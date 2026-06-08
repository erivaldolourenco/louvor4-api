package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.models.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, UUID> {
    Optional<AudioFile> findBySong_IdAndType(UUID songId, AudioType type);
    Optional<AudioFile> findByMedley_IdAndType(UUID medleyId, AudioType type);
    List<AudioFile> findBySong_IdInAndType(Collection<UUID> songIds, AudioType type);
}
