package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.UserUnavailability.CreateUserUnavailabilityRequest;
import br.com.louvor4.api.shared.dto.UserUnavailability.UserUnavailabilityResponse;

public interface UserUnavailabilityService {
    UserUnavailabilityResponse create(CreateUserUnavailabilityRequest request);
}
