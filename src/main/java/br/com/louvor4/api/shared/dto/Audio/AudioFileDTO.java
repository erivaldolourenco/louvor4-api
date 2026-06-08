package br.com.louvor4.api.shared.dto.Audio;

import br.com.louvor4.api.enums.AudioType;

import java.util.UUID;

public record AudioFileDTO(UUID songId, UUID medleyId, AudioType type, String audioUrl) {}
