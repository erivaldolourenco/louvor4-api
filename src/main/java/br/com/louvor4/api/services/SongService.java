package br.com.louvor4.api.services;

import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;

import java.util.List;

public interface SongService {
    SongDTO create(SongDTO createDto);
    List<SongDTO> getSongsFromUser();

}
