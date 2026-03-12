package br.com.louvor4.api.shared.dto.User;

import java.util.UUID;

public record UserDetailDTO(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String username,
        String phoneNumber,
        String profileImage,
        String planName
) {
}
