package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.MusicProjectType;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class MusicProjectDetailDTO implements Serializable {
    private UUID id;
    private String name;
    private MusicProjectType type;
    private String profileImage;
    private List<MemberDTO> members;

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

    public MusicProjectType getType() {
        return type;
    }

    public void setType(MusicProjectType type) {
        this.type = type;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public List<MemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<MemberDTO> members) {
        this.members = members;
    }
}
