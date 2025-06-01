package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.Ministry;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.models.UserMinistry;
import br.com.louvor4.api.models.embeddedid.UserMinistryId;
import br.com.louvor4.api.repositories.MinistryRepository;
import br.com.louvor4.api.repositories.UserMinistryRepository;
import br.com.louvor4.api.services.MinistryService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MinistryCreateDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MinistryServiceImpl implements MinistryService {


    private final MinistryRepository ministryRepository;
    private final UserService userService;
    private final UserMinistryRepository userMinistryRepository;

    public MinistryServiceImpl(MinistryRepository ministryRepository,
                               UserService userService,
                           UserMinistryRepository userMinistryRepository) {
        this.ministryRepository = ministryRepository;
        this.userService = userService;
        this.userMinistryRepository = userMinistryRepository;
    }

    @Override
    public Ministry createMinistry(MinistryCreateDTO ministryDTO) {
        User userCreator = userService.getUserById(ministryDTO.creatorId());

        Ministry ministry = new Ministry();
        ministry.setName(ministryDTO.name());
        ministry.setDescription(ministryDTO.description());
        Ministry savedMinistry = ministryRepository.save(ministry);
        addUserToMinistry(savedMinistry, userCreator, "ADMIN");

        return savedMinistry;
    }

    @Override
    public List<Ministry> getMinistriesByUser(UUID id) {
        List<UserMinistry> userMinistries = userMinistryRepository.findById_UserId(id);
        return userMinistries.stream().map(UserMinistry::getMinistry).collect(Collectors.toList());
    }

    @Override
    public boolean isUserMemberOfMinistry(UUID userId, UUID ministryId) {
        return userMinistryRepository.existsById_UserIdAndId_MinistryId(userId,ministryId);
    }

    private void addUserToMinistry(Ministry ministry, User user, String role) {
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
