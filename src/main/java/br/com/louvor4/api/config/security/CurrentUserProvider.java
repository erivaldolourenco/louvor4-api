package br.com.louvor4.api.config.security;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Usuário não autenticado.");
        }

        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário logado não encontrado."));
    }
}
