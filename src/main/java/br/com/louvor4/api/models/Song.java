package br.com.louvor4.api.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "songs")
public class Song {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, columnDefinition = "uuid")
    private User user;

    @NotBlank
    @Size(min = 3, max = 150)
    @Column(name = "artist", nullable = false, length = 150)
    private String artist;

    @NotBlank
    @Size(min = 3, max = 150)
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @NotBlank
    @Size(max = 5)
    @Pattern(regexp = "^[A-G](#|b)?m?$", message = "Tom inválido. Use C, D, E, F, G, A ou B com #, b e/ou m.")
    @Column(name = "musical_key", nullable = false, length = 5)
    private String key;

    @Column(name = "bpm")
    private Integer bpm;

    @NotBlank
    @Column(name = "youtube_url", nullable = false, length = 255)
    private String youTubeUrl;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "lyrics", columnDefinition = "TEXT")
    private String lyrics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chord_sheet_json", columnDefinition = "jsonb")
    private String chordSheetJson;

    @Column(name = "edit_chord_sheet_permission")
    private Boolean editChordSheetPermission = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getBpm() {
        return bpm;
    }

    public void setBpm(Integer bpm) {
        this.bpm = bpm;
    }

    public String getYouTubeUrl() {
        return youTubeUrl;
    }

    public void setYouTubeUrl(String youTubeUrl) {
        this.youTubeUrl = youTubeUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public String getChordSheetJson() {
        return chordSheetJson;
    }

    public void setChordSheetJson(String chordSheetJson) {
        this.chordSheetJson = chordSheetJson;
    }

    public Boolean isEditChordSheetPermission() {
        return editChordSheetPermission;
    }

    public void setEditChordSheetPermission(Boolean editChordSheetPermission) {
        this.editChordSheetPermission = editChordSheetPermission;
    }
}
