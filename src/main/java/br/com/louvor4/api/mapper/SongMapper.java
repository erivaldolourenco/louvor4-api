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
    Song toEntity(SongDTO dto);

    SongDTO toDto(Song entity);

    List<SongDTO> toDtoList(List<Song> entities);
}
