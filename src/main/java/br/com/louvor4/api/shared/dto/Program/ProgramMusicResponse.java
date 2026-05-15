package br.com.louvor4.api.shared.dto.Program;

import java.util.UUID;

public record ProgramMusicResponse(
        UUID id,
        String title,
        String artist
) {}
