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
                        name = "uq_event_member_skill", // Mudou aqui!
                        columnNames = {"event_id", "project_member_id", "project_skill_id"}
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
    @JoinColumn(name = "event_id", nullable = false, columnDefinition = "BINARY(16)")
    private Event event;

    // Alterado: Aponta para o Membro do Projeto, não para o User puro
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private MusicProjectMember member;

    // NOVO: A Skill que ele vai exercer NESTE evento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_skill_id", columnDefinition = "BINARY(16)")
    private ProjectSkill skill;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_participant_permissions",
            joinColumns = @JoinColumn(name = "event_participant_id", columnDefinition = "BINARY(16)"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 50)
    private Set<EventPermission> permissions = EnumSet.noneOf(EventPermission.class);

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

    public MusicProjectMember getMember() {
        return member;
    }

    public void setMember(MusicProjectMember member) {
        this.member = member;
    }

    public ProjectSkill getSkill() {
        return skill;
    }

    public void setSkill(ProjectSkill skill) {
        this.skill = skill;
    }

    public Set<EventPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<EventPermission> permissions) {
        this.permissions = permissions;
    }
}
