package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EmailVerificationToken;
import br.com.louvor4.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);
}
