package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.UserRole;
import br.com.louvor4.api.models.UserMinistry;
import br.com.louvor4.api.models.embeddedid.UserMinistryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserMinistryRepository extends JpaRepository<UserMinistry, UserMinistryId> {
    List<UserMinistry> findById_UserId(UUID userId);
    boolean existsById_UserIdAndId_MinistryId(UUID userId, UUID ministryId);
    boolean existsById_UserIdAndId_MinistryIdAndRole(UUID userId, UUID ministryId, UserRole role);

}
