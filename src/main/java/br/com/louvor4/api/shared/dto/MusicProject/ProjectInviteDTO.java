package br.com.louvor4.api.shared.dto.MusicProject;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProjectInviteDTO {
    private UUID memberId;
    private UUID projectId;
    private String projectName;
    private String projectProfileImage;
    private UUID invitedByUserId;
    private LocalDateTime invitedAt;

    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectProfileImage() { return projectProfileImage; }
    public void setProjectProfileImage(String projectProfileImage) { this.projectProfileImage = projectProfileImage; }

    public UUID getInvitedByUserId() { return invitedByUserId; }
    public void setInvitedByUserId(UUID invitedByUserId) { this.invitedByUserId = invitedByUserId; }

    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
}
