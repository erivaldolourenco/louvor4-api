package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ProjectSongCategoryService;
import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;
import br.com.louvor4.api.shared.dto.Song.SongCategoryRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("music-project/{projectId}/song-categories")
public class ProjectSongCategoryController {

    private final ProjectSongCategoryService categoryService;

    public ProjectSongCategoryController(ProjectSongCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<SongCategoryDTO>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(categoryService.listByProject(projectId));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.canManageProjectSongs(#projectId)")
    public ResponseEntity<SongCategoryDTO> create(
            @PathVariable UUID projectId,
            @RequestBody @Valid SongCategoryRequestDTO requestDto) {
        SongCategoryDTO dto = categoryService.create(projectId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("@projectSecurity.canManageProjectSongs(#projectId)")
    public ResponseEntity<SongCategoryDTO> update(
            @PathVariable UUID projectId,
            @PathVariable UUID categoryId,
            @RequestBody @Valid SongCategoryRequestDTO requestDto) {
        return ResponseEntity.ok(categoryService.update(projectId, categoryId, requestDto));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("@projectSecurity.canManageProjectSongs(#projectId)")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId, @PathVariable UUID categoryId) {
        categoryService.delete(projectId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
