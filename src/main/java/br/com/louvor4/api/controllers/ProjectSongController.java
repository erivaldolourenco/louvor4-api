package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ProjectSongService;
import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongDTO;
import br.com.louvor4.api.shared.dto.ProjectSong.ProjectSongRequestDTO;
import br.com.louvor4.api.shared.dto.Song.AssignSongCategoriesDTO;
import br.com.louvor4.api.shared.dto.Song.ChordSheetDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("music-project/{projectId}/songs")
public class ProjectSongController {

    private final ProjectSongService projectSongService;

    public ProjectSongController(ProjectSongService projectSongService) {
        this.projectSongService = projectSongService;
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<ProjectSongDTO>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectSongService.listByProject(projectId));
    }

    @GetMapping("/{songId}")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<ProjectSongDTO> getById(@PathVariable UUID projectId, @PathVariable UUID songId) {
        return ResponseEntity.ok(projectSongService.getById(projectId, songId));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.canManageProjectSongs(#projectId)")
    public ResponseEntity<ProjectSongDTO> create(
            @PathVariable UUID projectId,
            @RequestBody @Valid ProjectSongRequestDTO requestDto) {
        ProjectSongDTO dto = projectSongService.create(projectId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{songId}")
    @PreAuthorize("@projectSecurity.canManageProjectSongs(#projectId)")
    public ResponseEntity<ProjectSongDTO> update(
            @PathVariable UUID projectId,
            @PathVariable UUID songId,
            @RequestBody @Valid ProjectSongRequestDTO requestDto) {
        return ResponseEntity.ok(projectSongService.update(projectId, songId, requestDto));
    }

    @PutMapping("/{songId}/categories")
    @PreAuthorize("@projectSecurity.canManageProjectSongs(#projectId)")
    public ResponseEntity<ProjectSongDTO> assignCategories(
            @PathVariable UUID projectId,
            @PathVariable UUID songId,
            @RequestBody @Valid AssignSongCategoriesDTO assignDto) {
        return ResponseEntity.ok(projectSongService.assignCategories(projectId, songId, assignDto.categoryIds()));
    }

    @DeleteMapping("/{songId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId, @PathVariable UUID songId) {
        projectSongService.delete(projectId, songId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{songId}/chord-sheet")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<ChordSheetDTO> getChordSheet(@PathVariable UUID projectId, @PathVariable UUID songId) {
        return ResponseEntity.ok(projectSongService.getChordSheet(projectId, songId));
    }

    @PutMapping("/{songId}/chord-sheet")
    @PreAuthorize("@projectSecurity.canManageProjectSongs(#projectId)")
    public ResponseEntity<ChordSheetDTO> updateChordSheet(
            @PathVariable UUID projectId,
            @PathVariable UUID songId,
            @RequestBody @Valid ChordSheetDTO chordSheetDto) {
        return ResponseEntity.ok(projectSongService.updateChordSheet(projectId, songId, chordSheetDto.chordSheetJson()));
    }
}
