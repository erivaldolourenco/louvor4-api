package br.com.louvor4.api.shared.dto.Song;

import br.com.louvor4.api.enums.SongAudioType;

import java.util.UUID;

public record SongAudioDTO(UUID songId, SongAudioType type, String audioUrl) {}
