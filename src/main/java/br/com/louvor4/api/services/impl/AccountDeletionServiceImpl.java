package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.AccountDeletionRequest;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.AccountDeletionRequestRepository;
import br.com.louvor4.api.repositories.EmailVerificationTokenRepository;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.NotificationDeviceRepository;
import br.com.louvor4.api.repositories.PasswordResetTokenRepository;
import br.com.louvor4.api.repositories.RefreshTokenRepository;
import br.com.louvor4.api.repositories.UserNotificationRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.repositories.UserUnavailabilityRepository;
import br.com.louvor4.api.services.AccountDeletionService;
import br.com.louvor4.api.services.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountDeletionServiceImpl implements AccountDeletionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final NotificationDeviceRepository notificationDeviceRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserUnavailabilityRepository userUnavailabilityRepository;
    private final AccountDeletionRequestRepository accountDeletionRequestRepository;
    private final EmailService emailService;
    private final MusicProjectMemberRepository musicProjectMemberRepository;

    public AccountDeletionServiceImpl(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            NotificationDeviceRepository notificationDeviceRepository,
            UserNotificationRepository userNotificationRepository,
            UserUnavailabilityRepository userUnavailabilityRepository,
            AccountDeletionRequestRepository accountDeletionRequestRepository,
            EmailService emailService,
            MusicProjectMemberRepository musicProjectMemberRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.notificationDeviceRepository = notificationDeviceRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.userUnavailabilityRepository = userUnavailabilityRepository;
        this.accountDeletionRequestRepository = accountDeletionRequestRepository;
        this.emailService = emailService;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
    }

    @Override
    @Transactional
    public void deleteAccount(User user) {
        UUID userId = user.getId();

        refreshTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        emailVerificationTokenRepository.deleteByUser(user);
        notificationDeviceRepository.deleteByUserId(userId);
        userNotificationRepository.deleteByUserId(userId);
        userUnavailabilityRepository.deleteAll(userUnavailabilityRepository.findAllByUserId(userId));
        accountDeletionRequestRepository.deleteByUser(user);

        // Sai dos projetos marcando o vínculo como REMOVED. As participações em eventos (inclusive futuros)
        // são mantidas de propósito: o card aparece como "Conta excluída" e o líder vê a vaga na escala,
        // e as músicas que ele adicionou continuam no repertório.
        // Projetos em que é OWNER continuam com ele como proprietário: remover deixaria o projeto sem dono.
        List<MusicProjectMember> memberships = musicProjectMemberRepository
                .findByUser_IdAndStatus(userId, ProjectMemberStatus.ACTIVE).stream()
                .filter(member -> member.getProjectRole() != ProjectMemberRole.OWNER)
                .toList();
        memberships.forEach(member -> member.setStatus(ProjectMemberStatus.REMOVED));
        musicProjectMemberRepository.saveAll(memberships);

        user.setFirstName("Usuário");
        user.setLastName("removido");
        user.setUsername("usuario-removido-" + userId);
        user.setEmail("deleted-" + userId + "@louvor4.invalid");
        user.setPhoneNumber(null);
        user.setProfileImage(null);
        user.setProfileImageHash(null);
        user.setPassword(null);
        user.setGoogleId(null);
        user.setEmailVerified(false);
        // Soft delete marcando deleted_at direto, sem userRepository.delete(): o delete() coloca o User
        // em estado "removido" no Hibernate e o flush falha (TransientObjectException) ao atualizar
        // entidades que ainda referenciam ele, como os MusicProjectMember marcados como REMOVED acima.
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void requestDeletion(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Informe seu e-mail.");
        }
        userRepository.findByEmail(email.trim()).ifPresent(user -> {
            accountDeletionRequestRepository.deleteByUser(user);

            String token = UUID.randomUUID().toString().replace("-", "");
            AccountDeletionRequest request = new AccountDeletionRequest();
            request.setUser(user);
            request.setToken(token);
            request.setExpiryDate(LocalDateTime.now().plusHours(24));
            accountDeletionRequestRepository.save(request);

            emailService.sendAccountDeletionConfirmation(user.getEmail(), token);
        });
    }

    @Override
    @Transactional
    public void confirmDeletion(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token inválido.");
        }
        AccountDeletionRequest request = accountDeletionRequestRepository.findByToken(token.trim())
                .orElseThrow(() -> new ValidationException("Link inválido ou já utilizado."));

        if (request.isExpired()) {
            accountDeletionRequestRepository.delete(request);
            throw new ValidationException("O link expirou. Solicite novamente a exclusão da conta.");
        }

        User user = request.getUser();
        accountDeletionRequestRepository.delete(request);
        deleteAccount(user);
    }
}
