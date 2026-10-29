package br.com.louvor4.api.models;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/** Categoria das músicas do projeto (equivalente a {@link SongCategory}, mas do projeto). */
@Entity
@Table(
        name = "project_song_categories",
        uniqueConstraints = @UniqueConstraint(name = "uq_project_song_category_name", columnNames = {"music_project_id", "name"})
)
public class ProjectSongCategory {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "music_project_id", nullable = false, columnDefinition = "uuid")
    private MusicProject musicProject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

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

    public MusicProject getMusicProject() {
        return musicProject;
    }

    public void setMusicProject(MusicProject musicProject) {
        this.musicProject = musicProject;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
