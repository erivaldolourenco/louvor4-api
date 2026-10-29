package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;
import br.com.louvor4.api.shared.dto.Song.SongCategoryRequestDTO;

import java.util.List;
import java.util.UUID;

public interface ProjectSongCategoryService {
    List<SongCategoryDTO> listByProject(UUID projectId);
    SongCategoryDTO create(UUID projectId, SongCategoryRequestDTO requestDto);
    SongCategoryDTO update(UUID projectId, UUID categoryId, SongCategoryRequestDTO requestDto);
    void delete(UUID projectId, UUID categoryId);
}
