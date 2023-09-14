package br.com.louvor4.louvor4api.repositories;

import br.com.louvor4.louvor4api.models.MinistryPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinistryPermissionRepository extends JpaRepository<MinistryPermission, Long> {
    MinistryPermission getMinistryPermissionByDescription(String description);
}
