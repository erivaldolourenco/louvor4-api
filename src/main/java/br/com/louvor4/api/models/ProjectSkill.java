package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.SkillIcon;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "project_skills")
public class ProjectSkill {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "icon_key", nullable = true, length = 50)
    @Enumerated(EnumType.STRING)
    private SkillIcon iconKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "music_project_id", nullable = false)
    private MusicProject musicProject;

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

    public SkillIcon getIconKey() {
        return iconKey;
    }

    public void setIconKey(SkillIcon iconKey) {
        this.iconKey = iconKey;
    }

    public MusicProject getMusicProject() {
        return musicProject;
    }

    public void setMusicProject(MusicProject musicProject) {
        this.musicProject = musicProject;
    }
}
