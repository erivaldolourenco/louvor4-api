package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.EventRoteiroService;
import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.shared.dto.Event.*;
import br.com.louvor4.api.shared.dto.Song.AddEventSetlistItemDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final EventRoteiroService eventRoteiroService;

    public EventController(EventService eventService, EventRoteiroService eventRoteiroService) {
        this.eventService = eventService;
        this.eventRoteiroService = eventRoteiroService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#id)")
    public ResponseEntity<EventDetailDto> findById(@PathVariable UUID id) {
        EventDetailDto eventDetailDto = eventService.getEventById(id);
        return ResponseEntity.ok(eventDetailDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#id)")
    public ResponseEntity<Void> deleteEventById(@PathVariable UUID id) {
        eventService.deleteEventById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> updateEvent(@PathVariable UUID eventId, @RequestBody @Valid UpdateEventDto eventDto) {
        eventService.updateEventBy(eventId, eventDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/participants")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<List<EventParticipantResponseDTO>> getParticipants(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getParticipants(eventId));
    }

    @PostMapping("/{eventId}/participants")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> addOrUpdateParticipant(@PathVariable UUID eventId, @RequestBody @Valid List<EventParticipantDTO> participantDto) {
        eventService.addOrUpdateParticipantsToEvent(eventId, participantDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/participants/{participantId}/accept")
    public ResponseEntity<Void> acceptParticipation(@PathVariable UUID participantId) {
        eventService.acceptParticipation(participantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/participants/{participantId}/decline")
    public ResponseEntity<Void> declineParticipation(@PathVariable UUID participantId) {
        eventService.declineParticipation(participantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/songs")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<Void> addSetListItem(@PathVariable UUID eventId, @RequestBody @Valid List<AddEventSetlistItemDTO> addEventSetlistItemDto) {
        eventService.addSetListItemToEvent(eventId, addEventSetlistItemDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{eventId}/setlist/{setlistItemId}")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<Void> deleteSetlistItemFromEvent(@PathVariable UUID eventId, @PathVariable UUID setlistItemId) {
        eventService.removeSetlistItemFromEvent(eventId, setlistItemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/setlist")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<List<SetlistDTO>> getSetlist(@PathVariable UUID eventId) {
        List<SetlistDTO> setlist = eventService.getSetlist(eventId);
        return ResponseEntity.ok(setlist);
    }

    @GetMapping("/{eventId}/roteiro/pdf")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<byte[]> getRoteiroAsPdf(@PathVariable UUID eventId) {
        EventRoteiroService.Result result = eventRoteiroService.generatePdf(eventId);
        String filename = sanitizeFilename(result.eventTitle()) + "-roteiro.pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(result.pdf());
    }

    private String sanitizeFilename(String title) {
        if (title == null || title.isBlank()) return "roteiro";
        return title.trim()
                .replaceAll("[^a-zA-Z0-9\\-_. áéíóúâêîôûãõàèìòùç]", "")
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }
}
