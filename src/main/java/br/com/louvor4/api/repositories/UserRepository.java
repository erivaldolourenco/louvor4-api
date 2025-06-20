package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findById(UUID idUser);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
