package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.NotificationDeviceService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.shared.dto.notification.RegisterDeviceRequest;
import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("notifications")
public class NotificationController {

    private final NotificationDeviceService deviceService;
    private final PushSenderService senderService;

    public NotificationController(NotificationDeviceService deviceService, PushSenderService senderService) {
        this.deviceService = deviceService;
        this.senderService = senderService;
    }

    @PostMapping("/device-register")
    public ResponseEntity<Void> deviceRegister(@RequestBody @Valid RegisterDeviceRequest req) {
        deviceService.registerDevice(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test/{userId}")
    public ResponseEntity<String> sendTestNotification(@PathVariable UUID userId) throws FirebaseMessagingException {
        senderService.sendToUser(userId, "Vc foi adicionado ao evento", "voce foi adicionado");
        return ResponseEntity.ok("Notificação enviada");
    }


}
