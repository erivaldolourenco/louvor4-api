package br.com.louvor4.api.shared.dto.Statistics;

import java.util.UUID;

public record SkillCountDTO(UUID skillId,
                            String skillName,
                            String skillIconKey,
                            long total) {
}
