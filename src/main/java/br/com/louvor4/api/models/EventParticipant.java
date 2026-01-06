package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.EventPermission;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "event_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_event_user",
                        columnNames = {"event_id", "user_id"}
                )
        }
)
public class EventParticipant {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, columnDefinition = "BINARY(16)"
    )
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private User user;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_participant_permissions", joinColumns = @JoinColumn(name = "event_participant_id", columnDefinition = "BINARY(16)"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 50)
    private Set<EventPermission> permissions =
            EnumSet.noneOf(EventPermission.class);


    public boolean hasPermission(EventPermission permission) {
        return permissions.contains(permission);
    }

    public void grant(EventPermission permission) {
        permissions.add(permission);
    }

    public void revoke(EventPermission permission) {
        permissions.remove(permission);
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<EventPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<EventPermission> permissions) {
        this.permissions = permissions;
    }
}
