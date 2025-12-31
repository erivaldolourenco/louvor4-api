package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.MusicProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MusicProjectRepository extends JpaRepository<MusicProject, UUID> {
        MusicProject getMusicProjectById(UUID projectID);
}
