package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.ProgramItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_program_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_event_program_setlist_item",
                        columnNames = {"event_id", "setlist_item_id"}
                )
        },
        indexes = {
                @Index(name = "idx_event_program_items_event_id", columnList = "event_id"),
                @Index(name = "idx_event_program_items_event_id_position", columnList = "event_id, position")
        }
)
public class EventProgramItem {

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
    private ProgramItemType type;

    @NotNull
    @Min(1)
    @Column(name = "position", nullable = false)
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_item_id", columnDefinition = "uuid")
    private EventSetlistItem setlistItem;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        var now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isMusic() {
        return ProgramItemType.MUSIC.equals(this.type);
    }

    public boolean isText() {
        return ProgramItemType.TEXT.equals(this.type);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public ProgramItemType getType() { return type; }
    public void setType(ProgramItemType type) { this.type = type; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public EventSetlistItem getSetlistItem() { return setlistItem; }
    public void setSetlistItem(EventSetlistItem setlistItem) { this.setlistItem = setlistItem; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
