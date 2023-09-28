package br.com.louvor4.louvor4api.repositories;

import br.com.louvor4.louvor4api.models.AppPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<AppPermission, Long> {
     AppPermission findByDescription(String description);
}
