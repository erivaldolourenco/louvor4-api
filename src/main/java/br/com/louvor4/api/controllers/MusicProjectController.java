package br.com.louvor4.api.controllers;


import br.com.louvor4.api.enums.MusicProjectType;
import br.com.louvor4.api.shared.dto.MusicProjectDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("music-project")
public class MusicProjectController {
    @GetMapping("/{id}")
    public ResponseEntity<MusicProjectDTO> findById(@PathVariable UUID id) {
        MusicProjectDTO dto = new MusicProjectDTO(
                UUID.randomUUID(),
                "Banda Areli",
                MusicProjectType.BAND,
                "https://picsum.photos/200?random=2"
        );

        return ResponseEntity.ok(dto);
    }
}
