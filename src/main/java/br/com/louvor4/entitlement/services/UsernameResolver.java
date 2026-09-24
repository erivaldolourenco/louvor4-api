package br.com.louvor4.entitlement.services;

import java.util.Optional;
import java.util.UUID;

public interface UsernameResolver {

    Optional<String> findUsername(UUID userId);
}
