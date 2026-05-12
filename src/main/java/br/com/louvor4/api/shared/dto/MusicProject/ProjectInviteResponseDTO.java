package br.com.louvor4.api.shared.dto.MusicProject;

import jakarta.validation.constraints.NotNull;

public class ProjectInviteResponseDTO {
    @NotNull
    private Boolean accepted;

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
}
