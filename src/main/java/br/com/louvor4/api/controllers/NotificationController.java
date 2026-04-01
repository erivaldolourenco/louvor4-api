package br.com.louvor4.api.controllers;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.services.NotificationDeviceService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.notification.RegisterDeviceRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("notifications")
public class NotificationController {

    private final NotificationDeviceService deviceService;
    private final UserNotificationService userNotificationService;
    private final CurrentUserProvider currentUserProvider;

    public NotificationController(
            NotificationDeviceService deviceService,
            UserNotificationService userNotificationService,
            CurrentUserProvider currentUserProvider
    ) {
        this.deviceService = deviceService;
        this.userNotificationService = userNotificationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/device-register")
    public ResponseEntity<Void> deviceRegister(@RequestBody @Valid RegisterDeviceRequest req) {
        deviceService.registerDevice(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{notificationId}/read")
    public ResponseEntity<Void> notificationRead(@PathVariable UUID notificationId){
        var user = currentUserProvider.get();
        userNotificationService.markAsRead(user.getId(), notificationId);
        return ResponseEntity.noContent().build();
    }
}
