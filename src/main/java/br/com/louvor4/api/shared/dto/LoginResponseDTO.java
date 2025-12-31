package br.com.louvor4.api.shared.dto;

import br.com.louvor4.api.shared.dto.User.UserDTO;

public record LoginResponseDTO(String token, UserDTO user) {
}
