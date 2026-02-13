package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationMessageRepository extends JpaRepository<NotificationMessage, UUID> {
}
