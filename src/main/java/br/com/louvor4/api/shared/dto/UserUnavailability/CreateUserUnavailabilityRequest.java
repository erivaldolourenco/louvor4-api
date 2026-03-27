package br.com.louvor4.api.shared.dto.UserUnavailability;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreateUserUnavailabilityRequest {

    @Size(max = 500)
    private String description;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private Boolean appliesToAllProjects;

    private List<UUID> projectIds;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getAppliesToAllProjects() {
        return appliesToAllProjects;
    }

    public void setAppliesToAllProjects(Boolean appliesToAllProjects) {
        this.appliesToAllProjects = appliesToAllProjects;
    }

    public List<UUID> getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(List<UUID> projectIds) {
        this.projectIds = projectIds;
    }
}
