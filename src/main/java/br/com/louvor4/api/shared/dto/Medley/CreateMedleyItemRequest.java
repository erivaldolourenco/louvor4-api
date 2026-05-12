package br.com.louvor4.api.shared.dto.Medley;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateMedleyItemRequest(
        @NotNull(message = "Id da música é obrigatório.")
        UUID songId,

        @NotBlank(message = "Tom é obrigatório.")
        @Size(max = 5, message = "Tom deve ter no máximo 5 caracteres.")
        @Pattern(regexp = "^[A-G](#|b)?m?$", message = "Tom inválido. Use C, D, E, F, G, A ou B com #, b e/ou m.")
        String key,

        @Size(max = 1000, message = "Notas devem ter no máximo 1000 caracteres.")
        String notes,

        @NotNull(message = "Sequência é obrigatória.")
        @Min(value = 1, message = "Sequência deve ser maior que zero.")
        Integer sequence
) {
}
