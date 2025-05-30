package br.com.louvor4.api.services;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.shared.dto.CreateUserDTO;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    User create(CreateUserDTO user);
}
