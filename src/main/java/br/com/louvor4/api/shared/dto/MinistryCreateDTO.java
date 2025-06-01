package br.com.louvor4.api.shared.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MinistryCreateDTO(
        @NotBlank(message = "O nome do ministério é obrigatório")
        @Size(min = 3, max = 255, message = "O nome deve ter entre 3 e 255 caracteres")
        String name,

        @Size(max = 1000, message = "A descrição não pode ultrapassar 1000 caracteres")
        String description,

        @NotNull(message = "O ID do criador é obrigatório")
        UUID creatorId
) {}
