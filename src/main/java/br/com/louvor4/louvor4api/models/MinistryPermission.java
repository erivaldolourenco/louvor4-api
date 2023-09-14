package br.com.louvor4.louvor4api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_ministry_permission")
public class MinistryPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column
    private String description;

    public MinistryPermission() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
