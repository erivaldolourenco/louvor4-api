package br.com.louvor4.api.models.embeddedid;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
public class UserMinistryId implements Serializable {
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "ministry_id")
    private UUID ministryId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getMinistryId() {
        return ministryId;
    }

    public void setMinistryId(UUID ministryId) {
        this.ministryId = ministryId;
    }
}
