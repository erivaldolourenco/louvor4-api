package br.com.louvor4.api.shared.dto;

import java.util.UUID;

public record UserDetailDTO(UUID id, String firstName, String lastName, String email, String phoneNumber, String profileImage) {
}
