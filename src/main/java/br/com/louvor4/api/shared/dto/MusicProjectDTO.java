package br.com.louvor4.api.shared.dto;

import br.com.louvor4.api.enums.MusicProjectType;

import java.util.UUID;

public class MusicProjectDTO {

    private UUID id;
    private String name;
    private MusicProjectType type;
    private String avatarUrl;

    public MusicProjectDTO(UUID id, String name, MusicProjectType type, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.avatarUrl = avatarUrl;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MusicProjectType getType() {
        return type;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}

