package br.com.louvor4.api.validations;

import br.com.louvor4.api.enums.EventPermission;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventSong;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class EventValidation {

    public void validateRequestAllowEmpty(List<EventParticipantDTO> participants) {
        if (participants == null) return;

        if (participants.isEmpty()) return;

        Set<UUID> seen = new HashSet<>();
        for (EventParticipantDTO dto : participants) {
            if (dto.getMemberId() == null) {
                throw new ValidationException("memberId é obrigatório.");
            }
            if (!seen.add(dto.getMemberId())) {
                throw new ValidationException("Participante duplicado no payload: " + dto.getMemberId());
            }
        }
    }

    public void canAddSong(EventParticipant participant){
        if (!participant.getPermissions().contains(EventPermission.ADD_SONG)) {
            throw new ValidationException("Você não tem permissão para remover músicas neste evento.");
        }
    }

    public void validateSongBelongsToEvent(EventSong eventSong, UUID eventId){
        if (eventSong.getEvent() == null || !eventId.equals(eventSong.getEvent().getId())) {
            throw new ValidationException("Esta música não pertence ao evento informado.");
        }
    }

    public void validateDeletionRules(Integer participantCount, Integer songCount) {
        if (isPresent(participantCount)) {
            throw new ValidationException("Não é possível excluir o evento: existem participantes associados.");
        }
        if (isPresent(songCount)) {
            throw new ValidationException("Não é possível excluir o evento: existem músicas associadas.");
        }
    }

    private boolean isPresent(Integer count) {
        return count != null && count > 0;
    }

}
