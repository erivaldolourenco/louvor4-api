package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.AudioType;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audio_files")
public class AudioFile {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", columnDefinition = "uuid")
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medley_id", columnDefinition = "uuid")
    private Medley medley;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AudioType type;

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

    public Medley getMedley() { return medley; }
    public void setMedley(Medley medley) { this.medley = medley; }

    public AudioType getType() { return type; }
    public void setType(AudioType type) { this.type = type; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
