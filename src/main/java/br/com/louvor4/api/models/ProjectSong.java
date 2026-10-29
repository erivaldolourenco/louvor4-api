package br.com.louvor4.api.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Música cujo dono é o projeto (não um usuário). Continua no projeto quando quem a criou sai.
 * Exclusão é lógica (deleted_at): a música some da lista do projeto, mas os repertórios de
 * eventos que já a usam continuam carregando — por isso não há @SQLRestriction aqui.
 */
@Entity
@Table(
        name = "project_songs",
        indexes = {
                @Index(name = "idx_project_songs_project_id", columnList = "music_project_id")
        }
)
public class ProjectSong implements SongData {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "music_project_id", nullable = false, columnDefinition = "uuid")
    private MusicProject musicProject;

    // Só para exibição; vira null se a conta de quem criou for excluída.
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by_user_id", columnDefinition = "uuid")
    private UUID createdByUserId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "artist", nullable = false, length = 150)
    private String artist;

    @Column(name = "musical_key", nullable = false, length = 5)
    private String key;

    @Column(name = "bpm")
    private Integer bpm;

    @Column(name = "isrc", length = 32)
    private String isrc;

    @Column(name = "album", length = 150)
    private String album;

    @Column(name = "youtube_url", length = 255)
    private String youTubeUrl;

    @Column(name = "spotify_url", length = 255)
    private String spotifyUrl;

    @Column(name = "deezer_url", length = 255)
    private String deezerUrl;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "notes", length = 1000)
    private String notes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chord_sheet_json", columnDefinition = "jsonb")
    private String chordSheetJson;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_song_category_assignments",
            joinColumns = @JoinColumn(name = "project_song_id"),
            inverseJoinColumns = @JoinColumn(name = "project_song_category_id")
    )
    private Set<ProjectSongCategory> categories = new HashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MusicProject getMusicProject() {
        return musicProject;
    }

    public void setMusicProject(MusicProject musicProject) {
        this.musicProject = musicProject;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
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

    public String getIsrc() {
        return isrc;
    }

    public void setIsrc(String isrc) {
        this.isrc = isrc;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getYouTubeUrl() {
        return youTubeUrl;
    }

    public void setYouTubeUrl(String youTubeUrl) {
        this.youTubeUrl = youTubeUrl;
    }

    public String getSpotifyUrl() {
        return spotifyUrl;
    }

    public void setSpotifyUrl(String spotifyUrl) {
        this.spotifyUrl = spotifyUrl;
    }

    public String getDeezerUrl() {
        return deezerUrl;
    }

    public void setDeezerUrl(String deezerUrl) {
        this.deezerUrl = deezerUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getChordSheetJson() {
        return chordSheetJson;
    }

    public void setChordSheetJson(String chordSheetJson) {
        this.chordSheetJson = chordSheetJson;
    }

    public Set<ProjectSongCategory> getCategories() {
        return categories;
    }

    public void setCategories(Set<ProjectSongCategory> categories) {
        this.categories = categories;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
