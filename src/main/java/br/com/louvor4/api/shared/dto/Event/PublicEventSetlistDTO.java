package br.com.louvor4.api.shared.dto.Event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PublicEventSetlistDTO(
        String title,
        String description,
        LocalDate date,
        LocalTime time,
        String location,
        List<PublicSetlistItemDTO> repertoire
) {
}
