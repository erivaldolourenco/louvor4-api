package br.com.louvor4.api.shared.dto;

import java.util.UUID;

public record UserDTO(UUID id, String firstName, String lastName, String email) {
}
