package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.AccountDeletionRequest;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.models.UserUnavailability;
import br.com.louvor4.api.repositories.AccountDeletionRequestRepository;
import br.com.louvor4.api.repositories.EmailVerificationTokenRepository;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.NotificationDeviceRepository;
import br.com.louvor4.api.repositories.PasswordResetTokenRepository;
import br.com.louvor4.api.repositories.RefreshTokenRepository;
import br.com.louvor4.api.repositories.UserNotificationRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.repositories.UserUnavailabilityRepository;
import br.com.louvor4.api.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock NotificationDeviceRepository notificationDeviceRepository;
    @Mock UserNotificationRepository userNotificationRepository;
    @Mock UserUnavailabilityRepository userUnavailabilityRepository;
    @Mock AccountDeletionRequestRepository accountDeletionRequestRepository;
    @Mock EmailService emailService;
    @Mock MusicProjectMemberRepository musicProjectMemberRepository;
    @InjectMocks AccountDeletionServiceImpl service;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setFirstName("Erivaldo");
        user.setLastName("Lourenco");
        user.setUsername("erivaldo");
        user.setEmail("erivaldo@example.com");
        user.setPhoneNumber("11999999999");
        user.setProfileImage("https://cdn/x.png");
        user.setProfileImageHash("abc123");
        user.setPassword("hashed");
        user.setGoogleId("google-123");
        user.setEmailVerified(true);
    }

    @Test
    void deleteAccount_anonymizesPersonalDataAndCascadesRelatedRecords() {
        when(userUnavailabilityRepository.findAllByUserId(userId)).thenReturn(List.of(new UserUnavailability()));

        service.deleteAccount(user);

        assertThat(user.getFirstName()).isEqualTo("Usuário");
        assertThat(user.getLastName()).isEqualTo("removido");
        assertThat(user.getUsername()).contains(userId.toString());
        assertThat(user.getEmail()).contains(userId.toString());
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getProfileImage()).isNull();
        assertThat(user.getProfileImageHash()).isNull();
        assertThat(user.getPassword()).isNull();
        assertThat(user.getGoogleId()).isNull();
        assertThat(user.getEmailVerified()).isFalse();

        verify(refreshTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(emailVerificationTokenRepository).deleteByUser(user);
        verify(notificationDeviceRepository).deleteByUserId(userId);
        verify(userNotificationRepository).deleteByUserId(userId);
        verify(accountDeletionRequestRepository).deleteByUser(user);
        verify(userUnavailabilityRepository).deleteAll(anyList());
        assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteAccount_marksMembershipsRemovedExceptOwnedOnesWithoutTouchingParticipations() {
        MusicProjectMember member = new MusicProjectMember();
        member.setProjectRole(ProjectMemberRole.MEMBER);
        MusicProjectMember admin = new MusicProjectMember();
        admin.setProjectRole(ProjectMemberRole.ADMIN);
        MusicProjectMember owner = new MusicProjectMember();
        owner.setProjectRole(ProjectMemberRole.OWNER);
        when(musicProjectMemberRepository.findByUser_IdAndStatus(userId, ProjectMemberStatus.ACTIVE))
                .thenReturn(List.of(member, admin, owner));
        when(userUnavailabilityRepository.findAllByUserId(userId)).thenReturn(List.of());

        service.deleteAccount(user);

        assertThat(member.getStatus()).isEqualTo(ProjectMemberStatus.REMOVED);
        assertThat(admin.getStatus()).isEqualTo(ProjectMemberStatus.REMOVED);
        assertThat(owner.getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
        verify(musicProjectMemberRepository).saveAll(List.of(member, admin));
    }

    @Test
    void requestDeletion_whenUserExists_createsTokenAndSendsEmail() {
        when(userRepository.findByEmail("erivaldo@example.com")).thenReturn(Optional.of(user));

        service.requestDeletion("erivaldo@example.com");

        verify(accountDeletionRequestRepository).deleteByUser(user);

        ArgumentCaptor<AccountDeletionRequest> captor = ArgumentCaptor.forClass(AccountDeletionRequest.class);
        verify(accountDeletionRequestRepository).save(captor.capture());
        AccountDeletionRequest saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiryDate()).isAfter(LocalDateTime.now().plusHours(23));

        verify(emailService).sendAccountDeletionConfirmation(eq("erivaldo@example.com"), eq(saved.getToken()));
    }

    @Test
    void requestDeletion_whenUserDoesNotExist_doesNothingSilently() {
        when(userRepository.findByEmail("naoexiste@example.com")).thenReturn(Optional.empty());

        service.requestDeletion("naoexiste@example.com");

        verify(accountDeletionRequestRepository, never()).save(any());
        verify(emailService, never()).sendAccountDeletionConfirmation(any(), any());
    }

    @Test
    void confirmDeletion_withValidToken_deletesAccountAndConsumesToken() {
        AccountDeletionRequest request = new AccountDeletionRequest();
        request.setUser(user);
        request.setToken("valid-token");
        request.setExpiryDate(LocalDateTime.now().plusHours(1));
        when(accountDeletionRequestRepository.findByToken("valid-token")).thenReturn(Optional.of(request));
        when(userUnavailabilityRepository.findAllByUserId(userId)).thenReturn(List.of());

        service.confirmDeletion("valid-token");

        verify(accountDeletionRequestRepository).delete(request);
        assertThat(user.getDeletedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void confirmDeletion_withExpiredToken_throwsAndDeletesRequestWithoutDeletingAccount() {
        AccountDeletionRequest request = new AccountDeletionRequest();
        request.setUser(user);
        request.setToken("expired-token");
        request.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        when(accountDeletionRequestRepository.findByToken("expired-token")).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.confirmDeletion("expired-token"))
                .isInstanceOf(ValidationException.class);

        verify(accountDeletionRequestRepository).delete(request);
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmDeletion_withUnknownToken_throwsValidationException() {
        when(accountDeletionRequestRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmDeletion("unknown"))
                .isInstanceOf(ValidationException.class);

        verify(userRepository, never()).save(any());
    }
}
