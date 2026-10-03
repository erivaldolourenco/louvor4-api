package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Responsibility.MyResponsibilityDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ResponsibilityAssignmentRequestDTO;

import java.util.List;
import java.util.UUID;

public interface ResponsibilityAssignmentService {
    List<ResponsibilityAssignmentDTO> listHistory(UUID projectId, UUID responsibilityId);
    ResponsibilityAssignmentDTO assign(UUID projectId, UUID responsibilityId, ResponsibilityAssignmentRequestDTO requestDto);
    ResponsibilityAssignmentDTO update(UUID projectId, UUID responsibilityId, UUID assignmentId, ResponsibilityAssignmentRequestDTO requestDto);
    void delete(UUID projectId, UUID responsibilityId, UUID assignmentId);
    List<MyResponsibilityDTO> listMine();
}
