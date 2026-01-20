package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SongRepository extends JpaRepository<Song, UUID> {
    List<Song> getSongByUser_Id(UUID userId);
    Optional<Song> getSongById(UUID songId);
}
