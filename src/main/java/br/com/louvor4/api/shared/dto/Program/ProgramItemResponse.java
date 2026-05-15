package br.com.louvor4.api.shared.dto.Program;

import br.com.louvor4.api.enums.ProgramItemType;

import java.util.UUID;

public record ProgramItemResponse(
        UUID id,
        ProgramItemType type,
        Integer position,
        String title,
        String description,
        ProgramMusicResponse music
) {}
