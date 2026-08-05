package br.com.louvor4.api.services.external.music.deezer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeezerTrack(
        Long id,
        String title,
        String link,
        Long duration,
        String preview,
        String isrc,
        DeezerArtist artist,
        DeezerAlbum album
) {}
