package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Statistics.ProjectInviteStatsResponse;
import br.com.louvor4.api.shared.dto.Statistics.ProjectScheduleStatsResponse;

import java.util.UUID;

public interface ProjectStatisticsService {
    ProjectScheduleStatsResponse getScheduleStats(UUID projectId, String from, String to);
    ProjectInviteStatsResponse getInviteStats(UUID projectId, String from, String to);
}
