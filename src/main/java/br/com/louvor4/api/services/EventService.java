package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import br.com.louvor4.api.shared.dto.Event.SetlistDTO;
import br.com.louvor4.api.shared.dto.Event.UpdateEventDto;
import br.com.louvor4.api.shared.dto.Event.UserEventDetailDto;
import br.com.louvor4.api.shared.dto.Song.AddEventSetlistItemDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface EventService {

    void addOrUpdateParticipantsToEvent(UUID eventId, List<EventParticipantDTO> participantDto);

    void acceptParticipation(UUID participantId);

    void declineParticipation(UUID participantId);

    List<UserEventDetailDto> getEventsByUser();

    Page<UserEventDetailDto> getPastEventsByUser(Pageable pageable);

    EventDetailDto getEventById(UUID eventId);

    List<EventParticipantResponseDTO> getParticipants(UUID eventId);

    void addSetListItemToEvent(UUID eventId, @Valid List<AddEventSetlistItemDTO> addEventSetlistItemDto);

    List<SetlistDTO> getSetlist(UUID eventId);

    void removeSetlistItemFromEvent(UUID eventId, UUID setlistItemId);

    void deleteEventById(UUID id);

    void updateEventBy(UUID eventId, @Valid UpdateEventDto eventDto);


}
