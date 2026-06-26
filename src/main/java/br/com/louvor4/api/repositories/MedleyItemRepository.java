package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.MedleyItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MedleyItemRepository extends JpaRepository<MedleyItem, UUID> {
    boolean existsBySong_Id(UUID songId);
}
