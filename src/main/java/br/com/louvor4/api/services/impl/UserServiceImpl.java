package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.mapper.UserMapper;
import br.com.louvor4.api.models.EmailVerificationToken;
import br.com.louvor4.api.models.Plan;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.EmailVerificationTokenRepository;
import br.com.louvor4.api.repositories.PlanRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import br.com.louvor4.api.shared.dto.User.UserCreateDTO;
import br.com.louvor4.api.shared.dto.User.UserDetailDTO;
import br.com.louvor4.api.shared.dto.User.UserUpdateDTO;
import br.com.louvor4.api.exceptions.ValidationException;
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
    private final SongService songService;
    private final CurrentUserProvider currentUserProvider;
    private final StorageService storageService;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PlanRepository planRepository;

    public UserServiceImpl(
            UserRepository userRepository,
            SongService songService,
            CurrentUserProvider currentUserProvider,
            StorageService storageService,
            UserMapper userMapper,
            EmailService emailService,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PlanRepository planRepository
    ) {
        this.userRepository = userRepository;
        this.songService = songService;
        this.currentUserProvider = currentUserProvider;
        this.storageService = storageService;
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.planRepository = planRepository;
    }

    @Override
    public User create(UserCreateDTO userDTO) {
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new ValidationException("o usuario " + userDTO.getUsername() + " ja existe");
        }
        if (userDTO.getEmail() != null && userRepository.existsByEmail(userDTO.getEmail())) {
            throw new ValidationException("o email " + userDTO.getEmail() + " ja existe");
        }
        String encryptedPassword = new BCryptPasswordEncoder().encode(userDTO.getPassword());
        User userEntity = new User();
        userEntity.setEmail(userDTO.getEmail());
        userEntity.setFirstName(userDTO.getFirstName());
        userEntity.setLastName(userDTO.getLastName());
        userEntity.setUsername(userDTO.getUsername());
        userEntity.setPassword(encryptedPassword);
        userEntity.setEmailVerified(false);
        userEntity.setPlan(getDefaultPlan());
        User saved = userRepository.save(userEntity);
        sendEmailVerification(saved);
        return saved;
    }

    private Plan getDefaultPlan() {
        return planRepository.findByName("FREE")
                .orElseThrow(() -> new ValidationException("Plano padrão não encontrado."));
    }

    private void sendEmailVerification(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        emailVerificationTokenRepository.deleteByUser(user);
        String code = java.util.UUID.randomUUID().toString().replace("-", "");
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(code);
        token.setUser(user);
        token.setExpiryDate(java.time.LocalDateTime.now().plusDays(2));
        emailVerificationTokenRepository.save(token);
        emailService.sendEmailVerificationCode(user.getEmail(), code);
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
        return userMapper.toDto(saved);
    }

    @Override
    public String updateImage(MultipartFile profileImage) {
        User user = currentUserProvider.get();
        if (isNotNullOrEmpty(profileImage)) {
            String fileUrl = storageService.uploadFileWithPrefix(
                    profileImage,
                    FileCategory.USER_PROFILE,
                    "user-profile-" + user.getId()
            );
            user.setProfileImage(fileUrl);
            user.setProfileImageHash(extractHashFromUrl(fileUrl));
        }
        User saved = userRepository.save(user);
        return saved.getProfileImage();
    }

    private String extractHashFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == url.length() - 1) return null;
        String filename = url.substring(lastSlash + 1);
        int lastDash = filename.lastIndexOf('-');
        int lastDot = filename.lastIndexOf('.');
        if (lastDash <= 0) return null;
        if (lastDot > lastDash) {
            return filename.substring(lastDash + 1, lastDot);
        }
        return filename.substring(lastDash + 1);
    }

    @Override
    public User findUserById(UUID idUser) {
        return userRepository.findById(idUser)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Usuário com id '%s' não encontrado.", idUser)));
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(String.format("Usuário com username '%s' não encontrado.", username)));
    }

    @Override
    public List<SongDTO> getSongs() {
        return songService.getSongsFromUser();
    }

}
