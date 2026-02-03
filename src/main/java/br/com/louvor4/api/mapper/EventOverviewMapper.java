package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.shared.dto.eventOverview.MonthEventItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Mapper(
        componentModel = "spring",
        uses = { MemberMapper.class, EventSongMapper.class, SongMapper.class }
)
public interface EventOverviewMapper {

    @Mapping(target = "eventId", source = "id")
    @Mapping(target = "eventName", source = "title")
    @Mapping(target = "day", expression = "java(toLocalDate(event.getStartAt()))")
    @Mapping(target = "time", expression = "java(toLocalTime(event.getStartAt()))")
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "songs", ignore = true)
    MonthEventItem toMonthItem(Event event);


    default LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalDate() : null;
    }

    default LocalTime toLocalTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.toLocalTime() : null;
    }
}
