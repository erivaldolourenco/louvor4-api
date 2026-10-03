package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectResponsibility;
import br.com.louvor4.api.models.ProjectResponsibilityAssignment;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityAssignmentRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityRepository;
import br.com.louvor4.api.services.ProjectResponsibilityService;
import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityDTO;
import br.com.louvor4.api.shared.dto.Responsibility.ProjectResponsibilityRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectResponsibilityServiceImpl implements ProjectResponsibilityService {

    private static final String DUPLICATE_NAME_MESSAGE = "Este projeto já tem uma responsabilidade com esse nome.";

    private final ProjectResponsibilityRepository responsibilityRepository;
    private final MusicProjectRepository musicProjectRepository;
    private final ProjectResponsibilityAssignmentRepository assignmentRepository;

    public ProjectResponsibilityServiceImpl(ProjectResponsibilityRepository responsibilityRepository,
                                            MusicProjectRepository musicProjectRepository,
                                            ProjectResponsibilityAssignmentRepository assignmentRepository) {
        this.responsibilityRepository = responsibilityRepository;
        this.musicProjectRepository = musicProjectRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponsibilityDTO> listByProject(UUID projectId) {
        // Uma consulta para todos os períodos do projeto, agrupados por responsabilidade
        Map<UUID, List<ProjectResponsibilityAssignment>> assignmentsByResponsibility =
                assignmentRepository.findAllByProjectId(projectId).stream()
                        .collect(Collectors.groupingBy(a -> a.getResponsibility().getId()));
        LocalDate today = LocalDate.now();

        return responsibilityRepository.findByMusicProject_IdOrderByPositionAscNameAsc(projectId).stream()
                .map(r -> toDto(r, ResponsibilityTimeline.of(assignmentsByResponsibility.get(r.getId()), today)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponsibilityDTO getById(UUID projectId, UUID responsibilityId) {
        return withTimeline(findOrThrow(projectId, responsibilityId));
    }

    @Override
    @Transactional
    public ProjectResponsibilityDTO create(UUID projectId, ProjectResponsibilityRequestDTO requestDto) {
        MusicProject project = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new ValidationException("Projeto não encontrado."));

        String name = requestDto.name().trim();
        if (responsibilityRepository.existsByMusicProject_IdAndNameIgnoreCase(projectId, name)) {
            throw new ValidationException(DUPLICATE_NAME_MESSAGE);
        }

        ProjectResponsibility responsibility = new ProjectResponsibility();
        responsibility.setName(name);
        responsibility.setDescription(normalizeDescription(requestDto.description()));
        responsibility.setMusicProject(project);
        // Novas responsabilidades entram no fim da lista do mural
        responsibility.setPosition(responsibilityRepository.findMaxPositionByProjectId(projectId) + 1);

        return ProjectResponsibilityDTO.fromEntity(responsibilityRepository.save(responsibility));
    }

    @Override
    @Transactional
    public ProjectResponsibilityDTO update(UUID projectId, UUID responsibilityId, ProjectResponsibilityRequestDTO requestDto) {
        ProjectResponsibility responsibility = findOrThrow(projectId, responsibilityId);

        String name = requestDto.name().trim();
        if (responsibilityRepository.existsByMusicProject_IdAndNameIgnoreCaseAndIdNot(projectId, name, responsibilityId)) {
            throw new ValidationException(DUPLICATE_NAME_MESSAGE);
        }

        responsibility.setName(name);
        responsibility.setDescription(normalizeDescription(requestDto.description()));
        return withTimeline(responsibilityRepository.save(responsibility));
    }

    @Override
    @Transactional
    public void delete(UUID projectId, UUID responsibilityId) {
        ProjectResponsibility responsibility = findOrThrow(projectId, responsibilityId);
        // Histórico sai junto com a responsabilidade
        assignmentRepository.deleteByResponsibilityId(responsibility.getId());
        responsibilityRepository.delete(responsibility);
    }

    private ProjectResponsibilityDTO withTimeline(ProjectResponsibility responsibility) {
        List<ProjectResponsibilityAssignment> history =
                assignmentRepository.findHistoryByResponsibilityId(responsibility.getId());
        return toDto(responsibility, ResponsibilityTimeline.of(history, LocalDate.now()));
    }

    private ProjectResponsibilityDTO toDto(ProjectResponsibility responsibility, ResponsibilityTimeline timeline) {
        return ProjectResponsibilityDTO.fromEntity(responsibility, timeline.current(), timeline.next(), timeline.last());
    }

    // Responsabilidade de outro projeto recebe a mesma mensagem de "não encontrada",
    // para não revelar que o id existe.
    private ProjectResponsibility findOrThrow(UUID projectId, UUID responsibilityId) {
        return responsibilityRepository.findByIdAndMusicProject_Id(responsibilityId, projectId)
                .orElseThrow(() -> new ValidationException("Responsabilidade não encontrada."));
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
