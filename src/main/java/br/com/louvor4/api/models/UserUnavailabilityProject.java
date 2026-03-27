package br.com.louvor4.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_unavailability_projects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_unavailability_project",
                        columnNames = {"unavailability_id", "project_id"}
                )
        }
)
public class UserUnavailabilityProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unavailability_id", nullable = false, columnDefinition = "uuid")
    private UserUnavailability unavailability;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, columnDefinition = "uuid")
    private MusicProject project;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserUnavailability getUnavailability() {
        return unavailability;
    }

    public void setUnavailability(UserUnavailability unavailability) {
        this.unavailability = unavailability;
    }

    public MusicProject getProject() {
        return project;
    }

    public void setProject(MusicProject project) {
        this.project = project;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
