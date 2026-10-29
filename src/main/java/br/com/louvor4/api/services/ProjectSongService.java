package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongDTO;
import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongRequestDTO;
import br.com.louvor4.api.shared.dto.Song.ChordSheetDTO;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ProjectSongService {
    List<ProjectSongDTO> listByProject(UUID projectId);
    ProjectSongDTO getById(UUID projectId, UUID songId);
    ProjectSongDTO create(UUID projectId, ProjectSongRequestDTO requestDto);
    ProjectSongDTO update(UUID projectId, UUID songId, ProjectSongRequestDTO requestDto);
    void delete(UUID projectId, UUID songId);
    ProjectSongDTO assignCategories(UUID projectId, UUID songId, Set<UUID> categoryIds);
    ChordSheetDTO getChordSheet(UUID projectId, UUID songId);
    ChordSheetDTO updateChordSheet(UUID projectId, UUID songId, String chordSheetJson);
}
