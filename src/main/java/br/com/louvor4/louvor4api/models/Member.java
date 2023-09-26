package br.com.louvor4.louvor4api.models;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "tb_member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "id_person")
    private Person person;

    @ManyToOne
    @JoinColumn(name = "id_ministry")
    private Ministry ministry;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "tb_member_permission", joinColumns = {@JoinColumn(name = "id_member")}
            , inverseJoinColumns = {@JoinColumn(name = "id_ministry_permission")})
    private List<MinistryPermission> ministryPermissions;


//    @Column
//    private List<MemberRoles> memberRoles;

    public Member() {
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public List<MinistryPermission> getMinistryPermissions() {
        return ministryPermissions;
    }

    public void setMinistryPermissions(List<MinistryPermission> ministryPermissions) {
        this.ministryPermissions = ministryPermissions;
    }

    public Ministry getMinistry() {
        return ministry;
    }

    public void setMinistry(Ministry ministry) {
        this.ministry = ministry;
    }
}
