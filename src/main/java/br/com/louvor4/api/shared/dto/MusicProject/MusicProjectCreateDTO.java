package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.MusicProjectType;

public class MusicProjectCreateDTO {
    private String name;
    private MusicProjectType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MusicProjectType getType() {
        return type;
    }

    public void setType(MusicProjectType type) {
        this.type = type;
    }
}
