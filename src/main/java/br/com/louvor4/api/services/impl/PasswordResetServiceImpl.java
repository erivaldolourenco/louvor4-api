package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.PasswordResetToken;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.PasswordResetTokenRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.PasswordResetService;
import br.com.louvor4.api.shared.dto.authentication.ResetPasswordRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;

    public PasswordResetServiceImpl(UserRepository userRepository, PasswordResetTokenRepository tokenRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = tokenRepository;
        this.emailService = emailService;
    }


    @Transactional
    @Override
    public void generateAndSendToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com este e-mail"));

        resetTokenRepository.deleteByUser(user);
        String code = String.format("%06d", new java.util.Random().nextInt(1000000));
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(code);
        resetToken.setUser(user);
        resetToken.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(15)); // Expira em 15 min
        resetTokenRepository.save(resetToken);
        emailService.sendPasswordResetCode(user.getEmail(), code);
    }

    @Transactional
    @Override
    public void validateAndResetPassword(ResetPasswordRequest request) {
        // 1. Buscar o token no banco
        PasswordResetToken resetToken = resetTokenRepository.findByToken(request.code())
                .orElseThrow(() -> new RuntimeException("Código inválido ou inexistente."));

        // 2. Verificar se o e-mail enviado bate com o dono do token
        if (!resetToken.getUser().getEmail().equals(request.email())) {
            throw new RuntimeException("Este código não pertence a este usuário.");
        }

        // 3. Verificar se o código expirou
        if (resetToken.isExpired()) {
            resetTokenRepository.delete(resetToken);
            throw new RuntimeException("O código expirou. Solicite um novo.");
        }

        // 4. Se chegou aqui, está tudo certo! Atualizamos a senha.
        User user = resetToken.getUser();

        // IMPORTANTE: Use o seu PasswordEncoder do Spring Security aqui!
        // Exemplo: user.setPassword(passwordEncoder.encode(request.newPassword()));
        String encryptedPassword = new BCryptPasswordEncoder().encode(request.newPassword());
        user.setPassword(encryptedPassword); // Ajuste conforme sua lógica de criptografia

        userRepository.save(user);

        // 5. Apagar o token para ele não ser usado uma segunda vez (Segurança)
        resetTokenRepository.delete(resetToken);
    }
}
