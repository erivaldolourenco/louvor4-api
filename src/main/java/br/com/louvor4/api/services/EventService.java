package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface EventService {

    void addParticipantsToEvent(UUID eventId, List<EventParticipantDTO> participantDto);
    List<EventDetailDto> getEventsByUser();
    EventDetailDto getEventById(UUID eventId);

    List<EventParticipantResponseDTO> getParticipants(UUID eventId);
}
