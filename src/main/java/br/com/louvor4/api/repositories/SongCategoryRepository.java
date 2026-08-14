package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.SongCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SongCategoryRepository extends JpaRepository<SongCategory, UUID> {
    List<SongCategory> findByUser_IdOrderByNameAsc(UUID userId);
    boolean existsByIdAndUser_Id(UUID id, UUID userId);
    boolean existsByUser_IdAndNameIgnoreCase(UUID userId, String name);
    boolean existsByUser_IdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);
}
