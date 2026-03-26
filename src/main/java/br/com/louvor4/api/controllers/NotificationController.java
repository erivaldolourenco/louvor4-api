package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.NotificationDeviceService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.shared.dto.notification.RegisterDeviceRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("notifications")
public class NotificationController {

    private final NotificationDeviceService deviceService;

    public NotificationController(NotificationDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/device-register")
    public ResponseEntity<Void> deviceRegister(@RequestBody @Valid RegisterDeviceRequest req) {
        deviceService.registerDevice(req);
        return ResponseEntity.ok().build();
    }
}
