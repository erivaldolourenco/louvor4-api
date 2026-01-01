package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import br.com.louvor4.api.shared.dto.User.UserCreateDTO;
import br.com.louvor4.api.shared.dto.User.UserDetailDTO;
import br.com.louvor4.api.shared.dto.User.UserUpdateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static br.com.louvor4.api.shared.util.ObjectUtils.isNotNull;
import static br.com.louvor4.api.shared.util.ObjectUtils.isNotNullOrEmpty;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MusicProjectService musicProjectService;
    private final SongService songService;
    private final CurrentUserProvider currentUserProvider;
    private final StorageService storageService;

    public UserServiceImpl(UserRepository userRepository, MusicProjectService musicProjectService, SongService songService, CurrentUserProvider currentUserProvider, StorageService storageService) {
        this.userRepository = userRepository;
        this.musicProjectService = musicProjectService;
        this.songService = songService;
        this.currentUserProvider = currentUserProvider;
        this.storageService = storageService;
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
    public UserDetailDTO update(UserUpdateDTO updateDto) {
        User user = currentUserProvider.get();

        if (isNotNull(updateDto.firstName())) {
            user.setFirstName(updateDto.firstName().trim());
        }
        if (isNotNull(updateDto.lastName())) {
            user.setLastName(updateDto.lastName().trim());
        }
        if (isNotNull(updateDto.email())) {
            user.setEmail(updateDto.email().trim());
        }
        if (isNotNull(updateDto.phoneNumber())) {
            user.setPhoneNumber(updateDto.phoneNumber().trim());
        }

        User saved = userRepository.save(user);
        return toDetailDto(saved);
    }

    @Override
    public String updateImage(MultipartFile profileImage) {
        User user = currentUserProvider.get();
        if (isNotNullOrEmpty(profileImage)) {
            String fileUrl = storageService.uploadFile(profileImage, FileCategory.PROJECT_PROFILE);
            user.setProfileImage(fileUrl);
        }
        User saved = userRepository.save(user);

        return saved.getProfileImage();
    }

    private UserDetailDTO toDetailDto(User user) {
        UserDetailDTO userDetailDTO = new UserDetailDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getProfileImage());
        return userDetailDTO;
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


    @Override
    public List<MusicProjectDTO> getMusicProjects() {
        return musicProjectService.getMusicProjectFromUser();
    }

    @Override
    public List<SongDTO> getSongs() {
        return songService.getSongsFromUser();
    }
}
