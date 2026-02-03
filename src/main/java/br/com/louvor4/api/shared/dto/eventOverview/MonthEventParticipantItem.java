package br.com.louvor4.api.shared.dto.eventOverview;

import java.util.UUID;

public record MonthEventParticipantItem(UUID memberId,
                                        String firstName,
                                        String lastName,
                                        String profileImage) {
}
