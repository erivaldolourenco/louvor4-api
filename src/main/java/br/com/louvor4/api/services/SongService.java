package br.com.louvor4.api.services;


import br.com.louvor4.api.shared.dto.Song.ChordSheetDTO;
import br.com.louvor4.api.shared.dto.Song.ChordSheetEditPermissionDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import br.com.louvor4.api.shared.dto.Song.SongLyricsDTO;

import java.util.List;
import java.util.UUID;

public interface SongService {
    SongDTO create(SongDTO createDto);
    List<SongDTO> getSongsFromUser();
    SongDTO update(SongDTO updateDto);
    SongDTO get(UUID songId);
    void delete(UUID songId);
    SongLyricsDTO getLyrics(UUID songId);
    SongLyricsDTO updateLyrics(UUID songId, String lyrics);
    ChordSheetDTO getChordSheet(UUID songId);
    ChordSheetDTO updateChordSheet(UUID songId, String chordSheetJson);
    void deleteChordSheet(UUID songId);
    ChordSheetDTO importChordSheet(UUID songId, String chordSheetJson);
    ChordSheetEditPermissionDTO updateChordSheetEditPermission(UUID songId, boolean editPermission);
}
