package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import br.com.louvor4.api.shared.dto.Event.UpdateEventDto;
import br.com.louvor4.api.shared.dto.Event.UserEventDetailDto;
import br.com.louvor4.api.shared.dto.Song.AddEventSongDTO;
import br.com.louvor4.api.shared.dto.Song.EventSongDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public interface EventService {

    void addOrUpdateParticipantsToEvent(UUID eventId, List<EventParticipantDTO> participantDto);
    void acceptParticipation(UUID participantId);
    void declineParticipation(UUID participantId);
    List<UserEventDetailDto> getEventsByUser();
    EventDetailDto getEventById(UUID eventId);

    List<EventParticipantResponseDTO> getParticipants(UUID eventId);

    void addSongsToEvent(UUID eventId, @Valid List<AddEventSongDTO> addEventSongsDto);

    List<EventSongDTO> getEventSongs(UUID eventId);

    void removeSongFromEvent(UUID eventId, UUID eventSongId);

    void deleteEventById(UUID id);

    void updateEventBy(UUID eventId, @Valid UpdateEventDto eventDto);
}
