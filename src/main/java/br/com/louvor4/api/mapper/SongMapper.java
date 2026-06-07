package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SongMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(source = "referenceAudioUrl", target = ".")
    Song toEntity(SongDTO dto);

    @Mapping(target = "referenceAudioUrl", ignore = true)
    SongDTO toDto(Song entity);

    @Mapping(target = "referenceAudioUrl", ignore = true)
    List<SongDTO> toDtoList(List<Song> entities);
}
