package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.MusicProjectType;

import java.util.UUID;

public class MusicProjectUpdateDTO {
    private UUID id;
    private String name;
    private MusicProjectType type;
    private String profileImage;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public MusicProjectType getType() {
        return type;
    }

    public void setType(MusicProjectType type) {
        this.type = type;
    }


}
