package br.com.louvor4.api.services;
import br.com.louvor4.api.models.Ministry;
import br.com.louvor4.api.shared.dto.MinistryCreateDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface MinistryService {
    Ministry createMinistry(MinistryCreateDTO ministryDTO);

    List<Ministry> getMinistriesByUser(UUID id);

    boolean isUserMemberOfMinistry(UUID userId, UUID ministryId);

}
