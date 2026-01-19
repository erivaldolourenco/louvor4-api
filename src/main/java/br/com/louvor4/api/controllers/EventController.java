package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import br.com.louvor4.api.shared.dto.Song.AddEventSongDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDto> findById(@PathVariable UUID id) {
        EventDetailDto eventDetailDto = eventService.getEventById(id);
        return ResponseEntity.ok(eventDetailDto);
    }

    @GetMapping("/{eventId}/participants")
    public ResponseEntity<List<EventParticipantResponseDTO>> getParticipants(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getParticipants(eventId));
    }

    @PostMapping("/{eventId}/participants")
    public ResponseEntity<Void> addOrUpdateParticipant(@PathVariable UUID eventId, @RequestBody @Valid List<EventParticipantDTO> participantDto) {
        eventService.addOrUpdateParticipantsToEvent(eventId, participantDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{eventId}/songs")
    public ResponseEntity<Void> addSong(@PathVariable UUID eventId, @RequestBody @Valid AddEventSongDTO addEventSongDto) {
        eventService.addSongToEvent(eventId, addEventSongDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
