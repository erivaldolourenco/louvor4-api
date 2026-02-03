package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.EventSong;
import br.com.louvor4.api.shared.dto.Song.EventSongDTO;
import br.com.louvor4.api.shared.dto.eventOverview.MonthEventSongItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventSongMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "song.title")
    @Mapping(target = "artist", source = "song.artist")
    @Mapping(target = "key", source = "song.key")
    @Mapping(target = "bpm", source = "song.bpm")
    @Mapping(target = "youTubeUrl", source = "song.youTubeUrl")
    @Mapping(target = "addedBy", source = "entity.addedBy.member.user.firstName")
    EventSongDTO toSongDto(EventSong entity);

    List<EventSongDTO> toSongDtoList(List<EventSong> entities);

    @Mapping(target = "songId", source = "song.id")
    @Mapping(target = "title", source = "song.title")
    @Mapping(target = "artist", source = "song.artist")
    @Mapping(target = "key", source = "song.key")
    @Mapping(target = "addedBy", source = "entity.addedBy.member.user.firstName")
    MonthEventSongItem toMonthOverviewSong(EventSong entity);
}
