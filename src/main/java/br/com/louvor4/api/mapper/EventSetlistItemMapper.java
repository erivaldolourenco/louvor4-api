package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.shared.dto.Song.EventSongDTO;
import br.com.louvor4.api.shared.dto.eventOverview.MonthEventSongItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventSetlistItemMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "title", source = "song.title")
    @Mapping(target = "artist", source = "song.artist")
    @Mapping(target = "key", expression = "java(resolveKey(entity))")
    @Mapping(target = "bpm", source = "song.bpm")
    @Mapping(target = "youTubeUrl", source = "song.youTubeUrl")
    @Mapping(target = "notes", expression = "java(resolveNotes(entity))")
    @Mapping(target = "addedBy", source = "addedBy.member.user.firstName")
    EventSongDTO toSongDto(EventSetlistItem entity);

    List<EventSongDTO> toSongDtoList(List<EventSetlistItem> entities);

    @Mapping(target = "songId", source = "song.id")
    @Mapping(target = "title", source = "song.title")
    @Mapping(target = "artist", source = "song.artist")
    @Mapping(target = "key", expression = "java(resolveKey(entity))")
    @Mapping(target = "addedBy", source = "addedBy.member.user.firstName")
    MonthEventSongItem toMonthOverviewSong(EventSetlistItem entity);

    default String resolveKey(EventSetlistItem entity) {
        if (entity == null) {
            return null;
        }
        return entity.getKey() != null ? entity.getKey() : (entity.getSong() != null ? entity.getSong().getKey() : null);
    }

    default String resolveNotes(EventSetlistItem entity) {
        if (entity == null) {
            return null;
        }
        return entity.getNotes() != null ? entity.getNotes() : (entity.getSong() != null ? entity.getSong().getNotes() : null);
    }
}
