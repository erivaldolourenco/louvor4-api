package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }


    @GetMapping("/{songId}")
    public ResponseEntity<SongDTO> create(@PathVariable UUID songId) {
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

}
