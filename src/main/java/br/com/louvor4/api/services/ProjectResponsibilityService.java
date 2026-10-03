package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityRequestDTO;

import java.util.List;
import java.util.UUID;

public interface ProjectResponsibilityService {
    List<ProjectResponsibilityDTO> listByProject(UUID projectId);
    ProjectResponsibilityDTO getById(UUID projectId, UUID responsibilityId);
    ProjectResponsibilityDTO create(UUID projectId, ProjectResponsibilityRequestDTO requestDto);
    ProjectResponsibilityDTO update(UUID projectId, UUID responsibilityId, ProjectResponsibilityRequestDTO requestDto);
    void delete(UUID projectId, UUID responsibilityId);
}
