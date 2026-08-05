package br.com.louvor4.api.services.external.music.deezer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeezerArtist(String name) {}
