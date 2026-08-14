package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.SongCategory;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.SongCategoryRepository;
import br.com.louvor4.api.services.SongCategoryService;
import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;
import br.com.louvor4.api.shared.dto.Song.SongCategoryRequestDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SongCategoryServiceImpl implements SongCategoryService {

    private final SongCategoryRepository songCategoryRepository;
    private final CurrentUserProvider currentUserProvider;

    public SongCategoryServiceImpl(SongCategoryRepository songCategoryRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.songCategoryRepository = songCategoryRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public List<SongCategoryDTO> getMine() {
        UUID userId = currentUserProvider.get().getId();
        return songCategoryRepository.findByUser_IdOrderByNameAsc(userId).stream()
                .map(SongCategoryDTO::fromEntity)
                .toList();
    }

    @Override
    public SongCategoryDTO create(SongCategoryRequestDTO requestDto) {
        User user = currentUserProvider.get();

        if (songCategoryRepository.existsByUser_IdAndNameIgnoreCase(user.getId(), requestDto.name())) {
            throw new ValidationException("Você já tem uma categoria com esse nome.");
        }

        SongCategory category = new SongCategory();
        category.setName(requestDto.name());
        category.setUser(user);

        SongCategory saved = songCategoryRepository.save(category);
        return SongCategoryDTO.fromEntity(saved);
    }

    @Override
    public SongCategoryDTO update(UUID categoryId, SongCategoryRequestDTO requestDto) {
        SongCategory category = songCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ValidationException("Categoria não encontrada."));

        if (songCategoryRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(
                category.getUser().getId(), requestDto.name(), categoryId)) {
            throw new ValidationException("Você já tem uma categoria com esse nome.");
        }

        category.setName(requestDto.name());
        SongCategory saved = songCategoryRepository.save(category);
        return SongCategoryDTO.fromEntity(saved);
    }

    @Override
    public void delete(UUID categoryId) {
        SongCategory category = songCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ValidationException("Categoria não encontrada."));
        songCategoryRepository.delete(category);
    }
}
