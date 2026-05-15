package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.SetlistItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_setlist_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_event_setlist_song", columnNames = {"event_id", "song_id", "type"})
        },
        indexes = {
                @Index(name = "idx_event_setlist_items_event_id", columnList = "event_id"),
                @Index(name = "idx_event_setlist_items_event_id_sequence", columnList = "event_id, sequence")
        }
)
public class EventSetlistItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, columnDefinition = "uuid")
    private Event event;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SetlistItemType type;

    @NotNull
    @Min(1)
    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", columnDefinition = "uuid")
    private Song song;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medley_id", columnDefinition = "uuid")
    private Medley medley;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "added_by_participant_id", nullable = false, columnDefinition = "uuid")
    private EventParticipant addedBy;

    @Column(name = "musical_key", length = 10)
    private String key;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        validateByType();

        var now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        validateByType();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isSong() {
        return SetlistItemType.SONG.equals(this.type);
    }

    public boolean isMedley() {
        return SetlistItemType.MEDLEY.equals(this.type);
    }

    /**
     * Regras de negócio do item de repertório:
     * - Quando o tipo for SONG, o campo song deve estar preenchido.
     * - Quando o tipo for MEDLEY, o relacionamento específico será adicionado futuramente.
     * - O campo addedBy deve ser preenchido para rastrear quem adicionou o item.
     */
    private void validateByType() {
        if (isSong() && this.song == null) {
            throw new IllegalStateException("Item de repertório do tipo SONG deve conter uma música.");
        }
        if (this.addedBy == null) {
            throw new IllegalStateException("Item de repertório deve conter o participante que o adicionou.");
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public SetlistItemType getType() {
        return type;
    }

    public void setType(SetlistItemType type) {
        this.type = type;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Song getSong() {
        return song;
    }

    public void setSong(Song song) {
        this.song = song;
    }

    public Medley getMedley() {
        return medley;
    }

    public void setMedley(Medley medley) {
        this.medley = medley;
    }

    public EventParticipant getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(EventParticipant addedBy) {
        this.addedBy = addedBy;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
