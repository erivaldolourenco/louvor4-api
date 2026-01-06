package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "musicProject", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "startAt", expression = "java(toLocalDateTime(dto.startDate(), dto.startTime()))")
    Event toEntity(CreateEventDto dto);

    @Mapping(target = "projectId", source = "musicProject.id")
    CreateEventDto toDto(Event entity);

    @Mapping(target = "projectId", source = "musicProject.id")
    @Mapping(target = "projectTitle", source = "musicProject.name")
    @Mapping(target = "projectImageUrl", source = "musicProject.profileImage")
    @Mapping(target = "date", expression = "java(toLocalDate(entity.getStartAt()))")
    @Mapping(target = "time", expression = "java(toTime(entity.getStartAt()))")
    EventDetailDto toDetailDto(Event entity);


    default LocalDateTime toLocalDateTime(String date, String time) {
        if (date == null || time == null) return null;
        return LocalDateTime.parse(date + "T" + time);
    }


    default LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    default Time toTime(LocalDateTime dateTime) {
        return dateTime != null ? Time.valueOf(dateTime.toLocalTime()) : null;
    }
}
