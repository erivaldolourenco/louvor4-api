package br.com.louvor4.api.shared.dto.Statistics;

import java.util.List;
import java.util.UUID;

public record MemberScheduleStatsDTO(UUID memberId,
                                     UUID userId,
                                     String firstName,
                                     String lastName,
                                     String profileImage,
                                     long total,
                                     List<SkillCountDTO> bySkill,
                                     List<MonthScheduleStatsDTO> byMonth) {
}
