package br.com.louvor4.api.shared.dto.Audio;

import br.com.louvor4.api.enums.AudioType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AudioFileDTO(UUID songId, UUID medleyId, AudioType type, String audioUrl) {}
