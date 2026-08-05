package br.com.louvor4.api.services.external.music.deezer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeezerSearchResponse(List<DeezerTrack> data) {}
