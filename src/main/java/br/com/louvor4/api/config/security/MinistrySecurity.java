package br.com.louvor4.api.config.security;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.services.MinistryService;
import br.com.louvor4.api.services.UserService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("ministrySecurity")
public class MinistrySecurity {

    private final UserService userService;
    private final MinistryService ministryService;

    public MinistrySecurity(UserService userService, MinistryService ministryService) {
        this.userService = userService;
        this.ministryService = ministryService;
    }

    public boolean isAdmin(String username, UUID ministryId) {
        User user = userService.findByUsername(username);
        return ministryService.isUserMemberAdminOfMinistry(user.getId(), ministryId);
    }

    public boolean isMember(String username, UUID ministryId) {
        User user = userService.findByUsername(username);
        return ministryService.isUserMemberOfMinistry(user.getId(), ministryId);
    }
}
