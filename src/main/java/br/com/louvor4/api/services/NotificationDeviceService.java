package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.notification.RegisterDeviceRequest;
import jakarta.validation.Valid;

public interface NotificationDeviceService {
    void registerDevice(@Valid RegisterDeviceRequest req);
}
