package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.AuthProvider;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.GoogleAuthService;
import br.com.louvor4.entitlement.enums.SubscriptionStatus;
import br.com.louvor4.entitlement.models.Plans;
import br.com.louvor4.entitlement.models.Subscription;
import br.com.louvor4.entitlement.repositories.PlansRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final UserRepository userRepository;
    private final PlansRepository plansRepository;
    private final SubscriptionRepository subscriptionRepository;

    public GoogleAuthServiceImpl(
            GoogleIdTokenVerifier googleIdTokenVerifier,
            UserRepository userRepository,
            PlansRepository plansRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.userRepository = userRepository;
        this.plansRepository = plansRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    @Transactional
    public User authenticate(String idToken) {
        GoogleIdToken.Payload payload = verify(idToken);

        String googleId = payload.getSubject();
        String email = payload.getEmail();

        if (email == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ValidationException("Conta Google sem e-mail verificado.");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return createFromGoogle(payload, googleId, email);
        }

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return user;
    }

    private GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdToken token = googleIdTokenVerifier.verify(idToken);
            if (token == null) {
                throw new ValidationException("Token do Google inválido ou expirado.");
            }
            return token.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new ValidationException("Não foi possível validar o token do Google.");
        }
    }

    private User createFromGoogle(GoogleIdToken.Payload payload, String googleId, String email) {
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");

        User user = new User();
        user.setEmail(email);
        user.setUsername(generateUsernameFromEmail(email));
        user.setFirstName(firstName != null ? firstName : "Usuário");
        user.setLastName(lastName != null ? lastName : "");
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setGoogleId(googleId);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);
        createFreeSubscription(saved);
        return saved;
    }

    private String generateUsernameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        String base = atIndex > 0 ? email.substring(0, atIndex) : email;

        if (!userRepository.existsByUsername(base)) {
            return base;
        }

        LocalDate today = LocalDate.now();

        String withYear = base + today.getYear();
        if (!userRepository.existsByUsername(withYear)) {
            return withYear;
        }

        String withYearMonth = base + String.format("%d%02d", today.getYear(), today.getMonthValue());
        if (!userRepository.existsByUsername(withYearMonth)) {
            return withYearMonth;
        }

        String withYearMonthDay = base + String.format("%d%02d%02d", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        if (!userRepository.existsByUsername(withYearMonthDay)) {
            return withYearMonthDay;
        }

        String withRandomSuffix;
        do {
            withRandomSuffix = base + ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        } while (userRepository.existsByUsername(withRandomSuffix));

        return withRandomSuffix;
    }

    private void createFreeSubscription(User user) {
        Plans freePlan = plansRepository.findByName("FREE")
                .orElseThrow(() -> new ValidationException("Plano FREE não encontrado."));
        Subscription subscription = new Subscription();
        subscription.setUserId(user.getId());
        subscription.setPlan(freePlan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartedAt(LocalDateTime.now());
        subscriptionRepository.save(subscription);
    }
}
