package br.com.louvor4.louvor4api.repositories;

import br.com.louvor4.louvor4api.models.Permission;
import br.com.louvor4.louvor4api.models.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
     Permission findByDescription(String description);
}
