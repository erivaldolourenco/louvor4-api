package br.com.louvor4.api.shared.dto.Medley;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMedleyRequest(
        @NotBlank(message = "Nome do medley é obrigatório.")
        @Size(min = 3, max = 150, message = "Nome do medley deve ter entre 3 e 150 caracteres.")
        String name,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String description,

        @NotEmpty(message = "Medley deve conter ao menos um item.")
        @Valid
        List<CreateMedleyItemRequest> items
) {
}
