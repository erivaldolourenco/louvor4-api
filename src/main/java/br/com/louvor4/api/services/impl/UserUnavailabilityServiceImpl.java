package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.UserUnavailabilityMapper;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.models.UserUnavailability;
import br.com.louvor4.api.models.UserUnavailabilityProject;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.repositories.UserUnavailabilityRepository;
import br.com.louvor4.api.services.UserUnavailabilityService;
import br.com.louvor4.api.shared.dto.UserUnavailability.CreateUserUnavailabilityRequest;
import br.com.louvor4.api.shared.dto.UserUnavailability.UserUnavailabilityResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserUnavailabilityServiceImpl implements UserUnavailabilityService {

    private final UserUnavailabilityRepository userUnavailabilityRepository;
    private final MusicProjectRepository musicProjectRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserUnavailabilityMapper userUnavailabilityMapper;

    public UserUnavailabilityServiceImpl(
            UserUnavailabilityRepository userUnavailabilityRepository,
            MusicProjectRepository musicProjectRepository,
            MusicProjectMemberRepository musicProjectMemberRepository,
            CurrentUserProvider currentUserProvider,
            UserUnavailabilityMapper userUnavailabilityMapper
    ) {
        this.userUnavailabilityRepository = userUnavailabilityRepository;
        this.musicProjectRepository = musicProjectRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.userUnavailabilityMapper = userUnavailabilityMapper;
    }

    @Override
    @Transactional
    public UserUnavailabilityResponse create(CreateUserUnavailabilityRequest request) {
        validateDateRange(request);

        User currentUser = currentUserProvider.get();
        boolean appliesToAllProjects = Boolean.TRUE.equals(request.getAppliesToAllProjects());
        List<UUID> normalizedProjectIds = normalizeProjectIds(request.getProjectIds());

        if (!appliesToAllProjects && normalizedProjectIds.isEmpty()) {
            throw new ValidationException("Ao informar appliesToAllProjects como false, ao menos um projeto deve ser enviado.");
        }

        List<MusicProject> projects = appliesToAllProjects
                ? List.of()
                : validateAndLoadProjects(currentUser, normalizedProjectIds);

        UserUnavailability unavailability = buildUnavailability(request, currentUser, appliesToAllProjects);

        if (!appliesToAllProjects) {
            List<UserUnavailabilityProject> links = projects.stream()
                    .map(project -> buildProjectLink(unavailability, project))
                    .toList();
            unavailability.setProjects(new ArrayList<>(links));
        }

        UserUnavailability saved = userUnavailabilityRepository.save(unavailability);
        return userUnavailabilityMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserUnavailabilityResponse> listFromCurrentUser() {
        User currentUser = currentUserProvider.get();
        return userUnavailabilityRepository.findAllByUserId(currentUser.getId()).stream()
                .map(userUnavailabilityMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteFromCurrentUser(UUID unavailabilityId) {
        if (unavailabilityId == null) {
            throw new ValidationException("Id da indisponibilidade é obrigatório.");
        }

        User currentUser = currentUserProvider.get();
        UserUnavailability unavailability = userUnavailabilityRepository
                .findByIdAndUserId(unavailabilityId, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Indisponibilidade não encontrada."));

        userUnavailabilityRepository.delete(unavailability);
    }

    private void validateDateRange(CreateUserUnavailabilityRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ValidationException("startDate não pode ser maior que endDate.");
        }
    }

    private List<UUID> normalizeProjectIds(List<UUID> projectIds) {
        if (projectIds == null || projectIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> uniqueIds = new LinkedHashSet<>();
        for (UUID projectId : projectIds) {
            if (projectId == null) {
                throw new ValidationException("projectIds não pode conter valores nulos.");
            }
            uniqueIds.add(projectId);
        }
        return List.copyOf(uniqueIds);
    }

    private List<MusicProject> validateAndLoadProjects(User currentUser, List<UUID> projectIds) {
        List<MusicProject> projects = musicProjectRepository.findAllById(projectIds);
        if (projects.size() != projectIds.size()) {
            throw new ValidationException("Um ou mais projetos informados não existem.");
        }

        List<MusicProjectMember> memberships = musicProjectMemberRepository.getMusicProjectMembersByUser_Id(currentUser.getId());
        Set<UUID> allowedProjectIds = memberships.stream()
                .map(MusicProjectMember::getMusicProject)
                .map(MusicProject::getId)
                .collect(Collectors.toSet());

        boolean hasProjectOutsideUserContext = projectIds.stream()
                .anyMatch(projectId -> !allowedProjectIds.contains(projectId));
        if (hasProjectOutsideUserContext) {
            throw new ValidationException("Um ou mais projetos informados não pertencem ao contexto do usuário.");
        }

        return projectIds.stream()
                .map(indexById(projects))
                .toList();
    }

    private Function<UUID, MusicProject> indexById(List<MusicProject> projects) {
        var projectsById = projects.stream()
                .collect(Collectors.toMap(MusicProject::getId, Function.identity()));
        return projectsById::get;
    }

    private UserUnavailability buildUnavailability(
            CreateUserUnavailabilityRequest request,
            User currentUser,
            boolean appliesToAllProjects
    ) {
        UserUnavailability unavailability = new UserUnavailability();
        unavailability.setUser(currentUser);
        unavailability.setDescription(request.getDescription());
        unavailability.setStartDate(request.getStartDate());
        unavailability.setEndDate(request.getEndDate());
        unavailability.setAppliesToAllProjects(appliesToAllProjects);
        return unavailability;
    }

    private UserUnavailabilityProject buildProjectLink(UserUnavailability unavailability, MusicProject project) {
        UserUnavailabilityProject link = new UserUnavailabilityProject();
        link.setUnavailability(unavailability);
        link.setProject(project);
        return link;
    }
}
