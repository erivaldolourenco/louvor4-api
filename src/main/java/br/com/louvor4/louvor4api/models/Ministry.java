package br.com.louvor4.louvor4api.models;

import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_ministry")
public class Ministry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(name = "name")
    private String name;
    @Column(name = "access_code")
    private String accessCode;

    @OneToMany(mappedBy = "ministry", cascade = CascadeType.ALL)
    private List<Member> members;


    public Ministry() {
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

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> member) {
        this.members = member;
    }
}
