package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.Medley;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedleyRepository extends JpaRepository<Medley, UUID> {
    List<Medley> findByUser_Id(UUID userId);
}
