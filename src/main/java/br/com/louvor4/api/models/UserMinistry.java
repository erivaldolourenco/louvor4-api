package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.UserRole;
import br.com.louvor4.api.models.embeddedid.UserMinistryId;
import jakarta.persistence.*;

@Entity
@Table(name = "user_ministries")
public class UserMinistry {
    @EmbeddedId
    private UserMinistryId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ministryId")
    @JoinColumn(name = "ministry_id")
    private Ministry ministry;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Ministry getMinistry() {
        return ministry;
    }

    public void setMinistry(Ministry ministry) {
        this.ministry = ministry;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserMinistryId getId() {
        return id;
    }

    public void setId(UserMinistryId id) {
        this.id = id;
    }
}
