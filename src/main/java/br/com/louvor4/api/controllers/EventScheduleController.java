package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ScheduleService;
import br.com.louvor4.api.shared.dto.Schedule.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/schedule")
public class EventScheduleController {

    private final ScheduleService scheduleService;

    public EventScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<List<ScheduleItemResponse>> getSchedule(@PathVariable UUID eventId) {
        return ResponseEntity.ok(scheduleService.getSchedule(eventId));
    }

    @PostMapping("/text")
    public ResponseEntity<Void> addTextItem(
            @PathVariable UUID eventId,
            @RequestBody @Valid CreateTextScheduleItemRequest request) {
        scheduleService.addTextItem(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<Void> updateTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId,
            @RequestBody @Valid UpdateTextScheduleItemRequest request) {
        scheduleService.updateTextItem(eventId, itemId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId) {
        scheduleService.deleteTextItem(eventId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID eventId,
            @RequestBody @Valid ReorderScheduleRequest request) {
        scheduleService.reorder(eventId, request);
        return ResponseEntity.noContent().build();
    }
}
