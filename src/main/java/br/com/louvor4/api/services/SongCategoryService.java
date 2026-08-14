package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;
import br.com.louvor4.api.shared.dto.Song.SongCategoryRequestDTO;

import java.util.List;
import java.util.UUID;

public interface SongCategoryService {
    List<SongCategoryDTO> getMine();
    SongCategoryDTO create(SongCategoryRequestDTO requestDto);
    SongCategoryDTO update(UUID categoryId, SongCategoryRequestDTO requestDto);
    void delete(UUID categoryId);
}
