package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import br.com.louvor4.api.shared.dto.Song.AddEventSongDTO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface EventService {

    void addOrUpdateParticipantsToEvent(UUID eventId, List<EventParticipantDTO> participantDto);
    List<EventDetailDto> getEventsByUser();
    EventDetailDto getEventById(UUID eventId);

    List<EventParticipantResponseDTO> getParticipants(UUID eventId);

    void addSongToEvent(UUID eventId, @Valid AddEventSongDTO addEventSongDto);
}
