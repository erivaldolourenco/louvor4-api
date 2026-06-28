package br.com.louvor4.entitlement.repositories;

import br.com.louvor4.entitlement.models.Entitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {

    Optional<Entitlement> findByKey(String key);
}
