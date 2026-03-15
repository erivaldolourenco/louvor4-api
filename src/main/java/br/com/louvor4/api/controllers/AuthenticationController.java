package br.com.louvor4.api.controllers;

import br.com.louvor4.api.config.security.TokenService;
import br.com.louvor4.api.config.security.UserDetailsImpl;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.RefreshToken;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.EmailVerificationTokenRepository;
import br.com.louvor4.api.repositories.RefreshTokenRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.PasswordResetService;
import br.com.louvor4.api.shared.dto.AuthenticationDTO;
import br.com.louvor4.api.shared.dto.LoginResponseDTO;
import br.com.louvor4.api.shared.dto.User.UserDTO;
import br.com.louvor4.api.shared.dto.authentication.ForgotPasswordRequest;
import br.com.louvor4.api.shared.dto.authentication.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("auth")
public class AuthenticationController {
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;

    public AuthenticationController(
            TokenService tokenService,
            AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            UserRepository userRepository
    ) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.userRepository = userRepository;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var userDetails = (UserDetailsImpl) auth.getPrincipal();
        Boolean verified = userDetails.getUser().getEmailVerified();
        if (verified == null || !verified) {
            throw new ValidationException("Você precisa validar seu e-mail para continuar. Verifique sua caixa de entrada.");
        }
        var accessToken = tokenService.generateToken(userDetails);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userDetails.getUser());
        refreshToken.setToken(tokenService.generateRefreshToken());
        refreshToken.setExpiresAt(tokenService.genRefreshExpirationDate());
        RefreshToken savedRefresh = refreshTokenRepository.save(refreshToken);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new LoginResponseDTO(
                accessToken,
                savedRefresh.getToken(),
                savedRefresh.getExpiresAt(),
                new UserDTO(
                userDetails.getUser().getId(),
                userDetails.getUser().getFirstName(),
                userDetails.getUser().getLastName(),
                userDetails.getUser().getEmail(),
                userDetails.getUser().getPlan() != null ? userDetails.getUser().getPlan().getName() : null,
                userDetails.getUser().getProfileImage(),
                userDetails.getUser().getProfileImageHash()
                )
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@RequestBody String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new ValidationException("Refresh token é obrigatório.");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue.trim())
                .orElseThrow(() -> new ValidationException("Refresh token inválido."));

        if (refreshToken.getRevokedAt() != null) {
            throw new ValidationException("Refresh token revogado.");
        }
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Refresh token expirado.");
        }

        UserDetailsImpl userDetails = new UserDetailsImpl(refreshToken.getUser());
        String newAccessToken = tokenService.generateToken(userDetails);
        return ResponseEntity.ok(new LoginResponseDTO(
                newAccessToken,
                refreshToken.getToken(),
                refreshToken.getExpiresAt(),
                new UserDTO(
                        userDetails.getUser().getId(),
                        userDetails.getUser().getFirstName(),
                        userDetails.getUser().getLastName(),
                        userDetails.getUser().getEmail(),
                        userDetails.getUser().getPlan() != null ? userDetails.getUser().getPlan().getName() : null,
                        userDetails.getUser().getProfileImage(),
                        userDetails.getUser().getProfileImageHash()
                )
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return ResponseEntity.noContent().build();
        }
        refreshTokenRepository.findByToken(refreshTokenValue.trim())
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.generateAndSendToken(request.email());
        return ResponseEntity.ok("Se o e-mail estiver cadastrado, um código foi enviado.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.validateAndResetPassword(request);
        return ResponseEntity.ok("Senha alterada com sucesso! Você já pode fazer login.");
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmailByToken(@RequestParam("token") String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new ValidationException("Token inválido.");
        }
        var token = emailVerificationTokenRepository.findByToken(tokenValue.trim())
                .orElseThrow(() -> new ValidationException("Token inválido ou inexistente."));

        if (token.isExpired()) {
            emailVerificationTokenRepository.delete(token);
            throw new ValidationException("O link expirou. Solicite um novo.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokenRepository.delete(token);
        return ResponseEntity.ok("E-mail verificado com sucesso.");
    }
}
