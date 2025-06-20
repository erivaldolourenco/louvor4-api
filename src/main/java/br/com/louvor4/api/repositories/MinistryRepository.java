package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.Ministry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MinistryRepository  extends JpaRepository<Ministry, UUID> {
    Optional<Ministry> findById(UUID id);
}
