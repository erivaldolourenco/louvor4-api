package br.com.louvor4.api.repositories.projections;

import java.util.UUID;

public interface EventProfileImageProjection {
    UUID getEventId();
    String getProfileImage();
}
