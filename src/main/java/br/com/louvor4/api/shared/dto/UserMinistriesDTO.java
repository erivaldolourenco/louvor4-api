package br.com.louvor4.api.shared.dto;

import java.util.UUID;

public record UserMinistriesDTO(UUID id, String name, String description, String profileImage, Long qtMembers) {
}
