package br.com.louvor4.api.services;
import br.com.louvor4.api.models.Ministry;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.shared.dto.MinistryCreateDTO;
import br.com.louvor4.api.shared.dto.MinistryUpdateDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public interface MinistryService {
    Ministry createMinistry(MinistryCreateDTO ministryDTO);

    Ministry updateMinistry(UUID ministryID, MinistryUpdateDTO ministryDTO, MultipartFile profileImage);

    List<Ministry> getMinistriesByUser(UUID id);

    boolean isUserMemberOfMinistry(UUID userId, UUID ministryId);

    boolean isUserMemberAdminOfMinistry(UUID userId, UUID ministryId);

    Ministry getMinistryById(UUID ministryId);

    void associateUsertoMinistry(User user, Ministry ministry);
}
