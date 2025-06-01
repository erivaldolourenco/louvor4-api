package br.com.louvor4.api.models;

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

    @Column(name = "role", nullable = false, length = 50)
    private String role;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UserMinistryId getId() {
        return id;
    }

    public void setId(UserMinistryId id) {
        this.id = id;
    }
}
