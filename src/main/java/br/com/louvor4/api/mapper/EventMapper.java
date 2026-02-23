package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.UpdateEventDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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
    @Mapping(target = "time", expression = "java(toLocalTime(entity.getStartAt()))")
    EventDetailDto toDetailDto(Event entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "musicProject", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "endAt", ignore = true)
    @Mapping(target = "startAt", expression = "java(mergeStartAt(dto.startDate(), dto.startTime(), entity.getStartAt()))")
    void updateEntityFromDto(UpdateEventDto dto, @MappingTarget Event entity);

    default LocalDateTime mergeStartAt(String date, String time, LocalDateTime current) {
        if (date == null || time == null) return current;
        return toLocalDateTime(date, time);
    }

    default LocalDateTime toLocalDateTime(String date, String time) {
        if (date == null || time == null) return null;
        return LocalDateTime.parse(date + "T" + time);
    }


    default LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    default LocalTime toLocalTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalTime() : null;
    }
}
