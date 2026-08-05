package br.com.louvor4.api.services.external.music.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpotifyAlbum(String name, List<SpotifyImage> images) {}
