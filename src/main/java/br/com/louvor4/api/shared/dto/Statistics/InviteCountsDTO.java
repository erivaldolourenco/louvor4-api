package br.com.louvor4.api.shared.dto.Statistics;

public record InviteCountsDTO(long sent,
                              long accepted,
                              long declined,
                              long pending,
                              double acceptanceRate) {  // accepted / sent, 0..1
}
