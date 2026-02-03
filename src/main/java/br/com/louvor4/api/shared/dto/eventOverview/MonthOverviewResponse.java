package br.com.louvor4.api.shared.dto.eventOverview;

import java.util.List;
import java.util.UUID;

public record MonthOverviewResponse(UUID projectId,
                                    String yearMonth,              // "2026-02"
                                    int eventsCount,
                                    List<MonthEventItem> events) {
}
