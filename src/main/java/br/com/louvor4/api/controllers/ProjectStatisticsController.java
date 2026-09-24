package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ProjectStatisticsService;
import br.com.louvor4.api.shared.dto.Statistics.ProjectInviteStatsResponse;
import br.com.louvor4.api.shared.dto.Statistics.ProjectScheduleStatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("music-project/{projectId}/statistics")
public class ProjectStatisticsController {

    private final ProjectStatisticsService projectStatisticsService;

    public ProjectStatisticsController(ProjectStatisticsService projectStatisticsService) {
        this.projectStatisticsService = projectStatisticsService;
    }

    // Quantas vezes cada membro foi escalado (convite aceito) por mês, separado por função.
    @GetMapping("/schedules")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<ProjectScheduleStatsResponse> getScheduleStats(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(projectStatisticsService.getScheduleStats(projectId, from, to));
    }

    // Convites de participação em eventos: enviados, aceitos, recusados e pendentes.
    @GetMapping("/invites")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<ProjectInviteStatsResponse> getInviteStats(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return ResponseEntity.ok(projectStatisticsService.getInviteStats(projectId, from, to));
    }
}
