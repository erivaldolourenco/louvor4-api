package br.com.louvor4.api.shared.dto.Statistics;

import java.util.List;
import java.util.UUID;

public record ProjectScheduleStatsResponse(UUID projectId,
                                           String from,             // "2026-01"
                                           String to,               // "2026-06"
                                           List<MemberScheduleStatsDTO> members) {
}
