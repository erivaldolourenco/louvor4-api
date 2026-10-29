package br.com.louvor4.api.models;

import java.util.UUID;

/**
 * Dados de exibição comuns a uma música pessoal ({@link Song}) e a uma música do projeto
 * ({@link ProjectSong}). Usado por quem só precisa mostrar a música do repertório
 * (setlist, roteiro, PDF), sem se importar de quem ela é.
 */
public interface SongData {
    UUID getId();
    String getTitle();
    String getArtist();
    String getKey();
    Integer getBpm();
    String getYouTubeUrl();
    String getSpotifyUrl();
    String getDeezerUrl();
    String getCoverUrl();
    String getNotes();
}
