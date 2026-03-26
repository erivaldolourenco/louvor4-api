package br.com.louvor4.api.shared.dto.notification;

import java.util.List;

public record UserNotificationListResponse(
        List<UserNotificationItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
