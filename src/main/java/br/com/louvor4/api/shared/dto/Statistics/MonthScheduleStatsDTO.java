package br.com.louvor4.api.shared.dto.Statistics;

import java.util.List;

public record MonthScheduleStatsDTO(String month,              // "2026-02"
                                    long total,
                                    List<SkillCountDTO> bySkill) {
}
