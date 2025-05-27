package br.com.louvor4.api.shared.dto;

public record LoginResponseDTO(String token, UserDTO user) {
}
