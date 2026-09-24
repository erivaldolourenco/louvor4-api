package br.com.louvor4.api.config.security;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.entitlement.services.UsernameResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RepositoryUsernameResolver implements UsernameResolver {

    private final UserRepository userRepository;

    public RepositoryUsernameResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<String> findUsername(UUID userId) {
        return userRepository.findById(userId).map(User::getUsername);
    }
}
