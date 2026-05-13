package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.shared.dto.Program.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/program")
public class EventProgramController {

    private final ProgramService programService;

    public EventProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<List<ProgramItemResponse>> getProgram(@PathVariable UUID eventId) {
        return ResponseEntity.ok(programService.getProgram(eventId));
    }

    @PostMapping("/text")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> addTextItem(
            @PathVariable UUID eventId,
            @RequestBody @Valid CreateTextProgramItemRequest request) {
        programService.addTextItem(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> updateTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId,
            @RequestBody @Valid UpdateTextProgramItemRequest request) {
        programService.updateTextItem(eventId, itemId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> deleteTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId) {
        programService.deleteTextItem(eventId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID eventId,
            @RequestBody @Valid ReorderProgramRequest request) {
        programService.reorder(eventId, request);
        return ResponseEntity.noContent().build();
    }
}
