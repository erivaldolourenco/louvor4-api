package br.com.louvor4.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/push")
public class PushController {

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscribe(@RequestBody PushSubscriptionDTO dto) {
        System.out.println("✅ Subscription recebida: " + dto.endpoint());
        return ResponseEntity.ok().build();
    }

    public record PushSubscriptionDTO(
            String endpoint,
            Long expirationTime,
            Keys keys
    ) {
        public record Keys(String p256dh, String auth) {}
    }
}
