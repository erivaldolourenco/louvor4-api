package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.mapper.UserMapper;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.EmailVerificationTokenRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.AccountDeletionService;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.MedleyService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.entitlement.repositories.PlansRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplDeleteAccountTest {

    @Mock UserRepository userRepository;
    @Mock SongService songService;
    @Mock MedleyService medleyService;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock StorageService storageService;
    @Mock UserMapper userMapper;
    @Mock EmailService emailService;
    @Mock EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock PlansRepository plansRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock AccountDeletionService accountDeletionService;
    @InjectMocks UserServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        when(currentUserProvider.get()).thenReturn(user);
    }

    @Test
    void deleteAccount_delegatesToAccountDeletionServiceForCurrentUser() {
        service.deleteAccount();

        verify(accountDeletionService).deleteAccount(user);
    }
}
