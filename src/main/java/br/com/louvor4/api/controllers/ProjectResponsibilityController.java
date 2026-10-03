package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ProjectResponsibilityService;
import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("music-project/{projectId}/responsibilities")
public class ProjectResponsibilityController {

    private final ProjectResponsibilityService responsibilityService;

    public ProjectResponsibilityController(ProjectResponsibilityService responsibilityService) {
        this.responsibilityService = responsibilityService;
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<ProjectResponsibilityDTO>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(responsibilityService.listByProject(projectId));
    }

    @GetMapping("/{responsibilityId}")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<ProjectResponsibilityDTO> getById(
            @PathVariable UUID projectId,
            @PathVariable UUID responsibilityId) {
        return ResponseEntity.ok(responsibilityService.getById(projectId, responsibilityId));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<ProjectResponsibilityDTO> create(
            @PathVariable UUID projectId,
            @RequestBody @Valid ProjectResponsibilityRequestDTO requestDto) {
        ProjectResponsibilityDTO dto = responsibilityService.create(projectId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{responsibilityId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<ProjectResponsibilityDTO> update(
            @PathVariable UUID projectId,
            @PathVariable UUID responsibilityId,
            @RequestBody @Valid ProjectResponsibilityRequestDTO requestDto) {
        return ResponseEntity.ok(responsibilityService.update(projectId, responsibilityId, requestDto));
    }

    @DeleteMapping("/{responsibilityId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID responsibilityId) {
        responsibilityService.delete(projectId, responsibilityId);
        return ResponseEntity.noContent().build();
    }
}
