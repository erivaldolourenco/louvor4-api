package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.UserCreateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User create(UserCreateDTO userDTO) {
        String encryptedPassword = new BCryptPasswordEncoder().encode(userDTO.getPassword());
        User userEntity = new User();
        userEntity.setEmail(userDTO.getEmail());
        userEntity.setPassword(userDTO.getPassword());
        userEntity.setFirstName(userDTO.getFirstName());
        userEntity.setLastName(userDTO.getLastName());
        userEntity.setUsername(userDTO.getUsername());
        userEntity.setPassword(encryptedPassword);
        return userRepository.save(userEntity);
    }

    @Override
    public User getUserById(UUID idUser) {
        return userRepository.findById(idUser)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Usuário com id '%s' não encontrado.", idUser)));
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Usuário com username '%s' não encontrado.", username)));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Usuário com e-mail '%s' não encontrado.", email)));
    }
}
