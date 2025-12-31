package br.com.louvor4.api.shared.dto;

import br.com.louvor4.api.shared.dto.User.UserDetailDTO;

import java.util.List;
import java.util.UUID;

public record MinistryDetailDTO(
        UUID id,
        String name,
        String description,
        String profileImage,
        List<UserDetailDTO> members
) {}
