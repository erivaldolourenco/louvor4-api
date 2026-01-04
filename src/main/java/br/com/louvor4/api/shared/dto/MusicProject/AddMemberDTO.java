package br.com.louvor4.api.shared.dto.MusicProject;

import jakarta.validation.constraints.NotNull;


public class AddMemberDTO {
    @NotNull
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
