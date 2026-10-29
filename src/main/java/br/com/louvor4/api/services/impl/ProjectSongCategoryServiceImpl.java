package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectSongCategory;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.ProjectSongCategoryRepository;
import br.com.louvor4.api.services.ProjectSongCategoryService;
import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;
import br.com.louvor4.api.shared.dto.Song.SongCategoryRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectSongCategoryServiceImpl implements ProjectSongCategoryService {

    private static final String DUPLICATE_NAME = "O projeto já tem uma categoria com esse nome.";

    private final ProjectSongCategoryRepository categoryRepository;
    private final MusicProjectRepository musicProjectRepository;

    public ProjectSongCategoryServiceImpl(ProjectSongCategoryRepository categoryRepository,
                                          MusicProjectRepository musicProjectRepository) {
        this.categoryRepository = categoryRepository;
        this.musicProjectRepository = musicProjectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SongCategoryDTO> listByProject(UUID projectId) {
        return categoryRepository.findByMusicProject_IdOrderByNameAsc(projectId).stream()
                .map(ProjectSongCategoryServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SongCategoryDTO create(UUID projectId, SongCategoryRequestDTO requestDto) {
        MusicProject project = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new ValidationException("Projeto não encontrado."));
        String name = requestDto.name().trim();
        if (categoryRepository.existsByMusicProject_IdAndNameIgnoreCase(projectId, name)) {
            throw new ValidationException(DUPLICATE_NAME);
        }

        ProjectSongCategory category = new ProjectSongCategory();
        category.setMusicProject(project);
        category.setName(name);
        return toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public SongCategoryDTO update(UUID projectId, UUID categoryId, SongCategoryRequestDTO requestDto) {
        ProjectSongCategory category = findOrThrow(projectId, categoryId);
        String name = requestDto.name().trim();
        if (categoryRepository.existsByMusicProject_IdAndNameIgnoreCaseAndIdNot(projectId, name, categoryId)) {
            throw new ValidationException(DUPLICATE_NAME);
        }

        category.setName(name);
        return toDto(categoryRepository.save(category));
    }

    // Excluir a categoria só a tira das músicas; as músicas continuam.
    @Override
    @Transactional
    public void delete(UUID projectId, UUID categoryId) {
        ProjectSongCategory category = findOrThrow(projectId, categoryId);
        categoryRepository.deleteAssignmentsByCategoryId(categoryId);
        categoryRepository.delete(category);
    }

    private ProjectSongCategory findOrThrow(UUID projectId, UUID categoryId) {
        return categoryRepository.findByIdAndMusicProject_Id(categoryId, projectId)
                .orElseThrow(() -> new ValidationException("Categoria não encontrada."));
    }

    private static SongCategoryDTO toDto(ProjectSongCategory category) {
        return new SongCategoryDTO(category.getId(), category.getName());
    }
}
