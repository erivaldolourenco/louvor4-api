package br.com.louvor4.api.controllers;


import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectCreateDTO;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDTO;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDetailDTO;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("music-project")
public class MusicProjectController {

    private final MusicProjectService musicProjectService;

    public MusicProjectController(MusicProjectService musicProjectService) {
        this.musicProjectService = musicProjectService;
    }


    @PostMapping("/create")
    public ResponseEntity<MusicProjectDetailDTO> create(@RequestBody @Valid MusicProjectCreateDTO createDto) {
        MusicProjectDetailDTO dto = musicProjectService.create(createDto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MusicProjectDetailDTO> findById(@PathVariable UUID id) {
        MusicProjectDetailDTO musicProjectDetailDTO = musicProjectService.getMusicProjectById(id);
        return ResponseEntity.ok(musicProjectDetailDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MusicProjectDetailDTO> update(@PathVariable UUID id, @RequestBody @Valid MusicProjectDTO updateDto) {
        MusicProjectDetailDTO musicProjectDetailDTO = musicProjectService.update(id,updateDto);
        return ResponseEntity.ok(musicProjectDetailDTO);
    }

    @PutMapping(value = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfileImage(
            @PathVariable UUID id,
            @RequestPart("profileImage") MultipartFile profileImage
    ) {
        String url = musicProjectService.updateImage(id, profileImage);
        return ResponseEntity.ok(url);
    }
}
