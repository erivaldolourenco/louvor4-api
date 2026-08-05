package br.com.louvor4.api.services.external.music.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyTrack(
        String id,
        String name,
        List<SpotifyArtist> artists,
        SpotifyAlbum album,
        @JsonProperty("duration_ms") Long durationMs,
        @JsonProperty("preview_url") String previewUrl,
        @JsonProperty("external_ids") SpotifyExternalIds externalIds,
        @JsonProperty("external_urls") SpotifyExternalUrls externalUrls
) {}
