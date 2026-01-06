package br.com.louvor4.api.shared.dto.Event;

import br.com.louvor4.api.enums.EventPermission;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public class EventParticipantDTO {
    @NotNull
    private UUID userId;

    private Set<EventPermission> permissions;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Set<EventPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<EventPermission> permissions) {
        this.permissions = permissions;
    }
}
