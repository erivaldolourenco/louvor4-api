package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.UserUnavailability.CreateUserUnavailabilityRequest;
import br.com.louvor4.api.shared.dto.UserUnavailability.UserUnavailabilityResponse;

import java.util.List;
import java.util.UUID;

public interface UserUnavailabilityService {
    UserUnavailabilityResponse create(CreateUserUnavailabilityRequest request);
    List<UserUnavailabilityResponse> listFromCurrentUser();
    void deleteFromCurrentUser(UUID unavailabilityId);
}
