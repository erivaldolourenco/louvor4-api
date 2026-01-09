package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ProjectSkillService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("skills")
public class SkillsController {

    private final ProjectSkillService projectSkillService;

    public SkillsController(ProjectSkillService projectSkillService) {
        this.projectSkillService = projectSkillService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable UUID id) {
        projectSkillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

}
