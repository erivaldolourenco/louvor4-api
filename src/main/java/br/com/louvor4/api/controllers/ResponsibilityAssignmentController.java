package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ResponsibilityAssignmentService;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("music-project/{projectId}/responsibilities/{responsibilityId}/assignments")
public class ResponsibilityAssignmentController {

    private final ResponsibilityAssignmentService assignmentService;

    public ResponsibilityAssignmentController(ResponsibilityAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<ResponsibilityAssignmentDTO>> listHistory(
            @PathVariable UUID projectId,
            @PathVariable UUID responsibilityId) {
        return ResponseEntity.ok(assignmentService.listHistory(projectId, responsibilityId));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<ResponsibilityAssignmentDTO> assign(
            @PathVariable UUID projectId,
            @PathVariable UUID responsibilityId,
            @RequestBody @Valid ResponsibilityAssignmentRequestDTO requestDto) {
        ResponsibilityAssignmentDTO dto = assignmentService.assign(projectId, responsibilityId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{assignmentId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<ResponsibilityAssignmentDTO> update(
            @PathVariable UUID projectId,
            @PathVariable UUID responsibilityId,
            @PathVariable UUID assignmentId,
            @RequestBody @Valid ResponsibilityAssignmentRequestDTO requestDto) {
        return ResponseEntity.ok(assignmentService.update(projectId, responsibilityId, assignmentId, requestDto));
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID responsibilityId,
            @PathVariable UUID assignmentId) {
        assignmentService.delete(projectId, responsibilityId, assignmentId);
        return ResponseEntity.noContent().build();
    }
}
