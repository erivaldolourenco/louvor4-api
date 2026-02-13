package br.com.louvor4.api.shared.dto.notification;

import br.com.louvor4.api.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
        @NotBlank(message = "fcmToken é obrigatório")
        @Size(max = 512)
        String fcmToken,

        @NotNull(message = "platform é obrigatório")
        Platform platform,

        @Size(max = 150)
        String deviceId

) {
}
