package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.UserUnavailabilityProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserUnavailabilityProjectRepository extends JpaRepository<UserUnavailabilityProject, UUID> {
}
