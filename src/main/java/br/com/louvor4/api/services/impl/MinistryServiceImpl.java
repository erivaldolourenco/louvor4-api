package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.enums.UserRole;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.Ministry;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.models.UserMinistry;
import br.com.louvor4.api.models.embeddedid.UserMinistryId;
import br.com.louvor4.api.repositories.MinistryRepository;
import br.com.louvor4.api.repositories.UserMinistryRepository;
import br.com.louvor4.api.services.MinistryService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MinistryCreateDTO;
import br.com.louvor4.api.shared.dto.MinistryUpdateDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static br.com.louvor4.api.shared.util.ObjectUtils.isNotNullOrEmpty;

@Service
public class MinistryServiceImpl implements MinistryService {


    private final MinistryRepository ministryRepository;
    private final UserService userService;
    private final UserMinistryRepository userMinistryRepository;
    private final StorageService storageService;

    public MinistryServiceImpl(MinistryRepository ministryRepository,
                               UserService userService,
                               UserMinistryRepository userMinistryRepository, StorageService storageService) {
        this.ministryRepository = ministryRepository;
        this.userService = userService;
        this.userMinistryRepository = userMinistryRepository;

        this.storageService = storageService;
    }

    @Override
    public Ministry createMinistry(MinistryCreateDTO ministryDTO) {
        User userCreator = userService.getUserById(ministryDTO.creatorId());

        Ministry ministry = new Ministry();
        ministry.setName(ministryDTO.name());
        ministry.setDescription(ministryDTO.description());
        Ministry savedMinistry = ministryRepository.save(ministry);
        addUserToMinistry(savedMinistry, userCreator, UserRole.ADMIN);

        return savedMinistry;
    }

    @Override
    public Ministry updateMinistry(UUID ministryId, MinistryUpdateDTO ministryDTO, MultipartFile profileImage) {
        Ministry ministry = getMinistryById(ministryId);
        ministry.setDescription(ministryDTO.description());
        ministry.setName(ministryDTO.name());

        if (isNotNullOrEmpty(profileImage)) {
            String fileUrl = storageService.uploadFile(profileImage, FileCategory.MINISTRY_PROFILE);
            ministry.setProfileImage(fileUrl);
        }

        ministry = ministryRepository.save(ministry);
        return ministry;
    }

    @Override
    public List<Ministry> getMinistriesByUser(UUID id) {
        List<UserMinistry> userMinistries = userMinistryRepository.findById_UserId(id);
        return userMinistries.stream().map(UserMinistry::getMinistry).collect(Collectors.toList());
    }

    @Override
    public boolean isUserMemberOfMinistry(UUID userId, UUID ministryId) {
        return userMinistryRepository.existsById_UserIdAndId_MinistryId(userId, ministryId);
    }

    @Override
    public boolean isUserMemberAdminOfMinistry(UUID userId, UUID ministryId) {
        return userMinistryRepository.existsById_UserIdAndId_MinistryIdAndRole(userId, ministryId, UserRole.ADMIN);
    }

    @Override
    public Ministry getMinistryById(UUID ministryId) {
        return ministryRepository.findById(ministryId).orElseThrow(() -> new NotFoundException("Ministério não Encontrado"));
    }

    @Override
    public void associateUsertoMinistry(User user, Ministry ministry) {

        boolean isMember = userMinistryRepository.existsById(
                new UserMinistryId(user.getId(), ministry.getId())
        );

        if (isMember) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já está no ministério.");
        }

        UserMinistry associated = new UserMinistry();
        associated.setId(new UserMinistryId(user.getId(), ministry.getId()));
        associated.setUser(user);
        associated.setMinistry(ministry);
        associated.setRole(UserRole.MEMBER);

        userMinistryRepository.save(associated);
    }

    private void addUserToMinistry(Ministry ministry, User user, UserRole role) {
        UserMinistryId userMinistryId = new UserMinistryId();
        userMinistryId.setMinistryId(ministry.getId());
        userMinistryId.setUserId(user.getId());

        UserMinistry userMinistry = new UserMinistry();
        userMinistry.setId(userMinistryId);
        userMinistry.setUser(user);
        userMinistry.setMinistry(ministry);
        userMinistry.setRole(role);
        userMinistryRepository.save(userMinistry);

    }

}
