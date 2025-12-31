package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "music_project_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_project_user", columnNames = {"music_project_id", "user_id"})
        }
)
public class MusicProjectMember {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(columnDefinition = "CHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "music_project_id", nullable = false)
    private MusicProject musicProject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProjectMemberRole role = ProjectMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectMemberStatus status = ProjectMemberStatus.ACTIVE;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "added_by_user_id", columnDefinition = "BINARY(16)")
    private UUID addedByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MusicProject getMusicProject() {
        return musicProject;
    }

    public void setMusicProject(MusicProject musicProject) {
        this.musicProject = musicProject;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ProjectMemberRole getRole() {
        return role;
    }

    public void setRole(ProjectMemberRole role) {
        this.role = role;
    }

    public ProjectMemberStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectMemberStatus status) {
        this.status = status;
    }

    public UUID getAddedByUserId() {
        return addedByUserId;
    }

    public void setAddedByUserId(UUID addedByUserId) {
        this.addedByUserId = addedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
