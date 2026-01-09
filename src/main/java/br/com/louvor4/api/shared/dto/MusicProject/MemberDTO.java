package br.com.louvor4.api.shared.dto.MusicProject;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;

import java.util.Set;
import java.util.UUID;

public class MemberDTO {
    private UUID id;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String profileImage;
    private ProjectMemberRole projectRole;
    private ProjectMemberStatus status;
    private Set<String> skills;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public ProjectMemberRole getProjectRole() {
        return projectRole;
    }

    public void setProjectRole(ProjectMemberRole projectRole) {
        this.projectRole = projectRole;
    }

    public ProjectMemberStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectMemberStatus status) {
        this.status = status;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public void setSkills(Set<String> skills) {
        this.skills = skills;
    }
}
