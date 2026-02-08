package br.com.louvor4.api.controllers;

import br.com.louvor4.api.config.security.TokenService;
import br.com.louvor4.api.config.security.UserDetailsImpl;
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

@RestController
@RequestMapping("auth")
public class AuthenticationController {
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;

    public AuthenticationController(TokenService tokenService, AuthenticationManager authenticationManager, PasswordResetService passwordResetService) {
        this.tokenService = tokenService;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationDTO data){
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.username(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var userDetails = (UserDetailsImpl) auth.getPrincipal();
        var token = tokenService.generateToken(userDetails);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new LoginResponseDTO(token, new UserDTO(
                userDetails.getUser().getId(),
                userDetails.getUser().getFirstName(),
                userDetails.getUser().getLastName(),
                userDetails.getUser().getEmail()
                ) ));
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
}
