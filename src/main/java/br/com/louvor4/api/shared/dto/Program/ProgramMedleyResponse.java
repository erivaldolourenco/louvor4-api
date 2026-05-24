package br.com.louvor4.api.shared.dto.Program;

import java.util.List;
import java.util.UUID;

public record ProgramMedleyResponse(
        UUID id,
        String name,
        List<ProgramMusicResponse> songs
) {}
