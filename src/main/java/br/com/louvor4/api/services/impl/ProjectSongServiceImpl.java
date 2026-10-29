package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.MusicProjectType;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectSong;
import br.com.louvor4.api.models.ProjectSongCategory;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.ProjectSongCategoryRepository;
import br.com.louvor4.api.repositories.ProjectSongRepository;
import br.com.louvor4.api.services.ProjectSongService;
import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongDTO;
import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongRequestDTO;
import br.com.louvor4.api.shared.dto.Song.ChordSheetDTO;
import br.com.louvor4.api.validations.ChordSheetValidation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectSongServiceImpl implements ProjectSongService {

    private final ProjectSongRepository projectSongRepository;
    private final MusicProjectRepository musicProjectRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ChordSheetValidation chordSheetValidation;
    private final ProjectSongCategoryRepository categoryRepository;

    public ProjectSongServiceImpl(ProjectSongRepository projectSongRepository,
                                  MusicProjectRepository musicProjectRepository,
                                  CurrentUserProvider currentUserProvider,
                                  ChordSheetValidation chordSheetValidation,
                                  ProjectSongCategoryRepository categoryRepository) {
        this.projectSongRepository = projectSongRepository;
        this.musicProjectRepository = musicProjectRepository;
        this.currentUserProvider = currentUserProvider;
        this.chordSheetValidation = chordSheetValidation;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectSongDTO> listByProject(UUID projectId) {
        return projectSongRepository.findByMusicProject_IdAndDeletedAtIsNullOrderByTitleAsc(projectId).stream()
                .map(ProjectSongDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectSongDTO getById(UUID projectId, UUID songId) {
        return ProjectSongDTO.fromEntity(findOrThrow(projectId, songId));
    }

    @Override
    @Transactional
    public ProjectSongDTO create(UUID projectId, ProjectSongRequestDTO requestDto) {
        MusicProject project = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new ValidationException("Projeto não encontrado."));
        if (project.getType() == MusicProjectType.MEDIA) {
            throw new ValidationException("Projetos do tipo mídia não possuem repertório.");
        }

        ProjectSong song = new ProjectSong();
        song.setMusicProject(project);
        song.setCreatedByUserId(currentUserProvider.get().getId());
        applyRequest(song, requestDto);
        return ProjectSongDTO.fromEntity(projectSongRepository.save(song));
    }

    @Override
    @Transactional
    public ProjectSongDTO update(UUID projectId, UUID songId, ProjectSongRequestDTO requestDto) {
        ProjectSong song = findOrThrow(projectId, songId);
        applyRequest(song, requestDto);
        return ProjectSongDTO.fromEntity(projectSongRepository.save(song));
    }

    // Exclusão lógica: repertórios de eventos que já usam a música continuam mostrando.
    @Override
    @Transactional
    public void delete(UUID projectId, UUID songId) {
        ProjectSong song = findOrThrow(projectId, songId);
        song.setDeletedAt(LocalDateTime.now());
        projectSongRepository.save(song);
    }

    // Substitui as categorias da música; só aceita categorias do próprio projeto.
    @Override
    @Transactional
    public ProjectSongDTO assignCategories(UUID projectId, UUID songId, Set<UUID> categoryIds) {
        ProjectSong song = findOrThrow(projectId, songId);
        Set<UUID> ids = categoryIds == null ? Set.of() : categoryIds;
        List<ProjectSongCategory> categories = ids.isEmpty()
                ? List.of()
                : categoryRepository.findByIdInAndMusicProject_Id(ids, projectId);
        if (categories.size() != ids.size()) {
            throw new ValidationException("Uma ou mais categorias informadas não existem neste projeto.");
        }

        song.setCategories(new HashSet<>(categories));
        return ProjectSongDTO.fromEntity(projectSongRepository.save(song));
    }

    @Override
    @Transactional(readOnly = true)
    public ChordSheetDTO getChordSheet(UUID projectId, UUID songId) {
        ProjectSong song = findOrThrow(projectId, songId);
        return new ChordSheetDTO(song.getId(), song.getChordSheetJson(), null);
    }

    @Override
    @Transactional
    public ChordSheetDTO updateChordSheet(UUID projectId, UUID songId, String chordSheetJson) {
        ProjectSong song = findOrThrow(projectId, songId);
        chordSheetValidation.validate(chordSheetJson);

        song.setChordSheetJson(chordSheetJson);
        ProjectSong saved = projectSongRepository.save(song);
        return new ChordSheetDTO(saved.getId(), saved.getChordSheetJson(), null);
    }

    private void applyRequest(ProjectSong song, ProjectSongRequestDTO dto) {
        song.setTitle(dto.title().trim());
        song.setArtist(dto.artist().trim());
        song.setKey(dto.key().trim());
        song.setBpm(dto.bpm());
        song.setIsrc(blankToNull(dto.isrc()));
        song.setAlbum(blankToNull(dto.album()));
        song.setYouTubeUrl(blankToNull(dto.youTubeUrl()));
        song.setSpotifyUrl(blankToNull(dto.spotifyUrl()));
        song.setDeezerUrl(blankToNull(dto.deezerUrl()));
        song.setCoverUrl(blankToNull(dto.coverUrl()));
        song.setNotes(blankToNull(dto.notes()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ProjectSong findOrThrow(UUID projectId, UUID songId) {
        return projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId)
                .orElseThrow(() -> new ValidationException("Música não encontrada."));
    }
}
