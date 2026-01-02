package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MusicProject.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static br.com.louvor4.api.shared.util.ObjectUtils.isNotNull;
import static br.com.louvor4.api.shared.util.ObjectUtils.isNotNullOrEmpty;

@Service
public class MusicProjectServiceImpl implements MusicProjectService {
    private final MusicProjectRepository musicProjectRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final StorageService storageService;
    private final UserService userService;


    public MusicProjectServiceImpl(MusicProjectRepository musicProjectRepository, MusicProjectMemberRepository musicProjectMemberRepository, CurrentUserProvider currentUserProvider, StorageService storageService, UserService userService) {
        this.musicProjectRepository = musicProjectRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.storageService = storageService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public MusicProjectDetailDTO
    create(MusicProjectCreateDTO dto) {
        User creator = currentUserProvider.get();

        MusicProject project = buildProject(dto, creator);
        project = musicProjectRepository.save(project);

        addOwnerMember(project, creator);

        return toDetailDto(project);
    }

    @Override
    public MusicProjectDetailDTO update(UUID projectId, MusicProjectDTO updateDTO) {
        MusicProject musicProject = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("MusicProject não encontrado: " + projectId));

        if (isNotNull(updateDTO.getName())) {
            musicProject.setName(updateDTO.getName().trim());
        }
        if (isNotNull(updateDTO.getType())) {
            musicProject.setType(updateDTO.getType());
        }

        MusicProject saved = musicProjectRepository.save(musicProject);

        return toDetailDto(saved);
    }

    @Override
    public String updateImage(UUID projecId, MultipartFile profileImage){
        MusicProject musicProject = musicProjectRepository.findById(projecId)
                .orElseThrow(() -> new EntityNotFoundException("MusicProject não encontrado: " + projecId));
        if (isNotNullOrEmpty(profileImage)) {
            String fileUrl = storageService.uploadFile(profileImage, FileCategory.PROJECT_PROFILE);
            musicProject.setProfileImage(fileUrl);
        }
        MusicProject saved = musicProjectRepository.save(musicProject);

        return saved.getProfileImage();
    }


    @Override
    public MusicProjectDetailDTO getMusicProjectById(UUID projectId) {
        MusicProject musicProject =  musicProjectRepository.getMusicProjectById(projectId);
        return toDetailDto(musicProject);
    }

    @Override
    public List<MusicProjectDTO> getMusicProjectFromUser() {

        List<MusicProjectMember> musicProjectMember = musicProjectMemberRepository.getMusicProjectMembersByUser_Id(currentUserProvider.get().getId());

        return musicProjectMember.stream()
                .map(MusicProjectMember::getMusicProject)
                .distinct()
                .map(project -> {
                    MusicProjectDTO dto = new MusicProjectDTO();
                    dto.setId(project.getId());
                    dto.setName(project.getName());
                    dto.setType(project.getType());
                    dto.setProfileImage(project.getProfileImage());
                    return dto;
                })
                .toList();
    }

    @Override
    public void addMember(UUID projectId, AddMemberDTO addDto) {

        validIfUserExist(addDto.getUserId());

        validIfMemberExistInProject(projectId, addDto.getUserId());

        User creator = currentUserProvider.get();
        User userMember = userService.getUserById(addDto.getUserId());
        MusicProject musicProject = musicProjectRepository.getMusicProjectById(projectId);
        MusicProjectMember musicProjectMember = new MusicProjectMember();
        musicProjectMember.setUser(userMember);
        musicProjectMember.setMusicProject(musicProject);
        musicProjectMember.setAddedByUserId(creator.getId());
        musicProjectMember.setRole(ProjectMemberRole.MEMBER);

        musicProjectMemberRepository.save(musicProjectMember);
    }


    private MusicProject buildProject(MusicProjectCreateDTO dto, User creator) {
        MusicProject project = new MusicProject();
        project.setName(dto.getName());
        project.setType(dto.getType());
        project.setCreatedByUserId(creator.getId());
        project.setCreatedAt(LocalDateTime.now());

        return project;
    }

    private void addOwnerMember(MusicProject project, User creator) {


        validIfMemberExistInProject(project.getId(), creator.getId());

        MusicProjectMember ownerMember = new MusicProjectMember();
        ownerMember.setMusicProject(project);
        ownerMember.setUser(creator);
        ownerMember.setAddedByUserId(creator.getId());
        ownerMember.setRole(ProjectMemberRole.OWNER);
        ownerMember.setCreatedAt(LocalDateTime.now());

        musicProjectMemberRepository.save(ownerMember);

    }

    private void validIfMemberExistInProject(UUID projectId, UUID userId){
        boolean alreadyMember = musicProjectMemberRepository
                .existsByMusicProject_IdAndUser_Id(projectId, userId);
        if (alreadyMember) {
            throw new ValidationException("Usuário já é membro deste projeto.");
        }
    }

    private void validIfUserExist(UUID userId) {
        boolean exists = userService.existsById(userId);
        if (!exists) {
            throw new ValidationException("Usuário não encontrado.");
        }
    }

    private MusicProjectDetailDTO toDetailDto(MusicProject project) {
        MusicProjectDetailDTO out = new MusicProjectDetailDTO();
        out.setId(project.getId());
        out.setName(project.getName());
        out.setType(project.getType());
        out.setProfileImage(project.getProfileImage());

        List<MemberDTO> members = project.getMembers()
                .stream()
                .map(member -> {
                    User user = member.getUser();

                    MemberDTO dto = new MemberDTO();
                    dto.setId(user.getId());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setProfileImage(user.getProfileImage());
                    dto.setRole(member.getRole());

                    return dto;
                })
                .toList();

        out.setMembers(members);
        return out;
    }

}
