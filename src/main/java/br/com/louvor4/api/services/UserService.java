package br.com.louvor4.api.services;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.shared.dto.UserCreateDTO;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface UserService {
    User create(UserCreateDTO user);
    User getUserById(UUID idUser);
    User findByUsername(String username);
}
