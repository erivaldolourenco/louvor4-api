package br.com.louvor4.api.shared.dto.Event;

import java.sql.Time;
import java.time.LocalDate;
import java.util.UUID;

public record EventDetailDto(
    UUID id,
    UUID projectId,
    String title,
    String description,
    LocalDate date,
    Time time,
    String location,
    String projectTitle,
    String projectImageUrl
){}
