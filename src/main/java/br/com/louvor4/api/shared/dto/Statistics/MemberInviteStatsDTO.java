package br.com.louvor4.api.shared.dto.Statistics;

import java.util.UUID;

public record MemberInviteStatsDTO(UUID memberId,
                                   UUID userId,
                                   String firstName,
                                   String lastName,
                                   String profileImage,
                                   InviteCountsDTO counts) {
}
