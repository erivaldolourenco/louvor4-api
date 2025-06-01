package br.com.louvor4.api.shared.dto;

import java.util.List;
import java.util.UUID;

public record MinistryDetailDTO(
        UUID id,
        String name,
        String description,
        List<UserDTO> members
) {}
