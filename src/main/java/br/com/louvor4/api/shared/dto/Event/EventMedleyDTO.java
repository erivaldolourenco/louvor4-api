package br.com.louvor4.api.shared.dto.Event;

import br.com.louvor4.api.shared.dto.Medley.MedleyItemResponse;

import java.util.List;
import java.util.UUID;

public record EventMedleyDTO(
        UUID id,
        String name,
        String description,
        String notes,
        List<MedleyItemResponse> items
) {
}
