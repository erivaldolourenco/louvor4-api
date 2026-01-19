package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventSong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventSongRepository extends JpaRepository<EventSong, UUID> {
}
