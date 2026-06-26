package br.com.louvor4.api.repositories.projections;

import java.time.LocalDateTime;

public interface PastEventParticipantProjection {
    String getParticipantId();
    String getParticipantStatus();
    String getEventId();
    String getEventTitle();
    String getEventDescription();
    LocalDateTime getEventStartAt();
    String getEventLocation();
    String getProjectId();
    String getProjectName();
    String getProjectProfileImage();
    Integer getParticipantsCount();
    Integer getRepertoireCount();
}
