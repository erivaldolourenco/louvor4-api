package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.SongCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SongCategoryRepository extends JpaRepository<SongCategory, UUID> {
    List<SongCategory> findByUser_IdOrderByNameAsc(UUID userId);
    boolean existsByIdAndUser_Id(UUID id, UUID userId);
    boolean existsByUser_IdAndNameIgnoreCase(UUID userId, String name);
    boolean existsByUser_IdAndNameIgnoreCaseAndIdNot(UUID userId, String name, UUID id);

    // As FKs das tabelas de associação não têm cascade: tira a categoria das músicas e
    // dos medleys antes de excluí-la.
    @Modifying
    @Query(value = "DELETE FROM song_category_assignments WHERE song_category_id = :categoryId",
            nativeQuery = true)
    void deleteSongAssignmentsByCategoryId(@Param("categoryId") UUID categoryId);

    @Modifying
    @Query(value = "DELETE FROM medley_category_assignments WHERE song_category_id = :categoryId",
            nativeQuery = true)
    void deleteMedleyAssignmentsByCategoryId(@Param("categoryId") UUID categoryId);
}
