package br.com.louvor4.api.shared.dto.Statistics;

import java.util.List;
import java.util.UUID;

public record ProjectInviteStatsResponse(UUID projectId,
                                         String from,
                                         String to,
                                         InviteCountsDTO totals,
                                         List<MonthInviteStatsDTO> byMonth,
                                         List<MemberInviteStatsDTO> byMember) {
}
