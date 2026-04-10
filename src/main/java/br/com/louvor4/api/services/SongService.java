package br.com.louvor4.api.services;


import br.com.louvor4.api.shared.dto.Song.SongDTO;

import java.util.List;
import java.util.UUID;

public interface SongService {
    SongDTO create(SongDTO createDto);
    List<SongDTO> getSongsFromUser();
    SongDTO update(SongDTO updateDto);
    SongDTO get(UUID songId);
}
