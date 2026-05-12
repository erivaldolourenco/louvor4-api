package br.com.louvor4.api.shared.dto.Medley;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MedleyResponse(
        UUID id,
        String name,
        String description,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MedleyItemResponse> items
) {
}
