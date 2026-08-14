package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.SongCategoryService;
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
@RequestMapping("song-categories")
public class SongCategoryController {

    private final SongCategoryService songCategoryService;

    public SongCategoryController(SongCategoryService songCategoryService) {
        this.songCategoryService = songCategoryService;
    }

    @GetMapping
    public ResponseEntity<List<SongCategoryDTO>> getMine() {
        return ResponseEntity.ok(songCategoryService.getMine());
    }

    @PostMapping
    public ResponseEntity<SongCategoryDTO> create(@RequestBody @Valid SongCategoryRequestDTO requestDto) {
        SongCategoryDTO dto = songCategoryService.create(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("@projectSecurity.isSongCategoryOwner(#categoryId)")
    public ResponseEntity<SongCategoryDTO> update(
            @PathVariable UUID categoryId,
            @RequestBody @Valid SongCategoryRequestDTO requestDto) {
        SongCategoryDTO dto = songCategoryService.update(categoryId, requestDto);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("@projectSecurity.isSongCategoryOwner(#categoryId)")
    public ResponseEntity<Void> delete(@PathVariable UUID categoryId) {
        songCategoryService.delete(categoryId);
        return ResponseEntity.noContent().build();
    }
}
