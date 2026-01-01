package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectCreateDTO;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDetailDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("songs")
public class SongController {

    private final SongService songService;

    public SongController(SongService songService) {
        this.songService = songService;
    }


    @PostMapping("/create")
    public ResponseEntity<SongDTO> create(@RequestBody @Valid SongDTO createDto) {
        SongDTO dto = songService.create(createDto);
        return ResponseEntity.ok(dto);
    }

}
