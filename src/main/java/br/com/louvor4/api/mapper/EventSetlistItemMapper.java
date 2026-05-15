package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.models.MedleyItem;
import br.com.louvor4.api.shared.dto.Event.EventMedleyDTO;
import br.com.louvor4.api.shared.dto.Event.SetlistDTO;
import br.com.louvor4.api.shared.dto.Medley.MedleyItemResponse;
import br.com.louvor4.api.shared.dto.Song.EventSongDTO;
import br.com.louvor4.api.shared.dto.eventOverview.MonthEventSongItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

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

    default SetlistDTO toSetlistDto(EventSetlistItem entity) {
        if (entity == null) {
            return null;
        }

        return new SetlistDTO(
                entity.getId(),
                entity.getType(),
                resolveAddedBy(entity),
                resolveNotes(entity),
                entity.isSong() ? toSongDto(entity) : null,
                entity.isMedley() ? toMedleyDto(entity) : null
        );
    }

    default List<SetlistDTO> toSetlistDtoList(List<EventSetlistItem> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .map(this::toSetlistDto)
                .collect(Collectors.toList());
    }

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
        if (entity.getNotes() != null) {
            return entity.getNotes();
        }
        if (entity.getSong() != null) {
            return entity.getSong().getNotes();
        }
        if (entity.getMedley() != null) {
            return entity.getMedley().getNotes();
        }
        return null;
    }

    default String resolveAddedBy(EventSetlistItem entity) {
        if (entity == null
                || entity.getAddedBy() == null
                || entity.getAddedBy().getMember() == null
                || entity.getAddedBy().getMember().getUser() == null) {
            return null;
        }
        return entity.getAddedBy().getMember().getUser().getFirstName();
    }

    default EventMedleyDTO toMedleyDto(EventSetlistItem entity) {
        if (entity == null || entity.getMedley() == null) {
            return null;
        }

        return new EventMedleyDTO(
                entity.getMedley().getId(),
                entity.getMedley().getName(),
                entity.getMedley().getDescription(),
                entity.getMedley().getNotes(),
                entity.getMedley().getItems() == null
                        ? List.of()
                        : entity.getMedley().getItems().stream()
                        .map(this::toMedleyItemResponse)
                        .collect(Collectors.toList())
        );
    }

    default MedleyItemResponse toMedleyItemResponse(MedleyItem item) {
        if (item == null || item.getSong() == null) {
            return null;
        }

        return new MedleyItemResponse(
                item.getId(),
                item.getSong().getId(),
                item.getSong().getTitle(),
                item.getSong().getArtist(),
                item.getSong().getYouTubeUrl(),
                item.getKey(),
                item.getNotes(),
                item.getSequence()
        );
    }
}
