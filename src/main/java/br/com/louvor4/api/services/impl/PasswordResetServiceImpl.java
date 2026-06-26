package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.PasswordResetToken;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.PasswordResetTokenRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.PasswordResetService;
import br.com.louvor4.api.services.SmsService;
import br.com.louvor4.api.shared.dto.authentication.ForgotPasswordChannelsResponse;
import br.com.louvor4.api.shared.dto.authentication.ResetChannel;
import br.com.louvor4.api.shared.dto.authentication.ResetPasswordRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    @Autowired(required = false)
    private SmsService smsService;

    public PasswordResetServiceImpl(UserRepository userRepository, PasswordResetTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Override
    public ForgotPasswordChannelsResponse getAvailableChannels(String identifier) {
        User user = findUserByIdentifier(identifier);

        String maskedEmail = maskEmail(user.getEmail());
        String maskedPhone = (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank())
                ? maskPhone(user.getPhoneNumber())
                : null;

        return new ForgotPasswordChannelsResponse(maskedEmail, maskedPhone);
    }

    @Transactional
    @Override
    public void generateAndSendToken(String identifier, ResetChannel channel) {
        User user = findUserByIdentifier(identifier);

        if (channel == ResetChannel.SMS) {
            if (smsService == null) {
                throw new RuntimeException("Envio por SMS não está disponível no momento.");
            }
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
                throw new RuntimeException("Este usuário não possui número de celular cadastrado.");
            }
        }

        resetTokenRepository.deleteByUser(user);
        resetTokenRepository.flush();

        String code = String.format("%06d", new java.util.Random().nextInt(1000000));
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(code);
        resetToken.setUser(user);
        resetToken.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(15));
        resetTokenRepository.save(resetToken);

        if (channel == ResetChannel.SMS) {
            smsService.sendPasswordResetCode(user.getPhoneNumber(), code);
        } else {
            emailService.sendPasswordResetCode(user.getEmail(), code);
        }
    }

    @Transactional
    @Override
    public void validateAndResetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.code())
                .orElseThrow(() -> new RuntimeException("Código inválido ou inexistente."));

        User requestUser = findUserByIdentifier(request.identifier());
        if (!resetToken.getUser().getId().equals(requestUser.getId())) {
            throw new RuntimeException("Este código não pertence a este usuário.");
        }

        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new RuntimeException("O código expirou. Solicite um novo.");
        }

        User user = resetToken.getUser();
        String encryptedPassword = new BCryptPasswordEncoder().encode(request.newPassword());
        user.setPassword(encryptedPassword);
        userRepository.save(user);
        resetTokenRepository.delete(resetToken);
    }

    private User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Informe seu e-mail ou nome de usuário.");
        }
        if (identifier.contains("@")) {
            return userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        }
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(0, at));
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.length() <= 4) return "****";
        int visibleStart = Math.min(3, digits.length() - 4);
        String prefix = digits.substring(0, visibleStart);
        String suffix = digits.substring(digits.length() - 4);
        String mask = "*".repeat(digits.length() - visibleStart - 4);
        return prefix + mask + suffix;
    }
}
