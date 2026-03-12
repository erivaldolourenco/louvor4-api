package br.com.louvor4.api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "plans", uniqueConstraints = {
        @UniqueConstraint(name = "uq_plan_name", columnNames = {"name"})
})
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "max_projects", nullable = false)
    private Integer maxProjects;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxProjects() {
        return maxProjects;
    }

    public void setMaxProjects(Integer maxProjects) {
        this.maxProjects = maxProjects;
    }
}
