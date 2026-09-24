package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.AccountDeletionRequest;
import br.com.louvor4.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, UUID> {
    Optional<AccountDeletionRequest> findByToken(String token);

    @Modifying
    @Query("DELETE FROM AccountDeletionRequest r WHERE r.user = :user")
    void deleteByUser(@Param("user") User user);
}
