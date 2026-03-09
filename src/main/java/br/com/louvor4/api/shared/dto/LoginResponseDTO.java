package br.com.louvor4.api.shared.dto;

import br.com.louvor4.api.shared.dto.User.UserDTO;

import java.time.LocalDateTime;

public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        LocalDateTime expiresAt,
        UserDTO user
) {
}
