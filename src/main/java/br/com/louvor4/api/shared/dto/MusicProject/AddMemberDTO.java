package br.com.louvor4.api.shared.dto.MusicProject;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AddMemberDTO {
    @NotNull
    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
