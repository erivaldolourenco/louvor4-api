package br.com.louvor4.entitlement.repositories;

import br.com.louvor4.entitlement.models.Plans;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlansRepository extends JpaRepository<Plans, UUID> {

    Optional<Plans> findByName(String name);
}
