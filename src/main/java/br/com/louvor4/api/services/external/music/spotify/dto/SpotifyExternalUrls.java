package br.com.louvor4.api.services.external.music.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyExternalUrls(String spotify) {}
