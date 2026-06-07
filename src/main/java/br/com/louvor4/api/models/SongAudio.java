package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.SongAudioType;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "song_audio_files",
    uniqueConstraints = @UniqueConstraint(columnNames = {"song_id", "type"})
)
public class SongAudio {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false, columnDefinition = "uuid")
    private Song song;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SongAudioType type;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Song getSong() { return song; }
    public void setSong(Song song) { this.song = song; }

    public SongAudioType getType() { return type; }
    public void setType(SongAudioType type) { this.type = type; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
