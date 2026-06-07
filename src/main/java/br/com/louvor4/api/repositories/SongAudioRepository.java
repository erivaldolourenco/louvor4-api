package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.models.SongAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SongAudioRepository extends JpaRepository<SongAudio, UUID> {
    Optional<SongAudio> findBySong_IdAndType(UUID songId, SongAudioType type);
    List<SongAudio> findBySong_IdInAndType(Collection<UUID> songIds, SongAudioType type);
}
