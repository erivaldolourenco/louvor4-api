package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface EventService {

    void addParticipantToEvent(UUID eventId, EventParticipantDTO participantDto);
    List<EventDetailDto> getEventsByUser();
    EventDetailDto getEventById(UUID eventId);
}
