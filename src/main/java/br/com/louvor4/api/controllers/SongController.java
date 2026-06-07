package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.SongAudioType;
import br.com.louvor4.api.services.SongAudioService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Song.SongAudioDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("songs")
public class SongController {

    private final SongService songService;
    private final SongAudioService songAudioService;

    public SongController(SongService songService, SongAudioService songAudioService) {
        this.songService = songService;
        this.songAudioService = songAudioService;
    }

    @GetMapping("/{songId}")
    public ResponseEntity<SongDTO> get(@PathVariable UUID songId) {
        SongDTO dto = songService.get(songId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/create")
    public ResponseEntity<SongDTO> create(@RequestBody @Valid SongDTO createDto) {
        SongDTO dto = songService.create(createDto);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/update")
    public ResponseEntity<SongDTO> update(@RequestBody @Valid SongDTO updateDto) {
        SongDTO dto = songService.update(updateDto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping(value = "/{songId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SongAudioDTO> uploadAudio(
            @PathVariable UUID songId,
            @RequestParam SongAudioType type,
            @RequestParam("file") MultipartFile file) {
        SongAudioDTO dto = songAudioService.uploadAudio(songId, type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
