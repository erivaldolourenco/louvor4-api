package br.com.louvor4.api.repositories.projections;

import br.com.louvor4.api.enums.EventParticipantStatus;

import java.util.UUID;

public interface InviteStatsProjection {
    UUID getMemberId();
    UUID getUserId();
    String getFirstName();
    String getLastName();
    String getProfileImage();
    Integer getEventYear();
    Integer getEventMonth();
    EventParticipantStatus getStatus();
    Long getTotal();
}
