package br.com.louvor4.api.shared.dto.Event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventDetailDto(
    UUID id,
    UUID projectId,
    String title,
    String description,
    LocalDate date,
    LocalTime time,
    String location,
    String projectTitle,
    String projectImageUrl,
    Integer participantsCount,
    Integer repertoireCount
){}
